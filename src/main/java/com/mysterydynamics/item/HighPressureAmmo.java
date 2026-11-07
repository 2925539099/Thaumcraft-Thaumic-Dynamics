package com.mysterydynamics.item;

import com.mojang.logging.LogUtils;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import org.slf4j.Logger;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** 高压动力发射器专属弹药列表。 */
public final class HighPressureAmmo {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** 由其它模组提供的弹药 ID；找不到的会被跳过并记录警告。 */
    private static final List<ResourceLocation> MOD_AMMO_IDS = List.of(
            id("create", "copper_nugget"),
            id("thaumcraft", "thaumium_nugget"),
            id("thaumcraft", "void_metal_nugget"),
            id("thaumcraft", "quicksilver_drop"),
            id("thaumcraft", "tainted_goo"),
            id("thaumcraft", "nitor"),
            id("thaumcraft", "alumentum"),
            id("thaumcraft", "chicken_nugget"),
            id("thaumcraft", "beef_nugget"),
            id("thaumcraft", "pork_nugget"),
            id("thaumcraft", "fish_nugget"),
            id("thaumcraft", "triple_meat_treat"),
            id("thaumcraft", "gold_coin"),
            id("thaumcraft", "void_seed"));

    /** 首次使用时才构建，保证此时物品注册表已经填充完毕。 */
    private static volatile Set<Item> ammo;

    private HighPressureAmmo() {
    }

    private static ResourceLocation id(String namespace, String path) {
        return ResourceLocation.fromNamespaceAndPath(namespace, path);
    }

    private static Set<Item> ammo() {
        Set<Item> result = ammo;
        if (result == null) {
            Set<Item> built = new HashSet<>();
            built.add(Items.IRON_NUGGET);
            built.add(Items.GOLD_NUGGET);
            built.add(Items.LIGHTNING_ROD);
            for (ResourceLocation id : MOD_AMMO_IDS) {
                Optional<Item> item = BuiltInRegistries.ITEM.getOptional(id);
                if (item.isPresent() && item.get() != Items.AIR) {
                    built.add(item.get());
                } else {
                    LOGGER.warn("高压动力发射器弹药 {} 不存在，已跳过", id);
                }
            }
            result = Set.copyOf(built);
            ammo = result;
        }
        return result;
    }

    public static boolean isAmmo(ItemStack stack) {
        return !stack.isEmpty() && ammo().contains(stack.getItem());
    }
}
