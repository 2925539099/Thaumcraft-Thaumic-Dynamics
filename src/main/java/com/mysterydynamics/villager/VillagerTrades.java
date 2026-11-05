package com.mysterydynamics.villager;

import com.mysterydynamics.MysteryDynamics;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.village.VillagerTradesEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 神秘使 / 炼金师 / 机械师交易表。
 * 绿宝石买入价格使用 0.02 的轻微浮动。
 */
@EventBusSubscriber(modid = MysteryDynamics.MODID)
public final class VillagerTrades {

    private static final float FLOAT = 0.02F;

    private VillagerTrades() {
    }

    @SubscribeEvent
    public static void onVillagerTrades(VillagerTradesEvent event) {
        if (event.getType() == ModVillagers.THAUMATURGE.get()) {
            thaumaturge(event);
        } else if (event.getType() == ModVillagers.ALCHEMIST.get()) {
            alchemist(event);
        } else if (event.getType() == ModVillagers.MECHANIST.get()) {
            mechanist(event);
        }
    }

    private static void thaumaturge(VillagerTradesEvent e) {
        // 1 级
        add(e, 1, "thaumcraft:knowledge_fragment", 1, "minecraft:emerald", 1, 12, 2);
        add(e, 1, "minecraft:emerald", 4, "thaumcraft:enchanted_fabric", 4, 16, 1);
        for (String shard : List.of("thaumcraft:air_shard", "thaumcraft:fire_shard", "thaumcraft:water_shard",
                "thaumcraft:earth_shard", "thaumcraft:entropy_shard", "thaumcraft:order_shard")) {
            add(e, 1, shard, 3, "minecraft:emerald", 1, 12, 2);
        }
        add(e, 1, "thaumcraft:thaumium_ingot", 4, "minecraft:emerald", 3, 12, 2);
        add(e, 1, "thaumcraft:silverwood_log", 4, "minecraft:emerald", 1, 16, 1);
        // 2 级
        add(e, 2, "thaumcraft:quicksilver", 3, "minecraft:emerald", 1, 12, 5);
        add(e, 2, "minecraft:emerald", 1, "thaumcraft:thaumium_ingot", 1, 8, 5);
        add(e, 2, "minecraft:emerald", 3, "thaumcraft:arcane_stone", 4, 12, 5);
        addTwo(e, 2, "minecraft:emerald", 3, "thaumcraft:arcane_stone", 4,
                "thaumcraft:paving_stone_travel", 4, 8, 5);
        addTwo(e, 2, "minecraft:emerald", 3, "thaumcraft:arcane_stone", 4,
                "thaumcraft:paving_stone_warding", 4, 8, 5);
        // 3 级
        add(e, 3, "minecraft:emerald", 2, "thaumcraft:vis_filter", 1, 8, 10);
        add(e, 3, "minecraft:emerald", 5, "thaumcraft:salis_mundus", 1, 12, 10);
        add(e, 3, "minecraft:emerald", 13, "thaumcraft:primal_charm", 1, 4, 10);
        add(e, 3, "minecraft:emerald", 3, "thaumcraft:mirrored_glass", 1, 8, 10);
        add(e, 3, "thaumcraft:nitor", 1, "minecraft:emerald", 1, 12, 10);
        // 4 级
        add(e, 4, "minecraft:emerald", 4, "thaumcraft:wand_rod_greatwood", 1, 8, 15);
        add(e, 4, "minecraft:emerald", 16, "thaumcraft:node_stabilizer", 1, 3, 15);
        add(e, 4, "minecraft:emerald", 4, "thaumcraft:jar_of_essentia", 2, 8, 15);
        add(e, 4, "minecraft:emerald", 8, "thaumcraft:arcane_lifter", 1, 3, 15);
        // 5 级
        add(e, 5, "thaumcraft:zombie_brain", 24, "minecraft:emerald", 6, 4, 30);
        add(e, 5, "minecraft:emerald", 24, "thaumcraft:node_stabilizer_advanced", 1, 2, 30);
        add(e, 5, "minecraft:emerald", 8, "thaumcraft:silverwood_sapling", 1, 2, 30);
        add(e, 5, "minecraft:emerald", 16, "thaumcraft:alchemical_construct", 1, 4, 30);
    }

