package com.mysterydynamics.client;

import com.mysterydynamics.MysteryDynamics;

import net.minecraft.client.Minecraft;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ViewportEvent;

/** 涅里伊德之庇的水下视野：取消水面雾，并把可视距离提高到潜水头盔的两倍。 */
@EventBusSubscriber(modid = MysteryDynamics.MODID, value = Dist.CLIENT)
public final class NeriidsProtectionClientEvents {

    private NeriidsProtectionClientEvents() {
    }

    @SubscribeEvent
    public static void onRenderFog(ViewportEvent.RenderFog event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            return;
        }
        if (!minecraft.player.getItemBySlot(EquipmentSlot.HEAD)
                .is(MysteryDynamics.NERIIDS_PROTECTION.get())) {
            return;
        }
        if (!minecraft.level.getFluidState(event.getCamera().getBlockPosition())
                .is(FluidTags.WATER)) {
            return;
        }
        // Create 潜水头盔为 6.25；这里两倍。
        event.scaleFarPlaneDistance(12.5F);
        event.setCanceled(true);
    }
}
