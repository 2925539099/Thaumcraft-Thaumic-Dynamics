package com.mysterydynamics.mixin;

import com.mysterydynamics.item.HighPressureAmmo;
import com.mysterydynamics.item.HighPressureLauncherItem;
import com.simibubi.create.content.equipment.potatoCannon.PotatoCannonItem;

import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Predicate;

/**
 * 土豆加农炮不识别高压动力发射器的专属弹药，也不显示其角标。
 *
 * <p>在“可用弹药”判定层过滤：Player#getProjectile 会跳过专属弹药继续寻找土豆等普通弹药，
 * 而不是找到专属弹药后整体判空导致无法开火。高压动力发射器自身不受影响。</p>
 */
@Mixin(PotatoCannonItem.class)
public abstract class PotatoCannonAmmoMixin {

    @Inject(method = "getAllSupportedProjectiles", at = @At("RETURN"), cancellable = true)
    private void mystery$excludeHighPressureAmmo(CallbackInfoReturnable<Predicate<ItemStack>> cir) {
        if ((Object) this instanceof HighPressureLauncherItem) {
            return;
        }
        Predicate<ItemStack> original = cir.getReturnValue();
        cir.setReturnValue(stack -> !HighPressureAmmo.isAmmo(stack) && original.test(stack));
    }
}
