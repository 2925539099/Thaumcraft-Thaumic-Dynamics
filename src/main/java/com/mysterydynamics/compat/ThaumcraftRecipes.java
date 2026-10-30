package com.mysterydynamics.compat;

import com.mysterydynamics.MysteryDynamics;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;

import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;

/**
 * 与神秘时代（port.265+）的奥术工作台配方对接。
 *
 * <p>只有在检测到 thaumcraft 模组时才会加载本类，避免未安装时出现类缺失。</p>
 */
public final class ThaumcraftRecipes {

    private ThaumcraftRecipes() {
    }

    public static void register() {
        ItemStack frame = new ItemStack(MysteryDynamics.GOLEM_POWER_FRAME.get());
        ItemStack thaumium = stack("thaumcraft", "thaumium_ingot");
        ItemStack cogwheel = stack("create", "cogwheel");
        ItemStack casing = stack("create", "andesite_casing");
        ItemStack strawGolem = stack("thaumcraft", "golem_straw");
        ItemStack woodGolem = stack("thaumcraft", "golem_wood");
        ItemStack fleshGolem = stack("thaumcraft", "golem_flesh");
        ItemStack clayGolem = stack("thaumcraft", "golem_clay");
        ItemStack tallowGolem = stack("thaumcraft", "golem_tallow");
        ItemStack stoneGolem = stack("thaumcraft", "golem_stone");
        ItemStack ironGolem = stack("thaumcraft", "golem_iron");
        ItemStack thaumiumGolem = stack("thaumcraft", "golem_thaumium");
        ItemStack bone = stack("minecraft", "bone");
        ItemStack clayBall = stack("minecraft", "clay_ball");
        ItemStack stoneBricks = stack("minecraft", "stone_bricks");
        ItemStack magicTallow = stack("thaumcraft", "magic_tallow");
        ItemStack airCrystal = stack("thaumcraft", "air_crystal_cluster");
        ItemStack stick = new ItemStack(Items.STICK);
        ItemStack thaumiumBlock = stack("thaumcraft", "thaumium_block");
        ItemStack redstoneBlock = new ItemStack(Items.REDSTONE_BLOCK);
        ItemStack alchemicalConstruct = stack("thaumcraft", "alchemical_construct");
        ItemStack precisionMechanism = stack("create", "precision_mechanism");
        ItemStack shaft = stack("create", "shaft");

        // 空傀儡动力系统框架：三神秘锭 + 齿轮/安山机壳/齿轮，六种元要素各 5 点
        ThaumcraftApi.addArcaneCraftingRecipe(
                "STRAW_GOLEM_POWER",
                frame,
                new AspectList()
                        .add(Aspect.AIR, 5)
                        .add(Aspect.EARTH, 5)
                        .add(Aspect.FIRE, 5)
                        .add(Aspect.WATER, 5)
                        .add(Aspect.ORDER, 5)
                        .add(Aspect.ENTROPY, 5),
                "TTT", "GCG",
                'T', thaumium,
                'G', cogwheel,
                'C', casing);

        // 稻草：框架 / 稻草傀儡 / 木棍，水 3 + 地 6
        centered("STRAW_GOLEM_POWER",
                new ItemStack(MysteryDynamics.STRAW_GOLEM_POWER.get()),
                new AspectList().add(Aspect.WATER, 3).add(Aspect.EARTH, 6),
                1, 3,
                " F ", " S ", " K ",
                'F', frame, 'S', strawGolem, 'K', stick);

        // 木头：框架 / 木头傀儡，水 3 + 地 6 + 风 3
        centered("WOOD_GOLEM_POWER",
                new ItemStack(MysteryDynamics.WOOD_GOLEM_POWER.get()),
                new AspectList().add(Aspect.WATER, 3).add(Aspect.EARTH, 6).add(Aspect.AIR, 3),
                1, 2,
                " F ", " W ",
                'F', frame, 'W', woodGolem);

        // 血肉：框架 / 骨头 / 血肉傀儡，水 20 + 地 20 + 秩序 20
        centered("FLESH_GOLEM_POWER",
                new ItemStack(MysteryDynamics.FLESH_GOLEM_POWER.get()),
                new AspectList().add(Aspect.WATER, 20).add(Aspect.EARTH, 20).add(Aspect.ORDER, 20),
                1, 3,
                " F ", " B ", " S ",
                'F', frame, 'B', bone, 'S', fleshGolem);

        // 粘土：框架居中 / 粘土·粘土傀儡·粘土，火 30
        ThaumcraftApi.addArcaneCraftingRecipe(
                "CLAY_GOLEM_POWER",
                new ItemStack(MysteryDynamics.CLAY_GOLEM_POWER.get()),
                new AspectList().add(Aspect.FIRE, 30),
                " F ", "CGC",
                'F', frame,
                'C', clayBall,
                'G', clayGolem);

        // 油脂：魔力油脂·框架·魔力油脂 / 木棍·油脂傀儡·木棍，
        // 风/秩序/混沌/地各 10 + 水 20
        ThaumcraftApi.addArcaneCraftingRecipe(
                "TALLOW_GOLEM_POWER",
                new ItemStack(MysteryDynamics.TALLOW_GOLEM_POWER.get()),
                new AspectList()
                        .add(Aspect.AIR, 10)
                        .add(Aspect.ORDER, 10)
                        .add(Aspect.ENTROPY, 10)
                        .add(Aspect.EARTH, 10)
                        .add(Aspect.WATER, 20),
                "TFT", "SGS",
                'T', magicTallow,
                'F', frame,
                'S', stick,
                'G', tallowGolem);

        // 石头：框架居中 / 石砖·石头傀儡·石砖，
        // 风/秩序/混沌/地各 10 + 水 20 + 火 10
        ThaumcraftApi.addArcaneCraftingRecipe(
                "STONE_GOLEM_POWER",
                new ItemStack(MysteryDynamics.STONE_GOLEM_POWER.get()),
                new AspectList()
                        .add(Aspect.AIR, 10)
                        .add(Aspect.ORDER, 10)
                        .add(Aspect.ENTROPY, 10)
                        .add(Aspect.EARTH, 10)
                        .add(Aspect.WATER, 20)
                        .add(Aspect.FIRE, 10),
                " F ", "BGB",
                'F', frame,
                'B', stoneBricks,
                'G', stoneGolem);

        // 铁卫：框架 / 铁卫傀儡，地 20 + 秩序 10
        centered("IRON_GOLEM_POWER",
                new ItemStack(MysteryDynamics.IRON_GOLEM_POWER.get()),
                new AspectList().add(Aspect.EARTH, 20).add(Aspect.ORDER, 10),
                1, 2,
                " F ", " G ",
                'F', frame, 'G', ironGolem);

        // 神秘：框架 / 神秘傀儡 / 风之魔晶，地 20 + 秩序 10 + 风 30
        centered("MYSTERY_GOLEM_POWER",
                new ItemStack(MysteryDynamics.MYSTERY_GOLEM_POWER.get()),
                new AspectList().add(Aspect.EARTH, 20).add(Aspect.ORDER, 10).add(Aspect.AIR, 30),
                1, 3,
                " F ", " G ", " A ",
                'F', frame, 'G', thaumiumGolem, 'A', airCrystal);

        // 红石发动机：
        // 神秘锭 / 红石块 / 神秘锭
        // 炼金构材 / 精密构件 / 传动杆
        // 神秘锭 / 神秘方块 / 神秘锭
        // 风 10、秩序 20、水 30、地 30、火 10、混沌 10
        ThaumcraftApi.addArcaneCraftingRecipe(
                "REDSTONE_ENGINE",
                new ItemStack(MysteryDynamics.REDSTONE_ENGINE.get()),
                new AspectList()
                        .add(Aspect.AIR, 10)
                        .add(Aspect.ORDER, 20)
                        .add(Aspect.WATER, 30)
                        .add(Aspect.EARTH, 30)
                        .add(Aspect.FIRE, 10)
                        .add(Aspect.ENTROPY, 10),
                "TBT", "ACS", "TMT",
                'T', thaumium,
                'B', redstoneBlock,
                'A', alchemicalConstruct,
                'C', precisionMechanism,
                'S', shaft,
                'M', thaumiumBlock);

        // 涅里伊德之庇：注魔祭坛
        ItemStack copperDivingHelmet = stack("create", "copper_diving_helmet");
        ItemStack waterBreathingPotion = new ItemStack(Items.POTION);
        waterBreathingPotion.set(DataComponents.POTION_CONTENTS,
                new PotionContents(Potions.LONG_WATER_BREATHING));

        ThaumcraftApi.addInfusionCraftingRecipe(
                "NERIIDS_PROTECTION",
                new ItemStack(MysteryDynamics.NERIIDS_PROTECTION.get()),
                3,
                new AspectList()
                        .add(Aspect.AESTUS, 16)
                        .add(Aspect.WATER, 24)
                        .add(Aspect.TOOL, 4)
                        .add(Aspect.MOTION, 16),
                copperDivingHelmet,
                stack("thaumcraft", "thaumium_helm"),
                stack("thaumcraft", "thaumium_ingot"),
                stack("thaumcraft", "thaumium_ingot"),
                stack("thaumcraft", "thaumium_ingot"),
                stack("thaumcraft", "air_crystal_cluster"),
                new ItemStack(Items.OAK_DOOR),
                new ItemStack(Items.TURTLE_HELMET),
                waterBreathingPotion);

        // 高压动力发射系统：注魔祭坛
        ThaumcraftApi.addInfusionCraftingRecipe(
                "HIGH_PRESSURE_LAUNCHER",
                new ItemStack(MysteryDynamics.HIGH_PRESSURE_LAUNCHER.get()),
                8,
                new AspectList()
                        .add(Aspect.WEAPON, 12)
                        .add(Aspect.TOOL, 24)
                        .add(Aspect.MECHANISM, 24)
                        .add(Aspect.AIR, 32)
                        .add(Aspect.FIRE, 32)
                        .add(Aspect.METAL, 32),
                stack("create", "potato_cannon"),
                stack("thaumcraft", "alchemical_construct"),
                stack("thaumcraft", "bellows"),
                stack("thaumcraft", "air_shard"),
                stack("thaumcraft", "fire_shard"),
                stack("thaumcraft", "thaumium_ingot"),
                stack("thaumcraft", "thaumium_ingot"),
                stack("thaumcraft", "thaumium_ingot"),
                stack("thaumcraft", "thaumium_ingot"));
    }

    /** 书页居中显示（图案补到 3 格宽），合成时仍允许左右平移。 */
    private static void centered(String research, ItemStack output, AspectList aspects,
                                 int innerWidth, int innerHeight, Object... paddedRecipe) {
        ThaumcraftApi.getCraftingRecipes()
                .add(new CenteredArcaneRecipe(research, output, aspects, innerWidth, innerHeight, paddedRecipe));
    }

    private static ItemStack stack(String namespace, String path) {
        Item item = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(namespace, path));
        return new ItemStack(item);
    }
}
