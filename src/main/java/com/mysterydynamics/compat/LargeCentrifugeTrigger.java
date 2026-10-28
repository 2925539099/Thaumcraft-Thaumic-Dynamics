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

/** 法杖右击大型离心机结构，消耗六种 Vis 各 50 成型。 */
public final class LargeCentrifugeTrigger implements IWandTriggerManager {

    @Override
    public boolean performTrigger(Level level, ItemStack stack, Player player, BlockPos pos,
                                  Direction face, int event) {
        if (level.isClientSide || player == null
                || !(stack.getItem() instanceof TCFunctionalItems.WandCastingItem wand)) {
            return false;
        }
        LargeCentrifugeStructure.Match match = LargeCentrifugeStructure.find(level, pos);
        if (match == null) {
            return false;
        }
        AspectList cost = new AspectList()
                .add(Aspect.FIRE, 50)
                .add(Aspect.EARTH, 50)
                .add(Aspect.AIR, 50)
                .add(Aspect.ENTROPY, 50)
                .add(Aspect.WATER, 50)
                .add(Aspect.ORDER, 50);
        if (!wand.consumeVisCost(stack, player, cost, true)) {
            player.displayClientMessage(
                    Component.translatable("mystery_dynamics.large_centrifuge.no_vis"), false);
            return false;
        }
        if (!LargeCentrifugeStructure.form(level, match)) {
            return false;
        }
        player.displayClientMessage(
                Component.translatable("mystery_dynamics.large_centrifuge.formed"), true);
        level.playSound(null, pos, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
        return true;
    }
}
