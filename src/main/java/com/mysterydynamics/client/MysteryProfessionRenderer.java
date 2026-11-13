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

/** 模组村民职业使用玩家皮肤布局渲染；其他职业交给原版村民渲染器。 */
public class MysteryProfessionRenderer extends VillagerRenderer {

    private static final ResourceLocation THAUMATURGE =
            skin("thaumaturge");
    private static final ResourceLocation ALCHEMIST =
            skin("alchemist");
    private static final ResourceLocation MECHANIST =
            skin("mechanist");
    private static final ResourceLocation MECHANIST_LOW =
            skin("mechanist_low");

    private final PlayerModel<Villager> playerModel;

    public MysteryProfessionRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.playerModel = new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false);
    }

    private static ResourceLocation skin(String name) {
        return ResourceLocation.fromNamespaceAndPath(MysteryDynamics.MODID,
                "textures/entity/villager/profession/" + name + ".png");
    }

    private ResourceLocation skinFor(Villager villager) {
        var profession = villager.getVillagerData().getProfession();
        if (profession == ModVillagers.THAUMATURGE.get()) {
            return THAUMATURGE;
        }
        if (profession == ModVillagers.ALCHEMIST.get()) {
            return ALCHEMIST;
        }
        if (profession == ModVillagers.MECHANIST.get()) {
            return villager.getVillagerData().getLevel() >= 5 ? MECHANIST : MECHANIST_LOW;
        }
        return null;
    }

    @Override
    public void render(Villager villager, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight) {
        ResourceLocation skin = villager.isBaby() ? null : skinFor(villager);
        if (skin != null) {
            poseStack.pushPose();
            playerModel.young = false;
            playerModel.crouching = villager.isCrouching();
            playerModel.riding = villager.isPassenger();
            playerModel.setupAnim(villager,
                    villager.walkAnimation.position(partialTick),
                    villager.walkAnimation.speed(partialTick),
                    villager.tickCount + partialTick,
                    villager.getYHeadRot() - villager.yBodyRot,
                    villager.getXRot());
            VertexConsumer consumer = buffer.getBuffer(RenderType.entityCutoutNoCull(skin));
            playerModel.renderToBuffer(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY, -1);
            poseStack.popPose();
            return;
        }
        super.render(villager, entityYaw, partialTick, poseStack, buffer, packedLight);
    }
}
