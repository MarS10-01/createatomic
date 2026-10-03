package com.createatomic.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** Left behind by a reactor meltdown: very radioactive and smoking. */
public class RadioactiveDebrisBlock extends RadioactiveBlock {

    public RadioactiveDebrisBlock(Properties properties, double alpha, double beta, double gamma, double neutron) {
        super(properties, alpha, beta, gamma, neutron);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(3) == 0) {
            level.addParticle(ParticleTypes.LARGE_SMOKE, pos.getX() + random.nextDouble(), pos.getY() + 1.0,
                    pos.getZ() + random.nextDouble(), 0.0, 0.05, 0.0);
        }
        if (random.nextInt(6) == 0) {
            level.addParticle(ParticleTypes.ASH, pos.getX() + random.nextDouble(), pos.getY() + 1.0,
                    pos.getZ() + random.nextDouble(), 0.0, 0.02, 0.0);
        }
    }
}
