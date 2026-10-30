package com.mysterydynamics.item;

import com.mysterydynamics.MysteryDynamics;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingBreatheEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/** 涅里伊德之庇的服务器侧效果。 */
@EventBusSubscriber(modid = MysteryDynamics.MODID)
public final class NeriidsProtectionEvents {

    private NeriidsProtectionEvents() {
    }

    /** 与 Create 潜水头盔的 breatheUnderwater 相同的呼吸事件路径。 */
    @SubscribeEvent
    public static void onBreathe(LivingBreatheEvent event) {
        if (!(event.getEntity() instanceof Player player)
                || player.level().isClientSide) {
            return;
        }
        if (!player.isUnderWater()) {
            return;
        }
        if (!player.getItemBySlot(EquipmentSlot.HEAD)
                .is(MysteryDynamics.NERIIDS_PROTECTION.get())) {
            return;
        }
        event.setCanBreathe(true);
        event.setRefillAirAmount(player.getMaxAirSupply());
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
