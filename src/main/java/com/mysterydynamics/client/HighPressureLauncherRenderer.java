package com.mysterydynamics.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.mysterydynamics.MysteryDynamics;
import com.simibubi.create.foundation.item.render.CustomRenderedItemModel;
import com.simibubi.create.foundation.item.render.CustomRenderedItemModelRenderer;
import com.simibubi.create.foundation.item.render.PartialItemModelRenderer;

import net.createmod.catnip.animation.AnimationTickHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** 高压动力发射器：主体静态，齿轮按土豆加农炮的速度持续旋转。 */
public class HighPressureLauncherRenderer extends CustomRenderedItemModelRenderer {

    private static final ModelResourceLocation COG = ModelResourceLocation.standalone(
            ResourceLocation.fromNamespaceAndPath(MysteryDynamics.MODID,
                    "item/high_pressure_launcher_cog"));

    @Override
    protected void render(ItemStack stack, CustomRenderedItemModel model,
                          PartialItemModelRenderer partial, ItemDisplayContext transformType,
                          PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        partial.render(model.getOriginalModel(), light);

        BakedModel cog = Minecraft.getInstance().getModelManager().getModel(COG);
        float angle = AnimationTickHolder.getRenderTime() * -2.5F % 360.0F;

        ms.pushPose();
        ms.translate(0.0F, 0.03125F, 0.0F);
        ms.mulPose(Axis.ZP.rotationDegrees(angle));
        ms.translate(0.0F, -0.03125F, 0.0F);
        partial.render(cog, light);
        ms.popPose();
    }
}
