package com.mysterydynamics.item;

import com.mysterydynamics.client.HighPressureLauncherRenderer;
import com.simibubi.create.content.equipment.potatoCannon.PotatoCannonItem;
import com.simibubi.create.foundation.item.render.SimpleCustomRenderer;

import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.minecraft.world.item.ItemStack;

import java.util.function.Consumer;
import java.util.function.Predicate;

/** 高压动力发射器：暂时继承土豆加农炮的全部行为与数据。 */
public class HighPressureLauncherItem extends PotatoCannonItem {

    public HighPressureLauncherItem(Properties properties) {
        super(properties);
    }

    /** 只识别专属弹药，普通土豆加农炮弹药不进入选择列表。 */
    @Override
    public Predicate<ItemStack> getAllSupportedProjectiles() {
        return HighPressureAmmo::isAmmo;
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
