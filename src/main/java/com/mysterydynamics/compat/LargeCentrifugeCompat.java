package com.mysterydynamics.compat;

import com.mysterydynamics.MysteryDynamics;
import com.simibubi.create.AllBlocks;

import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import thaumcraft.api.aspects.EssentiaCapabilities;
import thaumcraft.api.aspects.IEssentiaTransport;
import thaumcraft.api.wands.WandTriggerRegistry;
import thaumcraft.common.items.TCFunctionalItems;
import thaumcraft.common.registry.TCBlocks;

/** 大型奥法离心机的法杖触发兼容层。 */
public final class LargeCentrifugeCompat {

    private LargeCentrifugeCompat() {
    }

    public static void register() {
        WandTriggerRegistry.registerWandBlockTrigger(new LargeCentrifugeTrigger(), 0,
                LargeCentrifugeCompat::isStructureBlock, "large_centrifuge");
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, true,
                LargeCentrifugeCompat::onRightClickBlock);
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlock(EssentiaCapabilities.TRANSPORT,
                (level, pos, state, blockEntity, side) -> LargeCentrifugeTransport.get(level, pos, side),
                MysteryDynamics.LARGE_CENTRIFUGE_CASING.get());
    }

    private static boolean isStructureBlock(BlockState state) {
        return state.is(TCBlocks.ARCANE_STONE_BRICKS.get())
                || state.is(TCBlocks.ALCHEMICAL_CONSTRUCT.get())
                || state.is(AllBlocks.WINDMILL_BEARING.get())
                || state.is(AllBlocks.CLUTCH.get());
    }

    private static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof Player player)) {
            return;
        }
        ItemStack stack = event.getItemStack();
        if (!(stack.getItem() instanceof TCFunctionalItems.WandCastingItem)) {
            return;
        }
        if (!isStructureBlock(event.getLevel().getBlockState(event.getPos()))) {
            return;
        }
        Direction face = event.getFace();
        if (new LargeCentrifugeTrigger().performTrigger(event.getLevel(), stack, player,
                event.getPos(), face == null ? Direction.UP : face, 0)) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
        }
    }
}
