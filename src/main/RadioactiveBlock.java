package com.createatomic.block;

import com.createatomic.radiation.IRadioactive;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** A block that emits radiation (found by the 6-block scan around players and Geiger counters). */
public class RadioactiveBlock extends Block implements IRadioactive {
    private final double[] emission;

    public RadioactiveBlock(Properties properties, double alpha, double beta, double gamma, double neutron) {
        super(properties);
        this.emission = new double[] {alpha, beta, gamma, neutron};
    }

    @Override
    public double[] emission(BlockState state) {
        return emission;
    }
}
