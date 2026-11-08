package com.mysterydynamics.compat;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.wands.IWandTriggerManager;
import thaumcraft.common.items.TCFunctionalItems;
import thaumcraft.common.registry.TCBlocks;
import thaumcraft.common.research.TCResearchManager;

/** 法杖右击结构任意方块时尝试成型。 */
public final class InfernalBoilerTrigger implements IWandTriggerManager {

    /** 触发结果：NOT_APPLICABLE 表示不是锅炉结构，交给其它多方块处理。 */
    public enum Outcome {
        NOT_APPLICABLE,
        FAILED,
        FORMED
    }

    @Override
    public boolean performTrigger(Level level, ItemStack stack, Player player, BlockPos pos,
                                  Direction face, int event) {
        return attempt(level, stack, player, pos) == Outcome.FORMED;
    }

    public static Outcome attempt(Level level, ItemStack stack, Player player, BlockPos pos) {
        if (level.isClientSide || player == null) {
            return Outcome.NOT_APPLICABLE;
        }
        if (!(stack.getItem() instanceof TCFunctionalItems.WandCastingItem wand)) {
            return Outcome.NOT_APPLICABLE;
        }
        BlockPos glass = InfernalBoilerStructure.findGlass(level, pos);
        if (glass == null) {
            return diagnose(level, player, pos);
        }
        if (!TCResearchManager.has(player, "INFERNAL_BOILER")) {
            player.displayClientMessage(
                    Component.translatable("mystery_dynamics.infernal_boiler.locked"), false);
            return Outcome.FAILED;
        }

        AspectList cost = new AspectList()
                .add(Aspect.FIRE, 100)
                .add(Aspect.EARTH, 100)
                .add(Aspect.AIR, 30)
                .add(Aspect.ENTROPY, 30)
                .add(Aspect.WATER, 30)
                .add(Aspect.ORDER, 30);
        if (!wand.consumeVisCost(stack, player, cost, true)) {
            player.displayClientMessage(
                    Component.translatable("mystery_dynamics.infernal_boiler.no_vis"), false);
            return Outcome.FAILED;
        }
        if (!InfernalBoilerStructure.form(level, glass)) {
            return Outcome.FAILED;
        }

        player.displayClientMessage(Component.translatable("mystery_dynamics.infernal_boiler.formed"), true);
        level.playSound(null, pos, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
        return Outcome.FORMED;
    }

    /**
     * 结构不匹配时的提示。普通结构方块可能属于炼狱熔炉等其他多方块，
     * 只有研究已完成、且附近确实有一座差不多搭好的锅炉时才输出锅炉专属提示。
     */
    private static Outcome diagnose(Level level, Player player, BlockPos pos) {
        if (!TCResearchManager.has(player, "INFERNAL_BOILER")) {
            return Outcome.NOT_APPLICABLE;
        }
        InfernalBoilerStructure.Mismatch mismatch = InfernalBoilerStructure.diagnose(level, pos);
        if (mismatch != null) {
            BlockPos at = mismatch.pos();
            player.displayClientMessage(Component.translatable(
                    "mystery_dynamics.infernal_boiler.incomplete",
                    at.getX(), at.getY(), at.getZ(), mismatch.expected()), false);
            return Outcome.FAILED;
        }
        // 守卫者玻璃只用于锅炉，点了它却离成型很远时给一句通用提示。
        if (level.getBlockState(pos).is(TCBlocks.WARDED_GLASS.get())) {
            player.displayClientMessage(
                    Component.translatable("mystery_dynamics.infernal_boiler.no_structure"), false);
            return Outcome.FAILED;
        }
        return Outcome.NOT_APPLICABLE;
    }
}
