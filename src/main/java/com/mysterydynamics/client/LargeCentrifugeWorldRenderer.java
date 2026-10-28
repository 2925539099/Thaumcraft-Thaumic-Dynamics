package com.mysterydynamics.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mysterydynamics.block.LargeCentrifugeControllerBlockEntity;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import java.util.Map;

/** 3×3×3 离心机不挂在单个方块截面的视锥裁剪上；视角偏离时由关卡阶段补画。 */
public final class LargeCentrifugeWorldRenderer {

    private LargeCentrifugeWorldRenderer() {
    }

    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) {
            return;
        }
        Camera camera = event.getCamera();
        Vec3 cameraPos = camera.getPosition();
        BlockPos cameraBlock = camera.getBlockPosition();
        int centerX = cameraBlock.getX() >> 4;
        int centerZ = cameraBlock.getZ() >> 4;
        int radius = Math.min(6, minecraft.options.getEffectiveRenderDistance());
        PoseStack pose = event.getPoseStack();
        MultiBufferSource.BufferSource bufferSource = minecraft.renderBuffers().bufferSource();

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                LevelChunk chunk = level.getChunkSource().getChunk(centerX + dx, centerZ + dz, false);
                if (chunk == null) {
                    continue;
                }
                for (Map.Entry<BlockPos, ?> entry : chunk.getBlockEntities().entrySet()) {
                    if (!(entry.getValue() instanceof LargeCentrifugeControllerBlockEntity controller)) {
                        continue;
                    }
                    if (LargeCentrifugeRenderer.wasRenderedThisFrame(controller.getBlockPos())) {
                        continue;
                    }
                    BlockPos pos = controller.getBlockPos();
                    AABB bounds = new AABB(pos.getX() - 1.0D, pos.getY() - 1.0D, pos.getZ() - 1.0D,
                            pos.getX() + 2.0D, pos.getY() + 2.0D, pos.getZ() + 2.0D).inflate(0.5D);
                    if (!event.getFrustum().isVisible(bounds)) {
                        continue;
                    }
                    int light = LevelRenderer.getLightColor(level, pos.above());
                    LargeCentrifugeRenderer.renderAt(controller,
                            event.getPartialTick().getGameTimeDeltaPartialTick(false),
                            pose, cameraPos, bufferSource, light);
                }
            }
        }

        bufferSource.endBatch(RenderType.entityCutoutNoCull(InventoryMenu.BLOCK_ATLAS));
        bufferSource.endBatch(RenderType.entityTranslucent(InventoryMenu.BLOCK_ATLAS));
        LargeCentrifugeRenderer.clearFrameCache();
    }
}
