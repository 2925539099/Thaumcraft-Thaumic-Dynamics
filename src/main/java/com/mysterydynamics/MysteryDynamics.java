package com.mysterydynamics;

import com.mysterydynamics.block.ArcaneTurbineBlock;
import com.mysterydynamics.block.ArcaneTurbineBlockEntity;
import com.mysterydynamics.block.GolemPowerBlock;
import com.mysterydynamics.block.GolemPowerBlockEntity;
import com.mysterydynamics.block.GolemPowerFrameBlock;
import com.mysterydynamics.block.InfernalBoilerBlock;
import com.mysterydynamics.block.InfernalBoilerBlockEntity;
import com.mysterydynamics.block.InfernalBoilerCasingBlock;
import com.mysterydynamics.block.InfernalBoilerCasingBlockEntity;
import com.mysterydynamics.block.InfernalBoilerFaceBlock;
import com.mysterydynamics.block.InfernalBoilerShellBlock;
import com.mysterydynamics.block.LargeCentrifugeCasingBlock;
import com.mysterydynamics.block.LargeCentrifugeCasingInfoBlockEntity;
import com.mysterydynamics.block.LargeCentrifugeControllerBlock;
import com.mysterydynamics.block.LargeCentrifugeControllerBlockEntity;
import com.mysterydynamics.block.LargeCentrifugeInputBlock;
import com.mysterydynamics.block.LargeCentrifugeInputBlockEntity;
import com.mysterydynamics.block.LargeCentrifugePortBlock;
import com.mysterydynamics.block.LargeCentrifugePortBlockEntity;
import com.mysterydynamics.block.RedstoneEngineBlock;
import com.mysterydynamics.block.RedstoneEngineBlockEntity;
import com.mysterydynamics.compat.InfernalBoilerConstruct;
import com.mysterydynamics.compat.InfernalBoilerStructure;
import com.mysterydynamics.compat.LargeCentrifugeConstruct;
import com.mysterydynamics.item.HighPressureLauncherItem;
import com.mysterydynamics.item.NeriidsProtectionItem;
import com.mysterydynamics.item.RevealingEngineerGogglesItem;
import com.simibubi.create.api.stress.BlockStressValues;
import com.simibubi.create.content.equipment.goggles.GogglesItem;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import thaumcraft.api.research.ResearchBookApi;
import thaumcraft.api.research.ResearchRecipePageRegistry;
import thaumcraft.common.registry.TCItems;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.List;

/**
 * Thaumcraft: 神秘动力学（Thaumic Dynamics）。
 *
 * <p>8 台傀儡动力系统（应力源）+ 1 个空框架。转速 / 应力来自「傀儡动力设计.txt」。</p>
 */
@Mod(MysteryDynamics.MODID)
public class MysteryDynamics {

    public static final String MODID = "mystery_dynamics";

