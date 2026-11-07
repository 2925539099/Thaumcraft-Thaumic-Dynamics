package com.mysterydynamics.block;

import com.mysterydynamics.MysteryDynamics;
import com.mysterydynamics.compat.LargeCentrifugeStructure;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/** 大型奥法离心机的不可见填充方块；被破坏时从附近控制器恢复整栋结构。 */
public class LargeCentrifugeCasingBlock extends Block implements EntityBlock {

    public LargeCentrifugeCasingBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LargeCentrifugeCasingInfoBlockEntity(
                MysteryDynamics.LARGE_CENTRIFUGE_CASING_BE.get(), pos, state);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState,
                         boolean movedByPiston) {
        boolean replaced = !newState.is(state.getBlock());
        BlockPos center = replaced && level.getBlockEntity(pos)
                instanceof LargeCentrifugeCasingInfoBlockEntity casing ? casing.center() : null;
        // super 负责在服务端移除方块实体；不调用会让旧 BE 残留在区块里。
        super.onRemove(state, level, pos, newState, movedByPiston);
        if (level.isClientSide || !replaced || LargeCentrifugeStructure.isRestoring()) {
            return;
        }
        LargeCentrifugeStructure.requestDismantleNear(level, pos, center);
    }
}
