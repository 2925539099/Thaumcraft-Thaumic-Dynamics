package com.mysterydynamics.item;

import com.mysterydynamics.MysteryDynamics;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;

import thaumcraft.api.IRepairable;
import thaumcraft.common.items.TCItemBehaviors;

import java.util.List;

/**
 * 旅行者战靴：神秘要塞甲胄套装第四件，同时继承旅行者之靴的移动机制。
 * 继承 TC 的 TravellerBootsItem，使原版旅行者靴的 tick、跳跃、水中移动等逻辑直接生效。
 */
public class TravelerBootsItem extends TCItemBehaviors.TravellerBootsItem implements IRepairable {

    private static final ResourceLocation ARMOR_MODIFIER =
            ResourceLocation.fromNamespaceAndPath("mystery_dynamics", "traveler_boots_armor");

    public TravelerBootsItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public Holder<ArmorMaterial> getMaterial() {
        return MysteryDynamics.TRAVELER_BOOTS_MATERIAL;
    }

    @Override
    public int getDefense() {
        return 3;
    }

    @Override
    public float getToughness() {
        return 0.0F;
    }

    @Override
    public ItemAttributeModifiers getDefaultAttributeModifiers() {
        return ItemAttributeModifiers.builder()
                .add(Attributes.ARMOR,
                        new AttributeModifier(ARMOR_MODIFIER, 3.0D,
                                AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.FEET)
                .add(Attributes.ARMOR_TOUGHNESS,
                        new AttributeModifier(ARMOR_MODIFIER.withSuffix("_toughness"), 0.0D,
                                AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.FEET)
                .build();
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.mystery_dynamics.traveler_boots.set")
                .withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("tooltip.mystery_dynamics.traveler_boots.full_set")
                .withStyle(ChatFormatting.DARK_PURPLE));
    }
}
