package com.createatomic.block.entity;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

import com.createatomic.block.ReactorCoreBlock;
import com.createatomic.registry.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Result of scanning the reactor multiblock around a controller.
 *
 * Rules:
 *  - the controller sits IN the wall of a sealed rectangular chamber (3x3x3 up to 7x7x7 inside)
 *  - the wall (every block touching the chamber) is Reactor Casing or a Reactor Controller
 *  - the chamber contains only air, Fuel Channels, Control Rods and Graphite Blocks
 *  - at least 1 fuel channel, and 1 control rod per 4 fuel channels
 *  - graphite blocks make it a graphite-moderated (RBMK-type) reactor and need at least one per fuel channel;
 *    without graphite it is a water-moderated pressurized water reactor (PWR)
 */
public final class ReactorStructure {
    public static final int MIN_DIM = 3;
    public static final int MAX_DIM = 7;
    private static final int MAX_CELLS = MAX_DIM * MAX_DIM * MAX_DIM;

    public boolean valid;
    public String error = "err_interior";
    public Object[] errorArgs = new Object[0];

    public int fuel;
    public int control;
    public int graphite;
    public int cells;
    public boolean rbmk;
    public BlockPos min = BlockPos.ZERO;
    public BlockPos max = BlockPos.ZERO;
    public Vec3 center = Vec3.ZERO;

    private boolean aborted;

    public static boolean isInterior(BlockState state) {
        return state.isAir()
                || state.is(ModBlocks.FUEL_CHANNEL.get())
                || state.is(ModBlocks.CONTROL_ROD.get())
                || state.is(ModBlocks.GRAPHITE_BLOCK.get());
    }

    public static boolean isBoundary(BlockState state) {
        return state.is(ModBlocks.REACTOR_CASING.get()) || state.getBlock() instanceof ReactorCoreBlock;
    }

    public static ReactorStructure scan(Level level, BlockPos controller) {
        ReactorStructure best = null;
        for (Direction direction : Direction.values()) {
            BlockPos start = controller.relative(direction);
            if (!level.isLoaded(start) || !isInterior(level.getBlockState(start))) {
                continue;
            }
            ReactorStructure attempt = flood(level, start);
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

    private void fail(String key, Object... args) {
        this.valid = false;
        this.error = key;
        this.errorArgs = args;
    }

    private static ReactorStructure flood(Level level, BlockPos start) {
        ReactorStructure result = new ReactorStructure();
        Set<BlockPos> visited = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        visited.add(start);
        queue.add(start);

        int minX = start.getX(), minY = start.getY(), minZ = start.getZ();
        int maxX = minX, maxY = minY, maxZ = minZ;

        while (!queue.isEmpty()) {
            BlockPos current = queue.poll();
            BlockState state = level.getBlockState(current);
            if (state.is(ModBlocks.FUEL_CHANNEL.get())) {
                result.fuel++;
            } else if (state.is(ModBlocks.CONTROL_ROD.get())) {
                result.control++;
            } else if (state.is(ModBlocks.GRAPHITE_BLOCK.get())) {
                result.graphite++;
            }
            minX = Math.min(minX, current.getX());
            minY = Math.min(minY, current.getY());
            minZ = Math.min(minZ, current.getZ());
            maxX = Math.max(maxX, current.getX());
            maxY = Math.max(maxY, current.getY());
            maxZ = Math.max(maxZ, current.getZ());

            for (Direction direction : Direction.values()) {
                BlockPos next = current.relative(direction);
                if (visited.contains(next)) {
                    continue;
                }
                if (!level.isLoaded(next)) {
                    result.cells = visited.size();
                    result.fail("err_foreign", next.getX(), next.getY(), next.getZ());
                    return result;
                }
                BlockState nextState = level.getBlockState(next);
                if (isInterior(nextState)) {
                    if (visited.size() >= MAX_CELLS) {
                        result.aborted = true;
                        result.cells = visited.size();
                        result.fail("err_open");
                        return result;
                    }
                    visited.add(next);
                    queue.add(next);
                } else if (!isBoundary(nextState)) {
                    result.cells = visited.size();
                    result.fail("err_foreign", next.getX(), next.getY(), next.getZ());
                    return result;
                }
            }
        }

        result.cells = visited.size();
        int dx = maxX - minX + 1;
        int dy = maxY - minY + 1;
        int dz = maxZ - minZ + 1;
        result.min = new BlockPos(minX, minY, minZ);
        result.max = new BlockPos(maxX, maxY, maxZ);
        result.center = new Vec3((minX + maxX + 1) / 2.0, (minY + maxY + 1) / 2.0, (minZ + maxZ + 1) / 2.0);

        if (dx < MIN_DIM || dy < MIN_DIM || dz < MIN_DIM) {
            result.fail("err_small");
        } else if (dx > MAX_DIM || dy > MAX_DIM || dz > MAX_DIM) {
            result.fail("err_large");
        } else if (result.cells != dx * dy * dz) {
            result.fail("err_shape");
        } else if (result.fuel == 0) {
            result.fail("err_fuel");
        } else if (result.control < (result.fuel + 3) / 4) {
            result.fail("err_control", (result.fuel + 3) / 4, result.control);
        } else if (result.graphite > 0 && result.graphite < result.fuel) {
            result.fail("err_graphite", result.fuel, result.graphite);
        } else {
            result.valid = true;
            result.rbmk = result.graphite > 0;
        }
        return result;
    }
}
