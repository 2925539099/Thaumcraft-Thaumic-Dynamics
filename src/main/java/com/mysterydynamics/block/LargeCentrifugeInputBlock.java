package com.mysterydynamics.block;

import com.mysterydynamics.MysteryDynamics;
import com.mysterydynamics.compat.LargeCentrifugeStructure;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.base.KineticBlock;
import com.simibubi.create.foundation.block.IBE;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 大型奥法离心机的顶部应力输入口。
 *
 * <p>它本身是 Create 动力网络中的一个被动成员：只接受上方传动轴，
 * 不提供应力，也不消耗应力；控制器读取它的转速。</p>
 */
public class LargeCentrifugeInputBlock extends KineticBlock
        implements EntityBlock, IRotate, IBE<LargeCentrifugeInputBlockEntity> {

    public LargeCentrifugeInputBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LargeCentrifugeInputBlockEntity(
                MysteryDynamics.LARGE_CENTRIFUGE_INPUT_BE.get(), pos, state);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return Direction.Axis.Y;
    }

    @Override
    public boolean hasShaftTowards(LevelReader level, BlockPos pos, BlockState state, Direction face) {
        return face == Direction.UP;
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                  BlockEntityType<T> type) {
        return (tickLevel, pos, tickState, blockEntity) -> {
            if (blockEntity instanceof LargeCentrifugeInputBlockEntity input) {
                input.tick();
            }
        };
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState,
                         boolean movedByPiston) {
        super.onRemove(state, level, pos, newState, movedByPiston);
        if (!level.isClientSide && !newState.is(state.getBlock())
                && !LargeCentrifugeStructure.isRestoring()) {
            LargeCentrifugeStructure.requestDismantleNear(level, pos);
        }
    }

    @Override
    public Class<LargeCentrifugeInputBlockEntity> getBlockEntityClass() {
        return LargeCentrifugeInputBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends LargeCentrifugeInputBlockEntity> getBlockEntityType() {
        return MysteryDynamics.LARGE_CENTRIFUGE_INPUT_BE.get();
    }
}
