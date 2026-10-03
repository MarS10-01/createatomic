package com.createatomic;

import com.createatomic.registry.ModArmorMaterials;
import com.createatomic.registry.ModBlockEntities;
import com.createatomic.registry.ModBlocks;
import com.createatomic.registry.ModCreativeTabs;
import com.createatomic.registry.ModEffects;
import com.createatomic.registry.ModItems;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

@Mod(CreateAtomic.MODID)
public class CreateAtomic {
    public static final String MODID = "createatomic";

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }

    public CreateAtomic(IEventBus modEventBus, ModContainer modContainer) {
        ModBlocks.BLOCKS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        ModEffects.EFFECTS.register(modEventBus);
        ModArmorMaterials.MATERIALS.register(modEventBus);
        ModCreativeTabs.TABS.register(modEventBus);
        modEventBus.addListener(CreateAtomic::registerCapabilities);
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        // Lets Create pipes / pumps feed coolant water into the reactor controller.
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, ModBlockEntities.REACTOR_CORE.get(),
                (be, side) -> be.getFluidHandler());
    }
}
