package com.mysterydynamics.villager;

import com.mysterydynamics.MysteryDynamics;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.village.VillagerTradesEvent;

/** 三个新职业的基础交易。 */
@EventBusSubscriber(modid = MysteryDynamics.MODID)
public final class VillagerTrades {

    private VillagerTrades() {
    }

    @SubscribeEvent
    public static void onVillagerTrades(VillagerTradesEvent event) {
        if (event.getType() == ModVillagers.THAUMATURGE.get()) {
            event.getTrades().get(1).add((trader, random) -> new MerchantOffer(
                    new ItemCost(Items.EMERALD, 6),
                    new ItemStack(MysteryDynamics.THAUMATURGE_WORKSTATION_ITEM.get()),
                    8, 2, 0.05F));
        }
        if (event.getType() == ModVillagers.ALCHEMIST.get()) {
            event.getTrades().get(1).add((trader, random) -> new MerchantOffer(
                    new ItemCost(Items.EMERALD, 4),
                    new ItemStack(MysteryDynamics.ALCHEMY_WORKSHOP_ITEM.get()),
                    8, 2, 0.05F));
        }
        if (event.getType() == ModVillagers.MECHANIST.get()) {
            event.getTrades().get(1).add((trader, random) -> new MerchantOffer(
                    new ItemCost(Items.EMERALD, 4),
                    new ItemStack(MysteryDynamics.MECHANIST_WORKSTATION_ITEM.get()),
                    8, 2, 0.05F));
        }
    }
}
