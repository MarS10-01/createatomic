package com.createatomic.block;

import com.createatomic.registry.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Extremely hot molten-fuel residue. It scorches the terrain underneath and slowly crawls across hot ground. */
public class CoriumBlock extends RadioactiveBlock {

    public CoriumBlock(Properties properties, double alpha, double beta, double gamma, double neutron) {
        super(properties, alpha, beta, gamma, neutron);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        int smoke = 2 + random.nextInt(3);
        level.addParticle(ParticleTypes.LARGE_SMOKE, pos.getX() + random.nextDouble(), pos.getY() + 1.0,
                pos.getZ() + random.nextDouble(), 0.0, 0.11, 0.0);
        if (random.nextBoolean()) {
            level.addParticle(ParticleTypes.ASH, pos.getX() + random.nextDouble(), pos.getY() + 1.0,
                    pos.getZ() + random.nextDouble(), 0.0, 0.025, 0.0);
        }
        if (random.nextInt(4) == 0) {
            level.addParticle(ParticleTypes.FLAME, pos.getX() + random.nextDouble(), pos.getY() + 1.01,
                    pos.getZ() + random.nextDouble(), 0.0, 0.02, 0.0);
        }
        for (int i = 0; i < smoke - 2; i++) {
            level.addParticle(ParticleTypes.SMOKE, pos.getX() + random.nextDouble(), pos.getY() + 1.15,
                    pos.getZ() + random.nextDouble(), 0.0, 0.08, 0.0);
        }
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        BlockPos below = pos.below();
        BlockState belowState = level.getBlockState(below);
        if (belowState.is(Blocks.GRASS_BLOCK) || belowState.is(Blocks.DIRT) || belowState.is(Blocks.PODZOL)
                || belowState.is(Blocks.COARSE_DIRT) || belowState.is(Blocks.ROOTED_DIRT)
                || belowState.is(ModBlocks.IRRADIATED_SOIL.get())) {
            level.setBlockAndUpdate(below, ModBlocks.SCORCHED_EARTH.get().defaultBlockState());
        } else if (belowState.is(Blocks.STONE) || belowState.is(Blocks.DEEPSLATE)
                || belowState.is(Blocks.GRAVEL) || belowState.is(Blocks.ANDESITE)
                || belowState.is(Blocks.DIORITE) || belowState.is(Blocks.GRANITE)
                || belowState.is(ModBlocks.CHARRED_STONE.get())) {
            level.setBlockAndUpdate(below, ModBlocks.CHARRED_STONE.get().defaultBlockState());
        }

        // A tiny, local crawl makes a hot pool grow a little without spraying corium through the 150-block fallout zone.
        if (random.nextInt(8) == 0) {
            BlockPos near = pos.offset(random.nextInt(3) - 1, 0, random.nextInt(3) - 1);
            if (level.getBlockState(near).isAir() && level.getBlockState(near.below()).isSolid()) {
                level.setBlockAndUpdate(near, ModBlocks.CORIUM.get().defaultBlockState());
            }
        }
    }
}
