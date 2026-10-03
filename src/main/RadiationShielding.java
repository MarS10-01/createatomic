package com.createatomic.radiation;

import com.createatomic.registry.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** Ray-marches from a source to a target and multiplies the transmission of every block in between. */
public final class RadiationShielding {
    // transmission per block, order: alpha, beta, gamma, neutron
    private static final double[] CASING = {0.0, 0.03, 0.55, 0.50};
    private static final double[] INTERNALS = {0.0, 0.10, 0.80, 0.60};
    private static final double[] GRAPHITE = {0.0, 0.20, 0.85, 0.70};
    private static final double[] REINFORCED = {0.0, 0.01, 0.35, 0.35};
    private static final double[] BORATED = {0.0, 0.01, 0.45, 0.08};
    private static final double[] HEAVY_METAL = {0.0, 0.05, 0.45, 0.75};
    private static final double[] WATER = {0.05, 0.50, 0.88, 0.50};
    private static final double[] LAVA = {0.0, 0.30, 0.70, 0.60};
    private static final double[] THIN_SOLID = {0.05, 0.50, 0.95, 0.90};
    private static final double[] PLANT = {0.40, 0.90, 0.99, 0.99};

    private RadiationShielding() {}

    public static double transmission(Level level, Vec3 from, Vec3 to, RadiationType type, BlockPos sourceBlock) {
        Vec3 delta = to.subtract(from);
        double length = delta.length();
        if (length < 0.75) {
            return 1.0;
        }
        int steps = Math.min(160, (int) Math.ceil(length / 0.5));
        Vec3 step = delta.scale(1.0 / steps);
        BlockPos targetBlock = BlockPos.containing(to);
        BlockPos last = sourceBlock;
        BlockPos.MutableBlockPos mp = new BlockPos.MutableBlockPos();
        double result = 1.0;

        for (int i = 1; i < steps; i++) {
            Vec3 p = from.add(step.scale(i));
            mp.set(p.x, p.y, p.z);
            if (mp.equals(last) || mp.equals(targetBlock)) {
                continue;
            }
            last = mp.immutable();
            if (!level.isLoaded(mp)) {
                continue;
            }
            BlockState state = level.getBlockState(mp);
            if (state.isAir()) {
                continue;
            }
            result *= factor(level, mp, state, type);
            if (result < 0.0005) {
                return 0.0;
            }
        }
        return result;
    }

    private static double factor(Level level, BlockPos pos, BlockState state, RadiationType type) {
        int i = type.ordinal();
        Block block = state.getBlock();

        if (block == ModBlocks.REACTOR_CASING.get() || block == ModBlocks.REACTOR_CORE.get()) {
            return CASING[i];
        }
        if (block == ModBlocks.FUEL_CHANNEL.get() || block == ModBlocks.CONTROL_ROD.get()) {
            return INTERNALS[i];
        }
        if (block == ModBlocks.GRAPHITE_BLOCK.get()) {
            return GRAPHITE[i];
        }
        if (block == ModBlocks.REINFORCED_CONCRETE.get()) {
            return REINFORCED[i];
        }
        if (block == ModBlocks.BORATED_CONCRETE.get()) {
            return BORATED[i];
        }
        if (state.is(BlockTags.BEACON_BASE_BLOCKS)) {
            return HEAVY_METAL[i];
        }
        if (!state.getFluidState().isEmpty()) {
            return state.getFluidState().is(FluidTags.WATER) ? WATER[i] : LAVA[i];
        }
        if (state.canOcclude()) {
            // generic opaque block: denser (harder) blocks absorb more gamma
            if (type == RadiationType.GAMMA) {
                double hardness = Math.max(0.0, state.getDestroySpeed(level, pos));
                return 0.90 - 0.45 * Math.min(hardness, 50.0) / 50.0;
            }
            return type == RadiationType.BETA ? 0.15 : type == RadiationType.ALPHA ? 0.0 : 0.75;
        }
        if (!state.getCollisionShape(level, pos).isEmpty()) {
            return THIN_SOLID[i];
        }
        return PLANT[i];
    }
}
