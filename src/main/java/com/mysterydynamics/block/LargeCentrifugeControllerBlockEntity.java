package com.mysterydynamics.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** 大型奥法离心机控制器：朝向存于方块状态，旋转角只做客户端插值。 */
public class LargeCentrifugeControllerBlockEntity extends BlockEntity {

    private float spin;

    public LargeCentrifugeControllerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public void setFormation(int rotation) {
        // 朝向已经写入方块状态，这里只重置客户端动画。
        this.spin = 0.0F;
        setChanged();
    }

    public int rotation() {
        BlockState state = getBlockState();
        return state.hasProperty(LargeCentrifugeControllerBlock.ROTATION)
                ? state.getValue(LargeCentrifugeControllerBlock.ROTATION) : 0;
    }

    public float spin(float partialTick) {
        return spin + 6.0F * partialTick;
    }

    public void tick() {
        if (level != null && level.isClientSide) {
            spin = (spin + 6.0F) % 360.0F;
        }
    }
}
