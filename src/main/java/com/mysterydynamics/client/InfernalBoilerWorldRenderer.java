package com.mysterydynamics.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mysterydynamics.block.InfernalBoilerBlockEntity;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 整栋锅炉不挂在单个 BlockEntity 的视锥裁剪上，而是在关卡渲染阶段扫描
 * 已加载区块中的 controller 统一绘制。这样视角/区块边界不会让整栋模型消失。
 *
 * <p>扫描区块里的全部 BE 很贵，所以 controller 列表最多每 20 tick 刷新一次，
 * 每帧只按缓存的坐标取 BE。</p>
 */
public final class InfernalBoilerWorldRenderer {

    private static final List<BlockPos> CONTROLLERS = new ArrayList<>();
    private static ClientLevel cachedLevel;
    private static long lastScanTick = Long.MIN_VALUE;

    private InfernalBoilerWorldRenderer() {
    }

    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) {
            // 断开连接后释放旧世界的引用。
            cachedLevel = null;
            CONTROLLERS.clear();
            return;
        }
        Camera camera = event.getCamera();
        Vec3 cameraPos = camera.getPosition();
        BlockPos cameraBlock = camera.getBlockPosition();
        int radius = Math.min(12, minecraft.options.getEffectiveRenderDistance());
        PoseStack pose = event.getPoseStack();
        MultiBufferSource.BufferSource bufferSource = minecraft.renderBuffers().bufferSource();

        long gameTime = level.getGameTime();
        if (level != cachedLevel || gameTime < lastScanTick
                || gameTime - lastScanTick >= 20L) {
            cachedLevel = level;
            lastScanTick = gameTime;
            refreshControllers(level, cameraBlock, radius);
        }

        for (BlockPos pos : CONTROLLERS) {
            if (!(level.getBlockEntity(pos) instanceof InfernalBoilerBlockEntity boiler)
                    || !boiler.isFormedMultiBlock()) {
                continue;
            }
            // 正常情况下由 BER 绘制（保留之前的正确外观）；
            // 只有 controller 所在 section 被裁剪、BER 没跑时才全局补画。
            if (InfernalBoilerRenderer.wasRenderedThisFrame(boiler.getBlockPos())) {
                continue;
            }
            BlockPos origin = boiler.boilerOrigin();
            AABB bounds = new AABB(origin.getX(), origin.getY(), origin.getZ(),
                    origin.getX() + 4.0D, origin.getY() + 5.0D, origin.getZ() + 4.0D)
                    .inflate(0.5D);
            if (!event.getFrustum().isVisible(bounds)) {
                continue;
            }
            int light = LevelRenderer.getLightColor(level, origin.offset(1, 2, 1));
            InfernalBoilerRenderer.renderAt(boiler, pose, cameraPos, bufferSource, light);
        }

        bufferSource.endBatch(RenderType.cutout());
        bufferSource.endBatch(RenderType.translucent());
        InfernalBoilerRenderer.clearFrameCache();
    }

    private static void refreshControllers(ClientLevel level, BlockPos cameraBlock, int radius) {
        CONTROLLERS.clear();
        int centerX = cameraBlock.getX() >> 4;
        int centerZ = cameraBlock.getZ() >> 4;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                LevelChunk chunk = level.getChunkSource().getChunk(centerX + dx, centerZ + dz, false);
                if (chunk == null) {
                    continue;
                }
                for (Map.Entry<BlockPos, ?> entry : chunk.getBlockEntities().entrySet()) {
                    if (entry.getValue() instanceof InfernalBoilerBlockEntity boiler
                            && boiler.isController()) {
                        CONTROLLERS.add(boiler.getBlockPos());
                    }
                }
            }
        }
    }
}
