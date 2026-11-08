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
    /** 护目镜标签只在这个距离内绘制（穿墙显示，过远只会造成视觉噪音）。 */
    private static final double LABEL_DISTANCE_SQ = 16.0D * 16.0D;
    private static final int FULL_BRIGHT = 0xF000F0;

    private LargeCentrifugeWorldRenderer() {
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
            if (pos.distToCenterSqr(cameraPos) <= LABEL_DISTANCE_SQ) {
                drawGoggleLabel(controller, pose, camera, bufferSource);
            }
        }

        bufferSource.endBatch(RenderType.entityCutoutNoCull(InventoryMenu.BLOCK_ATLAS));
        bufferSource.endBatch(RenderType.entityTranslucent(InventoryMenu.BLOCK_ATLAS));
        // 字体使用 textSeeThrough，必须在这里刷掉，否则标签会一直留在批处理里。
        bufferSource.endBatch();
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
                                        Camera camera, MultiBufferSource buffers) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || !GogglesItem.isWearingGoggles(minecraft.player)) {
            return;
        }
        BlockPos pos = controller.getBlockPos();
        Vec3 cameraPos = camera.getPosition();
        Aspect in = controller.inputAspect();
        Component name = in == null
                ? Component.translatable("mystery_dynamics.large_centrifuge.empty")
                : Component.translatable("tc.aspect." + in.getTag());
        int color = in == null ? 0xFFAAAAAA : (0xFF000000 | (in.getColor() & 0xFFFFFF));
        Component text = Component.empty().append(name).append(" " + controller.storedInput()
                + " / " + LargeCentrifugeControllerBlockEntity.CAPACITY);
        Component sub = controller.statusComponent();
        boolean slow = controller.speed() < LargeCentrifugeControllerBlockEntity.MIN_RPM;
        pose.pushPose();
        pose.translate(pos.getX() + 0.5D - cameraPos.x,
                pos.getY() + 1.95D - cameraPos.y,
                pos.getZ() + 0.5D - cameraPos.z);
        pose.mulPose(camera.rotation());
        pose.scale(-0.022F, -0.022F, 0.022F);
        var font = minecraft.font;
        var matrix = pose.last().pose();
        font.drawInBatch(text, -font.width(text) / 2.0F, 0, color, false,
                matrix, buffers, Font.DisplayMode.SEE_THROUGH, 0x66000000, FULL_BRIGHT);
        int subColor = slow ? 0xFFFF5555 : 0xFFAAAAAA;
        font.drawInBatch(sub, -font.width(sub) / 2.0F, 11, subColor, false,
                matrix, buffers, Font.DisplayMode.SEE_THROUGH, 0x66000000, FULL_BRIGHT);
        pose.popPose();
    }
}
