package com.createatomic.registry;

import java.util.function.Supplier;

import com.createatomic.CreateAtomic;
import com.createatomic.item.RadioactiveItem;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(CreateAtomic.MODID);

    // Materials (all weakly radioactive when carried)
    public static final DeferredItem<Item> RAW_RADICIA = ITEMS.register("raw_radicia",
            () -> new RadioactiveItem(new Item.Properties(), 0));
    public static final DeferredItem<Item> CRUSHED_RADICIA = ITEMS.register("crushed_radicia",
            () -> new RadioactiveItem(new Item.Properties(), 0));
    public static final DeferredItem<Item> RADICIA_INGOT = ITEMS.register("radicia_ingot",
            () -> new RadioactiveItem(new Item.Properties(), 0));

    // Reactor fuel
    public static final DeferredItem<Item> FUEL_ROD = ITEMS.register("radicia_fuel_rod",
            () -> new RadioactiveItem(new Item.Properties().stacksTo(16), 0));
    public static final DeferredItem<Item> SPENT_FUEL_ROD = ITEMS.register("spent_fuel_rod",
            () -> new RadioactiveItem(new Item.Properties().stacksTo(16), 1));

    // Block items
    public static final DeferredItem<BlockItem> RADICIA_ORE_ITEM = blockItem("radicia_ore", ModBlocks.RADICIA_ORE);
    public static final DeferredItem<BlockItem> DEEPSLATE_RADICIA_ORE_ITEM = blockItem("deepslate_radicia_ore",
            ModBlocks.DEEPSLATE_RADICIA_ORE);
    public static final DeferredItem<BlockItem> RADICIA_BLOCK_ITEM = blockItem("radicia_block", ModBlocks.RADICIA_BLOCK);
    public static final DeferredItem<BlockItem> REACTOR_CASING_ITEM = blockItem("reactor_casing", ModBlocks.REACTOR_CASING);
    public static final DeferredItem<BlockItem> REACTOR_CORE_ITEM = blockItem("reactor_core", ModBlocks.REACTOR_CORE);

    private static DeferredItem<BlockItem> blockItem(String name, Supplier<? extends Block> block) {
        return ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
    }
}
