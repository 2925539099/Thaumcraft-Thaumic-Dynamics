package com.mysterydynamics.item;

import com.mysterydynamics.client.HighPressureLauncherRenderer;
import com.simibubi.create.content.equipment.potatoCannon.PotatoCannonItem;
import com.simibubi.create.foundation.item.render.SimpleCustomRenderer;

import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

import java.util.function.Consumer;

/** 高压动力发射器：暂时继承土豆加农炮的全部行为与数据。 */
public class HighPressureLauncherItem extends PotatoCannonItem {

    public HighPressureLauncherItem(Properties properties) {
        super(properties);
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(SimpleCustomRenderer.create(this, new HighPressureLauncherRenderer()));
    }
}
