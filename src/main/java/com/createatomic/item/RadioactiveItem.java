package com.createatomic.item;

import com.createatomic.radiation.RadiationType;

import net.minecraft.world.item.Item;

/** An item that irradiates its owner (see Radiation.carried). */
public class RadioactiveItem extends Item {
    private final double[] emission;

    public RadioactiveItem(Properties properties, double alpha, double beta, double gamma, double neutron) {
        super(properties);
        this.emission = new double[] {alpha, beta, gamma, neutron};
    }

    public double emission(RadiationType type) {
        return emission[type.ordinal()];
    }
}
