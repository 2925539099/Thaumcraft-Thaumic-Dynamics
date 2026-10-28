package com.mysterydynamics.client;

import com.mysterydynamics.MysteryDynamics;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = MysteryDynamics.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class LargeCentrifugeClientEvents {

    private LargeCentrifugeClientEvents() {
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(MysteryDynamics.LARGE_CENTRIFUGE_CONTROLLER_BE.get(),
                LargeCentrifugeRenderer::new);
    }
}
