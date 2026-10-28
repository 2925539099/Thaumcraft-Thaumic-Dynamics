package com.mysterydynamics.block;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** 顶部应力输入口的动力 BlockEntity：只把上方动力网络的转速交给控制器。 */
public class LargeCentrifugeInputBlockEntity extends KineticBlockEntity {

    public LargeCentrifugeInputBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public float calculateStressApplied() {
        return 0.0F;
    }
}
