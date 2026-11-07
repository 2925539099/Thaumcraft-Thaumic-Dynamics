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
            // 输出随红石信号线性变化，0 信号停转
            event.getToolTip().add(Component.translatable("mystery_dynamics.tooltip.redstone_engine.scaling",
                    format(RedstoneEngineBlockEntity.rpmFor(1)), format(RedstoneEngineBlockEntity.MAX_RPM),
                    format(RedstoneEngineBlockEntity.stressFor(1)), format(RedstoneEngineBlockEntity.MAX_STRESS))
                    .withStyle(ChatFormatting.GRAY));
            event.getToolTip().add(Component.translatable("mystery_dynamics.tooltip.redstone_engine.off")
                    .withStyle(ChatFormatting.GRAY));
        } else if (block == MysteryDynamics.ARCANE_TURBINE.get()) {
            addStressTooltip(event, 64.0F, com.mysterydynamics.block.ArcaneTurbineBlockEntity.MAX_STRESS);
            event.getToolTip().add(Component.translatable("mystery_dynamics.tooltip.arcane_turbine.boiler")
                    .withStyle(ChatFormatting.GRAY));
        }
    }

    private static void addStressTooltip(ItemTooltipEvent event, float rpm, float maxStress) {
        var tooltip = event.getToolTip();
        tooltip.add(Component.translatable("mystery_dynamics.tooltip.stress")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("▮▮▮ ")
                .withStyle(ChatFormatting.YELLOW)
                .append(Component.translatable("mystery_dynamics.tooltip.rpm_value",
                        format(rpm)).withStyle(ChatFormatting.YELLOW)));
        tooltip.add(Component.translatable("mystery_dynamics.tooltip.max_stress",
                format(maxStress)).withStyle(ChatFormatting.DARK_GRAY));
    }

    private static String format(float value) {
        return String.format("%,.0f", value);
    }
}
