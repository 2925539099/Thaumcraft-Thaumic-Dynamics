package com.mysterydynamics.compat;

import com.mysterydynamics.MysteryDynamics;
import com.simibubi.create.content.equipment.potatoCannon.PotatoProjectileEntity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;

import thaumcraft.common.registry.TCItems;

/** 高压发射器的掉落回收与特殊弹药效果。 */
@EventBusSubscriber(modid = MysteryDynamics.MODID)
public final class HighPressureProjectileEvents {

    private HighPressureProjectileEvents() {
    }

    @SubscribeEvent
    public static void onImpact(ProjectileImpactEvent event) {
        if (!(event.getProjectile() instanceof PotatoProjectileEntity projectile)
                || projectile.level().isClientSide) {
            return;
        }

        ItemStack ammo = projectile.getItem();
        if (ammo.isEmpty()) {
            return;
        }

        Vec3 hit = event.getRayTraceResult().getLocation();
        if (event.getRayTraceResult() instanceof BlockHitResult) {
            if (ammo.is(TCItems.NITOR.get()) || ammo.is(TCItems.ALUMENTUM.get())) {
                if (ammo.is(TCItems.ALUMENTUM.get())) {
                    explode(projectile.level(), hit, 2.0F);
                }
                return;
            }
            ItemEntity drop = new ItemEntity(projectile.level(), hit.x, hit.y, hit.z,
                    ammo.copyWithCount(1));
            projectile.level().addFreshEntity(drop);
            return;
        }

        if (!(event.getRayTraceResult() instanceof EntityHitResult entityHit)) {
            return;
        }

        if (ammo.is(TCItems.ALUMENTUM.get())) {
            explode(projectile.level(), hit, 2.0F);
            return;
        }

        if (ammo.is(Items.LIGHTNING_ROD)) {
            lightning(projectile.level(), entityHit.getEntity());
            return;
        }

        if (ammo.is(TCItems.GOLD_COIN.get())) {
            piglinAnger(entityHit.getEntity());
        }
    }

    private static void explode(Level level, Vec3 pos, float power) {
        if (level instanceof ServerLevel server) {
            server.explode(null, pos.x, pos.y, pos.z, power, Level.ExplosionInteraction.NONE);
        }
    }

    private static void lightning(Level level, Entity target) {
        if (level instanceof ServerLevel server && server.canSeeSky(target.blockPosition())) {
            LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(server);
            if (bolt != null) {
                bolt.moveTo(target.position());
                server.addFreshEntity(bolt);
            }
        }
    }

    private static void piglinAnger(Entity entity) {
        if (!(entity instanceof LivingEntity target) || target.level().isClientSide) {
            return;
        }
        for (Piglin piglin : target.level().getEntitiesOfClass(
                Piglin.class, target.getBoundingBox().inflate(32.0D))) {
            if (piglin instanceof NeutralMob neutral) {
                neutral.setPersistentAngerTarget(target.getUUID());
                neutral.startPersistentAngerTimer();
            }
            piglin.setTarget(target);
        }
    }
}
