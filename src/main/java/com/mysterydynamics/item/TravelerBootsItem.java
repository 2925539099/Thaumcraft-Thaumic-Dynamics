package com.mysterydynamics.item;

import com.mysterydynamics.MysteryDynamics;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** 旅行者战靴：神秘要塞甲胄套装的第四件，计入套装倍率。 */
public class TravelerBootsItem extends ArmorItem {

    public TravelerBootsItem(Item.Properties properties) {
        super(MysteryDynamics.TRAVELER_BOOTS_MATERIAL, Type.BOOTS, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.mystery_dynamics.traveler_boots.set")
                .withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("tooltip.mystery_dynamics.traveler_boots.full_set")
                .withStyle(ChatFormatting.DARK_PURPLE));
    }
}
