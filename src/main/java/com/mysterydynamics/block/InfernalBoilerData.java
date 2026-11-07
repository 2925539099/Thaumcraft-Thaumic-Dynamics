package com.mysterydynamics.block;

import com.mysterydynamics.compat.InfernalBoilerCompat;
import com.simibubi.create.content.fluids.tank.BoilerData;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import com.simibubi.create.foundation.utility.CreateLang;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.List;
import java.util.Locale;


import net.neoforged.fml.ModList;

/**
 * 炼狱锅炉的锅炉数据。
 *
 * <p>未超频时严格沿用 Create 原版锅炉的效率公式：
 * 引擎数不超过热等级时效率为 1，超过后效率 = 热等级 / 引擎数。
 * 这样增加蒸汽引擎不会让总应力量反而下降。超频维持原有自定义上限。</p>
 */
public class InfernalBoilerData extends BoilerData {

    /** Create 蒸汽引擎单台最大应力输出。 */
    public static final float ENGINE_STRESS = 1024.0F;
    /** 原版满级锅炉护目镜显示：18 热 × 16 × 1024 = 294912 SU。 */
    public static final float VANILLA_MAX_BOILER_STRESS = 18.0F * 16.0F * ENGINE_STRESS;
    /** 超频时按原版满级锅炉最大应力的两倍计算。 */
    public static final float OVERCLOCK_TOTAL_STRESS = VANILLA_MAX_BOILER_STRESS * 2.0F;
    /** 模型上能装风箱的位置数，见 InfernalBoilerCompat.countBellows。 */
    private static final int MAX_BELLOWS = 3;
    private static final String INDENT = "    ";
    private static final String SUB_INDENT = "     ";

    private InfernalBoilerBlockEntity controller;

    @Override
    public void tick(FluidTankBlockEntity tank) {
        super.tick(tank);
        if (!(tank instanceof InfernalBoilerBlockEntity boiler) || !tank.isController()) {
            return;
        }
        this.controller = boiler;
        // 热量与引擎数只在服务端计算；客户端使用同步下来的值。
        // 客户端没有可靠的 fireVisTicks/引擎数，自行计算会让护目镜显示错误或来回闪烁。
        if (boiler.getLevel() == null || boiler.getLevel().isClientSide) {
            return;
        }
        int before = activeHeat;
        updateCustomHeat(boiler);
        // 引擎数量由 linkNearbySteamEngines/BoilerDataMixin 统一统计，
        // 这里不保存旧值，否则拆除涡轮机后会一直残留引擎数。
        this.attachedEngines = boiler.linkedEngineCount();
        if (before != activeHeat) {
            // 风箱/魔力/超频变化后立即同步热等级，不等 Create 的下一次采样。
            boiler.setChanged();
            boiler.sendData();
        }
    }

    @Override
    public boolean updateTemperature(FluidTankBlockEntity tank) {
        if (!(tank instanceof InfernalBoilerBlockEntity boiler)) {
            return super.updateTemperature(tank);
        }
        if (boiler.getLevel() == null || boiler.getLevel().isClientSide) {
            return false;
        }
        int before = activeHeat;
        updateCustomHeat(boiler);
        return before != activeHeat;
    }

    private void updateCustomHeat(InfernalBoilerBlockEntity boiler) {
        int heat;
        if (boiler.isOverclock()) {
            // 超频按原版满级锅炉的 18 热计算，才能显示/输出两倍最大应力。
            heat = 18;
        } else {
            // 按设计默认就是三级热量；水不再作为进入三级的前提。
            heat = 3;
            if (ModList.get().isLoaded("thaumcraft")) {
                heat += InfernalBoilerCompat.countBellows(boiler);
            }
            if (boiler.fireVisTicks() > 0) {
                heat += 9;
            }
        }
        this.activeHeat = Math.min(18, heat);
        this.passiveHeat = false;
        this.needsHeatLevelUpdate = false;
    }

    private boolean hasWater(FluidTankBlockEntity tank) {
        // 储罐里必须是水（岩浆等其它流体不算），或仍有管道供水。
        var fluid = tank.getTankInventory().getFluid();
        boolean storedWater = !fluid.isEmpty()
                && fluid.getFluid().is(net.minecraft.tags.FluidTags.WATER);
        return storedWater || (tank.boiler != null && tank.boiler.waterSupply > 0.0F);
    }

    @Override
    public boolean isActive() {
        return true;
    }

    @Override
    public boolean isPassive() {
        return false;
    }

    @Override
    public boolean isPassive(int attachedEngines) {
        return false;
    }

