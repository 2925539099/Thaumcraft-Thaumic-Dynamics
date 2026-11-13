package com.mysterydynamics.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mysterydynamics.MysteryDynamics;
import com.mysterydynamics.block.EssentiaIntegrationBlockEntity;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

/** 成型后由控制器统一绘制整个 3×3×3 结构。 */
public class EssentiaIntegrationRenderer implements BlockEntityRenderer<EssentiaIntegrationBlockEntity> {

    public EssentiaIntegrationRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(EssentiaIntegrationBlockEntity blockEntity, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {
        if (!blockEntity.isFormed()) {
            return;
        }
        poseStack.pushPose();
        // 模型坐标为 -16..32，控制器位于结构底层中心，平移一格后覆盖 0..3 格。
        poseStack.translate(1.0D, 1.0D, 1.0D);
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(
                MysteryDynamics.ESSENTIA_INTEGRATION_STATION_MODEL.get().defaultBlockState(),
                poseStack, buffer, packedLight, packedOverlay);
        poseStack.popPose();
    }
}
