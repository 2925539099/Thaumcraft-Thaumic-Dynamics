package com.mysterydynamics.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mysterydynamics.block.EssentiaIntegrationBlockEntity;

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

/** 与炼狱锅炉相同的视锥兜底：控制器离开视锥时仍绘制整座 3×3×3 模型。 */
public final class EssentiaIntegrationWorldRenderer {

    private static final List<BlockPos> CONTROLLERS = new ArrayList<>();
    private static ClientLevel cachedLevel;
    private static long lastScanTick = Long.MIN_VALUE;

    private EssentiaIntegrationWorldRenderer() {
    }

    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) {
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
        if (level != cachedLevel || gameTime < lastScanTick || gameTime - lastScanTick >= 20L) {
            cachedLevel = level;
            lastScanTick = gameTime;
            refreshControllers(level, cameraBlock, radius);
        }

        for (BlockPos pos : CONTROLLERS) {
            if (!(level.getBlockEntity(pos) instanceof EssentiaIntegrationBlockEntity station)
                    || !station.isFormed()) {
                continue;
            }
            if (EssentiaIntegrationRenderer.wasRenderedThisFrame(pos)) {
                continue;
            }
            AABB bounds = new AABB(pos.getX() - 1.0D, pos.getY(), pos.getZ() - 1.0D,
                    pos.getX() + 2.0D, pos.getY() + 3.0D, pos.getZ() + 2.0D).inflate(0.5D);
            if (!event.getFrustum().isVisible(bounds)) {
                continue;
            }
            int light = LevelRenderer.getLightColor(level, pos.offset(0, 1, 0));
            EssentiaIntegrationRenderer.renderAt(station, 0.0F, pose, cameraPos, bufferSource, light);
        }

        bufferSource.endBatch(RenderType.translucent());
        EssentiaIntegrationRenderer.clearFrameCache();
    }

    private static void refreshControllers(ClientLevel level, BlockPos cameraBlock, int radius) {
        CONTROLLERS.clear();
        int centerX = cameraBlock.getX() >> 4;
        int centerZ = cameraBlock.getZ() >> 4;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                LevelChunk chunk = level.getChunkSource().getChunk(centerX + dx, centerZ + dz, false);
                if (chunk == null) continue;
                for (Map.Entry<BlockPos, ?> entry : chunk.getBlockEntities().entrySet()) {
                    if (entry.getValue() instanceof EssentiaIntegrationBlockEntity station
                            && station.isFormed()) {
                        CONTROLLERS.add(station.getBlockPos());
                    }
                }
            }
        }
    }
}
