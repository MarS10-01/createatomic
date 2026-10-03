package com.createatomic.block.entity;

import java.util.HashSet;
import java.util.Set;

import com.createatomic.block.ReactorCoreBlock;
import com.createatomic.registry.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Dense, enclosed reactor vessel. The chamber is fully roofed; one heavy service hatch provides the
 * only normal maintenance entrance. Exterior fittings make the roof/walls look and function like a
 * real industrial reactor instead of a hollow box.
 */
public final class ReactorStructure {
    public static final int MIN_DIM = 9;
    public static final int MAX_DIM = 13;
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
    public int coolantPorts;
    public int steamOutlets;
    public int controlDrives;
    public int hatches;
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
        return state.is(ModBlocks.REACTOR_CASING.get())
                || state.is(ModBlocks.REACTOR_HATCH.get())
                || state.is(ModBlocks.COOLANT_MANIFOLD.get())
                || state.is(ModBlocks.STEAM_OUTLET.get())
                || state.is(ModBlocks.CONTROL_ROD_DRIVE.get())
                || state.getBlock() instanceof ReactorCoreBlock;
    }

    public boolean containsInterior(BlockPos pos) {
        return valid && pos.getX() >= min.getX() && pos.getX() <= max.getX()
                && pos.getY() >= min.getY() && pos.getY() <= max.getY()
                && pos.getZ() >= min.getZ() && pos.getZ() <= max.getZ();
    }

    public boolean containsStructureBlock(BlockPos pos) {
        if (!valid) return false;
        int minX = Math.min(min.getX(), max.getX()) - 1;
        int maxX = Math.max(min.getX(), max.getX()) + 1;
        int minY = min.getY() - 1;
        int maxY = max.getY() + 1;
        int minZ = Math.min(min.getZ(), max.getZ()) - 1;
        int maxZ = Math.max(min.getZ(), max.getZ()) + 1;
        if (pos.getX() < minX || pos.getX() > maxX || pos.getY() < minY || pos.getY() > maxY
                || pos.getZ() < minZ || pos.getZ() > maxZ) return false;
        return pos.getX() == minX || pos.getX() == maxX || pos.getY() == minY || pos.getY() == maxY
                || pos.getZ() == minZ || pos.getZ() == maxZ;
    }

    public Direction inward() {
        return inward;
    }

    public static ReactorStructure scan(Level level, BlockPos controller) {
        BlockState controllerState = level.getBlockState(controller);
        if (!(controllerState.getBlock() instanceof ReactorCoreBlock)) return new ReactorStructure();
        Direction.Axis axis = controllerState.getValue(RotatedPillarBlock.AXIS);
        if (axis == Direction.Axis.Y) {
            ReactorStructure result = new ReactorStructure();
            result.fail("err_horizontal");
            return result;
        }

        ReactorStructure best = null;
        for (Direction direction : Direction.values()) {
            if (direction.getAxis() != axis) continue;
            if (!level.isLoaded(controller.relative(direction))) continue;
            ReactorStructure attempt = scanCandidate(level, controller, direction);
            if (attempt.valid) return attempt;
            if (best == null || (best.aborted && !attempt.aborted)
                    || (best.aborted == attempt.aborted && attempt.cells > best.cells)) best = attempt;
        }
        return best != null ? best : new ReactorStructure();
    }

    public static BlockPos findController(Level level, BlockPos component) {
        int radius = MAX_DIM + 4;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (dx * dx + dy * dy + dz * dz > radius * radius) continue;
                    cursor.set(component.getX() + dx, component.getY() + dy, component.getZ() + dz);
                    if (!level.isLoaded(cursor) || !(level.getBlockState(cursor).getBlock() instanceof ReactorCoreBlock)) continue;
                    ReactorStructure structure = scan(level, cursor);
                    if (structure.valid && structure.containsStructureBlock(component)) return cursor.immutable();
                }
            }
        }
        return null;
    }

    private void fail(String key, Object... args) {
        valid = false;
        error = key;
        errorArgs = args;
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

        int minU = 0;
        while (minU > -MAX_DIM && isInterior(level.getBlockState(offset(controller, inward, 1, u, minU - 1, 0)))) minU--;
        BlockPos leftWall = offset(controller, inward, 1, u, minU - 1, 0);
        if (!isBoundary(level.getBlockState(leftWall))) {
            result.fail("err_wall", leftWall.getX(), leftWall.getY(), leftWall.getZ());
            return result;
        }

        int maxU = 0;
        while (maxU < MAX_DIM && isInterior(level.getBlockState(offset(controller, inward, 1, u, maxU + 1, 0)))) maxU++;
        BlockPos rightWall = offset(controller, inward, 1, u, maxU + 1, 0);
        if (!isBoundary(level.getBlockState(rightWall))) {
            result.fail("err_wall", rightWall.getX(), rightWall.getY(), rightWall.getZ());
            return result;
        }

        int width = maxU - minU + 1;
        if (width < MIN_DIM) { result.fail("err_small"); return result; }
        if (width > MAX_DIM) { result.fail("err_large"); return result; }

        int depth = 0;
        while (depth < MAX_DIM) {
            BlockPos p = offset(controller, inward, depth + 1, u, 0, 0);
            if (isInterior(level.getBlockState(p))) { depth++; continue; }
            if (!isBoundary(level.getBlockState(p))) {
                result.fail("err_foreign", p.getX(), p.getY(), p.getZ());
                return result;
            }
            break;
        }
        BlockPos farWall = offset(controller, inward, depth + 1, u, 0, 0);
        if (!isBoundary(level.getBlockState(farWall))) {
            result.fail("err_wall", farWall.getX(), farWall.getY(), farWall.getZ());
            return result;
        }
        if (depth < MIN_DIM) { result.fail("err_small"); return result; }

        int floorDistance = 0;
        while (floorDistance < SEARCH_LIMIT) {
            BlockPos p = offset(controller, inward, 1, u, 0, -(floorDistance + 1));
            if (isInterior(level.getBlockState(p))) { floorDistance++; continue; }
            if (!isBoundary(level.getBlockState(p))) {
                result.fail("err_foreign", p.getX(), p.getY(), p.getZ());
                return result;
            }
            break;
        }
        int floorY = start.getY() - floorDistance - 1;

        int chamberHeight = 0;
        while (chamberHeight < MAX_DIM) {
            int y = floorY + 1 + chamberHeight;
            boolean perimeterOk = true;
            for (int du = minU; du <= maxU && perimeterOk; du++) {
                perimeterOk &= isBoundary(level.getBlockState(offsetAtY(controller, inward, u, 0, du, y)));
                perimeterOk &= isBoundary(level.getBlockState(offsetAtY(controller, inward, u, depth + 1, du, y)));
            }
            for (int dn = 1; dn <= depth && perimeterOk; dn++) {
                perimeterOk &= isBoundary(level.getBlockState(offsetAtY(controller, inward, u, dn, minU, y)));
                perimeterOk &= isBoundary(level.getBlockState(offsetAtY(controller, inward, u, dn, maxU, y)));
            }
            if (!perimeterOk) break;
            chamberHeight++;
        }
        if (chamberHeight < MIN_DIM) { result.fail("err_small"); return result; }
        if (chamberHeight > MAX_DIM) { result.fail("err_large"); return result; }

        // The roof is now mandatory. It is solid industrial casing with exactly one service hatch.
        int hatchCount = 0, portCount = 0, outletCount = 0, driveCount = 0;
        int roofY = floorY + chamberHeight + 1;
        int roofMinU = minU - 1;
        int roofMaxU = maxU + 1;
        for (int dn = 0; dn <= depth + 1; dn++) {
            for (int du = roofMinU; du <= roofMaxU; du++) {
                BlockState state = level.getBlockState(offsetAtY(controller, inward, u, dn, du, roofY));
                if (!isBoundary(state)) {
                    result.fail("err_roof");
                    return result;
                }
                if (state.is(ModBlocks.REACTOR_HATCH.get())) hatchCount++;
                if (state.is(ModBlocks.COOLANT_MANIFOLD.get())) portCount++;
                if (state.is(ModBlocks.STEAM_OUTLET.get())) outletCount++;
                if (state.is(ModBlocks.CONTROL_ROD_DRIVE.get())) driveCount++;
            }
        }

        // Side-wall service manifolds are also counted, so they can be placed without wasting roof space.
        for (int y = floorY + 1; y <= roofY - 1; y++) {
            for (int du = minU - 1; du <= maxU + 1; du++) {
                for (int dn : new int[] {0, depth + 1}) {
                    BlockState state = level.getBlockState(offsetAtY(controller, inward, u, dn, du, y));
                    if (state.is(ModBlocks.COOLANT_MANIFOLD.get())) portCount++;
                    if (state.is(ModBlocks.STEAM_OUTLET.get())) outletCount++;
                    if (state.is(ModBlocks.CONTROL_ROD_DRIVE.get())) driveCount++;
                }
            }
            for (int dn = 1; dn <= depth; dn++) {
                for (int du : new int[] {minU - 1, maxU + 1}) {
                    BlockState state = level.getBlockState(offsetAtY(controller, inward, u, dn, du, y));
                    if (state.is(ModBlocks.COOLANT_MANIFOLD.get())) portCount++;
                    if (state.is(ModBlocks.STEAM_OUTLET.get())) outletCount++;
                    if (state.is(ModBlocks.CONTROL_ROD_DRIVE.get())) driveCount++;
                }
            }
        }
        if (hatchCount != 1) { result.fail("err_hatch", hatchCount); return result; }
        if (portCount < 2) { result.fail("err_ports", portCount); return result; }
        if (outletCount < 1) { result.fail("err_steam", outletCount); return result; }
        if (driveCount < 1) { result.fail("err_drive", driveCount); return result; }

        // Validate floor.
        for (int du = minU - 1; du <= maxU + 1; du++) {
            for (int dn = 0; dn <= depth + 1; dn++) {
                BlockPos p = offsetAtY(controller, inward, u, dn, du, floorY);
                if (!isBoundary(level.getBlockState(p))) {
                    result.fail("err_floor", p.getX(), p.getY(), p.getZ());
                    return result;
                }
            }
        }

        int fuel = 0, control = 0, graphite = 0, cells = 0;
        Set<BlockPos> visited = new HashSet<>();
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        for (int y = floorY + 1; y <= roofY - 1; y++) {
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
                    minX = Math.min(minX, p.getX()); minY = Math.min(minY, p.getY()); minZ = Math.min(minZ, p.getZ());
                    maxX = Math.max(maxX, p.getX()); maxY = Math.max(maxY, p.getY()); maxZ = Math.max(maxZ, p.getZ());
                    if (state.is(ModBlocks.FUEL_CHANNEL.get())) fuel++;
                    else if (state.is(ModBlocks.CONTROL_ROD.get())) control++;
                    else if (state.is(ModBlocks.GRAPHITE_BLOCK.get())) graphite++;
                }
            }
        }
        if (cells != width * depth * chamberHeight || visited.size() != cells) { result.fail("err_shape"); return result; }

        int minimumFuel = Math.max(12, (cells + 11) / 12);
        if (fuel < minimumFuel) { result.fail("err_fuel_density", minimumFuel, fuel); return result; }
        int minimumControl = Math.max(3, (fuel + 3) / 4);
        if (control < minimumControl) { result.fail("err_control", minimumControl, control); return result; }
        if (graphite > 0 && graphite < fuel) { result.fail("err_graphite", fuel, graphite); return result; }

        result.valid = true;
        result.rbmk = graphite > 0;
        result.fuel = fuel; result.control = control; result.graphite = graphite; result.cells = cells;
        result.width = width; result.height = chamberHeight; result.depth = depth;
        result.coolantPorts = portCount; result.steamOutlets = outletCount; result.controlDrives = driveCount; result.hatches = hatchCount;
        result.min = new BlockPos(minX, minY, minZ); result.max = new BlockPos(maxX, maxY, maxZ);
        result.center = new Vec3((minX + maxX + 1) / 2.0, (minY + maxY + 1) / 2.0, (minZ + maxZ + 1) / 2.0);
        return result;
    }

    private static BlockPos offset(BlockPos controller, Direction inward, int depth, Direction u, int du, int dy) {
        return controller.relative(inward, depth).relative(u, du).relative(Direction.DOWN, -dy);
    }

    private static BlockPos offsetAtY(BlockPos controller, Direction inward, Direction u, int depth, int du, int y) {
        BlockPos p = controller.relative(inward, depth).relative(u, du);
        return new BlockPos(p.getX(), y, p.getZ());
    }
}
