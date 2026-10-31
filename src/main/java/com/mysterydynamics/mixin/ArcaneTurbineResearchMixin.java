package com.mysterydynamics.mixin;

import net.minecraft.world.entity.player.Player;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.research.ResearchApi;
import thaumcraft.common.research.TCResearchManager;
import thaumcraft.common.research.TCResearchManager.RequestResult;

/**
 * 奥法涡轮机研究：点击直接扣除研究点并完成，不生成研究笔记。
 */
@Mixin(value = TCResearchManager.class, remap = false)
public abstract class ArcaneTurbineResearchMixin {

    private static final String KEY = "ARCANE_TURBINE";
    private static final int COST = 8;

    @Inject(method = "validateRequest", at = @At("HEAD"), cancellable = true)
    private static void mysterydynamics$directPurchase(Player player, String key,
                                                       CallbackInfoReturnable<RequestResult> cir) {
        if (!KEY.equals(key)) {
            return;
        }
        if (ResearchApi.isComplete(player, KEY)) {
            cir.setReturnValue(RequestResult.ALREADY_COMPLETE);
            return;
        }
        if (!ResearchApi.isComplete(player, "INFERNAL_BOILER")) {
            cir.setReturnValue(RequestResult.MISSING_REQUISITES);
            return;
        }
        if (!hasAll(player)) {
            cir.setReturnValue(RequestResult.INSUFFICIENT_ASPECTS);
            return;
        }
        TCResearchManager.consumeAspectPool(player, Aspect.MECHANISM, COST);
        TCResearchManager.consumeAspectPool(player, Aspect.CRAFT, COST);
        TCResearchManager.consumeAspectPool(player, Aspect.TOOL, COST);
        TCResearchManager.complete(player, KEY, true);
        cir.setReturnValue(RequestResult.COMPLETED);
    }

    private static boolean hasAll(Player player) {
        return TCResearchManager.getAspectPool(player, Aspect.MECHANISM) >= COST
                && TCResearchManager.getAspectPool(player, Aspect.CRAFT) >= COST
                && TCResearchManager.getAspectPool(player, Aspect.TOOL) >= COST;
    }
}
