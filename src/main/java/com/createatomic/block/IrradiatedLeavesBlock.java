package com.createatomic.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** Leaf replacement with a dedicated orange-brown radioactive texture. */
public class IrradiatedLeavesBlock extends RadioactiveBlock {
    public IrradiatedLeavesBlock(Properties properties) {
        super(properties, 0.0, 0.0009, 0.0015, 0.0002);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(18) == 0) {
            level.addParticle(ParticleTypes.ASH, pos.getX() + random.nextDouble(), pos.getY() + random.nextDouble(),
                    pos.getZ() + random.nextDouble(), 0.0, -0.005, 0.0);
        }
    }
}
