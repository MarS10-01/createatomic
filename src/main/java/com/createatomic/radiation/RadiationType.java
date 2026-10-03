package com.createatomic.radiation;

/**
 * The four kinds of ionizing radiation.
 *  - alpha  : huge biological effect but stopped by anything (even clothing); only matters from contact/inventory
 *  - beta   : short range, stopped by any solid block
 *  - gamma  : long range, needs dense shielding (concrete, metal blocks)
 *  - neutron: long range, passes through most things; absorbed by water and borated concrete
 */
public enum RadiationType {
    ALPHA("alpha", "\u03b1", 2.0, 20.0),
    BETA("beta", "\u03b2", 6.0, 1.0),
    GAMMA("gamma", "\u03b3", 160.0, 1.0),
    NEUTRON("neutron", "n", 150.0, 10.0);

    public static final RadiationType[] VALUES = values();

    public final String id;
    public final String symbol;
    /** Maximum travel distance in blocks. */
    public final double range;
    /** Radiation weighting factor (absorbed dose -> effective dose). */
    public final double weight;

    RadiationType(String id, String symbol, double range, double weight) {
        this.id = id;
        this.symbol = symbol;
        this.range = range;
        this.weight = weight;
    }
}