    @Override
    public MutableComponent getHeatLevelTextComponent() {
        if (controller != null && controller.isOverclock()) {
            return Component.empty().append(
                    Component.translatable("mystery_dynamics.infernal_boiler.goggles.overclock")
                            .withStyle(ChatFormatting.AQUA));
        }
        int heat = Math.min(18, Math.max(0, activeHeat));
        if (isPassive()) {
            return CreateLang.translateDirect("boiler.passive");
        }
        if (heat == 0) {
            return CreateLang.translateDirect("boiler.idle");
        }
        if (heat == 18) {
            return CreateLang.translateDirect("boiler.max_lvl");
        }
        return CreateLang.translateDirect("boiler.lvl", String.valueOf(heat));
    }

    @Override
    public int getTheoreticalHeatLevel() {
        return activeHeat;
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking, int boilerSize) {
        boolean result = super.addToGoggleTooltip(tooltip, isPlayerSneaking, boilerSize);
        if (!result || controller == null) {
            return result;
        }
        // Create 只有在接了引擎、且不处于“缺水显示进水速率”分支时，最后一行才是应力行。
        if (controller.isOverclock() && attachedEngines > 0
                && getMaxHeatLevelForWaterSupply() > 0 && !tooltip.isEmpty()) {
            String total = String.format(Locale.ROOT, "%,d",
                    Math.round(overclockCapacity(boilerSize)));
            tooltip.set(tooltip.size() - 1, Component.literal(SUB_INDENT).append(
                    Component.translatable("mystery_dynamics.infernal_boiler.goggles.overclock_capacity",
                            total, attachedEngines).withStyle(ChatFormatting.AQUA)));
        }

        tooltip.add(Component.empty());
        tooltip.add(Component.literal(INDENT).append(Component.translatable(
                "mystery_dynamics.infernal_boiler.goggles.fire_essentia",
                controller.fireEssentia(), InfernalBoilerBlockEntity.MAX_FIRE_ESSENTIA)
                .withStyle(ChatFormatting.GOLD)));
        int visTicks = controller.fireVisTicks();
        tooltip.add(Component.literal(INDENT).append(visTicks > 0
                ? Component.translatable("mystery_dynamics.infernal_boiler.goggles.vis_boost",
                        (visTicks + 19) / 20).withStyle(ChatFormatting.LIGHT_PURPLE)
                : Component.translatable("mystery_dynamics.infernal_boiler.goggles.vis_boost_none")
                        .withStyle(ChatFormatting.DARK_GRAY)));
        if (isPlayerSneaking) {
            int bellows = ModList.get().isLoaded("thaumcraft")
                    ? InfernalBoilerCompat.countBellows(controller) / 2 : 0;
            tooltip.add(Component.literal(INDENT).append(Component.translatable(
                    "mystery_dynamics.infernal_boiler.goggles.bellows", bellows, MAX_BELLOWS)
                    .withStyle(ChatFormatting.GRAY)));
        } else {
            tooltip.add(Component.literal(INDENT).append(
                    Component.translatable("mystery_dynamics.goggles.sneak_for_details")
                            .withStyle(ChatFormatting.DARK_GRAY)));
        }
        return result;
    }

    /**
     * 超频时的实际总应力：蒸汽引擎效率被 Create 限制在 1，仍按原版公式出力；
     * 只有奥法涡轮机共享两倍的锅炉应力上限（单台仍受上限约束）。
     */
    private float overclockCapacity(int boilerSize) {
        int engines = Math.max(1, attachedEngines);
        int boilerLevel = Math.min(activeHeat, Math.min(getMaxHeatLevelForWaterSupply(),
                getMaxHeatLevelForBoilerSize(boilerSize)));
        // 与 Create 护目镜相同的原版公式，按蒸汽引擎占比分摊。
        float vanillaTotal = getEngineEfficiency(boilerSize) * 16.0F
                * Math.max(boilerLevel, attachedEngines) * ENGINE_STRESS;
        int steamEngines = Math.min(controller.linkedSteamEngines(), engines);
        float steamPart = vanillaTotal * steamEngines / engines;
        float turbinePart = controller.linkedTurbines()
                * Math.min(ArcaneTurbineBlockEntity.MAX_STRESS, OVERCLOCK_TOTAL_STRESS / engines);
        return steamPart + turbinePart;
    }

    @Override
    public float getEngineEfficiency(int totalTankSize) {
        if (controller == null || attachedEngines <= 0) {
            return 0.0F;
        }
        int heat = Math.max(0, activeHeat);
        if (heat <= 0) {
            return 0.0F;
        }
        if (!hasWater(controller)) {
            return 0.0F;
        }
        if (!controller.isOverclock()) {
            // Create 原版：引擎数超过热等级后，单台效率 = 热等级 / 引擎数。
            return attachedEngines <= heat ? 1.0F : (float) heat / attachedEngines;
        }
        // 超频只翻倍锅炉总应力上限，不把每台蒸汽引擎的出力直接 ×2。
        // 蒸汽引擎仍按普通效率分配，单台仍受 16384 SU 上限约束。
        return attachedEngines <= heat ? 1.0F : (float) heat / attachedEngines;
    }
}
