package com.mysterydynamics.client;

import com.mysterydynamics.MysteryDynamics;
import com.simibubi.create.content.equipment.potatoCannon.PotatoCannonItemRenderer;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterItemDecorationsEvent;
import net.neoforged.neoforge.common.NeoForge;

import thaumcraft.api.research.ResearchBookApi;

/** 客户端注册（专用服务器不会加载本类）。 */
@EventBusSubscriber(modid = MysteryDynamics.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientEvents {

    static {
        // 尽早注册别名，确保研究书构建页面时已经生效。
        ResearchBookApi.addPageAlias(key -> switch (key) {
            case "ESSENTIADISTILLATION_LINK" -> "DISTILESSENTIA";
            case "INFERNALFURNACE_LINK" -> "INFERNALFURNACE";
            default -> key;
        });
    }

    private ClientEvents() {
    }

    @SubscribeEvent
    public static void registerItemDecorations(RegisterItemDecorationsEvent event) {
        event.register(MysteryDynamics.HIGH_PRESSURE_LAUNCHER.get(),
                PotatoCannonItemRenderer.DECORATOR);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(MysteryDynamics.REDSTONE_ENGINE_BE.get(), RedstoneEngineRenderer::new);
        event.registerBlockEntityRenderer(MysteryDynamics.INFERNAL_BOILER_BE.get(),
                InfernalBoilerRenderer::new);
        NeoForge.EVENT_BUS.addListener(InfernalBoilerWorldRenderer::onRenderLevel);
        preloadResearchIcons();
    }

    private static void preloadResearchIcons() {
        // 自定义研究图标不会自动进入纹理管理器；不预加载时研究书会画成黑紫缺失纹理。
        var textureManager = Minecraft.getInstance().getTextureManager();
        textureManager.getTexture(ResourceLocation.fromNamespaceAndPath(
                MysteryDynamics.MODID, "textures/research/test_icon.png"));
        textureManager.getTexture(ResourceLocation.fromNamespaceAndPath(
                MysteryDynamics.MODID, "textures/misc/r_infernal_boiler.png"));
    }
}
