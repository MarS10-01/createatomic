package com.createatomic.block;

import com.createatomic.effect.RadiationEffect;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** A block that irradiates nearby entities on random ticks. */
public class RadioactiveBlock extends Block {
    private final int amplifier;
    private final double radius;

    public RadioactiveBlock(Properties properties, int amplifier, double radius) {
        super(properties);
        this.amplifier = amplifier;
        this.radius = radius;
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        RadiationEffect.irradiate(level, pos, radius, 400, amplifier);
    }
}
