package com.mysterydynamics.block;

import com.mojang.serialization.MapCodec;
import com.mysterydynamics.compat.InfernalBoilerStructure;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * 炼狱锅炉正面观察窗；overclock 时显示超级燃烧层。
 *
 * <p>只由成型过程放置，掉落表为空：拆掉观察窗会触发整栋恢复，原位换回守卫者玻璃。
 * 创造模式单独放下的观察窗不属于任何锅炉，拆除时什么也不发生。</p>
 */
public class InfernalBoilerFaceBlock extends HorizontalDirectionalBlock {

    public static final MapCodec<InfernalBoilerFaceBlock> CODEC =
            simpleCodec(InfernalBoilerFaceBlock::new);
    public static final BooleanProperty OVERCLOCK = BooleanProperty.create("overclock");

    public InfernalBoilerFaceBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.SOUTH)
                .setValue(OVERCLOCK, false));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(FACING, OVERCLOCK);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState,
                         boolean movedByPiston) {
        // 与 shell 相同：成型锅炉的任意部件被拆（含被装置带走）都恢复整栋。
        // 不属于任何锅炉时 requestDismantle 找不到所属结构，什么也不做。
        if (!level.isClientSide && !newState.is(state.getBlock())
                && !InfernalBoilerStructure.isRestoring()) {
            InfernalBoilerStructure.requestDismantle(level, pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
