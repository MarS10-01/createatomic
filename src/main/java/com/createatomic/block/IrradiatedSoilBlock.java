package com.createatomic.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** Actual replacement terrain block used in the fallout zone. */
public class IrradiatedSoilBlock extends RadioactiveBlock {
    public IrradiatedSoilBlock(Properties properties) {
        super(properties, 0.0, 0.0015, 0.0022, 0.0003);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(20) == 0) {
            level.addParticle(ParticleTypes.ASH, pos.getX() + random.nextDouble(), pos.getY() + 1.0,
                    pos.getZ() + random.nextDouble(), 0.0, 0.02, 0.0);
        }
    }
}
