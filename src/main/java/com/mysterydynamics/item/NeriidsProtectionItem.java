package com.mysterydynamics.item;

import com.mysterydynamics.MysteryDynamics;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;

import java.util.List;

/** 涅里伊德之庇：水下呼吸、清晰水下视野、水下挖掘加速。 */
public class NeriidsProtectionItem extends ArmorItem {

    public NeriidsProtectionItem(Item.Properties properties) {
        super(MysteryDynamics.NERIIDS_PROTECTION_MATERIAL, Type.HELMET, properties);
    }

    @Override
    public ItemAttributeModifiers getDefaultAttributeModifiers(ItemStack stack) {
        return super.getDefaultAttributeModifiers(stack)
                .withModifierAdded(
                        Attributes.SUBMERGED_MINING_SPEED,
                        new AttributeModifier(
                                ResourceLocation.fromNamespaceAndPath(
                                        MysteryDynamics.MODID, "neriids_submerged_mining"),
                                9.0D,
                                AttributeModifier.Operation.ADD_MULTIPLIED_BASE),
                        EquipmentSlotGroup.HEAD);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip,
                                TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.mystery_dynamics.neriids_protection")
                .withStyle(ChatFormatting.AQUA));
    }
}
