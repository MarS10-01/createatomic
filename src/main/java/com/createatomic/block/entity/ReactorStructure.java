package com.createatomic.block.entity;

import java.util.HashSet;
import java.util.Set;

import com.createatomic.block.ReactorCoreBlock;
import com.createatomic.registry.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RotatedPillarBlock;
import net.minecraft.world.level.material.FluidTags;
import net.minecraft.world.phys.Vec3;

/**
 * Multiblock scan for the accessible reactor chamber.
 *
 * The controller stays in one of the four vertical walls. The top of the reactor is intentionally OPEN,
 * so the player can climb into the vessel while building and maintaining it.
 *
 * Internal dimensions are 9..15 blocks wide/deep/high. The floor and four side walls are Reactor Casing.
 * The interior may contain air, water, fuel channels, control rods and graphite moderators.
 */
public final class ReactorStructure {
    public static final int MIN_DIM = 9;
    public static final int MAX_DIM = 15;
    private static final int SEARCH_LIMIT = MAX_DIM + 2;

    public boolean valid;
    public String error = "err_interior";
    public Object[] errorArgs = new Object[0];

    public int fuel;
    public int control;
    public int graphite;
    public int cells;
    public int width;
    public int height;
    public int depth;
    public boolean rbmk;
    public BlockPos min = BlockPos.ZERO;
    public BlockPos max = BlockPos.ZERO;
    public Vec3 center = Vec3.ZERO;

    private boolean aborted;
    private Direction inward = Direction.NORTH;

    public static boolean isInterior(BlockState state) {
        return state.isAir()
                || state.getFluidState().is(FluidTags.WATER)
                || state.is(ModBlocks.FUEL_CHANNEL.get())
                || state.is(ModBlocks.CONTROL_ROD.get())
                || state.is(ModBlocks.GRAPHITE_BLOCK.get());
    }

    public static boolean isBoundary(BlockState state) {
        return state.is(ModBlocks.REACTOR_CASING.get()) || state.getBlock() instanceof ReactorCoreBlock;
    }

    public boolean containsInterior(BlockPos pos) {
        return valid && pos.getX() >= min.getX() && pos.getX() <= max.getX()
                && pos.getY() >= min.getY() && pos.getY() <= max.getY()
                && pos.getZ() >= min.getZ() && pos.getZ() <= max.getZ();
    }

    public boolean containsStructureBlock(BlockPos pos) {
        if (!valid) {
            return false;
        }
        boolean inX = pos.getX() >= min.getX() - 1 && pos.getX() <= max.getX() + 1;
        boolean inY = pos.getY() >= min.getY() - 1 && pos.getY() <= max.getY();
        boolean inZ = pos.getZ() >= min.getZ() - 1 && pos.getZ() <= max.getZ() + 1;
        if (!(inX && inY && inZ)) {
            return false;
        }
        return pos.getX() == min.getX() - 1 || pos.getX() == max.getX() + 1
                || pos.getZ() == min.getZ() - 1 || pos.getZ() == max.getZ() + 1
                || pos.getY() == min.getY() - 1;
    }

    public Direction inward() {
        return inward;
    }

    public static ReactorStructure scan(Level level, BlockPos controller) {
        BlockState controllerState = level.getBlockState(controller);
        if (!(controllerState.getBlock() instanceof ReactorCoreBlock)) {
            return new ReactorStructure();
        }
        Direction.Axis axis = controllerState.getValue(RotatedPillarBlock.AXIS);
        if (axis == Direction.Axis.Y) {
            ReactorStructure result = new ReactorStructure();
            result.fail("err_horizontal");
            return result;
        }

        ReactorStructure best = null;
        for (Direction direction : Direction.values()) {
            if (direction.getAxis() != axis) {
                continue;
            }
            BlockPos first = controller.relative(direction);
            if (!level.isLoaded(first)) {
                continue;
            }
            ReactorStructure attempt = scanCandidate(level, controller, direction);
            if (attempt.valid) {
                return attempt;
            }
            if (best == null
                    || (best.aborted && !attempt.aborted)
                    || (best.aborted == attempt.aborted && attempt.cells > best.cells)) {
                best = attempt;
            }
        }
        return best != null ? best : new ReactorStructure();
    }

