package com.mysterydynamics.block;

import com.mysterydynamics.MysteryDynamics;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.block.IBE;

import net.createmod.catnip.math.VoxelShaper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 红石发动机：输出随红石信号变化（1~15 → 32~256 RPM，128~8192 SU）。
 *
 * <p>正面是输出轴，扳手顶/底面旋转朝向，侧面翻转转向；转向会像傀儡动力系统一样
 * 在同一网络内同步。</p>
 */
public class RedstoneEngineBlock extends HorizontalKineticBlock implements IBE<RedstoneEngineBlockEntity> {

    /**
     * 与 bbmodel 对应的不完整方块碰撞/轮廓形状（模型为南向，单位 1/16）。
     * 参考 Create 应力表/转速表用 VoxelShaper 按朝向旋转思路。
     */
    private static final VoxelShape SHAPE = Shapes.or(
            shapeBox(0, 0, 0, 16, 2, 16),        // 底座
            shapeBox(12, 2, 0, 14, 4, 15),       // 右下角柱
            shapeBox(3, 5, 4, 13, 11, 10),       // 中部转子
            shapeBox(3, 3, 0.1, 13, 13, 2.1),    // 后板
            shapeBox(4, 4, 2, 12, 12, 11),       // 主体
            shapeBox(5, 7, 4, 11, 13, 10),       // 上部转子
            shapeBox(2, 12, 0, 4, 14, 15),       // 左上角柱
            shapeBox(12, 12, 0, 14, 14, 15),     // 右上角柱
            shapeBox(3.5, 3.5, 2, 12.5, 12.5, 5),// 前圈
            shapeBox(5, 5, 9, 11, 11, 13),       // 后圈
            shapeBox(3, 3, 12, 13, 13, 14),      // 前板
            shapeBox(2, 2, 0, 4, 4, 15),         // 左下角柱
            shapeBox(6, 6, 6, 10, 10, 16));      // 输出轴

    private static final VoxelShaper SHAPER = VoxelShaper.forHorizontal(SHAPE, Direction.SOUTH);

    public RedstoneEngineBlock(Properties properties) {
        super(properties);
    }

    private static VoxelShape shapeBox(double x1, double y1, double z1, double x2, double y2, double z2) {
        return Shapes.box(x1 / 16.0, y1 / 16.0, z1 / 16.0, x2 / 16.0, y2 / 16.0, z2 / 16.0);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPER.get(state.getValue(HORIZONTAL_FACING));
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return false;
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return state.getValue(HORIZONTAL_FACING).getAxis();
    }

    /**
     * 输出轴朝向玩家看的方向（背对玩家），方便直接接在要驱动的机械上。
     * 旁边已有朝向本格的传动时，输出轴直接对准它（与 Create 创造马达一致）；潜行放置则忽略。
     */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction preferred = getPreferredHorizontalFacing(context);
        boolean sneaking = context.getPlayer() != null && context.getPlayer().isShiftKeyDown();
        if (preferred != null && !sneaking) {
            return defaultBlockState().setValue(HORIZONTAL_FACING, preferred);
        }
        return defaultBlockState().setValue(HORIZONTAL_FACING, context.getHorizontalDirection());
    }

    @Override
    public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
        return face == state.getValue(HORIZONTAL_FACING);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof RedstoneEngineBlockEntity engine) {
            engine.markPlaced(level.getBestNeighborSignal(pos));
        }
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block,
                                   BlockPos fromPos, boolean isMoving) {
        super.neighborChanged(state, level, pos, block, fromPos, isMoving);
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof RedstoneEngineBlockEntity engine) {
            // 只登记新信号，由方块实体限频应用，避免红石时钟反复重算整网被 Create 判定闪烁
            engine.queueSignal(level.getBestNeighborSignal(pos));
        }
    }

    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        Direction face = context.getClickedFace();
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();

        if (face.getAxis() == Direction.Axis.Y) {
            BlockState rotated = getRotatedBlockState(state, face);
            if (!rotated.canSurvive(level, pos)) {
                return InteractionResult.PASS;
            }
            KineticBlockEntity.switchToBlockState(level, pos, updateAfterWrenched(rotated, context));
            if (level.getBlockState(pos) != state) {
                IWrenchable.playRotateSound(level, pos);
            }
            return InteractionResult.SUCCESS;
        }

        if (!level.isClientSide && level.getBlockEntity(pos) instanceof RedstoneEngineBlockEntity engine) {
            boolean toggled = engine.toggleDirection();
            if (context.getPlayer() != null) {
                // 网络里还有别的动力源时拒绝翻转，提示玩家原因
                String key = !toggled ? "mystery_dynamics.direction.blocked_other_generator"
                        : engine.isReversed() ? "mystery_dynamics.direction.ccw"
                        : "mystery_dynamics.direction.cw";
                context.getPlayer().displayClientMessage(
                        net.minecraft.network.chat.Component.translatable(key), true);
            }
            if (toggled) {
                IWrenchable.playRotateSound(level, pos);
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public Class<RedstoneEngineBlockEntity> getBlockEntityClass() {
        return RedstoneEngineBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends RedstoneEngineBlockEntity> getBlockEntityType() {
        return MysteryDynamics.REDSTONE_ENGINE_BE.get();
    }
}
