package com.mysterydynamics.client;

import com.mysterydynamics.MysteryDynamics;
import com.mysterydynamics.villager.ModVillagers;

import net.minecraft.client.model.VillagerModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.npc.Villager;

import com.mojang.blaze3d.vertex.PoseStack;

/** 机械师 1~4 级时覆盖职业皮肤；5 级使用默认的满级皮肤。 */
public class MechanistSkinLayer extends RenderLayer<Villager, VillagerModel<Villager>> {

    private static final ResourceLocation LOW_LEVEL =
            ResourceLocation.fromNamespaceAndPath(MysteryDynamics.MODID,
                    "textures/entity/villager/profession/mechanist_low.png");

    public MechanistSkinLayer(RenderLayerParent<Villager, VillagerModel<Villager>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, Villager villager,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
                       float netHeadYaw, float headPitch) {
        if (villager.isBaby()
                || villager.getVillagerData().getProfession() != ModVillagers.MECHANIST.get()
                || villager.getVillagerData().getLevel() >= 5) {
            return;
        }
        VillagerModel<Villager> model = getParentModel();
        model.hatVisible(true);
        renderColoredCutoutModel(model, LOW_LEVEL, poseStack, buffer, packedLight, villager, -1);
    }
}
