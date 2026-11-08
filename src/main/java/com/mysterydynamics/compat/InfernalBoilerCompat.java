package com.mysterydynamics.compat;

import com.mysterydynamics.MysteryDynamics;
import com.mysterydynamics.block.InfernalBoilerBlockEntity;

import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.bus.api.EventPriority;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.EssentiaCapabilities;
import thaumcraft.api.aspects.IEssentiaTransport;
import thaumcraft.api.wands.WandTriggerRegistry;
import thaumcraft.common.blocks.BellowsBlock;
import thaumcraft.common.items.TCFunctionalItems;
import thaumcraft.common.registry.TCBlocks;
import thaumcraft.common.vis.TCVisHelper;

/** 只在安装神秘时代时加载的兼容层。 */
public final class InfernalBoilerCompat {

    private static final int[][] BELLOW_POSITIONS = {{4, 0, 1}, {4, 0, 2}, {2, 0, -1}};

    private InfernalBoilerCompat() {
    }

    public static void register() {
        // 不只在玻璃上：点击任意结构方块都能触发成型。
        WandTriggerRegistry.registerWandBlockTrigger(new InfernalBoilerTrigger(), 0,
                InfernalBoilerCompat::isStructureBlock,
                "infernal_boiler_structure");

        // 守卫者玻璃的 useItemOn 会拦截法杖，所以额外在交互事件里处理所有结构方块。
        // receiveCanceled = false：领地/保护模组取消交互后不再成型。
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, false,
                InfernalBoilerCompat::onRightClickBlock);
    }

    private static boolean isStructureBlock(BlockState state) {
        return state.is(Blocks.OBSIDIAN)
                || state.is(Blocks.NETHER_BRICKS)
                || state.is(TCBlocks.THAUMIUM_BLOCK.get())
                || state.is(TCBlocks.ALCHEMICAL_CONSTRUCT.get())
                || state.is(TCBlocks.ADVANCED_ALCHEMICAL_CONSTRUCT.get())
                || state.is(TCBlocks.ALCHEMICAL_FURNACE.get())
                || state.is(TCBlocks.ADVANCED_ALCHEMICAL_FURNACE.get())
                || state.is(TCBlocks.ARCANE_FURNACE.get())
                || state.is(TCBlocks.ARCANE_FURNACE_CASING.get())
                || state.is(TCBlocks.WARDED_GLASS.get());
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
        InfernalBoilerTrigger.Outcome outcome =
                InfernalBoilerTrigger.attempt(event.getLevel(), stack, player, event.getPos());
        if (outcome != InfernalBoilerTrigger.Outcome.NOT_APPLICABLE) {
            // 失败时同样吞掉这次点击：否则法杖物品还会经由 WandTriggerRegistry
            // 再触发一次，导致提示与结构扫描重复。
            event.setCanceled(true);
            event.setCancellationResult(outcome == InfernalBoilerTrigger.Outcome.FORMED
                    ? InteractionResult.SUCCESS : InteractionResult.CONSUME);
        }
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(EssentiaCapabilities.TRANSPORT,
                MysteryDynamics.INFERNAL_BOILER_BE.get(),
                (be, side) -> new InfernalBoilerEssentiaHandler(be, side, be.getBlockPos()));
        event.registerBlockEntity(EssentiaCapabilities.TRANSPORT,
                MysteryDynamics.INFERNAL_BOILER_CASING_BE.get(),
                (be, side) -> be.boilerController() == null ? null
                        : new InfernalBoilerEssentiaHandler(be.boilerController(), side, be.getBlockPos()));
        event.registerBlock(EssentiaCapabilities.TRANSPORT,
                (level, pos, state, blockEntity, side) -> {
                    InfernalBoilerBlockEntity controller =
                            InfernalBoilerStructure.findController(level, pos);
                    return controller == null ? null
                            : new InfernalBoilerEssentiaHandler(controller, side, pos);
                },
                MysteryDynamics.INFERNAL_BOILER_CASING.get(),
                MysteryDynamics.INFERNAL_BOILER_SHELL.get(),
                MysteryDynamics.INFERNAL_BOILER_FACE.get());
    }

    /**
     * 从 pasted7 端口相邻的源质管道主动抽取 1 点火源质。
     * 这是对 Create/Thaumcraft 吸力判定的兼容：锅炉本身相当于一个带吸力的容器。
     */
    public static void pullFireEssentiaFromPort(InfernalBoilerBlockEntity boiler) {
        if (boiler.fireEssentia() >= InfernalBoilerBlockEntity.MAX_FIRE_ESSENTIA) {
            return;
        }
        Level level = boiler.getLevel();
        if (level == null || level.isClientSide) {
            return;
        }
        if (!boiler.hasFormation()) {
            return;
        }
        var origin = boiler.boilerOrigin();
        var port = InfernalBoilerStructure.world(origin, 2, 1, 3, boiler.rotation());
        Direction front = boiler.frontDirection();
        for (Direction side : new Direction[]{front, front.getOpposite()}) {
            var neighbor = port.relative(side);
            var transport = EssentiaCapabilities.find(level, neighbor, side.getOpposite());
            if (transport == null) {
                continue;
            }
            int taken = transport.takeEssentia(Aspect.FIRE, 1, side.getOpposite());
            if (taken > 0) {
                boiler.addFireEssentia(taken);
                return;
            }
        }
    }

    /**
     * 尝试从附近节点/中继器抽取 1 点 Ignis，给锅炉 200 tick 的 +9 热等级。
     * 只在锅炉真正工作（接有引擎）时抽取，空闲时不白白消耗灵气。
     */
    public static void tickVis(InfernalBoilerBlockEntity boiler) {
        Level level = boiler.getLevel();
        if (level == null || level.isClientSide || boiler.fireVisTicks() > 0
                || !boiler.isWorking()) {
            return;
        }
        if (level.getGameTime() % 20L != 0L) {
            return;
        }
        int drained = TCVisHelper.drainFromNearbyEnergizedNodes(level, boiler.getBlockPos(), Aspect.FIRE, 1);
        if (drained <= 0) {
            // 普通灵气节点（未充能）也要算；工作台/炼狱熔炉的节点通常就是普通节点。
            drained = TCVisHelper.drainFromNearbyNodes(level, boiler.getBlockPos(), Aspect.FIRE, 1, 8);
        }
        if (drained <= 0) {
            drained = TCVisHelper.drainFromNearbyRelays(level, boiler.getBlockPos(), Aspect.FIRE, 1,
                    TCVisHelper.newVisited(boiler.getBlockPos()));
        }
        if (drained > 0) {
            boiler.setFireVisTicks(200);
        }
    }

    /**
     * 只有模型里 pasted6 贴图对应的三个面能装风箱：
     * 东侧 (4,0,1)/(4,0,2)，北侧 (2,0,-1)。
     * 每个 +2 热等级，最多三个。
     */
    public static int countBellows(InfernalBoilerBlockEntity boiler) {
        Level level = boiler.getLevel();
        if (level == null) {
            return 0;
        }
        var origin = boiler.hasFormation()
                ? boiler.boilerOrigin() : boiler.getBlockPos().offset(-1, -3, -1);
        int rotation = boiler.rotation();
        int count = 0;
        for (int[] p : BELLOW_POSITIONS) {
            var pos = InfernalBoilerStructure.world(origin, p[0], p[1], p[2], rotation);
            if (level.getBlockState(pos).getBlock() instanceof BellowsBlock) {
                count++;
            }
        }
        return count * 2;
    }
}
