package com.createatomic.client;

import com.createatomic.CreateAtomic;
import com.createatomic.registry.ModBlockEntities;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = CreateAtomic.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class CreateAtomicClient {

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.REACTOR_CORE.get(), ReactorCoreRenderer::new);
    }
}
