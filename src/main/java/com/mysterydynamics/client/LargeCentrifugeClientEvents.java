package com.mysterydynamics.client;

import com.mysterydynamics.MysteryDynamics;

import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.common.NeoForge;

@EventBusSubscriber(modid = MysteryDynamics.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class LargeCentrifugeClientEvents {

    private LargeCentrifugeClientEvents() {
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(MysteryDynamics.LARGE_CENTRIFUGE_CONTROLLER_BE.get(),
                LargeCentrifugeRenderer::new);
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        // 游戏总线监听只注册一次；放在 RegisterRenderers 里可能随事件重复触发而重复注册。
        NeoForge.EVENT_BUS.addListener(LargeCentrifugeWorldRenderer::onRenderLevel);
    }

    @SubscribeEvent
    public static void registerReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener((ResourceManagerReloadListener) manager ->
                LargeCentrifugeRenderer.clearCaches());
    }
}
