package com.mysterydynamics.block;

import com.mysterydynamics.MysteryDynamics;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.block.IBE;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 傀儡动力系统（应力源）。
 *
 * <p>每台设备的额定转速和总应力容量由注册时传入；传动轴与正面垂直，
 * 所以 facing=north 时东西两面是输出端，facing=east/west 时南北两面是输出端。</p>
 *
 * <p>转向只有手持机械动力扳手时才能调整：右键顶/底面旋转整机朝向，
 * 右键侧面翻转顺时针/逆时针。</p>
 */
public class GolemPowerBlock extends HorizontalKineticBlock implements IBE<GolemPowerBlockEntity> {

    private final float rpm;
    private final float capacityPerRpm;

    public GolemPowerBlock(Properties properties, float rpm, float totalStressUnits) {
        super(properties);
        this.rpm = rpm;
        this.capacityPerRpm = totalStressUnits / rpm;
    }

    public float rpm() {
        return rpm;
    }

    /** 每 RPM 提供的应力容量（Create 的应力容量按 1 RPM 登记）。 */
    public float capacityPerRpm() {
        return capacityPerRpm;
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return state.getValue(HORIZONTAL_FACING).getClockWise().getAxis();
    }

    /**
     * 传动轴与正面垂直，不能直接用父类“正面朝向相邻传动”的放置逻辑。
     * 旁边有传动时，让旋转轴与该传动对齐；两个候选朝向里优先正面朝向玩家。
     * 潜行放置或没有可对齐的传动时保持原来的“正面朝向玩家”。
     */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction preferred = getPreferredHorizontalFacing(context);
        boolean sneaking = context.getPlayer() != null && context.getPlayer().isShiftKeyDown();
        if (preferred == null || sneaking) {
            return super.getStateForPlacement(context);
        }
        Direction towardsPlayer = context.getHorizontalDirection().getOpposite();
        // getClockWise().getAxis() == preferred.getAxis() 的朝向只有与 preferred 垂直的两个
        Direction facing = towardsPlayer.getAxis() != preferred.getAxis()
                ? towardsPlayer
                : preferred.getClockWise();
        return defaultBlockState().setValue(HORIZONTAL_FACING, facing);
    }

    @Override
    public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
        return face.getAxis() == getRotationAxis(state);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof GolemPowerBlockEntity dynamo) {
            dynamo.markPlaced();
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

        if (!level.isClientSide && level.getBlockEntity(pos) instanceof GolemPowerBlockEntity dynamo) {
            if (!dynamo.toggleDirection()) {
                // 网络里还有别的动力源，翻转会与之冲突，拒绝并提示
                if (context.getPlayer() != null) {
                    context.getPlayer().displayClientMessage(net.minecraft.network.chat.Component.translatable(
                            "mystery_dynamics.direction.blocked_other_generator"), true);
                }
                return InteractionResult.SUCCESS;
            }
            notifyDirection(context.getPlayer(), dynamo);
            IWrenchable.playRotateSound(level, pos);
        }
        return InteractionResult.SUCCESS;
    }

    private static void notifyDirection(net.minecraft.world.entity.player.Player player,
                                        GolemPowerBlockEntity dynamo) {
        if (player != null) {
            player.displayClientMessage(
                    net.minecraft.network.chat.Component.translatable(
                            dynamo.isReversed() ? "mystery_dynamics.direction.ccw" : "mystery_dynamics.direction.cw"),
                    true);
        }
    }

    @Override
    public Class<GolemPowerBlockEntity> getBlockEntityClass() {
        return GolemPowerBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends GolemPowerBlockEntity> getBlockEntityType() {
        return MysteryDynamics.GOLEM_POWER_BE.get();
    }
}
