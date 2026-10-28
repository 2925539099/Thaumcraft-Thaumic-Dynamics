package com.mysterydynamics.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.mysterydynamics.block.LargeCentrifugeControllerBlockEntity;
import com.simibubi.create.content.equipment.goggles.GogglesItem;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
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
                    drawGoggleLabel(controller, pose, camera, bufferSource, light);
                }
            }
        }

        bufferSource.endBatch(RenderType.entityCutoutNoCull(InventoryMenu.BLOCK_ATLAS));
        bufferSource.endBatch(RenderType.entityTranslucent(InventoryMenu.BLOCK_ATLAS));
        LargeCentrifugeRenderer.clearFrameCache();
    }

    /** 与神秘时代源质罐/揭示护目镜类似：戴护目镜时在控制器上方显示内部源质。 */
    private static void drawGoggleLabel(LargeCentrifugeControllerBlockEntity controller, PoseStack pose,
                                        Camera camera, MultiBufferSource buffers, int light) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || !GogglesItem.isWearingGoggles(minecraft.player)) {
            return;
        }
        BlockPos pos = controller.getBlockPos();
        Vec3 cameraPos = camera.getPosition();
        Component text = Component.literal("源质 " + controller.storedInput() + " / 24")
                .withStyle(ChatFormatting.AQUA);
        Component sub = Component.literal(controller.isProcessing()
                        ? "离心中 · 输出 " + controller.storedOutput()
                        : controller.storedOutput() > 0 ? "输出中 " + controller.storedOutput() : "待输入")
                .withStyle(ChatFormatting.GRAY);
        pose.pushPose();
        pose.translate(pos.getX() + 0.5D - cameraPos.x,
                pos.getY() + 1.95D - cameraPos.y,
                pos.getZ() + 0.5D - cameraPos.z);
        pose.mulPose(camera.rotation());
        pose.scale(-0.022F, -0.022F, 0.022F);
        var font = minecraft.font;
        var matrix = pose.last().pose();
        font.drawInBatch(text, -font.width(text) / 2.0F, 0, 0xFFFFFFFF, false,
                matrix, buffers, Font.DisplayMode.SEE_THROUGH, 0x66000000, light);
        font.drawInBatch(sub, -font.width(sub) / 2.0F, 11, 0xFFFFFFFF, false,
                matrix, buffers, Font.DisplayMode.SEE_THROUGH, 0x66000000, light);
        pose.popPose();
    }
}
