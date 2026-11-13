package com.mysterydynamics.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mysterydynamics.MysteryDynamics;
import com.mysterydynamics.villager.ModVillagers;

import net.minecraft.client.model.VillagerModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.VillagerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.npc.Villager;

/**
 * 模组村民职业使用村民模型 + 专用村民 UV 皮肤直接渲染。
 * 跳过原版 VillagerProfessionLayer，因此不会出现职业服装第二层和等级腰牌。
 */
public class MysteryProfessionRenderer extends VillagerRenderer {

    private static final ResourceLocation THAUMATURGE = skin("thaumaturge");
    private static final ResourceLocation ALCHEMIST = skin("alchemist");
    private static final ResourceLocation MECHANIST = skin("mechanist");
    private static final ResourceLocation MECHANIST_LOW = skin("mechanist_low");

    public MysteryProfessionRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    private static ResourceLocation skin(String name) {
        return ResourceLocation.fromNamespaceAndPath(MysteryDynamics.MODID,
                "textures/entity/villager/profession/" + name + "_villager.png");
    }

    private ResourceLocation skinFor(Villager villager) {
        var profession = villager.getVillagerData().getProfession();
        if (profession == ModVillagers.THAUMATURGE.get()) return THAUMATURGE;
        if (profession == ModVillagers.ALCHEMIST.get()) return ALCHEMIST;
        if (profession == ModVillagers.MECHANIST.get()) {
            return villager.getVillagerData().getLevel() >= 5 ? MECHANIST : MECHANIST_LOW;
        }
        return null;
    }

    @Override
    public void render(Villager villager, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight) {
        ResourceLocation skin = villager.isBaby() ? null : skinFor(villager);
        if (skin == null) {
            super.render(villager, entityYaw, partialTick, poseStack, buffer, packedLight);
            return;
        }
        VillagerModel<Villager> model = getModel();
        model.setupAnim(villager,
                villager.walkAnimation.position(partialTick),
                villager.walkAnimation.speed(partialTick),
                villager.tickCount + partialTick,
                villager.getYHeadRot() - villager.yBodyRot,
                villager.getXRot());
        model.hatVisible(true);
        VertexConsumer consumer = buffer.getBuffer(RenderType.entityCutoutNoCull(skin));
        model.renderToBuffer(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY, -1);
    }
}
