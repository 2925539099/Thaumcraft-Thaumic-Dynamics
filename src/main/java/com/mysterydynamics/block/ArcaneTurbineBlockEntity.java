package com.mysterydynamics.block;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;

import net.createmod.catnip.animation.AnimationTickHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

/** 奥法涡轮机的动力端：读取 Create 网络转速并驱动扇叶/传动杆旋转。 */
public class ArcaneTurbineBlockEntity extends KineticBlockEntity {

    public ArcaneTurbineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public float calculateStressApplied() {
        return getBlockState().getValue(ArcaneTurbineBlock.HALF) == DoubleBlockHalf.LOWER
                ? 8.0F : 0.0F;
    }

    /** 用于 BER 的旋转角度；转速为 0 时完全静止。 */
    public float getRenderAngle(float partialTick) {
        float speed = getSpeed();
        if (level == null || Math.abs(speed) < 0.001F) {
            return 0.0F;
        }
        float time = AnimationTickHolder.getRenderTime(level) + partialTick;
        return (time * speed * 3.0F / 10.0F) % 360.0F;
    }
}
