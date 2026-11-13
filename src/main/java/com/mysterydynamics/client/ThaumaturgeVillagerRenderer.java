package com.mysterydynamics.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mysterydynamics.MysteryDynamics;
import com.mysterydynamics.villager.ModVillagers;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.VillagerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.npc.Villager;

/**
 * 神秘使使用玩家皮肤布局渲染；其他职业交给原版村民渲染器。
 * 这样可以避免把玩家皮肤硬套到村民 UV 上产生错位袍子。
 */
public class ThaumaturgeVillagerRenderer extends VillagerRenderer {

    private static final ResourceLocation SKIN =
            ResourceLocation.fromNamespaceAndPath(MysteryDynamics.MODID,
                    "textures/entity/villager/profession/thaumaturge.png");

    private final PlayerModel<Villager> playerModel;

    public ThaumaturgeVillagerRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.playerModel = new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false);
    }

    @Override
    public void render(Villager villager, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight) {
        if (!villager.isBaby()
                && villager.getVillagerData().getProfession() == ModVillagers.THAUMATURGE.get()) {
            poseStack.pushPose();
            playerModel.young = false;
            playerModel.crouching = villager.isCrouching();
            playerModel.riding = villager.isPassenger();
            playerModel.setupAnim(villager, 0.0F, 0.0F, villager.tickCount + partialTick,
                    villager.getYHeadRot() - villager.yBodyRot, villager.getXRot());
            VertexConsumer consumer = buffer.getBuffer(RenderType.entityCutoutNoCull(SKIN));
            playerModel.renderToBuffer(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY, -1);
            poseStack.popPose();
            return;
        }
        super.render(villager, entityYaw, partialTick, poseStack, buffer, packedLight);
    }
}
