package com.mysterydynamics.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import thaumcraft.client.gui.ThaumonomiconPagePlan;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Thaumonomicon 的页面布局来自 Thaumcraft 自带的 research_page_plan.json，
 * 自定义研究不在其中，CONSTRUCT/NORMAL 页会被直接丢弃。
 */
@Mixin(value = ThaumonomiconPagePlan.class, remap = false)
public abstract class ThaumonomiconPagePlanMixin {

    @Mutable
    @Shadow
    @Final
    private static Map<String, List<ThaumonomiconPagePlan.Kind>> CANONICAL;

    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void mysterydynamics$addResearchLayouts(CallbackInfo ci) {
        Map<String, List<ThaumonomiconPagePlan.Kind>> mutable = new LinkedHashMap<>(CANONICAL);
        mutable.put("INFERNAL_BOILER", List.of(
                ThaumonomiconPagePlan.Kind.TEXT,
                ThaumonomiconPagePlan.Kind.CONSTRUCT));
        mutable.put("REVEALING_ENGINEER_GOGGLES", List.of(
                ThaumonomiconPagePlan.Kind.TEXT,
                ThaumonomiconPagePlan.Kind.NORMAL));
        mutable.put("ARCANE_TURBINE", List.of(
                ThaumonomiconPagePlan.Kind.TEXT,
                ThaumonomiconPagePlan.Kind.ARCANE));
        CANONICAL = mutable;
    }
}
