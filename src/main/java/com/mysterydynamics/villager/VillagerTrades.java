package com.mysterydynamics.villager;

import com.mysterydynamics.MysteryDynamics;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.village.VillagerTradesEvent;

/**
 * 神秘使、炼金师、机械师的原版风格交易表。
 * 每个等级两笔：一笔收购原材料，一笔出售成品。
 */
@EventBusSubscriber(modid = MysteryDynamics.MODID)
public final class VillagerTrades {

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

    private static void thaumaturge(VillagerTradesEvent event) {
        // 1 级：收购普通神秘材料，出售基础容器
        add(event, 1, "thaumcraft:amber", 4, "minecraft:emerald", 1, 12, 2);
        add(event, 1, "minecraft:emerald", 1, "thaumcraft:glass_phial", 4, 16, 1);
        add(event, 1, "thaumcraft:greatwood_sapling", 6, "minecraft:emerald", 1, 12, 2);
        add(event, 1, "thaumcraft:shimmerleaf", 4, "minecraft:emerald", 1, 12, 2);
        // 2 级：收购水银，出售神秘锭
        add(event, 2, "thaumcraft:quicksilver", 3, "minecraft:emerald", 1, 12, 5);
        add(event, 2, "minecraft:emerald", 1, "thaumcraft:thaumium_ingot", 1, 8, 5);
        add(event, 2, "thaumcraft:cinderpearl", 3, "minecraft:emerald", 1, 12, 5);
        add(event, 2, "minecraft:emerald", 1, "thaumcraft:thaumium_nugget", 4, 8, 5);
        // 3 级：收购平衡碎片，出售奥术工作台
        add(event, 3, "thaumcraft:balanced_shard", 2, "minecraft:emerald", 1, 8, 10);
        add(event, 3, "minecraft:emerald", 6, "thaumcraft:arcane_workbench", 1, 4, 10);
        add(event, 3, "minecraft:emerald", 3, "thaumcraft:focus_pouch", 1, 4, 10);
        add(event, 3, "thaumcraft:greatwood_planks", 8, "minecraft:emerald", 1, 12, 10);
        // 4 级：收购神秘锭，出售揭示之护目镜
        add(event, 4, "thaumcraft:thaumium_ingot", 4, "minecraft:emerald", 1, 8, 15);
        add(event, 4, "minecraft:emerald", 8, "thaumcraft:goggles_revealing", 1, 3, 15);
        add(event, 4, "minecraft:emerald", 2, "thaumcraft:bath_salts", 3, 8, 15);
        add(event, 4, "minecraft:emerald", 8, "thaumcraft:focus_fire", 1, 3, 15);
        // 5 级：收购虚空金属，出售神秘使工作站
        add(event, 5, "thaumcraft:void_metal_ingot", 1, "minecraft:emerald", 6, 4, 30);
        add(event, 5, "minecraft:emerald", 12, "mystery_dynamics:thaumaturge_workstation", 1, 2, 30);
        add(event, 5, "minecraft:emerald", 1, "thaumcraft:silverwood_sapling", 1, 2, 30);
        add(event, 5, "minecraft:emerald", 2, "thaumcraft:balanced_shard", 1, 4, 30);
    }