    /** Find a valid controller for an interior component such as a fuel channel. */
    public static BlockPos findController(Level level, BlockPos component) {
        int radius = MAX_DIM + 3;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (dx * dx + dy * dy + dz * dz > radius * radius) {
                        continue;
                    }
                    cursor.set(component.getX() + dx, component.getY() + dy, component.getZ() + dz);
                    if (!level.isLoaded(cursor) || !(level.getBlockState(cursor).getBlock() instanceof ReactorCoreBlock)) {
                        continue;
                    }
                    ReactorStructure structure = scan(level, cursor);
                    if (structure.valid && structure.containsStructureBlock(component)) {
                        return cursor.immutable();
                    }
                }
            }
        }
        return null;
    }

    private void fail(String key, Object... args) {
        this.valid = false;
        this.error = key;
        this.errorArgs = args;
    }

    private static ReactorStructure scanCandidate(Level level, BlockPos controller, Direction inward) {
        ReactorStructure result = new ReactorStructure();
        result.inward = inward;
        BlockPos start = controller.relative(inward);
        if (!isInterior(level.getBlockState(start))) {
            result.fail("err_interior");
            return result;
        }

        Direction u = inward.getClockWise();

        // Determine chamber width from the controller row.
        int minU = 0;
        while (minU > -MAX_DIM && isInterior(level.getBlockState(offset(controller, inward, 1, u, minU - 1, 0)))) {
            minU--;
        }
        BlockPos leftWall = offset(controller, inward, 1, u, minU - 1, 0);
        if (!isBoundary(level.getBlockState(leftWall))) {
            result.fail("err_wall", leftWall.getX(), leftWall.getY(), leftWall.getZ());
            return result;
        }

        int maxU = 0;
        while (maxU < MAX_DIM && isInterior(level.getBlockState(offset(controller, inward, 1, u, maxU + 1, 0)))) {
            maxU++;
        }
        BlockPos rightWall = offset(controller, inward, 1, u, maxU + 1, 0);
        if (!isBoundary(level.getBlockState(rightWall))) {
            result.fail("err_wall", rightWall.getX(), rightWall.getY(), rightWall.getZ());
            return result;
        }

        int width = maxU - minU + 1;
        if (width < MIN_DIM) {
            result.fail("err_small");
            return result;
        }
        if (width > MAX_DIM) {
            result.fail("err_large");
            return result;
        }

        // Determine chamber depth along the controller axis.
        int depth = 0;
        while (depth < MAX_DIM) {
            BlockPos p = offset(controller, inward, depth + 1, u, 0, 0);
            if (isInterior(level.getBlockState(p))) {
                depth++;
                continue;
            }
            if (!isBoundary(level.getBlockState(p))) {
                result.fail("err_foreign", p.getX(), p.getY(), p.getZ());
                return result;
            }
            break;
        }
        BlockPos farWall = offset(controller, inward, depth + 1, u, 0, 0);
        BlockState farState = level.getBlockState(farWall);
        if (!isBoundary(farState)) {
            if (isInterior(farState)) {
                result.aborted = true;
                result.fail("err_large");
            } else {
                result.fail("err_wall", farWall.getX(), farWall.getY(), farWall.getZ());
            }
            return result;
        }
        if (depth < MIN_DIM) {
            result.fail("err_small");
            return result;
        }

        // Find the floor below the seed row.
        int floorDistance = 0;
        while (floorDistance < SEARCH_LIMIT) {
            BlockPos p = offset(controller, inward, 1, u, 0, -(floorDistance + 1));
            if (isInterior(level.getBlockState(p))) {
                floorDistance++;
                continue;
            }
            if (!isBoundary(level.getBlockState(p))) {
                result.fail("err_foreign", p.getX(), p.getY(), p.getZ());
                return result;
            }
            break;
        }
        int floorY = start.getY() - floorDistance - 1;

        // Find the open top by following the four side walls upward. The roof is NOT required.
        int chamberHeight = 0;
        while (chamberHeight < MAX_DIM) {
            int y = floorY + 1 + chamberHeight;
            boolean perimeterOk = true;
            // Front and back walls.
            for (int du = minU; du <= maxU && perimeterOk; du++) {
                BlockPos front = offsetAtY(controller, inward, u, 0, du, y);
                BlockPos back = offsetAtY(controller, inward, depth + 1, du, 0, y);
                perimeterOk &= isBoundary(level.getBlockState(front));
                perimeterOk &= isBoundary(level.getBlockState(back));
            }
            // Left and right walls.
            for (int dn = 1; dn <= depth && perimeterOk; dn++) {
                BlockPos left = offsetAtY(controller, inward, u, dn, minU, y);
                BlockPos right = offsetAtY(controller, inward, u, dn, maxU, y);
                perimeterOk &= isBoundary(level.getBlockState(left));
                perimeterOk &= isBoundary(level.getBlockState(right));
            }
            if (!perimeterOk) {
                break;
            }
            chamberHeight++;
        }

        if (chamberHeight < MIN_DIM) {
            result.fail("err_small");
            return result;
        }
        if (chamberHeight > MAX_DIM) {
            result.fail("err_large");
            return result;
        }
        if (floorY + chamberHeight < start.getY()) {
            result.fail("err_height");
            return result;
        }

        BlockPos topEntrance = offsetAtY(controller, inward, Math.max(1, depth / 2 + 1), u, 0, floorY + chamberHeight + 1);
        if (!level.getBlockState(topEntrance).isAir()) {
            result.fail("err_roof");
            return result;
        }

        // Validate floor and every interior cell of the discovered prism.
        for (int du = minU; du <= maxU; du++) {
            for (int dn = 0; dn <= depth + 1; dn++) {
                BlockPos p = offsetAtY(controller, inward, u, dn, du, floorY);
                if (!isBoundary(level.getBlockState(p))) {
                    result.fail("err_floor", p.getX(), p.getY(), p.getZ());
                    return result;
                }
            }
        }

        int fuel = 0;
        int control = 0;
        int graphite = 0;
        int cells = 0;
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;

        Set<BlockPos> visited = new HashSet<>();

        for (int y = floorY + 1; y <= floorY + chamberHeight; y++) {
            for (int dn = 1; dn <= depth; dn++) {
                for (int du = minU; du <= maxU; du++) {
                    BlockPos p = offsetAtY(controller, inward, u, dn, du, y);
                    BlockState state = level.getBlockState(p);
                    if (!isInterior(state)) {
                        result.fail("err_foreign", p.getX(), p.getY(), p.getZ());
                        return result;
                    }
                    visited.add(p.immutable());
                    cells++;
                    minX = Math.min(minX, p.getX());
                    minY = Math.min(minY, p.getY());
                    minZ = Math.min(minZ, p.getZ());
                    maxX = Math.max(maxX, p.getX());
                    maxY = Math.max(maxY, p.getY());
                    maxZ = Math.max(maxZ, p.getZ());
                    if (state.is(ModBlocks.FUEL_CHANNEL.get())) {
                        fuel++;
                    } else if (state.is(ModBlocks.CONTROL_ROD.get())) {
                        control++;
                    } else if (state.is(ModBlocks.GRAPHITE_BLOCK.get())) {
                        graphite++;
                    }
                }
            }
        }

        if (cells != width * depth * chamberHeight || visited.size() != cells) {
            result.fail("err_shape");
            return result;
        }
        if (fuel == 0) {
            result.fail("err_fuel");
            return result;
        }
        if (control < (fuel + 3) / 4) {
            result.fail("err_control", (fuel + 3) / 4, control);
            return result;
        }
        if (graphite > 0 && graphite < fuel) {
            result.fail("err_graphite", fuel, graphite);
            return result;
        }

        result.valid = true;
        result.rbmk = graphite > 0;
        result.fuel = fuel;
        result.control = control;
        result.graphite = graphite;
        result.cells = cells;
        result.width = width;
        result.height = chamberHeight;
        result.depth = depth;
        result.min = new BlockPos(minX, minY, minZ);
        result.max = new BlockPos(maxX, maxY, maxZ);
        result.center = new Vec3((minX + maxX + 1) / 2.0, (minY + maxY + 1) / 2.0,
                (minZ + maxZ + 1) / 2.0);
        return result;
    }

    private static BlockPos offset(BlockPos controller, Direction inward, int depth, Direction u, int du, int dy) {
        return controller.relative(inward, depth).relative(u, du).relative(Direction.DOWN, -dy);
    }

    private static BlockPos offsetAtY(BlockPos controller, Direction inward, Direction u, int depth, int du, int y) {
        BlockPos p = controller.relative(inward, depth).relative(u, du);
        return new BlockPos(p.getX(), y, p.getZ());
    }

    private ReactorStructure() {
    }
}
