package com.mysterydynamics.compat;

import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.crafting.InfusionRecipe;

import java.util.ArrayList;
import java.util.List;

/** 注魔配方：允许任意属于指定鱼类标签的物品替代展示用的生鳕鱼。 */
public class TaggedInfusionRecipe extends InfusionRecipe {

    private final TagKey<Item> tag;

    public TaggedInfusionRecipe(String research, ItemStack output, int instability, AspectList aspects,
                                ItemStack central, TagKey<Item> tag, ItemStack... components) {
        super(research, output, instability, aspects, central, components);
        this.tag = tag;
    }

    @Override
    public boolean matches(List<ItemStack> items, ItemStack central, Player player) {
        List<ItemStack> normalized = new ArrayList<>(items.size());
        for (ItemStack stack : items) {
            normalized.add(stack.is(tag) ? new ItemStack(Items.COD, stack.getCount()) : stack);
        }
        return super.matches(normalized, central, player);
    }
}
