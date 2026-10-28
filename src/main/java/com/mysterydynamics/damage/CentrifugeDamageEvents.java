package com.mysterydynamics.damage;

import com.mysterydynamics.MysteryDynamics;

import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * 离心机伤害只针对本机自己的伤害类型解除外部模组的取消。
 * 部分防护（星铸护甲、灵魂信标等）会直接取消 LivingIncomingDamageEvent，
 * 导致玩家完全免疫环境伤害。
 */
@EventBusSubscriber(modid = MysteryDynamics.MODID)
public final class CentrifugeDamageEvents {

    private CentrifugeDamageEvents() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof Player
                && event.getSource().is(ModDamageTypes.CENTRIFUGE)
                && event.isCanceled()) {
            event.setCanceled(false);
        }
    }
}
