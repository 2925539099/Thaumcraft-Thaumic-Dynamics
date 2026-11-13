package com.mysterydynamics.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mysterydynamics.villager.ModVillagers;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.VillagerProfessionLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.Villager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 神秘使等模组职业跳过生物群系服装层和职业服装层，只保留腰牌。
 * 底层已由 MysticVillagerRenderer 替换为干净皮肤。
 */
@Mixin(VillagerProfessionLayer.class)
public abstract class VillagerProfessionLayerMixin {

    @WrapOperation(
            method = "render",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/entity/layers/VillagerProfessionLayer;renderColoredCutoutModel(Lnet/minecraft/client/model/EntityModel;Lnet/minecraft/resources/ResourceLocation;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/LivingEntity;I)V"))
    private static void mystery$onlyBadge(EntityModel<?> model, ResourceLocation texture, PoseStack pose,
                                           MultiBufferSource buffer, int light, LivingEntity entity, int color,
                                           Operation<Void> original) {
        if (entity instanceof Villager villager && isMysteryProfession(villager)) {
            String path = texture.getPath();
            if (path.contains("/type/") || path.contains("/profession/")) {
                return;
            }
        }
        original.call(model, texture, pose, buffer, light, entity, color);
    }

    private static boolean isMysteryProfession(Villager villager) {
        var profession = villager.getVillagerData().getProfession();
        return profession == ModVillagers.THAUMATURGE.get()
                || profession == ModVillagers.ALCHEMIST.get()
                || profession == ModVillagers.MECHANIST.get();
    }
}