    private static void alchemist(VillagerTradesEvent e) {
        // 1 级
        add(e, 1, "thaumcraft:glass_phial", 12, "minecraft:emerald", 1, 12, 2);
        add(e, 1, "minecraft:emerald", 1, "thaumcraft:glass_phial", 8, 16, 1);
        addRange(e, 1, "minecraft:coal", 3, 16, "minecraft:emerald", 1, 12, 2);
        addPhialCost(e, 1, 4, 4, 12, 2);
        // 2 级
        addTwo(e, 2, "minecraft:raw_iron", 4, "minecraft:emerald", 16,
                "thaumcraft:native_iron_cluster", 4, 8, 5);
        addTwo(e, 2, "minecraft:raw_copper", 4, "minecraft:emerald", 16,
                "thaumcraft:native_copper_cluster", 4, 8, 5);
        add(e, 2, "minecraft:emerald", 8, "thaumcraft:balanced_shard", 2, 12, 5);
        add(e, 2, "minecraft:emerald", 12, "thaumcraft:bath_salts", 2, 8, 5);
        add(e, 2, "minecraft:emerald", 1, "thaumcraft:glass_phial", 4, 8, 5);
        // 3 级
        addGolem(e, 3, 4, 16, 12, 10);
        add(e, 3, "minecraft:emerald", 3, "thaumcraft:crucible", 1, 6, 10);
        add(e, 3, "minecraft:emerald", 2, "thaumcraft:essentia_tube", 4, 8, 10);
        add(e, 3, "minecraft:glowstone_dust", 4, "minecraft:emerald", 1, 12, 10);
        // 4 级
        add(e, 4, "minecraft:emerald", 3, "thaumcraft:alumentum", 1, 8, 15);
        add(e, 4, "minecraft:emerald", 6, "thaumcraft:essentia_filter", 1, 4, 15);
        add(e, 4, "minecraft:emerald", 8, "thaumcraft:essentia_buffer", 1, 3, 15);
        add(e, 4, "minecraft:emerald", 24, "thaumcraft:ethereal_bloom", 1, 3, 15);
        // 5 级
        add(e, 5, "minecraft:emerald", 12, "thaumcraft:bucket_death", 1, 1, 30);
        add(e, 5, "minecraft:emerald", 12, "thaumcraft:essentia_crystallizer", 1, 2, 30);
        add(e, 5, "minecraft:emerald", 6, "thaumcraft:mnemonic_matrix", 1, 3, 30);
        addPhialResultRange(e, 5, 8, 32, 4, 4, 30);
    }

    private static void mechanist(VillagerTradesEvent e) {
        // 1 级
        addRange(e, 1, "create:shaft", 8, 18, "minecraft:emerald", 1, 12, 2);
        add(e, 1, "minecraft:emerald", 1, "create:shaft", 4, 16, 1);
        addRange(e, 1, "create:andesite_alloy", 1, 8, "minecraft:emerald", 1, 12, 2);
        addRange(e, 1, "minecraft:andesite", 8, 32, "minecraft:emerald", 1, 12, 2);
        // 2 级
        add(e, 2, "minecraft:emerald", 6, "create:andesite_casing", 1, 12, 5);
        add(e, 2, "minecraft:emerald", 2, "create:hand_crank", 1, 8, 5);
        add(e, 2, "minecraft:emerald", 1, "create:belt_connector", 2, 8, 5);
        add(e, 2, "minecraft:emerald", 2, "create:water_wheel", 1, 8, 5);
        // 3 级
        add(e, 3, "create:brass_ingot", 3, "minecraft:emerald", 1, 8, 10);
        add(e, 3, "minecraft:emerald", 4, "create:gearbox", 1, 6, 10);
        add(e, 3, "minecraft:emerald", 6, "create:mechanical_pump", 1, 4, 10);
        add(e, 3, "minecraft:emerald", 2, "create:depot", 1, 4, 10);
        // 4 级
        add(e, 4, "minecraft:emerald", 14, "create:electron_tube", 1, 8, 15);
        add(e, 4, "minecraft:emerald", 8, "create:steam_engine", 1, 3, 15);
        add(e, 4, "minecraft:emerald", 8, "create:deployer", 1, 3, 15);
        add(e, 4, "minecraft:emerald", 32, "create:crushing_wheel", 1, 3, 15);
        // 5 级
        add(e, 5, "create:sweet_roll", 8, "minecraft:emerald", 1, 16, 30);
        add(e, 5, "minecraft:emerald", 18, "create:mechanical_crafter", 1, 8, 30);
        add(e, 5, "minecraft:emerald", 12, "create:blaze_burner", 1, 2, 30);
        add(e, 5, "minecraft:emerald", 16, "create:precision_mechanism", 1, 4, 30);
    }