    private static void alchemist(VillagerTradesEvent event) {
        // 1 级：收购玻璃瓶，出售玻璃安瓿
        add(event, 1, "minecraft:glass_bottle", 6, "minecraft:emerald", 1, 12, 2);
        add(event, 1, "minecraft:emerald", 1, "thaumcraft:glass_phial", 2, 16, 1);
        add(event, 1, "thaumcraft:cinderpearl", 3, "minecraft:emerald", 1, 12, 2);
        add(event, 1, "minecraft:sugar", 4, "minecraft:emerald", 1, 12, 2);
        // 2 级：收购炼金燃料，出售世界盐
        add(event, 2, "thaumcraft:alumentum", 4, "minecraft:emerald", 1, 12, 5);
        add(event, 2, "minecraft:emerald", 1, "thaumcraft:salis_mundus", 2, 12, 5);
        add(event, 2, "minecraft:emerald", 2, "thaumcraft:bath_salts", 2, 8, 5);
        add(event, 2, "minecraft:emerald", 1, "thaumcraft:glass_phial", 4, 8, 5);
        // 3 级：收购下界疣，出售坩埚
        add(event, 3, "minecraft:nether_wart", 8, "minecraft:emerald", 1, 12, 10);
        add(event, 3, "minecraft:emerald", 3, "thaumcraft:crucible", 1, 6, 10);
        add(event, 3, "minecraft:emerald", 2, "thaumcraft:essentia_tube", 4, 8, 10);
        add(event, 3, "minecraft:glowstone_dust", 4, "minecraft:emerald", 1, 12, 10);
        // 4 级：收购水银，出售源质过滤器
        add(event, 4, "thaumcraft:quicksilver", 3, "minecraft:emerald", 1, 8, 15);
        add(event, 4, "minecraft:emerald", 6, "thaumcraft:essentia_filter", 1, 4, 15);
        add(event, 4, "minecraft:emerald", 8, "thaumcraft:essentia_crystallizer", 1, 3, 15);
        add(event, 4, "minecraft:emerald", 4, "thaumcraft:essentia_buffer", 1, 3, 15);
        // 5 级：收购虚空金属，出售小型炼金台
        add(event, 5, "thaumcraft:void_metal_ingot", 1, "minecraft:emerald", 6, 4, 30);
        add(event, 5, "minecraft:emerald", 12, "mystery_dynamics:alchemy_workshop", 1, 2, 30);
        add(event, 5, "minecraft:emerald", 6, "thaumcraft:arcane_alembic", 1, 3, 30);
        add(event, 5, "minecraft:emerald", 1, "thaumcraft:alumentum", 3, 4, 30);
    }

    private static void mechanist(VillagerTradesEvent event) {
        // 1 级：收购齿轮，出售传动杆
        add(event, 1, "create:cogwheel", 8, "minecraft:emerald", 1, 12, 2);
        add(event, 1, "minecraft:emerald", 1, "create:shaft", 4, 16, 1);
        add(event, 1, "create:andesite_casing", 4, "minecraft:emerald", 1, 12, 2);
        add(event, 1, "create:copper_casing", 4, "minecraft:emerald", 1, 12, 2);
        // 2 级：收购安山合金，出售扳手
        add(event, 2, "create:andesite_alloy", 6, "minecraft:emerald", 1, 12, 5);
        add(event, 2, "minecraft:emerald", 2, "create:wrench", 1, 8, 5);
        add(event, 2, "minecraft:emerald", 2, "create:belt_connector", 2, 8, 5);
        add(event, 2, "minecraft:emerald", 1, "create:cogwheel", 2, 8, 5);
        // 3 级：收购精密构件，出售齿轮箱
        add(event, 3, "create:precision_mechanism", 3, "minecraft:emerald", 1, 8, 10);
        add(event, 3, "minecraft:emerald", 4, "create:gearbox", 1, 6, 10);
        add(event, 3, "minecraft:emerald", 6, "create:mechanical_press", 1, 4, 10);
        add(event, 3, "minecraft:emerald", 2, "create:encased_fan", 1, 4, 10);
        // 4 级：收购电子管，出售器械制作台
        add(event, 4, "create:electron_tube", 1, "minecraft:emerald", 4, 8, 15);
        add(event, 4, "minecraft:emerald", 8, "mystery_dynamics:mechanist_workstation", 1, 3, 15);
        add(event, 4, "minecraft:emerald", 8, "create:deployer", 1, 3, 15);
        add(event, 4, "minecraft:emerald", 4, "create:millstone", 1, 3, 15);
        // 5 级：收购黄铜板，出售奥法涡轮机
        add(event, 5, "create:brass_sheet", 4, "minecraft:emerald", 1, 8, 30);
        add(event, 5, "minecraft:emerald", 16, "mystery_dynamics:arcane_turbine", 1, 2, 30);
        add(event, 5, "minecraft:emerald", 10, "create:mechanical_mixer", 1, 2, 30);
        add(event, 5, "minecraft:emerald", 2, "create:brass_casing", 1, 4, 30);
    }

    private static void add(VillagerTradesEvent event, int level, String costId, int costCount,
                            String resultId, int resultCount, int maxUses, int xp) {
        Item cost = item(costId);
        Item result = item(resultId);
        if (cost == Items.AIR || result == Items.AIR) {
            return;
        }
        event.getTrades().get(level).add((trader, random) -> new MerchantOffer(
                new ItemCost(cost, costCount),
                new ItemStack(result, resultCount),
                maxUses, xp, 0.05F));
    }

    private static Item item(String id) {
        return BuiltInRegistries.ITEM.get(ResourceLocation.parse(id));
    }
}
