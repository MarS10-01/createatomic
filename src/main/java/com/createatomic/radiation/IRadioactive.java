package com.createatomic.radiation;

import net.minecraft.world.level.block.state.BlockState;

/** Implemented by blocks that emit radiation. */
public interface IRadioactive {
    /** Absorbed dose rate (Gy/s) at a distance of 1 block, indexed by RadiationType ordinal. */
    double[] emission(BlockState state);
}
