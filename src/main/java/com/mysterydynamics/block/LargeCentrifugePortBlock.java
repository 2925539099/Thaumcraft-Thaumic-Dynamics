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

/** 大型奥法离心机底座两侧的源质端口：西面输入，东面输出。 */
public class LargeCentrifugePortBlock extends Block implements EntityBlock {

    private final boolean input;

    public LargeCentrifugePortBlock(BlockBehaviour.Properties properties, boolean input) {
        super(properties);
        this.input = input;
    }

    public boolean isInput() {
        return input;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LargeCentrifugePortBlockEntity(
                MysteryDynamics.LARGE_CENTRIFUGE_PORT_BE.get(), pos, state, input);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState,
                         boolean movedByPiston) {
        if (!level.isClientSide && !newState.is(state.getBlock())
                && !LargeCentrifugeStructure.isRestoring()) {
            level.levelEvent(2001, pos, Block.getId(state));
            LargeCentrifugeStructure.requestDismantleNear(level, pos);
        }
    }
}
