package com.mysterydynamics.item;

import com.mysterydynamics.MysteryDynamics;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import java.util.List;

/** 涅里伊德之庇：水下呼吸、清晰水下视野、消除水下挖掘减缓。 */
public class NeriidsProtectionItem extends ArmorItem {

    public NeriidsProtectionItem(Item.Properties properties) {
        super(MysteryDynamics.NERIIDS_PROTECTION_MATERIAL, Type.HELMET, properties);
    }

    @Override
    public boolean supportsEnchantment(ItemStack stack, Holder<Enchantment> enchantment) {
        if (enchantment.is(Enchantments.AQUA_AFFINITY)) {
            return false;
        }
        return super.supportsEnchantment(stack, enchantment);
    }

    @Override
    public int getEnchantmentLevel(ItemStack stack, Holder<Enchantment> enchantment) {
        if (enchantment.is(Enchantments.AQUA_AFFINITY)) {
            return 1;
        }
        return super.getEnchantmentLevel(stack, enchantment);
    }

    @Override
    public ItemEnchantments getAllEnchantments(ItemStack stack,
                                               HolderLookup.RegistryLookup<Enchantment> lookup) {
        ItemEnchantments.Mutable enchants =
                new ItemEnchantments.Mutable(super.getAllEnchantments(stack, lookup));
        enchants.set(lookup.getOrThrow(Enchantments.AQUA_AFFINITY), 1);
        return enchants.toImmutable();
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip,
                                TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.mystery_dynamics.neriids_protection")
                .withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("tooltip.mystery_dynamics.neriids_protection.mining")
                .withStyle(ChatFormatting.AQUA));
    }
}
