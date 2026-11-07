package com.mysterydynamics.block;

import com.mysterydynamics.MysteryDynamics;
import com.mysterydynamics.compat.InfernalBoilerStructure;
import com.simibubi.create.content.fluids.tank.FluidTankBlock;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 成型后的底座填充块。
 *
 * <p>保留 FluidTankBlock 外壳只是为了沿用已有方块状态和储罐代理；
 * 底座不参与蒸汽引擎动力，只有 3×3×3 主体能带动引擎。</p>
 */
public class InfernalBoilerCasingBlock extends FluidTankBlock {

    public InfernalBoilerCasingBlock(BlockBehaviour.Properties properties) {
        super(properties, false);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new InfernalBoilerCasingBlockEntity(
                MysteryDynamics.INFERNAL_BOILER_CASING_BE.get(), pos, state);
    }

    @Override
    public Class<FluidTankBlockEntity> getBlockEntityClass() {
        return FluidTankBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends FluidTankBlockEntity> getBlockEntityType() {
        return MysteryDynamics.INFERNAL_BOILER_CASING_BE.get();
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState,
                        boolean movedByPiston) {
        // 不能走 FluidTankBlock 的连通性逻辑，否则 casing 会被并入锅炉 tank，
        // 破坏 controller/origin/rotation，拆除时就会恢复错层。
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                  LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return state;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState,
                         boolean movedByPiston) {
        if (newState.is(state.getBlock())) {
            return;
        }
        // 被活塞/装置带走（movedByPiston）同样要拆整栋：缺一块的成型锅炉无法自愈。
        // requestDismantle 会先读取本方块 BE 里记录的 origin，所以必须在移除 BE 之前调用。
        if (!level.isClientSide && !InfernalBoilerStructure.isRestoring()) {
            InfernalBoilerStructure.requestDismantle(level, pos);
        }
        // 不调用 FluidTankBlock.onRemove：它会对 controller 做 splitMulti，
        // 反过来清掉/改写正在恢复的底座方块，造成缺块和旋转错位。
        // 这里只做 BlockBehaviour.onRemove 的本职工作：移除本方块的 BE，避免残留在恢复后的方块上。
        if (state.hasBlockEntity()) {
            level.removeBlockEntity(pos);
        }
    }
}
