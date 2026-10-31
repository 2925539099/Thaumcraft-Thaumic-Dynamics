package com.mysterydynamics.block;

import com.mysterydynamics.compat.InfernalBoilerCompat;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.api.stress.BlockStressValues;
import com.simibubi.create.content.fluids.tank.BoilerData;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import com.simibubi.create.foundation.utility.CreateLang;

import net.minecraft.network.chat.MutableComponent;

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

    private InfernalBoilerBlockEntity controller;

    @Override
    public void tick(FluidTankBlockEntity tank) {
        super.tick(tank);
        if (!(tank instanceof InfernalBoilerBlockEntity boiler) || !tank.isController()) {
            return;
        }
        this.controller = boiler;
        updateCustomHeat(boiler);
        // 只在服务端写引擎数；客户端使用同步下来的值。
        // 否则客户端 linkedEngineCount 永远是0，护目镜会在0和真实值之间闪烁。
        if (boiler.getLevel() != null && !boiler.getLevel().isClientSide) {
            // 引擎数量由 linkNearbySteamEngines/BoilerDataMixin 统一统计，
            // 这里不保存旧值，否则拆除涡轮机后会一直残留引擎数。
            this.attachedEngines = boiler.linkedEngineCount();
        }
    }

    @Override
    public boolean updateTemperature(FluidTankBlockEntity tank) {
        if (!(tank instanceof InfernalBoilerBlockEntity boiler)) {
            return super.updateTemperature(tank);
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
        // 有任意储量（正常情况就是水）或仍有管道供水，都允许工作。
        // 完全空仓且没有 waterSupply 时才停机。
        return tank.getTankInventory().getFluidAmount() > 0
                || (tank.boiler != null && tank.boiler.waterSupply > 0.0F);
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