    private static void add(VillagerTradesEvent e, int level, String costId, int costCount,
                            String resultId, int resultCount, int uses, int xp) {
        Item cost = item(costId);
        Item result = item(resultId);
        if (cost == Items.AIR || result == Items.AIR) return;
        e.getTrades().get(level).add((trader, random) -> new MerchantOffer(
                new ItemCost(cost, costCount), new ItemStack(result, resultCount), uses, xp, FLOAT));
    }

    private static void addRange(VillagerTradesEvent e, int level, String costId, int min, int max,
                                 String resultId, int resultCount, int uses, int xp) {
        Item cost = item(costId);
        Item result = item(resultId);
        if (cost == Items.AIR || result == Items.AIR) return;
        e.getTrades().get(level).add((trader, random) -> new MerchantOffer(
                new ItemCost(cost, min + random.nextInt(max - min + 1)),
                new ItemStack(result, resultCount), uses, xp, FLOAT));
    }

    private static void addTwo(VillagerTradesEvent e, int level, String costA, int countA,
                               String costB, int countB, String resultId, int resultCount,
                               int uses, int xp) {
        Item a = item(costA), b = item(costB), r = item(resultId);
        if (a == Items.AIR || b == Items.AIR || r == Items.AIR) return;
        e.getTrades().get(level).add((trader, random) -> new MerchantOffer(
                new ItemCost(a, countA), Optional.of(new ItemCost(b, countB)),
                new ItemStack(r, resultCount), uses, xp, FLOAT));
    }

    private static void addGolem(VillagerTradesEvent e, int level, int min, int max, int uses, int xp) {
        List<Item> golems = List.of(
                item("thaumcraft:golem_straw"), item("thaumcraft:golem_wood"),
                item("thaumcraft:golem_clay"), item("thaumcraft:golem_stone"));
        e.getTrades().get(level).add((trader, random) -> new MerchantOffer(
                new ItemCost(Items.EMERALD, min + random.nextInt(max - min + 1)),
                new ItemStack(golems.get(random.nextInt(golems.size()))),
                uses, xp, FLOAT));
    }

    private static void addPhialCost(VillagerTradesEvent e, int level, int count,
                                     int emeralds, int uses, int xp) {
        e.getTrades().get(level).add((trader, random) -> new MerchantOffer(
                new ItemCost(randomPhial(random), count),
                new ItemStack(Items.EMERALD, emeralds), uses, xp, FLOAT));
    }

    private static void addPhialResultRange(VillagerTradesEvent e, int level, int min, int max,
                                            int resultCount, int uses, int xp) {
        e.getTrades().get(level).add((trader, random) -> new MerchantOffer(
                new ItemCost(Items.EMERALD, min + random.nextInt(max - min + 1)),
                new ItemStack(randomPhial(random), resultCount), uses, xp, FLOAT));
    }

    private static List<Item> PHIAL_ITEMS;

    private static Item randomPhial(RandomSource random) {
        if (PHIAL_ITEMS == null) {
            PHIAL_ITEMS = new ArrayList<>();
            for (var entry : BuiltInRegistries.ITEM.entrySet()) {
                ResourceLocation id = entry.getKey().location();
                if (id.getNamespace().equals("thaumcraft")
                        && id.getPath().startsWith("phial_of_essentia_")) {
                    PHIAL_ITEMS.add(entry.getValue());
                }
            }
        }
        if (PHIAL_ITEMS.isEmpty()) {
            return item("thaumcraft:phial_of_essentia");
        }
        return PHIAL_ITEMS.get(random.nextInt(PHIAL_ITEMS.size()));
    }

    private static Item item(String id) {
        return BuiltInRegistries.ITEM.get(ResourceLocation.parse(id));
    }
}
