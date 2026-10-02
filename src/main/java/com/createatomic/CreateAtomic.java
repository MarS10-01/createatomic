package com.createatomic;

import com.createatomic.registry.ModBlockEntities;
import com.createatomic.registry.ModBlocks;
import com.createatomic.registry.ModCreativeTabs;
import com.createatomic.registry.ModEffects;
import com.createatomic.registry.ModItems;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

@Mod(CreateAtomic.MODID)
public class CreateAtomic {
    public static final String MODID = "createatomic";

    public CreateAtomic(IEventBus modEventBus, ModContainer modContainer) {
        ModBlocks.BLOCKS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        ModEffects.EFFECTS.register(modEventBus);
        ModCreativeTabs.TABS.register(modEventBus);
    }
}
