package com.createatomic.compat.jei;

import java.util.function.Supplier;

import com.createatomic.CreateAtomic;
import com.createatomic.registry.ModItems;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

/**
 * JEI integration. All crafting, smelting and Create (crushing / pressing) recipes of the mod are normal recipes,
 * so JEI and Create's own JEI plugin already list them. This plugin adds an information page to every key item
 * (what it does, where it comes from). Create's Ponder guide (W) also works on items inside JEI.
 */
@JeiPlugin
public class AtomicJeiPlugin implements IModPlugin {
    private static final ResourceLocation UID = CreateAtomic.id("jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        info(registration, ModItems.RADICIA_ORE_ITEM, "ore");
        info(registration, ModItems.DEEPSLATE_RADICIA_ORE_ITEM, "ore");
        info(registration, ModItems.RAW_RADICIA, "raw");
        info(registration, ModItems.RADICIA_INGOT, "ingot");
        info(registration, ModItems.FUEL_ROD, "fuel_rod");
        info(registration, ModItems.SPENT_FUEL_ROD, "spent_fuel_rod");
        info(registration, ModItems.REACTOR_CASING_ITEM, "reactor_casing");
        info(registration, ModItems.REACTOR_CORE_ITEM, "reactor_core");
        info(registration, ModItems.FUEL_CHANNEL_ITEM, "fuel_channel");
        info(registration, ModItems.CONTROL_ROD_ITEM, "control_rod");
        info(registration, ModItems.GRAPHITE_BLOCK_ITEM, "graphite");
        info(registration, ModItems.REINFORCED_CONCRETE_ITEM, "reinforced_concrete");
        info(registration, ModItems.BORATED_CONCRETE_ITEM, "borated_concrete");
        info(registration, ModItems.GEIGER_COUNTER, "geiger_counter");
        info(registration, ModItems.ANTI_RAD_PILLS, "anti_rad_pills");
        info(registration, ModItems.HAZMAT_HELMET, "hazmat");
        info(registration, ModItems.HAZMAT_CHESTPLATE, "hazmat");
        info(registration, ModItems.HAZMAT_LEGGINGS, "hazmat");
        info(registration, ModItems.HAZMAT_BOOTS, "hazmat");
        info(registration, ModItems.CORIUM_ITEM, "corium");
    }

    private static void info(IRecipeRegistration registration, Supplier<? extends ItemLike> item, String key) {
        registration.addItemStackInfo(new ItemStack(item.get()), Component.translatable("jei.createatomic.info." + key));
    }
}
