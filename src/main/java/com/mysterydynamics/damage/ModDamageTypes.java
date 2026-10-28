package com.mysterydynamics.damage;

import com.mysterydynamics.MysteryDynamics;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.level.Level;

/** 大型奥法离心机旋转区域的伤害类型。 */
public final class ModDamageTypes {

    public static final ResourceKey<DamageType> CENTRIFUGE = ResourceKey.create(
            Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath(MysteryDynamics.MODID, "centrifuge"));

    private ModDamageTypes() {
    }

    public static DamageSource centrifuge(Level level) {
        return new DamageSource(level.registryAccess()
                .registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(CENTRIFUGE));
    }
}
