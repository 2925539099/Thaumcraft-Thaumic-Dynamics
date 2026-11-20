package com.mysterydynamics.client;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

import thaumcraft.common.items.FortressArmorItem;

/** 四件神秘要塞甲胄齐全时，额外渲染旅行者战靴的护腿层。 */
public class TravelerBootsSetLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            "mystery_dynamics", "textures/models/armor/traveler_boots_layer_2.png");

    private final HumanoidModel<AbstractClientPlayer> model;

    public TravelerBootsSetLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent,
                                 EntityModelSet models) {
        super(parent);
        this.model = new HumanoidModel<>(models.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR));
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight,
                       AbstractClientPlayer player, float limbSwing, float limbSwingAmount,
                       float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        if (!hasFullFortressSet(player)) {
            return;
        }

        PlayerModel<AbstractClientPlayer> parent = getParentModel();
        parent.copyPropertiesTo(model);
        model.young = parent.young;
        model.crouching = parent.crouching;
        model.riding = parent.riding;
        model.setAllVisible(false);
        model.leftLeg.visible = true;
        model.rightLeg.visible = true;

        model.renderToBuffer(poseStack,
                bufferSource.getBuffer(RenderType.armorCutoutNoCull(TEXTURE)),
                packedLight, OverlayTexture.NO_OVERLAY);
    }

    private static boolean hasFullFortressSet(AbstractClientPlayer player) {
        for (EquipmentSlot slot : new EquipmentSlot[]{
                EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack stack = player.getItemBySlot(slot);
            if (!FortressArmorItem.isFortressPiece(stack)) {
                return false;
            }
        }
        return true;
    }
}
