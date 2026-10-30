package com.mysterydynamics.mixin;

import com.mysterydynamics.MysteryDynamics;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 高压动力发射器使用任何弹药时，装填时间都固定为 5 Tick。 */
@Mixin(com.simibubi.create.content.equipment.potatoCannon.PotatoCannonItem.class)
public abstract class PotatoCannonItemMixin {

    @Inject(method = "use", at = @At("RETURN"))
    private void mystery$forceReload(Level level, Player player, InteractionHand hand,
                                     CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir) {
        ItemStack held = player.getItemInHand(hand);
        if (!held.is(MysteryDynamics.HIGH_PRESSURE_LAUNCHER.get())) {
            return;
        }
        if (cir.getReturnValue().getResult() != InteractionResult.SUCCESS) {
            return;
        }
        player.getCooldowns().removeCooldown(held.getItem());
        player.getCooldowns().addCooldown(held.getItem(), 5);
    }
}
