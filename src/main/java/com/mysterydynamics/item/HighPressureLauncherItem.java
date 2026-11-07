package com.mysterydynamics.item;

import com.mysterydynamics.client.HighPressureLauncherRenderer;
import com.simibubi.create.content.equipment.potatoCannon.PotatoCannonItem;
import com.simibubi.create.foundation.item.render.SimpleCustomRenderer;

import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.function.Consumer;
import java.util.function.Predicate;

/** 高压动力发射器：暂时继承土豆加农炮的全部行为与数据。 */
public class HighPressureLauncherItem extends PotatoCannonItem {

    /**
     * 服务端正在由高压动力发射器开火时为 true。
     * Create 在 use() 内同步生成弹体，EntityJoinLevelEvent 据此判断弹体是否来自本发射器，
     * 避免把另一只手上土豆加农炮的弹体也算进来。只在服务端线程读写。
     */
    private static boolean firing;
    /** 本次开火的发射器带有 Create 的“土豆回收”附魔：命中方块时由 Create 自己回收弹药。 */
    private static boolean firingWithRecovery;

    private static final net.minecraft.resources.ResourceKey<net.minecraft.world.item.enchantment.Enchantment>
            POTATO_RECOVERY = net.minecraft.resources.ResourceKey.create(
                    net.minecraft.core.registries.Registries.ENCHANTMENT,
                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("create", "potato_recovery"));

    public HighPressureLauncherItem(Properties properties) {
        super(properties);
    }

    public static boolean isFiring() {
        return firing;
    }

    public static boolean isFiringWithRecovery() {
        return firingWithRecovery;
    }

    private static boolean hasPotatoRecovery(Level level, ItemStack stack) {
        return level.registryAccess().lookup(net.minecraft.core.registries.Registries.ENCHANTMENT)
                .flatMap(registry -> registry.get(POTATO_RECOVERY))
                .map(holder -> net.minecraft.world.item.enchantment.EnchantmentHelper
                        .getItemEnchantmentLevel(holder, stack) > 0)
                .orElse(false);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide) {
            return super.use(level, player, hand);
        }
        boolean previous = firing;
        boolean previousRecovery = firingWithRecovery;
        firing = true;
        firingWithRecovery = hasPotatoRecovery(level, player.getItemInHand(hand));
        try {
            return super.use(level, player, hand);
        } finally {
            firing = previous;
            firingWithRecovery = previousRecovery;
        }
    }

    /** 专属弹药与土豆加农炮弹药都可以识别。 */
    @Override
    public Predicate<ItemStack> getAllSupportedProjectiles() {
        Predicate<ItemStack> createAmmo = super.getAllSupportedProjectiles();
        return stack -> HighPressureAmmo.isAmmo(stack) || createAmmo.test(stack);
    }

    /** 与神秘剑一致：神秘材质附魔亲和度 22。 */
    @Override
    public int getEnchantmentValue() {
        return 22;
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(SimpleCustomRenderer.create(this, new HighPressureLauncherRenderer()));
    }
}
