package com.mysterydynamics.item;

import com.mysterydynamics.MysteryDynamics;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** 涅里伊德之庇：水下呼吸、清晰水下视野、水下挖掘加速。 */
public class NeriidsProtectionItem extends ArmorItem {

    public NeriidsProtectionItem(Item.Properties properties) {
        super(MysteryDynamics.NERIIDS_PROTECTION_MATERIAL, Type.HELMET, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip,
                                TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.mystery_dynamics.neriids_protection")
                .withStyle(ChatFormatting.AQUA));
    }
}
