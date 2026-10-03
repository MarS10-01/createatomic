package com.createatomic.radiation;

import com.createatomic.item.HazmatArmorItem;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** How much of each radiation type the worn armor blocks (0..1). */
public final class HazmatProtection {
    // full hazmat set: alpha, beta, gamma, neutron
    private static final double[] FULL_SET = {1.0, 0.98, 0.75, 0.65};

    private HazmatProtection() {}

    public static double fraction(Player player, RadiationType type) {
        int hazmatPieces = 0;
        int armorPieces = 0;
        for (ItemStack stack : player.getArmorSlots()) {
            if (stack.isEmpty()) {
                continue;
            }
            armorPieces++;
            if (stack.getItem() instanceof HazmatArmorItem) {
                hazmatPieces++;
            }
        }
        double hazmat = hazmatPieces / 4.0 * FULL_SET[type.ordinal()];
        // any ordinary armor stops most alpha particles
        double generic = type == RadiationType.ALPHA ? Math.min(1.0, armorPieces * 0.25) : 0.0;
        return Math.max(hazmat, generic);
    }
}
