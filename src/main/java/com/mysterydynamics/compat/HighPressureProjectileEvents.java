package com.mysterydynamics.compat;

import com.mysterydynamics.MysteryDynamics;
import com.mysterydynamics.item.HighPressureAmmo;
import com.mysterydynamics.item.HighPressureLauncherItem;
import com.simibubi.create.api.equipment.potatoCannon.PotatoCannonProjectileType;
import com.simibubi.create.content.equipment.potatoCannon.PotatoProjectileEntity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;

import thaumcraft.common.registry.TCItems;

/** 高压发射器的掉落回收与特殊弹药效果。 */
@EventBusSubscriber(modid = MysteryDynamics.MODID)
public final class HighPressureProjectileEvents {

    /** 写在弹体持久数据里的标记：此弹体由高压动力发射器发射（随实体存档）。 */
    private static final String LAUNCHER_TAG = MysteryDynamics.MODID + ":high_pressure";

    /** 猪灵仇恨持续时间，与原版 PiglinAi 一致（600 Tick）。 */
    private static final long ANGER_TICKS = 600L;

    private HighPressureProjectileEvents() {
    }

    /**
     * 标记高压发射器射出的弹体；土豆加农炮的普通弹药也按两倍初速飞行。
     * 只认正在开火的发射器（另一只手的土豆加农炮不受影响），从存档读回的弹体不再重复加速。
     */
    @SubscribeEvent
    public static void onProjectileJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide
                || event.loadedFromDisk()
                || !HighPressureLauncherItem.isFiring()
                || !(event.getEntity() instanceof PotatoProjectileEntity projectile)
                || !(projectile.getOwner() instanceof Player)) {
            return;
        }
        // 标记“由本模组回收弹药”；带土豆回收附魔时 Create 已会回收，避免掉两份。
        if (!HighPressureLauncherItem.isFiringWithRecovery()) {
            projectile.getPersistentData().putBoolean(LAUNCHER_TAG, true);
        }
        if (!HighPressureAmmo.isAmmo(projectile.getItem())) {
            projectile.setDeltaMovement(projectile.getDeltaMovement().scale(2.0D));
        }
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
                    explode(projectile, hit, 2.0F);
                }
                return;
            }
            // 只回收高压发射器射出的专属弹药；自带方块命中动作（如放置方块）的弹药不回收，防止复制
            if (!projectile.getPersistentData().getBoolean(LAUNCHER_TAG)
                    || !HighPressureAmmo.isAmmo(ammo)
                    || hasBlockHitAction(projectile)) {
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
            explode(projectile, hit, 2.0F);
            return;
        }

        if (ammo.is(Items.LIGHTNING_ROD)) {
            lightning(projectile, entityHit.getEntity());
            return;
        }

        if (ammo.is(TCItems.GOLD_COIN.get())) {
            piglinAnger(entityHit.getEntity());
        }
    }

    private static boolean hasBlockHitAction(PotatoProjectileEntity projectile) {
        PotatoCannonProjectileType type = projectile.getProjectileType();
        return type != null && type.onBlockHit().isPresent();
    }

    /** 以弹体为爆炸来源，伤害与击杀记录归属到射手；不破坏方块。 */
    private static void explode(PotatoProjectileEntity projectile, Vec3 pos, float power) {
        if (projectile.level() instanceof ServerLevel server) {
            server.explode(projectile, pos.x, pos.y, pos.z, power, Level.ExplosionInteraction.NONE);
        }
    }

    /** 只有雷暴天气且目标头顶露天时才会落雷。 */
    private static void lightning(PotatoProjectileEntity projectile, Entity target) {
        if (projectile.level() instanceof ServerLevel server
                && server.canSeeSky(target.blockPosition())
                && server.isThundering()) {
            LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(server);
            if (bolt != null) {
                bolt.moveTo(target.position());
                if (projectile.getOwner() instanceof ServerPlayer player) {
                    bolt.setCause(player);
                }
                server.addFreshEntity(bolt);
            }
        }
    }

    /** 让附近猪灵仇恨被金币击中的生物；猪灵由 Brain 驱动，需要写入记忆而非 setTarget。 */
    private static void piglinAnger(Entity entity) {
        if (!(entity instanceof LivingEntity target)
                || target.level().isClientSide
                || !target.isAlive()
                || (target instanceof Player player && (player.isCreative() || player.isSpectator()))) {
            return;
        }
        for (Piglin piglin : target.level().getEntitiesOfClass(
                Piglin.class, target.getBoundingBox().inflate(32.0D))) {
            if (piglin == target || !piglin.canAttack(target)) {
                continue;
            }
            piglin.getBrain().eraseMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
            piglin.getBrain().setMemoryWithExpiry(MemoryModuleType.ANGRY_AT, target.getUUID(), ANGER_TICKS);
            piglin.getBrain().setMemory(MemoryModuleType.ATTACK_TARGET, target);
        }
    }
}
