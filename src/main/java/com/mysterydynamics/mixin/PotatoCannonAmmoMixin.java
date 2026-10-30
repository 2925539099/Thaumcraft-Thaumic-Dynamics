package com.mysterydynamics.mixin;

import com.mysterydynamics.item.HighPressureAmmo;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.equipment.potatoCannon.PotatoCannonItem;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 土豆加农炮不识别高压动力发射器的专属弹药，也不显示其角标。 */
@Mixin(PotatoCannonItem.class)
public abstract class PotatoCannonAmmoMixin {

    @Inject(method = "getAmmo", at = @At("RETURN"), cancellable = true)
    private static void mystery$hideHighPressureAmmo(Player player, ItemStack cannon,
                                                     CallbackInfoReturnable<PotatoCannonItem.Ammo> cir) {
        PotatoCannonItem.Ammo ammo = cir.getReturnValue();
        if (ammo == null) {
            return;
        }
        if (cannon.is(AllItems.POTATO_CANNON.get()) && HighPressureAmmo.isAmmo(ammo.stack())) {
            cir.setReturnValue(null);
        }
    }
}
