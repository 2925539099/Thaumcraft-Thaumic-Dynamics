package com.mysterydynamics.compat;

import com.mysterydynamics.MysteryDynamics;
import com.mysterydynamics.item.HighPressureAmmo;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.equipment.potatoCannon.PotatoCannonItem;

import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/** 隔离高压动力发射器与土豆加农炮的弹药池。 */
@EventBusSubscriber(modid = MysteryDynamics.MODID)
public final class HighPressureAmmoRestrictionEvents {

    private HighPressureAmmoRestrictionEvents() {
    }

    @SubscribeEvent
    public static void onRightClick(PlayerInteractEvent.RightClickItem event) {
        ItemStack held = event.getItemStack();

        if (held.is(MysteryDynamics.HIGH_PRESSURE_LAUNCHER.get())) {
            PotatoCannonItem.Ammo ammo = PotatoCannonItem.getAmmo(event.getEntity(), held);
            if (ammo == null || !HighPressureAmmo.isAmmo(ammo.stack())) {
                event.setCanceled(true);
            }
            return;
        }

        if (held.is(AllItems.POTATO_CANNON.get())) {
            PotatoCannonItem.Ammo ammo = PotatoCannonItem.getAmmo(event.getEntity(), held);
            if (ammo != null && HighPressureAmmo.isAmmo(ammo.stack())) {
                event.setCanceled(true);
            }
        }
    }
}
