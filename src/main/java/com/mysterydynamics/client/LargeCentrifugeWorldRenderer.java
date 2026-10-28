package com.mysterydynamics.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mysterydynamics.block.LargeCentrifugeControllerBlockEntity;
import com.mysterydynamics.compat.LargeCentrifugeStructure;
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
import thaumcraft.api.aspects.Aspect;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** 3×3×3 离心机不挂在单个方块截面的视锥裁剪上；视角偏离时由关卡阶段补画。 */
public final class LargeCentrifugeWorldRenderer {

    private static final List<BlockPos> CONTROLLERS = new ArrayList<>();
    private static ClientLevel cachedLevel;
    private static long lastScanTick = Long.MIN_VALUE;

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
        int radius = Math.min(6, minecraft.options.getEffectiveRenderDistance());
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
            if (!(level.getBlockEntity(pos) instanceof LargeCentrifugeControllerBlockEntity controller)) {
                continue;
            }
            AABB bounds = new AABB(pos.getX() - 1.0D, pos.getY() - 1.0D, pos.getZ() - 1.0D,
                    pos.getX() + 2.0D, pos.getY() + 2.0D, pos.getZ() + 2.0D).inflate(0.5D);
            if (!event.getFrustum().isVisible(bounds)) {
                continue;
            }
            int light = LevelRenderer.getLightColor(level, pos.above());
            if (!LargeCentrifugeRenderer.wasRenderedThisFrame(pos)) {
                LargeCentrifugeRenderer.renderAt(controller,
                        event.getPartialTick().getGameTimeDeltaPartialTick(false),
                        pose, cameraPos, bufferSource, light);
            }
            drawGoggleLabel(controller, pose, camera, bufferSource, light);
        }

        bufferSource.endBatch(RenderType.entityCutoutNoCull(InventoryMenu.BLOCK_ATLAS));
        bufferSource.endBatch(RenderType.entityTranslucent(InventoryMenu.BLOCK_ATLAS));
        LargeCentrifugeRenderer.clearFrameCache();
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
                    if (entry.getValue() instanceof LargeCentrifugeControllerBlockEntity controller) {
                        CONTROLLERS.add(controller.getBlockPos());
                    }
                }
            }
        }
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
        Aspect in = controller.inputAspect();
        String name = in == null ? "空" : Component.translatable("tc.aspect." + in.getTag()).getString();
        int color = in == null ? 0xFFAAAAAA : (0xFF000000 | (in.getColor() & 0xFFFFFF));
        Component text = Component.literal(name + " " + controller.storedInput() + " / 24");
        float speed = controller.speed();
        Component sub = Component.literal(speed < 32.0F
                        ? "转速不足 · 需要 32 RPM"
                        : controller.isProcessing()
                        ? "离心中 · 输出 " + controller.storedOutput()
                        : controller.storedOutput() > 0
                        ? "输出中 " + controller.storedOutput() : "待输入")
                .withStyle(speed < 32.0F ? ChatFormatting.RED : ChatFormatting.GRAY);
        pose.pushPose();
        pose.translate(pos.getX() + 0.5D - cameraPos.x,
                pos.getY() + 1.95D - cameraPos.y,
                pos.getZ() + 0.5D - cameraPos.z);
        pose.mulPose(camera.rotation());
        pose.scale(-0.022F, -0.022F, 0.022F);
        var font = minecraft.font;
        var matrix = pose.last().pose();
        font.drawInBatch(text, -font.width(text) / 2.0F, 0, color, false,
                matrix, buffers, Font.DisplayMode.SEE_THROUGH, 0x66000000, light);
        int subColor = speed < 32.0F ? 0xFFFF5555 : 0xFFAAAAAA;
        font.drawInBatch(sub, -font.width(sub) / 2.0F, 11, subColor, false,
                matrix, buffers, Font.DisplayMode.SEE_THROUGH, 0x66000000, light);
        pose.popPose();

        // 九个基座方块各自显示一份源质信息；不依赖旧存档里的 BlockEntity。
        Component base = Component.literal((in == null ? "空" : name)
                + " " + controller.storedInput() + "/24");
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                BlockPos basePos = LargeCentrifugeStructure.world(pos, x, -1, z,
                        controller.rotation());
                drawTextAt(base, basePos.getX() + 0.5D, basePos.getY() + 1.12D,
                        basePos.getZ() + 0.5D, pose, camera, font, buffers, light, color, 0.015F);
            }
        }
    }

    private static void drawTextAt(Component text, double x, double y, double z,
                                   PoseStack pose, Camera camera, Font font,
                                   MultiBufferSource buffers, int light, int color, float scale) {
        Vec3 cameraPos = camera.getPosition();
        pose.pushPose();
        pose.translate(x - cameraPos.x, y - cameraPos.y, z - cameraPos.z);
        pose.mulPose(camera.rotation());
        pose.scale(-scale, -scale, scale);
        var matrix = pose.last().pose();
        font.drawInBatch(text, -font.width(text) / 2.0F, 0, color, false,
                matrix, buffers, Font.DisplayMode.SEE_THROUGH, 0x55000000, light);
        pose.popPose();
    }
}
