package com.createatomic;

import com.createatomic.block.entity.ReactorCoreBlockEntity;
import com.createatomic.block.entity.ReactorStructure;
import com.createatomic.registry.ModArmorMaterials;
import com.createatomic.registry.ModBlockEntities;
import com.createatomic.registry.ModBlocks;
import com.createatomic.registry.ModCreativeTabs;
import com.createatomic.registry.ModEffects;
import com.createatomic.registry.ModItems;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
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
        // Controller block entity: normal Create fluid automation and manual bucket filling.
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, ModBlockEntities.REACTOR_CORE.get(),
                (be, side) -> be.getFluidHandler());

        // Every exterior casing block becomes a coolant connection point. This removes the old bottleneck
        // where the controller's kinetic shaft could occupy the only usable face for a Create pipe.
        event.registerBlock(Capabilities.FluidHandler.BLOCK,
                (level, pos, state, be, side) -> findFluidHandler(level, pos),
                ModBlocks.REACTOR_CASING.get(), ModBlocks.COOLANT_MANIFOLD.get());
    }

    private static net.neoforged.neoforge.fluids.capability.IFluidHandler findFluidHandler(
            net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos) {
        var controller = ReactorStructure.findController(level, pos);
        if (controller == null) {
            return null;
        }
        BlockEntity blockEntity = level.getBlockEntity(controller);
        return blockEntity instanceof ReactorCoreBlockEntity reactor ? reactor.getFluidHandler() : null;
    }
}
