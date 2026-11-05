package com.mysterydynamics.block;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

/** 可水平转向的装饰工作站方块。 */
public class StationBlock extends HorizontalDirectionalBlock implements IWrenchable {

    public StationBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    public MapCodec<StationBlock> codec() {
        return simpleCodec(StationBlock::new);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.setValue(FACING, mirror.mirror(state.getValue(FACING)));
    }

    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        if (!context.getLevel().isClientSide) {
            Direction facing = state.getValue(FACING);
            Direction clicked = context.getClickedFace();
            Direction next = clicked.getAxis().isHorizontal() ? clicked : facing.getClockWise();
            context.getLevel().setBlock(context.getClickedPos(),
                    state.setValue(FACING, next), Block.UPDATE_ALL);
        }
        return InteractionResult.SUCCESS;
    }
}
