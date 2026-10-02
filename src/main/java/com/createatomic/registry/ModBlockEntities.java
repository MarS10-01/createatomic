package com.createatomic.registry;

import com.createatomic.CreateAtomic;
import com.createatomic.block.entity.ReactorCoreBlockEntity;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, CreateAtomic.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ReactorCoreBlockEntity>> REACTOR_CORE =
            BLOCK_ENTITIES.register("reactor_core",
                    () -> BlockEntityType.Builder.of(ReactorCoreBlockEntity::new, ModBlocks.REACTOR_CORE.get())
                            .build(null));
}
