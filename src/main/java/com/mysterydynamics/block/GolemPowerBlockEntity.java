package com.mysterydynamics.block;

import com.simibubi.create.content.kinetics.KineticNetwork;
import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * 傀儡动力系统的方块实体。
 *
 * <p>转速 / 容量都从方块本身读取（{@link GolemPowerBlock}），
 * 所以 8 台设备共用同一个方块实体类型。</p>
 */
public class GolemPowerBlockEntity extends GeneratingKineticBlockEntity {

    private boolean reversed;
    /** 由 Block#setPlacedBy 标记：下一次 initialize 时按相邻传动对齐方向。 */
    private boolean pendingPlacementAlign;

    public GolemPowerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public boolean isReversed() {
        return reversed;
    }

    public GolemPowerBlock block() {
        return (GolemPowerBlock) getBlockState().getBlock();
    }

    public void markPlaced() {
        pendingPlacementAlign = true;
    }

    /** 放置对齐：相邻网络是逆时针时跟随，顺时针则保持初始方向。 */
    public void alignToConnection() {
        if (level == null || level.isClientSide) {
            return;
        }
        Direction.Axis axis = ((IRotate) getBlockState().getBlock()).getRotationAxis(getBlockState());
        for (Direction dir : Direction.values()) {
            if (dir.getAxis() != axis) {
                continue;
            }
            BlockEntity neighbour = level.getBlockEntity(worldPosition.relative(dir));
            if (!(neighbour instanceof KineticBlockEntity kinetic)) {
                continue;
            }
            float speed = kinetic.getTheoreticalSpeed();
            if (speed < 0) {
                reversed = true;
                setChanged();
                sendData();
                return;
            }
            if (speed > 0) {
                return;
            }
        }
    }

    /**
     * 翻转方向；同一网络里的所有傀儡动力系统一起翻转。
     *
     * <p>经齿轮/变速箱连接的设备本来就可能一正一反，所以每台各自取反，
     * 而不是统一设成同一个值。网络里还有其他正在输出的动力源时无法一起翻转，
     * 直接拒绝并返回 false。</p>
     */
    public boolean toggleDirection() {
        if (level == null || level.isClientSide) {
            reversed = !reversed;
            return true;
        }
        if (hasOtherGenerator()) {
            return false;
        }

        List<GolemPowerBlockEntity> group = collectConnectedDevices();
        for (GolemPowerBlockEntity dynamo : group) {
            dynamo.reversed = !dynamo.reversed;
            dynamo.setChanged();
            dynamo.sendData();
        }

        // 由网络里“没有上级来源”的那台负责重算，避免跟随者被判定为方向冲突拆掉
        GolemPowerBlockEntity driver = this;
        for (GolemPowerBlockEntity dynamo : group) {
            if (!dynamo.hasSource()) {
                driver = dynamo;
                break;
            }
        }
        driver.updateGeneratedRotation();
        return true;
    }

    /** 网络里是否还有傀儡动力系统以外、正在输出转速的动力源。 */
    private boolean hasOtherGenerator() {
        if (!hasNetwork()) {
            return false;
        }
        for (KineticBlockEntity member : getOrCreateNetwork().members.keySet()) {
            if (member instanceof GeneratingKineticBlockEntity generator
                    && !(member instanceof GolemPowerBlockEntity)
                    && generator.getGeneratedSpeed() != 0) {
                return true;
            }
        }
        return false;
    }

    private List<GolemPowerBlockEntity> collectConnectedDevices() {
        List<GolemPowerBlockEntity> group = new ArrayList<>();
        group.add(this);
        if (!hasNetwork()) {
            return group;
        }
        KineticNetwork network = getOrCreateNetwork();
        for (KineticBlockEntity member : network.members.keySet()) {
            if (member instanceof GolemPowerBlockEntity dynamo && dynamo != this) {
                group.add(dynamo);
            }
        }
        return group;
    }

    @Override
    public void initialize() {
        super.initialize();
        if (level == null || level.isClientSide) {
            return;
        }
        // 只有刚放下（需要对齐）或还没接入网络时才重算转速。
        // 区块重新加载时 Create 会自己从 NBT 恢复转速，不需要再触发一次网络更新。
        if (pendingPlacementAlign) {
            pendingPlacementAlign = false;
            alignToConnection();
            updateGeneratedRotation();
        } else if (!hasNetwork()) {
            updateGeneratedRotation();
        }
    }

    @Override
    public float getGeneratedSpeed() {
        return reversed ? -block().rpm() : block().rpm();
    }

    /** 护目镜：在 Create 的应力信息后追加转向；潜行时提示扳手用法。 */
    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        super.addToGoggleTooltip(tooltip, isPlayerSneaking);
        tooltip.add(Component.literal("    ").append(Component.translatable(
                reversed ? "mystery_dynamics.direction.ccw" : "mystery_dynamics.direction.cw"))
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("    ").append(Component.translatable(isPlayerSneaking
                        ? "mystery_dynamics.goggles.wrench_side_to_reverse"
                        : "mystery_dynamics.goggles.sneak_for_details"))
                .withStyle(ChatFormatting.DARK_GRAY));
        return true;
    }


    @Override
    protected void write(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        compound.putBoolean("Reversed", reversed);
        super.write(compound, registries, clientPacket);
    }

    @Override
    protected void read(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(compound, registries, clientPacket);
        reversed = compound.getBoolean("Reversed");
    }
}