    static {
        // 结构页必须双端可见，否则服务端加载研究会丢弃 CONSTRUCT 页面。
        ResearchBookApi.addConstructSource(key -> {
            if ("InfernalBoiler".equalsIgnoreCase(key)
                    || "INFERNAL_BOILER".equalsIgnoreCase(key)) {
                return java.util.List.of(InfernalBoilerConstruct.create());
            }
            if ("LargeCentrifuge".equalsIgnoreCase(key)
                    || "LARGE_CENTRIFUGE".equalsIgnoreCase(key)) {
                return java.util.List.of(LargeCentrifugeConstruct.create());
            }
            return java.util.List.of();
        });
    }

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MODID);
    public static final DeferredRegister<ArmorMaterial> ARMOR_MATERIALS =
            DeferredRegister.create(Registries.ARMOR_MATERIAL, MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    // ---- 8 台傀儡动力系统：id, 转速 RPM, 总应力 SU ----
    public static final DeferredBlock<GolemPowerBlock> STRAW_GOLEM_POWER =
            registerGolem("straw_golem_power", 160.0F, 128.0F);
    public static final DeferredBlock<GolemPowerBlock> WOOD_GOLEM_POWER =
            registerGolem("wood_golem_power", 128.0F, 256.0F);
    public static final DeferredBlock<GolemPowerBlock> FLESH_GOLEM_POWER =
            registerGolem("flesh_golem_power", 160.0F, 384.0F);
    public static final DeferredBlock<GolemPowerBlock> CLAY_GOLEM_POWER =
            registerGolem("clay_golem_power", 128.0F, 512.0F);
    public static final DeferredBlock<GolemPowerBlock> TALLOW_GOLEM_POWER =
            registerGolem("tallow_golem_power", 256.0F, 128.0F);
    public static final DeferredBlock<GolemPowerBlock> STONE_GOLEM_POWER =
            registerGolem("stone_golem_power", 64.0F, 1024.0F);
    public static final DeferredBlock<GolemPowerBlock> IRON_GOLEM_POWER =
            registerGolem("iron_golem_power", 32.0F, 2048.0F);
    public static final DeferredBlock<GolemPowerBlock> MYSTERY_GOLEM_POWER =
            registerGolem("mystery_golem_power", 128.0F, 4096.0F);

    public static final List<DeferredBlock<GolemPowerBlock>> GENERATORS = List.of(
            STRAW_GOLEM_POWER, WOOD_GOLEM_POWER, FLESH_GOLEM_POWER, CLAY_GOLEM_POWER,
            TALLOW_GOLEM_POWER, STONE_GOLEM_POWER, IRON_GOLEM_POWER, MYSTERY_GOLEM_POWER);

    /** 空框架：纯装饰。 */
    public static final DeferredBlock<GolemPowerFrameBlock> GOLEM_POWER_FRAME = BLOCKS.register(
            "golem_power_frame", () -> new GolemPowerFrameBlock(deviceProperties()));

    /** 红石发动机：红石信号 1~15 → 32~256 RPM、128~8192 SU。 */
    public static final DeferredBlock<RedstoneEngineBlock> REDSTONE_ENGINE = BLOCKS.register(
            "redstone_engine", () -> new RedstoneEngineBlock(redstoneEngineProperties()));

    /** 炼狱锅炉 3×3×3 主体，继承 Create 流体储罐。 */
    public static final DeferredBlock<InfernalBoilerBlock> INFERNAL_BOILER = BLOCKS.register(
            "infernal_boiler", () -> new InfernalBoilerBlock(infernalBoilerProperties()));

    /** 炼狱锅炉正面观察窗；超频时显示超级燃烧层。 */
    public static final DeferredBlock<InfernalBoilerFaceBlock> INFERNAL_BOILER_FACE = BLOCKS.register(
            "infernal_boiler_face", () -> new InfernalBoilerFaceBlock(infernalBoilerFaceProperties()));

    /** 成型后底座填充块；不渲染，由 shell 整体模型覆盖。 */
    public static final DeferredBlock<InfernalBoilerCasingBlock> INFERNAL_BOILER_CASING = BLOCKS.register(
            "infernal_boiler_casing", () -> new InfernalBoilerCasingBlock(infernalBoilerCasingProperties()));

    /** 整栋锅炉模型（4×4×5）的承载方块。 */
    public static final DeferredBlock<InfernalBoilerShellBlock> INFERNAL_BOILER_SHELL = BLOCKS.register(
            "infernal_boiler_shell", () -> new InfernalBoilerShellBlock(infernalBoilerShellProperties()));

    /** 大型奥法离心机：3×3×3 中间层中心控制器。 */
    public static final DeferredBlock<LargeCentrifugeControllerBlock> LARGE_CENTRIFUGE_CONTROLLER =
            BLOCKS.register("large_centrifuge_controller",
                    () -> new LargeCentrifugeControllerBlock(largeCentrifugeProperties()));

    /** 大型奥法离心机不可见填充块。 */
    public static final DeferredBlock<LargeCentrifugeCasingBlock> LARGE_CENTRIFUGE_CASING =
            BLOCKS.register("large_centrifuge_casing",
                    () -> new LargeCentrifugeCasingBlock(largeCentrifugeProperties()));

    /** 大型奥法离心机顶部应力输入口。 */
    public static final DeferredBlock<LargeCentrifugeInputBlock> LARGE_CENTRIFUGE_INPUT =
            BLOCKS.register("large_centrifuge_input",
                    () -> new LargeCentrifugeInputBlock(largeCentrifugeProperties()));

    /** 大型奥法离心机西面源质输入端口。 */
    public static final DeferredBlock<LargeCentrifugePortBlock> LARGE_CENTRIFUGE_INPUT_PORT =
            BLOCKS.register("large_centrifuge_input_port",
                    () -> new LargeCentrifugePortBlock(largeCentrifugeProperties(), true));

    /** 大型奥法离心机东面源质输出端口。 */
    public static final DeferredBlock<LargeCentrifugePortBlock> LARGE_CENTRIFUGE_OUTPUT_PORT =
            BLOCKS.register("large_centrifuge_output_port",
                    () -> new LargeCentrifugePortBlock(largeCentrifugeProperties(), false));

    /** 奥法涡轮机：两格高，一次放置成型。 */
    public static final DeferredBlock<ArcaneTurbineBlock> ARCANE_TURBINE =
            BLOCKS.register("arcane_turbine",
                    () -> new ArcaneTurbineBlock(arcaneTurbineProperties()));

    // ---- 物品 ----
    public static final DeferredItem<BlockItem> STRAW_ITEM = ITEMS.registerSimpleBlockItem(STRAW_GOLEM_POWER);
    public static final DeferredItem<BlockItem> WOOD_ITEM = ITEMS.registerSimpleBlockItem(WOOD_GOLEM_POWER);
    public static final DeferredItem<BlockItem> FLESH_ITEM = ITEMS.registerSimpleBlockItem(FLESH_GOLEM_POWER);
    public static final DeferredItem<BlockItem> CLAY_ITEM = ITEMS.registerSimpleBlockItem(CLAY_GOLEM_POWER);
    public static final DeferredItem<BlockItem> TALLOW_ITEM = ITEMS.registerSimpleBlockItem(TALLOW_GOLEM_POWER);
    public static final DeferredItem<BlockItem> STONE_ITEM = ITEMS.registerSimpleBlockItem(STONE_GOLEM_POWER);
    public static final DeferredItem<BlockItem> IRON_ITEM = ITEMS.registerSimpleBlockItem(IRON_GOLEM_POWER);
    public static final DeferredItem<BlockItem> MYSTERY_ITEM = ITEMS.registerSimpleBlockItem(MYSTERY_GOLEM_POWER);
    public static final DeferredItem<BlockItem> FRAME_ITEM = ITEMS.registerSimpleBlockItem(GOLEM_POWER_FRAME);
    public static final DeferredItem<BlockItem> REDSTONE_ENGINE_ITEM = ITEMS.registerSimpleBlockItem(REDSTONE_ENGINE);
    public static final DeferredItem<BlockItem> INFERNAL_BOILER_ITEM = ITEMS.registerSimpleBlockItem(INFERNAL_BOILER);
    public static final DeferredItem<BlockItem> INFERNAL_BOILER_FACE_ITEM = ITEMS.registerSimpleBlockItem(INFERNAL_BOILER_FACE);
    public static final DeferredItem<BlockItem> ARCANE_TURBINE_ITEM = ITEMS.registerSimpleBlockItem(ARCANE_TURBINE);

    // ---- 揭示之工程师护目镜：护甲值继承揭示之护目镜，无耐久 ----
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> REVEALING_ENGINEER_GOGGLES_MATERIAL =
            ARMOR_MATERIALS.register("revealing_engineer_goggles", () -> new ArmorMaterial(
                    java.util.Map.of(
                            ArmorItem.Type.BOOTS, 1,
                            ArmorItem.Type.LEGGINGS, 2,
                            ArmorItem.Type.CHESTPLATE, 3,
                            ArmorItem.Type.HELMET, 1),
                    25,
                    SoundEvents.ARMOR_EQUIP_GOLD,
                    () -> Ingredient.of(Items.SLIME_BALL),
                    List.of(new ArmorMaterial.Layer(
                            ResourceLocation.fromNamespaceAndPath(MODID, "revealing_engineer_goggles"), "", false)),
                    0.0F,
                    0.0F));

    public static final DeferredItem<RevealingEngineerGogglesItem> REVEALING_ENGINEER_GOGGLES =
            ITEMS.register("revealing_engineer_goggles",
                    () -> new RevealingEngineerGogglesItem(
                            new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));

    // ---- 涅里伊德之庇：3 点护甲，耐久与神秘头盔一致 ----
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> NERIIDS_PROTECTION_MATERIAL =
            ARMOR_MATERIALS.register("neriids_protection", () -> new ArmorMaterial(
                    java.util.Map.of(
                            ArmorItem.Type.BOOTS, 1,
                            ArmorItem.Type.LEGGINGS, 2,
                            ArmorItem.Type.CHESTPLATE, 3,
                            ArmorItem.Type.HELMET, 3),
                    25,
                    SoundEvents.ARMOR_EQUIP_IRON,
                    () -> Ingredient.of(TCItems.THAUMIUM_INGOT.get()),
                    List.of(new ArmorMaterial.Layer(
                            ResourceLocation.fromNamespaceAndPath(MODID, "neriids_protection"), "", false)),
                    0.0F,
                    0.0F));

    public static final DeferredItem<NeriidsProtectionItem> NERIIDS_PROTECTION =
            ITEMS.register("neriids_protection",
                    () -> new NeriidsProtectionItem(
                            new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));

    // ---- 高压动力发射器：暂用土豆加农炮的全部数据 ----
    public static final DeferredItem<HighPressureLauncherItem> HIGH_PRESSURE_LAUNCHER =
            ITEMS.register("high_pressure_launcher",
                    () -> new HighPressureLauncherItem(
                            new Item.Properties().stacksTo(1).durability(400).rarity(Rarity.UNCOMMON)));

    // ---- 方块实体（8 台设备共用） ----
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GolemPowerBlockEntity>> GOLEM_POWER_BE =
            BLOCK_ENTITY_TYPES.register("golem_power", () -> BlockEntityType.Builder.of(
                    (pos, state) -> new GolemPowerBlockEntity(blockEntityType(), pos, state),
                    GENERATORS.stream().map(DeferredBlock::get).toArray(Block[]::new)).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RedstoneEngineBlockEntity>> REDSTONE_ENGINE_BE =
            BLOCK_ENTITY_TYPES.register("redstone_engine", () -> BlockEntityType.Builder.of(
                    (pos, state) -> new RedstoneEngineBlockEntity(redstoneEngineType(), pos, state),
                    REDSTONE_ENGINE.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<InfernalBoilerBlockEntity>> INFERNAL_BOILER_BE =
            BLOCK_ENTITY_TYPES.register("infernal_boiler", () -> BlockEntityType.Builder.of(
                    (pos, state) -> new InfernalBoilerBlockEntity(infernalBoilerType(), pos, state),
                    INFERNAL_BOILER.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<InfernalBoilerCasingBlockEntity>> INFERNAL_BOILER_CASING_BE =
            BLOCK_ENTITY_TYPES.register("infernal_boiler_casing", () -> BlockEntityType.Builder.of(
                    (pos, state) -> new InfernalBoilerCasingBlockEntity(infernalBoilerCasingType(), pos, state),
                    INFERNAL_BOILER_CASING.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ArcaneTurbineBlockEntity>> ARCANE_TURBINE_BE =
            BLOCK_ENTITY_TYPES.register("arcane_turbine", () -> BlockEntityType.Builder.of(
                    (pos, state) -> new ArcaneTurbineBlockEntity(arcaneTurbineType(), pos, state),
                    ARCANE_TURBINE.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LargeCentrifugeControllerBlockEntity>> LARGE_CENTRIFUGE_CONTROLLER_BE =
            BLOCK_ENTITY_TYPES.register("large_centrifuge_controller", () -> BlockEntityType.Builder.of(
                    (pos, state) -> new LargeCentrifugeControllerBlockEntity(
                            largeCentrifugeControllerType(), pos, state),
                    LARGE_CENTRIFUGE_CONTROLLER.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LargeCentrifugeInputBlockEntity>> LARGE_CENTRIFUGE_INPUT_BE =
            BLOCK_ENTITY_TYPES.register("large_centrifuge_input", () -> BlockEntityType.Builder.of(
                    (pos, state) -> new LargeCentrifugeInputBlockEntity(
                            largeCentrifugeInputType(), pos, state),
                    LARGE_CENTRIFUGE_INPUT.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LargeCentrifugePortBlockEntity>> LARGE_CENTRIFUGE_PORT_BE =
            BLOCK_ENTITY_TYPES.register("large_centrifuge_port", () -> BlockEntityType.Builder.of(
                    (pos, state) -> new LargeCentrifugePortBlockEntity(
                            largeCentrifugePortType(), pos, state,
                            state.is(LARGE_CENTRIFUGE_INPUT_PORT.get())),
                    LARGE_CENTRIFUGE_INPUT_PORT.get(), LARGE_CENTRIFUGE_OUTPUT_PORT.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LargeCentrifugeCasingInfoBlockEntity>> LARGE_CENTRIFUGE_CASING_BE =
            BLOCK_ENTITY_TYPES.register("large_centrifuge_casing", () -> BlockEntityType.Builder.of(
                    (pos, state) -> new LargeCentrifugeCasingInfoBlockEntity(
                            largeCentrifugeCasingType(), pos, state),
                    LARGE_CENTRIFUGE_CASING.get()).build(null));

    /** 独立创造模式标签页。 */
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = CREATIVE_TABS.register("main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.mystery_dynamics"))
                    .icon(() -> new ItemStack(MYSTERY_ITEM.get()))
                    .displayItems((params, output) -> {
                        GENERATORS.forEach(block -> output.accept(block.get()));
                        output.accept(GOLEM_POWER_FRAME.get());
                        output.accept(REDSTONE_ENGINE.get());
                        output.accept(INFERNAL_BOILER.get());
                        output.accept(INFERNAL_BOILER_FACE.get());
                        output.accept(REVEALING_ENGINEER_GOGGLES.get());
                        output.accept(NERIIDS_PROTECTION.get());
                        output.accept(HIGH_PRESSURE_LAUNCHER.get());
                        output.accept(ARCANE_TURBINE_ITEM.get());
                    })
                    .build());

    public MysteryDynamics(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        ARMOR_MATERIALS.register(modEventBus);
        BLOCK_ENTITY_TYPES.register(modEventBus);
        CREATIVE_TABS.register(modEventBus);
        modEventBus.addListener(MysteryDynamics::commonSetup);
        modEventBus.addListener(MysteryDynamics::registerCapabilities);
    }

    private static DeferredBlock<GolemPowerBlock> registerGolem(String name, float rpm, float stressUnits) {
        return BLOCKS.register(name, () -> new GolemPowerBlock(deviceProperties(), rpm, stressUnits));
    }

    private static BlockBehaviour.Properties deviceProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.STONE)
                // 与石头一致：硬度 1.5，爆炸抗性 6.0
                .strength(1.5F, 6.0F)
                .requiresCorrectToolForDrops()
                .sound(SoundType.STONE)
                // 模型不是满方块（前后开口），不能按满方块遮挡邻居，
                // 否则贴着设备的方块面会被剔除，出现“透视”破洞
                .noOcclusion();
    }

    private static BlockEntityType<?> blockEntityType() {
        return GOLEM_POWER_BE.get();
    }

    /**
     * 与神秘方块（thaumcraft:thaumium_block）一致：
     * 金属地图色、硬度 4、抗性 20、石质音效、需要正确工具才掉落。
     * 额外保留 noOcclusion 以适配红石发动机的非满模型。
     */
    private static BlockBehaviour.Properties redstoneEngineProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .strength(4.0F, 20.0F)
                .requiresCorrectToolForDrops()
                .sound(SoundType.STONE)
                .noOcclusion();
    }

    private static BlockEntityType<?> redstoneEngineType() {
        return REDSTONE_ENGINE_BE.get();
    }

    private static BlockEntityType<?> infernalBoilerType() {
        return INFERNAL_BOILER_BE.get();
    }

    private static BlockEntityType<?> infernalBoilerCasingType() {
        return INFERNAL_BOILER_CASING_BE.get();
    }

    private static BlockEntityType<?> largeCentrifugeControllerType() {
        return LARGE_CENTRIFUGE_CONTROLLER_BE.get();
    }

    private static BlockEntityType<?> arcaneTurbineType() {
        return ARCANE_TURBINE_BE.get();
    }

    private static BlockEntityType<?> largeCentrifugeInputType() {
        return LARGE_CENTRIFUGE_INPUT_BE.get();
    }

    private static BlockEntityType<?> largeCentrifugePortType() {
        return LARGE_CENTRIFUGE_PORT_BE.get();
    }

    private static BlockEntityType<?> largeCentrifugeCasingType() {
        return LARGE_CENTRIFUGE_CASING_BE.get();
    }

    private static BlockBehaviour.Properties arcaneTurbineProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_PURPLE)
                .strength(2.0F, 6.0F)
                .sound(SoundType.COPPER)
                .requiresCorrectToolForDrops()
                .noOcclusion()
                .noLootTable()
                .dynamicShape();
    }

    private static BlockBehaviour.Properties largeCentrifugeProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .strength(3.0F, 12.0F)
                .sound(SoundType.METAL)
                .noOcclusion()
                .noLootTable();
    }

    private static BlockBehaviour.Properties infernalBoilerProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .strength(4.0F, 20.0F)
                .requiresCorrectToolForDrops()
                .sound(SoundType.STONE)
                .noOcclusion();
    }

    private static BlockBehaviour.Properties infernalBoilerFaceProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_LIGHT_BLUE)
                .strength(0.3F)
                .sound(SoundType.GLASS)
                .noOcclusion();
    }

    private static BlockBehaviour.Properties infernalBoilerCasingProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.NETHER)
                .strength(2.0F, 6.0F)
                .sound(SoundType.NETHER_BRICKS)
                .noOcclusion();
    }

    private static BlockBehaviour.Properties infernalBoilerShellProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.NETHER)
                .strength(2.0F, 6.0F)
                .sound(SoundType.NETHER_BRICKS)
                .noOcclusion();
    }

    private static IFluidHandler findBoilerTank(Level level, BlockPos pos) {
        InfernalBoilerBlockEntity controller = InfernalBoilerStructure.findController(level, pos);
        return controller == null ? null : controller.boiler.createHandler();
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, INFERNAL_BOILER_BE.get(),
                (be, side) -> {
                    FluidTankBlockEntity controller = be.isController() ? be : be.getControllerBE();
                    return controller == null ? null : controller.boiler.createHandler();
                });
        // 底座/正面/shell 本身没有 BE，但管道应能把水送进整栋锅炉的储罐。
        event.registerBlock(Capabilities.FluidHandler.BLOCK,
                (level, pos, state, blockEntity, side) -> findBoilerTank(level, pos),
                INFERNAL_BOILER_CASING.get(), INFERNAL_BOILER_SHELL.get(),
                INFERNAL_BOILER_FACE.get());
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK,
                INFERNAL_BOILER_CASING_BE.get(), (be, side) -> be);
        if (ModList.get().isLoaded("thaumcraft")) {
            com.mysterydynamics.compat.InfernalBoilerCompat.registerCapabilities(event);
            com.mysterydynamics.compat.LargeCentrifugeCompat.registerCapabilities(event);
        }
    }

    private static void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            // 高压动力发射器沿用土豆加农炮的 Shift 概要，但移除蔬菜那句。
            com.simibubi.create.foundation.item.ItemDescription.useKey(
                    HIGH_PRESSURE_LAUNCHER.get(), "item.mystery_dynamics.high_pressure_launcher");
            com.simibubi.create.foundation.item.TooltipModifier.REGISTRY.register(
                    HIGH_PRESSURE_LAUNCHER.get(),
                    new com.simibubi.create.foundation.item.ItemDescription.Modifier(
                            HIGH_PRESSURE_LAUNCHER.get(),
                            net.createmod.catnip.lang.FontHelper.Palette.STANDARD_CREATE));
            for (DeferredBlock<GolemPowerBlock> holder : GENERATORS) {
                GolemPowerBlock block = holder.get();
                BlockStressValues.CAPACITIES.register(block, () -> (double) block.capacityPerRpm());
                BlockStressValues.RPM.register(block,
                        new BlockStressValues.GeneratedRpm(Math.round(block.rpm()), false));
            }
            // 红石发动机的容量随信号变化，具体由方块实体覆盖；这里登记满信号值供提示使用
            BlockStressValues.CAPACITIES.register(REDSTONE_ENGINE.get(),
                    () -> (double) (RedstoneEngineBlockEntity.MAX_STRESS / RedstoneEngineBlockEntity.MAX_RPM));
            BlockStressValues.RPM.register(REDSTONE_ENGINE.get(),
                    new BlockStressValues.GeneratedRpm(Math.round(RedstoneEngineBlockEntity.MAX_RPM), false));
            // 安装了神秘时代时才注册奥术工作台配方（未安装时不加载 compat 类）
            if (net.neoforged.fml.ModList.get().isLoaded("thaumcraft")) {
                com.mysterydynamics.compat.ThaumcraftRecipes.register();
                com.mysterydynamics.compat.InfernalBoilerCompat.register();
                com.mysterydynamics.compat.LargeCentrifugeCompat.register();
            }
            // 让工程师护目镜的信息覆盖层也识别本物品：头盔栏或 Curio 头部栏。
            GogglesItem.addIsWearingPredicate(player ->
                    player.getItemBySlot(EquipmentSlot.HEAD).is(REVEALING_ENGINEER_GOGGLES.get())
                            || CuriosApi.getCuriosInventory(player)
                            .map(inv -> inv.isEquipped(REVEALING_ENGINEER_GOGGLES.get()))
                            .orElse(false));
            // 两种无序合成都放进同一个研究页，页面内可切换/分组显示。
            ResearchRecipePageRegistry.registerNormalRecipes("REVEALING_ENGINEER_GOGGLES",
                    ResourceLocation.fromNamespaceAndPath(MODID, "revealing_engineer_goggles_slime"),
                    ResourceLocation.fromNamespaceAndPath(MODID, "revealing_engineer_goggles_string"));
        });
    }
}
