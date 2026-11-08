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
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * 红石发动机方块实体。
 *
 * <p>信号 0 时停转；1~15 线性映射到 32~256 RPM 与 128~8192 SU。
 * 转速/容量都由缓存的 {@link #signal} 计算，不会每 tick 读取世界。</p>
 */
public class RedstoneEngineBlockEntity extends GeneratingKineticBlockEntity {

    public static final float MIN_RPM = 32.0F;
    public static final float MAX_RPM = 256.0F;
    public static final float MIN_STRESS = 128.0F;
    public static final float MAX_STRESS = 8192.0F;

    /** 两次实际应用信号之间至少间隔的 tick 数（信号变化最多延迟这么久生效）。 */
    private static final int SIGNAL_COOLDOWN = 10;

    private int signal;
    /** 最近一次收到的红石信号；与 {@link #signal} 不同时等冷却结束再应用。 */
    private int pendingSignal;
    private int signalCooldown;
    private boolean reversed;
    /** 由 Block#setPlacedBy 标记：下一次 initialize 时按相邻传动对齐方向。 */
    private boolean pendingPlacementAlign;

    public RedstoneEngineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public int getSignal() {
        return signal;
    }

    public boolean isReversed() {
        return reversed;
    }

    /** 放置时调用：记录信号，并让 initialize 只在对齐时执行一次转速更新。 */
    public void markPlaced(int signal) {
        this.signal = Mth.clamp(signal, 0, 15);
        this.pendingSignal = this.signal;
        this.pendingPlacementAlign = true;
    }

    /**
     * 红石信号变化时调用。冷却结束时立即应用，否则先记下，
     * 由 {@link #tick()} 在冷却结束后应用最新值（包括 0）。
     * 这样红石时钟最多每 {@link #SIGNAL_COOLDOWN} tick 重算一次网络。
     */
    public void queueSignal(int newSignal) {
        pendingSignal = Mth.clamp(newSignal, 0, 15);
        if (signalCooldown <= 0) {
            applySignal(pendingSignal);
        }
    }

    /** 真正写入信号并重算转速；只有真的变化才重算。 */
    private void applySignal(int newSignal) {
        int clamped = Mth.clamp(newSignal, 0, 15);
        pendingSignal = clamped;
        if (clamped == signal) {
            return;
        }
        signal = clamped;
        signalCooldown = SIGNAL_COOLDOWN;
        setChanged();
        sendData();
        updateGeneratedRotation();
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide) {
            return;
        }
        if (signalCooldown > 0) {
            signalCooldown--;
        } else if (pendingSignal != signal) {
            applySignal(pendingSignal);
        }
    }

    public static float rpmFor(int signal) {
        if (signal <= 0) {
            return 0;
        }
        float t = (signal - 1) / 14.0F;
        return MIN_RPM + (MAX_RPM - MIN_RPM) * t;
    }

    public static float stressFor(int signal) {
        if (signal <= 0) {
            return 0;
        }
        float t = (signal - 1) / 14.0F;
        return MIN_STRESS + (MAX_STRESS - MIN_STRESS) * t;
    }

    public RedstoneEngineBlock block() {
        return (RedstoneEngineBlock) getBlockState().getBlock();
    }

    @Override
    public float getGeneratedSpeed() {
        float rpm = rpmFor(signal);
        return reversed ? -rpm : rpm;
    }

    @Override
    public float calculateAddedStressCapacity() {
        float rpm = rpmFor(signal);
        float capacity = rpm <= 0 ? 0 : stressFor(signal) / rpm;
        lastCapacityProvided = capacity;
        return capacity;
    }

    @Override
    public void initialize() {
        super.initialize();
        if (level == null || level.isClientSide) {
            return;
        }
        if (pendingPlacementAlign) {
            pendingPlacementAlign = false;
            alignToConnection();
            updateGeneratedRotation();
            return;
        }
        // 蓝图炮/指令放置、动态结构拆装、卸载期间红石变化都不会经过 setPlacedBy，
        // 这里重新读取一次实际信号；没变化且已在网络中时不触发网络重算。
        int actual = Mth.clamp(level.getBestNeighborSignal(worldPosition), 0, 15);
        if (actual != signal) {
            signal = actual;
            pendingSignal = actual;
            setChanged();
            sendData();
            updateGeneratedRotation();
        } else if (!hasNetwork()) {
            updateGeneratedRotation();
        }
    }

    /** 放置对齐：相邻网络是逆时针时跟随，顺时针则保持初始方向。 */
    public void alignToConnection() {
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
     * 翻转方向；同一网络里的红石发动机会一起翻转。
     *
     * <p>经齿轮/变速箱连接的发动机可能本来就一正一反，所以每台各自取反。
     * 网络里还有其他正在输出的动力源时无法一起翻转，直接拒绝并返回 false。</p>
     */
    public boolean toggleDirection() {
        if (level == null || level.isClientSide) {
            reversed = !reversed;
            return true;
        }
        if (hasOtherGenerator()) {
            return false;
        }

        List<RedstoneEngineBlockEntity> group = collectConnectedEngines();
        for (RedstoneEngineBlockEntity engine : group) {
            engine.reversed = !engine.reversed;
            engine.setChanged();
            engine.sendData();
        }

        RedstoneEngineBlockEntity driver = this;
        for (RedstoneEngineBlockEntity engine : group) {
            if (!engine.hasSource()) {
                driver = engine;
                break;
            }
        }
        driver.updateGeneratedRotation();
        return true;
    }

    /** 网络里是否还有红石发动机以外、正在输出转速的动力源。 */
    private boolean hasOtherGenerator() {
        if (!hasNetwork()) {
            return false;
        }
        for (KineticBlockEntity member : getOrCreateNetwork().members.keySet()) {
            if (member instanceof GeneratingKineticBlockEntity generator
                    && !(member instanceof RedstoneEngineBlockEntity)
                    && generator.getGeneratedSpeed() != 0) {
                return true;
            }
        }
        return false;
    }

    /** 护目镜：在 Create 的应力信息后追加当前信号与对应输出。 */
    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        super.addToGoggleTooltip(tooltip, isPlayerSneaking);
        tooltip.add(Component.literal("    ").append(Component.translatable(
                        "mystery_dynamics.goggles.redstone_engine.signal", signal))
                .withStyle(ChatFormatting.GRAY));
        if (signal <= 0) {
            tooltip.add(Component.literal("    ").append(Component.translatable(
                            "mystery_dynamics.goggles.redstone_engine.off"))
                    .withStyle(ChatFormatting.DARK_GRAY));
        } else {
            tooltip.add(Component.literal("    ").append(Component.translatable(
                            "mystery_dynamics.goggles.redstone_engine.output",
                            String.format("%,.0f", rpmFor(signal)),
                            String.format("%,.0f", stressFor(signal))))
                    .withStyle(ChatFormatting.AQUA));
        }
        return true;
    }

    private List<RedstoneEngineBlockEntity> collectConnectedEngines() {
        List<RedstoneEngineBlockEntity> group = new ArrayList<>();
        group.add(this);
        if (!hasNetwork()) {
            return group;
        }
        KineticNetwork network = getOrCreateNetwork();
        for (KineticBlockEntity member : network.members.keySet()) {
            if (member instanceof RedstoneEngineBlockEntity engine && engine != this) {
                group.add(engine);
            }
        }
        return group;
    }

    @Override
    protected void write(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        compound.putInt("Signal", signal);
        compound.putBoolean("Reversed", reversed);
        super.write(compound, registries, clientPacket);
    }

    @Override
    protected void read(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(compound, registries, clientPacket);
        signal = Mth.clamp(compound.getInt("Signal"), 0, 15);
        if (!clientPacket) {
            pendingSignal = signal;
        }
        reversed = compound.getBoolean("Reversed");
    }
}
