package com.mysterydynamics.block;

import com.mysterydynamics.compat.InfernalBoilerStructure;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/** 整栋锅炉模型的承载方块，放在底座西北角；模型覆盖 4×4×5。 */
public class InfernalBoilerShellBlock extends Block {

    public static final BooleanProperty OVERCLOCK = BooleanProperty.create("overclock");

    public InfernalBoilerShellBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(OVERCLOCK, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(OVERCLOCK);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState,
                         boolean movedByPiston) {
        // movedByPiston 时同样拆整栋，避免成型锅炉缺了模型锚点却继续工作。
        if (!level.isClientSide && !newState.is(state.getBlock())
                && !InfernalBoilerStructure.isRestoring()) {
            InfernalBoilerStructure.requestDismantle(level, pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
