package com.mysterydynamics.item;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Set;

/** 高压动力发射器专属弹药列表。 */
public final class HighPressureAmmo {

    private static final Set<Item> AMMO = Set.of(
            Items.IRON_NUGGET,
            Items.GOLD_NUGGET,
            Items.LIGHTNING_ROD,
            item("create", "copper_nugget"),
            item("thaumcraft", "thaumium_nugget"),
            item("thaumcraft", "void_metal_nugget"),
            item("thaumcraft", "quicksilver_drop"),
            item("thaumcraft", "nitor"),
            item("thaumcraft", "alumentum"),
            item("thaumcraft", "chicken_nugget"),
            item("thaumcraft", "beef_nugget"),
            item("thaumcraft", "pork_nugget"),
            item("thaumcraft", "fish_nugget"),
            item("thaumcraft", "triple_meat_treat"),
            item("thaumcraft", "gold_coin"),
            item("thaumcraft", "void_seed"));

    private HighPressureAmmo() {
    }

    private static Item item(String namespace, String path) {
        return BuiltInRegistries.ITEM.get(
                ResourceLocation.fromNamespaceAndPath(namespace, path));
    }

    public static boolean isAmmo(ItemStack stack) {
        return !stack.isEmpty() && AMMO.contains(stack.getItem());
    }
}
