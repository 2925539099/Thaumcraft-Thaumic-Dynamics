package com.mysterydynamics.compat;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mysterydynamics.MysteryDynamics;
import com.simibubi.create.api.equipment.potatoCannon.PotatoProjectileBlockHitAction;
import com.simibubi.create.api.equipment.potatoCannon.PotatoProjectileEntityHitAction;
import com.simibubi.create.api.registry.CreateBuiltInRegistries;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;

/** 高压发射器专属的弹药命中行为。 */
public final class HighPressureAmmoActions {

    private HighPressureAmmoActions() {
    }

    public static void register() {
        registerEntity("no_drop", NoDrop.CODEC);
        registerEntity("potion_no_drop", PotionNoDrop.CODEC);
        registerEntity("lightning", Lightning.CODEC);
        registerEntity("piglin_anger", PiglinAnger.CODEC);
        registerEntity("explode", ExplosionEntity.CODEC);
        registerBlock("explode", ExplosionBlock.CODEC);
    }

    private static void registerEntity(String path, MapCodec<? extends PotatoProjectileEntityHitAction> codec) {
        Registry.register(CreateBuiltInRegistries.POTATO_PROJECTILE_ENTITY_HIT_ACTION,
                ResourceLocation.fromNamespaceAndPath(MysteryDynamics.MODID, path), codec);
    }

    private static void registerBlock(String path, MapCodec<? extends PotatoProjectileBlockHitAction> codec) {
        Registry.register(CreateBuiltInRegistries.POTATO_PROJECTILE_BLOCK_HIT_ACTION,
                ResourceLocation.fromNamespaceAndPath(MysteryDynamics.MODID, path), codec);
    }

    /** 命中生物时不掉落弹药，只造成伤害。 */
    public record NoDrop() implements PotatoProjectileEntityHitAction {
        public static final MapCodec<NoDrop> CODEC = MapCodec.unit(new NoDrop());

        @Override
        public boolean execute(ItemStack stack, EntityHitResult result, Type type) {
            return true;
        }

        @Override
        public MapCodec<? extends PotatoProjectileEntityHitAction> codec() {
            return CODEC;
        }
    }

    /** 命中生物时施加药水效果，并阻止弹药掉落。 */
    public record PotionNoDrop(Holder<MobEffect> effect, int level, int ticks)
            implements PotatoProjectileEntityHitAction {
        public static final MapCodec<PotionNoDrop> CODEC = RecordCodecBuilder.mapCodec(instance ->
                instance.group(
                        BuiltInRegistries.MOB_EFFECT.holderByNameCodec().fieldOf("effect")
                                .forGetter(PotionNoDrop::effect),
                        Codec.INT.fieldOf("level").forGetter(PotionNoDrop::level),
                        Codec.INT.fieldOf("ticks").forGetter(PotionNoDrop::ticks))
                        .apply(instance, PotionNoDrop::new));

        @Override
        public boolean execute(ItemStack stack, EntityHitResult result, Type type) {
            if (result.getEntity() instanceof LivingEntity living) {
                living.addEffect(new MobEffectInstance(effect, ticks, Math.max(0, level - 1)));
            }
            return true;
        }

        @Override
        public MapCodec<? extends PotatoProjectileEntityHitAction> codec() {
            return CODEC;
        }
    }

    /** 命中生物后，若露天则召唤闪电。 */
    public record Lightning() implements PotatoProjectileEntityHitAction {
        public static final MapCodec<Lightning> CODEC = MapCodec.unit(new Lightning());

        @Override
        public boolean execute(ItemStack stack, EntityHitResult result, Type type) {
            Entity target = result.getEntity();
            if (target.level() instanceof ServerLevel server && server.canSeeSky(target.blockPosition())) {
                LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(server);
                if (bolt != null) {
                    bolt.moveTo(target.position());
                    server.addFreshEntity(bolt);
                }
            }
            return false;
        }

        @Override
        public MapCodec<? extends PotatoProjectileEntityHitAction> codec() {
            return CODEC;
        }
    }

    /** 让附近猪灵仇恨被命中的生物。 */
    public record PiglinAnger() implements PotatoProjectileEntityHitAction {
        public static final MapCodec<PiglinAnger> CODEC = MapCodec.unit(new PiglinAnger());

        @Override
        public boolean execute(ItemStack stack, EntityHitResult result, Type type) {
            Entity entity = result.getEntity();
            if (!(entity instanceof LivingEntity target) || target.level().isClientSide) {
                return false;
            }
            for (Piglin piglin : target.level().getEntitiesOfClass(
                    Piglin.class, target.getBoundingBox().inflate(32.0D))) {
                if (piglin instanceof NeutralMob neutral) {
                    neutral.setPersistentAngerTarget(target.getUUID());
                    neutral.startPersistentAngerTimer();
                }
                piglin.setTarget(target);
            }
            return false;
        }

        @Override
        public MapCodec<? extends PotatoProjectileEntityHitAction> codec() {
            return CODEC;
        }
    }

    public record ExplosionEntity(float power) implements PotatoProjectileEntityHitAction {
        public static final MapCodec<ExplosionEntity> CODEC = RecordCodecBuilder.mapCodec(instance ->
                instance.group(Codec.FLOAT.fieldOf("power").forGetter(ExplosionEntity::power))
                        .apply(instance, ExplosionEntity::new));

        @Override
        public boolean execute(ItemStack stack, EntityHitResult result, Type type) {
            Entity target = result.getEntity();
            if (target.level() instanceof ServerLevel server) {
                server.explode(null, target.getX(), target.getY(), target.getZ(), power,
                        Level.ExplosionInteraction.NONE);
            }
            return false;
        }

        @Override
        public MapCodec<? extends PotatoProjectileEntityHitAction> codec() {
            return CODEC;
        }
    }

    public record ExplosionBlock(float power) implements PotatoProjectileBlockHitAction {
        public static final MapCodec<ExplosionBlock> CODEC = RecordCodecBuilder.mapCodec(instance ->
                instance.group(Codec.FLOAT.fieldOf("power").forGetter(ExplosionBlock::power))
                        .apply(instance, ExplosionBlock::new));

        @Override
        public boolean execute(LevelAccessor level, ItemStack stack, BlockHitResult result) {
            if (level instanceof ServerLevel server) {
                server.explode(null, result.getLocation().x, result.getLocation().y, result.getLocation().z,
                        power, Level.ExplosionInteraction.NONE);
            }
            return true;
        }

        @Override
        public MapCodec<? extends PotatoProjectileBlockHitAction> codec() {
            return CODEC;
        }
    }
}
