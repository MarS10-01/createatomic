package com.createatomic.registry;

import com.createatomic.CreateAtomic;
import com.createatomic.block.RadioactiveBlock;
import com.createatomic.block.CharredStoneBlock;
import com.createatomic.block.CoriumBlock;
import com.createatomic.block.FuelChannelBlock;
import com.createatomic.block.IrradiatedLeavesBlock;
import com.createatomic.block.IrradiatedSoilBlock;
import com.createatomic.block.RadioactiveDebrisBlock;
import com.createatomic.block.ReactorCasingBlock;
import com.createatomic.block.ReactorComponentBlock;
import com.createatomic.block.ReactorCoreBlock;
import com.createatomic.block.ReactorHatchBlock;
import com.createatomic.block.ReactorEquipmentBlock;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(CreateAtomic.MODID);

    // ---- ores and storage (emission order: alpha, beta, gamma, neutron; absorbed dose rate at 1 block)
    public static final DeferredBlock<Block> RADICIA_ORE = BLOCKS.register("radicia_ore",
            () -> new RadioactiveBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_ORE)
                    .strength(3.5f, 3.0f).requiresCorrectToolForDrops().lightLevel(s -> 3),
                    1.0e-5, 2.0e-5, 1.5e-5, 0.0));

    public static final DeferredBlock<Block> DEEPSLATE_RADICIA_ORE = BLOCKS.register("deepslate_radicia_ore",
            () -> new RadioactiveBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.DEEPSLATE_IRON_ORE)
                    .strength(4.5f, 3.0f).requiresCorrectToolForDrops().lightLevel(s -> 3),
                    1.0e-5, 2.0e-5, 1.5e-5, 0.0));

    public static final DeferredBlock<Block> RADICIA_BLOCK = BLOCKS.register("radicia_block",
            () -> new RadioactiveBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)
                    .strength(5.0f, 6.0f).requiresCorrectToolForDrops().lightLevel(s -> 6),
                    3.0e-5, 6.0e-5, 5.0e-5, 0.0));

    // ---- reactor structure
    public static final DeferredBlock<Block> REACTOR_CASING = BLOCKS.register("reactor_casing",
            () -> new ReactorCasingBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)
                    .strength(5.0f, 12.0f).requiresCorrectToolForDrops()));

    public static final DeferredBlock<ReactorCoreBlock> REACTOR_CORE = BLOCKS.register("reactor_core",
            () -> new ReactorCoreBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)
                    .strength(5.0f, 12.0f).requiresCorrectToolForDrops().noOcclusion()));

    public static final DeferredBlock<Block> FUEL_CHANNEL = BLOCKS.register("fuel_channel",
            () -> new FuelChannelBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)
                    .strength(4.0f, 8.0f).requiresCorrectToolForDrops().lightLevel(s -> 4)));

    public static final DeferredBlock<Block> CONTROL_ROD = BLOCKS.register("control_rod",
            () -> new ReactorComponentBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)
                    .strength(4.0f, 8.0f).requiresCorrectToolForDrops()));

    public static final DeferredBlock<Block> GRAPHITE_BLOCK = BLOCKS.register("graphite_block",
            () -> new ReactorComponentBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.COAL_BLOCK)
                    .strength(4.0f, 6.0f).requiresCorrectToolForDrops()));

    // ---- reactor service hardware
    public static final DeferredBlock<Block> REACTOR_HATCH = BLOCKS.register("reactor_hatch",
            () -> new ReactorHatchBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)
                    .strength(8.0f, 30.0f).requiresCorrectToolForDrops().noOcclusion()));

    public static final DeferredBlock<Block> COOLANT_MANIFOLD = BLOCKS.register("coolant_manifold",
            () -> new ReactorEquipmentBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)
                    .strength(7.0f, 24.0f).requiresCorrectToolForDrops()));

    public static final DeferredBlock<Block> STEAM_OUTLET = BLOCKS.register("steam_outlet",
            () -> new ReactorEquipmentBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)
                    .strength(7.0f, 24.0f).requiresCorrectToolForDrops()));

    public static final DeferredBlock<Block> CONTROL_ROD_DRIVE = BLOCKS.register("control_rod_drive",
            () -> new ReactorEquipmentBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)
                    .strength(7.0f, 24.0f).requiresCorrectToolForDrops()));

    // ---- radiation shielding
    public static final DeferredBlock<Block> REINFORCED_CONCRETE = BLOCKS.register("reinforced_concrete",
            () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.STONE)
                    .strength(6.0f, 40.0f).requiresCorrectToolForDrops()));

    public static final DeferredBlock<Block> BORATED_CONCRETE = BLOCKS.register("borated_concrete",
            () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.STONE)
                    .strength(6.0f, 40.0f).requiresCorrectToolForDrops()));

    // ---- meltdown aftermath (no item, no drops)
    public static final DeferredBlock<Block> RADIOACTIVE_DEBRIS = BLOCKS.register("radioactive_debris",
            () -> new RadioactiveDebrisBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.BLACKSTONE)
                    .strength(3.0f, 6.0f).lightLevel(s -> 7).noLootTable(),
                    0.0, 4.0e-3, 8.0e-3, 2.0e-3));

    // Permanent molten fuel residue left by a major reactor accident.
    public static final DeferredBlock<Block> CORIUM = BLOCKS.register("corium",
            () -> new CoriumBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.MAGMA_BLOCK)
                    .strength(4.0f, 20.0f).lightLevel(s -> 12).noLootTable(),
                    0.0, 0.015, 0.12, 0.025));

    // Real terrain replacements used after an accident: these blocks have their own textures.
    public static final DeferredBlock<Block> IRRADIATED_SOIL = BLOCKS.register("irradiated_soil",
            () -> new IrradiatedSoilBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.DIRT)
                    .strength(0.7f, 1.5f).randomTicks().noLootTable()));

    public static final DeferredBlock<Block> IRRADIATED_LEAVES = BLOCKS.register("irradiated_leaves",
            () -> new IrradiatedLeavesBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_LEAVES)
                    .strength(0.2f).noOcclusion().noLootTable()));

    public static final DeferredBlock<Block> SCORCHED_EARTH = BLOCKS.register("scorched_earth",
            () -> new IrradiatedSoilBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.DIRT)
                    .strength(1.0f, 2.0f).noLootTable()));

    public static final DeferredBlock<Block> CHARRED_STONE = BLOCKS.register("charred_stone",
            () -> new CharredStoneBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.STONE)
                    .strength(4.0f, 6.0f).requiresCorrectToolForDrops().noLootTable()));
}
