package com.mysterydynamics.client;

import com.mysterydynamics.MysteryDynamics;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.minecraft.resources.ResourceLocation;

/** 提前注册，确保模型烘焙前进入 PartialModel 表。 */
public final class ArcaneTurbinePartialModels {

    public static final PartialModel LOWER_ROTOR = PartialModel.of(
            ResourceLocation.fromNamespaceAndPath(MysteryDynamics.MODID, "block/arcane_turbine_lower_rotor"));
    public static final PartialModel UPPER_ROTOR = PartialModel.of(
            ResourceLocation.fromNamespaceAndPath(MysteryDynamics.MODID, "block/arcane_turbine_upper_rotor"));

    private ArcaneTurbinePartialModels() {
    }

    public static void init() {
        // 触发类加载。
    }
}
