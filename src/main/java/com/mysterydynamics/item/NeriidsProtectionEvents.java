package com.mysterydynamics.item;

import com.mysterydynamics.MysteryDynamics;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/** 涅里伊德之庇的服务器侧效果。 */
@EventBusSubscriber(modid = MysteryDynamics.MODID)
public final class NeriidsProtectionEvents {

    private static final String WATER_BREATHING_TAG = "mystery_dynamics.neriids_water_breathing";

    private NeriidsProtectionEvents() {
    }

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide) {
            return;
        }
        CompoundTag data = player.getPersistentData();
        boolean wearing = player.getItemBySlot(EquipmentSlot.HEAD)
                .is(MysteryDynamics.NERIIDS_PROTECTION.get());

        if (wearing) {
            MobEffectInstance current = player.getEffect(MobEffects.WATER_BREATHING);
            if (current == null || !current.isAmbient() || current.getDuration() != -1) {
                player.addEffect(new MobEffectInstance(
                        MobEffects.WATER_BREATHING, -1, 0, true, false, false));
            }
            data.putBoolean(WATER_BREATHING_TAG, true);
            return;
        }

        if (data.getBoolean(WATER_BREATHING_TAG)) {
            MobEffectInstance current = player.getEffect(MobEffects.WATER_BREATHING);
            if (current != null && current.isAmbient()
                    && !current.isVisible() && current.getDuration() == -1) {
                player.removeEffect(MobEffects.WATER_BREATHING);
            }
            data.remove(WATER_BREATHING_TAG);
        }
    }

    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        Player player = event.getEntity();
        if (!player.isUnderWater()) {
            return;
        }
        if (player.getItemBySlot(EquipmentSlot.HEAD)
                .is(MysteryDynamics.NERIIDS_PROTECTION.get())) {
            event.setNewSpeed(event.getNewSpeed() * 10.0F);
        }
    }
}
