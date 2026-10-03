package com.createatomic.registry;

import java.util.EnumMap;
import java.util.List;

import com.createatomic.CreateAtomic;

import net.minecraft.Util;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModArmorMaterials {
    public static final DeferredRegister<ArmorMaterial> MATERIALS =
            DeferredRegister.create(Registries.ARMOR_MATERIAL, CreateAtomic.MODID);

    // Textures: assets/createatomic/textures/models/armor/hazmat_layer_1.png and hazmat_layer_2.png
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> HAZMAT = MATERIALS.register("hazmat",
            () -> new ArmorMaterial(
                    Util.make(new EnumMap<>(ArmorItem.Type.class), map -> {
                        map.put(ArmorItem.Type.BOOTS, 1);
                        map.put(ArmorItem.Type.LEGGINGS, 2);
                        map.put(ArmorItem.Type.CHESTPLATE, 3);
                        map.put(ArmorItem.Type.HELMET, 1);
                        map.put(ArmorItem.Type.BODY, 3);
                    }),
                    9,
                    SoundEvents.ARMOR_EQUIP_IRON,
                    () -> Ingredient.of(Items.IRON_INGOT),
                    List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(CreateAtomic.MODID, "hazmat"))),
                    0.0F,
                    0.0F));
}
