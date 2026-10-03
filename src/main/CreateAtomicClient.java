package com.createatomic.client;

import com.createatomic.CreateAtomic;
import com.createatomic.client.ponder.AtomicPonderPlugin;
import com.createatomic.registry.ModBlockEntities;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.createmod.ponder.foundation.PonderIndex;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = CreateAtomic.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class CreateAtomicClient {

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        // Create's built-in guide: press W over our items to open the scenes
        event.enqueueWork(() -> PonderIndex.addPlugin(new AtomicPonderPlugin()));
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.REACTOR_CORE.get(), ReactorCoreRenderer::new);
    }
}
