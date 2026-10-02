package com.createatomic.registry;

import com.createatomic.CreateAtomic;
import com.createatomic.block.RadioactiveBlock;
import com.createatomic.block.ReactorCoreBlock;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(CreateAtomic.MODID);

    public static final DeferredBlock<Block> RADICIA_ORE = BLOCKS.register("radicia_ore",
            () -> new RadioactiveBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_ORE)
                    .strength(3.5f, 3.0f).requiresCorrectToolForDrops().randomTicks().lightLevel(s -> 3),
                    0, 4.0));

    public static final DeferredBlock<Block> DEEPSLATE_RADICIA_ORE = BLOCKS.register("deepslate_radicia_ore",
            () -> new RadioactiveBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.DEEPSLATE_IRON_ORE)
                    .strength(4.5f, 3.0f).requiresCorrectToolForDrops().randomTicks().lightLevel(s -> 3),
                    0, 4.0));

    public static final DeferredBlock<Block> RADICIA_BLOCK = BLOCKS.register("radicia_block",
            () -> new RadioactiveBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)
                    .strength(5.0f, 6.0f).requiresCorrectToolForDrops().randomTicks().lightLevel(s -> 6),
                    0, 6.0));

    public static final DeferredBlock<Block> REACTOR_CASING = BLOCKS.register("reactor_casing",
            () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)
                    .strength(5.0f, 12.0f).requiresCorrectToolForDrops()));

    public static final DeferredBlock<ReactorCoreBlock> REACTOR_CORE = BLOCKS.register("reactor_core",
            () -> new ReactorCoreBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)
                    .strength(5.0f, 12.0f).requiresCorrectToolForDrops().noOcclusion()));
}
