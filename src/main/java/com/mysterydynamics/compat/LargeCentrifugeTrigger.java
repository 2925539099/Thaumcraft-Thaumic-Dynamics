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
import thaumcraft.api.research.ResearchApi;
import thaumcraft.api.wands.IWandTriggerManager;
import thaumcraft.common.items.TCFunctionalItems;

/** 法杖右击大型离心机结构，消耗结构页显示的 Vis 并成型。 */
public final class LargeCentrifugeTrigger implements IWandTriggerManager {

    public static final String RESEARCH_KEY = "LARGE_CENTRIFUGE";

    /** 结构页底部显示的法杖激活需求；实际消耗必须与它共用同一份数据。 */
    public static AspectList activationCost() {
        return new AspectList()
                .add(Aspect.FIRE, 50)
                .add(Aspect.EARTH, 50)
                .add(Aspect.AIR, 50)
                .add(Aspect.ENTROPY, 50)
                .add(Aspect.WATER, 50)
                .add(Aspect.ORDER, 50);
    }

    /** 触发结果：NOT_APPLICABLE 表示不是离心机结构，交给其它多方块处理。 */
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
        if (level.isClientSide || player == null
                || !(stack.getItem() instanceof TCFunctionalItems.WandCastingItem wand)) {
            return Outcome.NOT_APPLICABLE;
        }
        // 与原版多方块一致：未完成研究时静默失败。
        if (!ResearchApi.isComplete(player, RESEARCH_KEY)) {
            return Outcome.NOT_APPLICABLE;
        }
        LargeCentrifugeStructure.Match match = LargeCentrifugeStructure.find(level, pos);
        if (match == null) {
            LargeCentrifugeStructure.Mismatch mismatch = LargeCentrifugeStructure.diagnose(level, pos);
            if (mismatch == null) {
                return Outcome.NOT_APPLICABLE;
            }
            BlockPos at = mismatch.pos();
            player.displayClientMessage(Component.translatable(
                    "mystery_dynamics.large_centrifuge.incomplete",
                    at.getX(), at.getY(), at.getZ(), mismatch.expected()), false);
            return Outcome.FAILED;
        }
        if (!wand.consumeVisCost(stack, player, activationCost(), true)) {
            player.displayClientMessage(
                    Component.translatable("mystery_dynamics.large_centrifuge.no_vis"), false);
            return Outcome.FAILED;
        }
        if (!LargeCentrifugeStructure.form(level, match)) {
            return Outcome.FAILED;
        }
        player.displayClientMessage(
                Component.translatable("mystery_dynamics.large_centrifuge.formed"), true);
        level.playSound(null, pos, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
        return Outcome.FORMED;
    }
}
