package com.mysterydynamics.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mysterydynamics.MysteryDynamics;
import com.mysterydynamics.villager.ModVillagers;

import net.minecraft.client.model.VillagerModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.npc.Villager;

/** 神秘使村民的独立皮肤层，确保使用随模组提供的 64×64 皮肤。 */
public class ThaumaturgeSkinLayer extends RenderLayer<Villager, VillagerModel<Villager>> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MysteryDynamics.MODID,
                    "textures/entity/villager/profession/thaumaturge.png");

    public ThaumaturgeSkinLayer(RenderLayerParent<Villager, VillagerModel<Villager>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, Villager villager,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
                       float netHeadYaw, float headPitch) {
        if (villager.isBaby()
                || villager.getVillagerData().getProfession() != ModVillagers.THAUMATURGE.get()) {
            return;
        }
        VillagerModel<Villager> model = getParentModel();
        model.hatVisible(true);
        renderColoredCutoutModel(model, TEXTURE, poseStack, buffer, packedLight, villager, -1);
    }
}
