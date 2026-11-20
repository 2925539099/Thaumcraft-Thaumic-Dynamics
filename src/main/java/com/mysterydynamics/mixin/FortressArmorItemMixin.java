package com.mysterydynamics.mixin;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import thaumcraft.common.items.FortressArmorData;
import thaumcraft.common.items.FortressArmorItem;

/** 将靴子纳入神秘要塞甲胄的套装防御与倍率计算。 */
@Mixin(FortressArmorItem.class)
public class FortressArmorItemMixin {

    @Inject(method = "protectionMultiplier", at = @At("RETURN"), cancellable = true)
    private static void mysterydynamics$addBootsMultiplier(Player player,
                                                           CallbackInfoReturnable<Float> cir) {
        ItemStack feet = player.getItemBySlot(EquipmentSlot.FEET);
        if (!FortressArmorItem.isFortressPiece(feet)) {
            return;
        }
        float bonus = 0.125F;
        if (FortressArmorData.mask(feet) >= 0) {
            bonus += 0.05F;
        }
        cir.setReturnValue(cir.getReturnValueF() + bonus);
    }

    @Inject(method = "fortressDefense", at = @At("RETURN"), cancellable = true)
    private static void mysterydynamics$addBootsDefense(Player player,
                                                        CallbackInfoReturnable<Integer> cir) {
        ItemStack feet = player.getItemBySlot(EquipmentSlot.FEET);
        if (FortressArmorItem.isFortressPiece(feet)
                && feet.getItem() instanceof ArmorItem armor) {
            cir.setReturnValue(cir.getReturnValueI() + armor.getDefense());
        }
    }
}
