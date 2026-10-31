package com.mysterydynamics.client;

import com.mysterydynamics.MysteryDynamics;
import com.mysterydynamics.block.GolemPowerBlock;
import com.mysterydynamics.block.RedstoneEngineBlockEntity;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/** 为应力源物品添加与 Create 一致的应力量提示。 */
public final class StressTooltipEvents {

    private StressTooltipEvents() {
    }

    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (!(stack.getItem() instanceof BlockItem blockItem)) {
            return;
        }
        var block = blockItem.getBlock();
        if (block instanceof GolemPowerBlock golem) {
            addStressTooltip(event, golem.rpm(), golem.rpm() * golem.capacityPerRpm());
        } else if (block == MysteryDynamics.REDSTONE_ENGINE.get()) {
            addStressTooltip(event, RedstoneEngineBlockEntity.MAX_RPM, RedstoneEngineBlockEntity.MAX_STRESS);
        }
    }

    private static void addStressTooltip(ItemTooltipEvent event, float rpm, float maxStress) {
        var tooltip = event.getToolTip();
        tooltip.add(Component.translatable("mystery_dynamics.tooltip.stress")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("▮▮▮ ")
                .withStyle(ChatFormatting.YELLOW)
                .append(Component.translatable("mystery_dynamics.tooltip.rpm",
                        format(rpm)).withStyle(ChatFormatting.YELLOW)));
        tooltip.add(Component.translatable("mystery_dynamics.tooltip.max_stress",
                format(maxStress)).withStyle(ChatFormatting.DARK_GRAY));
    }

    private static String format(float value) {
        return String.format("%,.0f", value);
    }
}
