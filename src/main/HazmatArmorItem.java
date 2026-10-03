package com.createatomic.item;

import net.minecraft.core.Holder;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;

/** Radiation protection suit piece. Protection is computed in HazmatProtection. */
public class HazmatArmorItem extends ArmorItem {
    public HazmatArmorItem(Holder<ArmorMaterial> material, Type type, Properties properties) {
        super(material, type, properties);
    }
}
