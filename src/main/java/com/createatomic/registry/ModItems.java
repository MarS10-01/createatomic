package com.createatomic.registry;

import java.util.function.Supplier;

import com.createatomic.CreateAtomic;
import com.createatomic.item.AntiRadPillsItem;
import com.createatomic.item.GeigerCounterItem;
import com.createatomic.item.HazmatArmorItem;
import com.createatomic.item.RadioactiveItem;

import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(CreateAtomic.MODID);

    // ---- materials (emission order: alpha, beta, gamma, neutron; dose rate at 1 block, scaled by sqrt(count) in inventory)
    public static final DeferredItem<Item> RAW_RADICIA = ITEMS.register("raw_radicia",
            () -> new RadioactiveItem(new Item.Properties(), 0.0015, 0.0035, 0.0025, 0.0002));
    public static final DeferredItem<Item> CRUSHED_RADICIA = ITEMS.register("crushed_radicia",
            () -> new RadioactiveItem(new Item.Properties(), 0.0022, 0.0050, 0.0035, 0.0003));
    public static final DeferredItem<Item> RADICIA_INGOT = ITEMS.register("radicia_ingot",
            () -> new RadioactiveItem(new Item.Properties(), 0.0020, 0.0055, 0.0040, 0.0003));
    public static final DeferredItem<Item> RADICIA_PLATE = ITEMS.register("radicia_plate",
            () -> new RadioactiveItem(new Item.Properties(), 0.0020, 0.0055, 0.0040, 0.0003));

    // ---- fuel
    public static final DeferredItem<Item> FUEL_ROD = ITEMS.register("radicia_fuel_rod",
            () -> new RadioactiveItem(new Item.Properties().stacksTo(16), 0.0, 0.025, 0.045, 0.008));
    public static final DeferredItem<Item> SPENT_FUEL_ROD = ITEMS.register("spent_fuel_rod",
            () -> new RadioactiveItem(new Item.Properties().stacksTo(16), 0.004, 0.080, 0.120, 0.025));

    // ---- safety equipment
    public static final DeferredItem<Item> GEIGER_COUNTER = ITEMS.register("geiger_counter",
            () -> new GeigerCounterItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> ANTI_RAD_PILLS = ITEMS.register("anti_rad_pills",
            () -> new AntiRadPillsItem(new Item.Properties().stacksTo(16)));

    public static final DeferredItem<Item> HAZMAT_HELMET = ITEMS.register("hazmat_helmet",
            () -> new HazmatArmorItem(ModArmorMaterials.HAZMAT, ArmorItem.Type.HELMET,
                    new Item.Properties().durability(ArmorItem.Type.HELMET.getDurability(15))));
    public static final DeferredItem<Item> HAZMAT_CHESTPLATE = ITEMS.register("hazmat_chestplate",
            () -> new HazmatArmorItem(ModArmorMaterials.HAZMAT, ArmorItem.Type.CHESTPLATE,
                    new Item.Properties().durability(ArmorItem.Type.CHESTPLATE.getDurability(15))));
    public static final DeferredItem<Item> HAZMAT_LEGGINGS = ITEMS.register("hazmat_leggings",
            () -> new HazmatArmorItem(ModArmorMaterials.HAZMAT, ArmorItem.Type.LEGGINGS,
                    new Item.Properties().durability(ArmorItem.Type.LEGGINGS.getDurability(15))));
    public static final DeferredItem<Item> HAZMAT_BOOTS = ITEMS.register("hazmat_boots",
            () -> new HazmatArmorItem(ModArmorMaterials.HAZMAT, ArmorItem.Type.BOOTS,
                    new Item.Properties().durability(ArmorItem.Type.BOOTS.getDurability(15))));

    // ---- block items
    public static final DeferredItem<BlockItem> RADICIA_ORE_ITEM = blockItem("radicia_ore", ModBlocks.RADICIA_ORE);
    public static final DeferredItem<BlockItem> DEEPSLATE_RADICIA_ORE_ITEM = blockItem("deepslate_radicia_ore",
            ModBlocks.DEEPSLATE_RADICIA_ORE);
    public static final DeferredItem<BlockItem> RADICIA_BLOCK_ITEM = blockItem("radicia_block", ModBlocks.RADICIA_BLOCK);
    public static final DeferredItem<BlockItem> REACTOR_CASING_ITEM = blockItem("reactor_casing", ModBlocks.REACTOR_CASING);
    public static final DeferredItem<BlockItem> REACTOR_CORE_ITEM = blockItem("reactor_core", ModBlocks.REACTOR_CORE);
    public static final DeferredItem<BlockItem> FUEL_CHANNEL_ITEM = blockItem("fuel_channel", ModBlocks.FUEL_CHANNEL);
    public static final DeferredItem<BlockItem> CONTROL_ROD_ITEM = blockItem("control_rod", ModBlocks.CONTROL_ROD);
    public static final DeferredItem<BlockItem> GRAPHITE_BLOCK_ITEM = blockItem("graphite_block", ModBlocks.GRAPHITE_BLOCK);
    public static final DeferredItem<BlockItem> REACTOR_HATCH_ITEM = blockItem("reactor_hatch", ModBlocks.REACTOR_HATCH);
    public static final DeferredItem<BlockItem> COOLANT_MANIFOLD_ITEM = blockItem("coolant_manifold", ModBlocks.COOLANT_MANIFOLD);
    public static final DeferredItem<BlockItem> STEAM_OUTLET_ITEM = blockItem("steam_outlet", ModBlocks.STEAM_OUTLET);
    public static final DeferredItem<BlockItem> CONTROL_ROD_DRIVE_ITEM = blockItem("control_rod_drive", ModBlocks.CONTROL_ROD_DRIVE);
    public static final DeferredItem<BlockItem> REINFORCED_CONCRETE_ITEM = blockItem("reinforced_concrete",
            ModBlocks.REINFORCED_CONCRETE);
    public static final DeferredItem<BlockItem> BORATED_CONCRETE_ITEM = blockItem("borated_concrete",
            ModBlocks.BORATED_CONCRETE);
    public static final DeferredItem<BlockItem> CORIUM_ITEM = blockItem("corium", ModBlocks.CORIUM);

    private static DeferredItem<BlockItem> blockItem(String name, Supplier<? extends Block> block) {
        return ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
    }
}
