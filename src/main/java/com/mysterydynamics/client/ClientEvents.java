package com.mysterydynamics.client;

import com.mysterydynamics.MysteryDynamics;
import com.simibubi.create.content.equipment.potatoCannon.PotatoCannonItemRenderer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.VillagerRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
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
        NeoForge.EVENT_BUS.addListener(StressTooltipEvents::onItemTooltip);
        ArcaneTurbinePartialModels.init();
    }

    private static final java.util.List<String> RESEARCH_ICONS = java.util.List.of(
            "textures/research/test_icon.png",
            "textures/research/cooperative_work.png",
            "textures/research/money_and_power.png",
            "textures/research/outsourcing.png",
            "textures/misc/r_infernal_boiler.png",
            "thaumcraft:textures/misc/r_infernalfurnace.png",
            "textures/misc/r_large_centrifuge.png");

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
        event.registerBlockEntityRenderer(MysteryDynamics.ARCANE_TURBINE_BE.get(),
                ArcaneTurbineRenderer::new);
        event.registerBlockEntityRenderer(MysteryDynamics.ESSENTIA_INTEGRATION_BE.get(),
                EssentiaIntegrationRenderer::new);
        event.registerEntityRenderer(EntityType.VILLAGER, ThaumaturgeVillagerRenderer::new);
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        // 游戏总线监听只注册一次（RegisterRenderers 不保证只触发一次）。
        NeoForge.EVENT_BUS.addListener(InfernalBoilerWorldRenderer::onRenderLevel);
        NeoForge.EVENT_BUS.addListener(EssentiaIntegrationWorldRenderer::onRenderLevel);
        event.enqueueWork(ClientEvents::preloadResearchIcons);
    }

    @SubscribeEvent
    public static void registerReloadListeners(
            net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent event) {
        // F3+T / 切换资源包后丢弃锅炉模型与 sprite 缓存。
        event.registerReloadListener(
                (net.minecraft.server.packs.resources.ResourceManagerReloadListener) manager ->
                        InfernalBoilerRenderer.clearCaches());
    }

    @SubscribeEvent
    public static void addLayers(EntityRenderersEvent.AddLayers event) {
        if (event.getRenderer(EntityType.VILLAGER) instanceof VillagerRenderer renderer) {
            renderer.addLayer(new MechanistSkinLayer(renderer));
        }
    }

    private static void preloadResearchIcons() {
        // 自定义研究图标不会自动进入纹理管理器；不预加载时研究书会画成黑紫缺失纹理。
        // 列表需与 data/mystery_dynamics/research/**/*.json 中的 "icon" 保持一致。
        var textureManager = Minecraft.getInstance().getTextureManager();
        for (String path : RESEARCH_ICONS) {
            ResourceLocation id = path.contains(":")
                    ? ResourceLocation.parse(path)
                    : ResourceLocation.fromNamespaceAndPath(MysteryDynamics.MODID, path);
            textureManager.getTexture(id);
        }
    }
}
