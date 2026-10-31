package com.mysterydynamics.block;

import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
import com.mysterydynamics.compat.InfernalBoilerStructure;

import net.createmod.catnip.animation.AnimationTickHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

/** 奥法涡轮机动力端：贴近锅炉产生应力，从底面输出。 */
public class ArcaneTurbineBlockEntity extends GeneratingKineticBlockEntity {

    public static final float MAX_STRESS = 147_456.0F;

    private int movementDirection = 1;
    private float lastEfficiency = -1.0F;
    private float lastGeneratedSpeed = 0.0F;
    private float lastCapacity = 0.0F;
    private FluidTankBlockEntity currentController;

    public ArcaneTurbineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void tick() {
        super.tick();
        float efficiency = boilerEfficiency();
        float speed = generatedSpeedFor(efficiency);
        float capacity = capacityFor(efficiency);
        if (Math.abs(efficiency - lastEfficiency) > 0.001F
                || Math.abs(speed - lastGeneratedSpeed) > 0.001F
                || Math.abs(capacity - lastCapacity) > 0.001F) {
            lastEfficiency = efficiency;
            lastGeneratedSpeed = speed;
            lastCapacity = capacity;
            updateGeneratedRotation();
            if (hasNetwork()) {
                notifyStressCapacityChange(capacity);
            }
        }
    }

    @Override
    public float getGeneratedSpeed() {
        if (getBlockState().getValue(ArcaneTurbineBlock.HALF) != DoubleBlockHalf.LOWER) {
            return 0.0F;
        }
        return generatedSpeedFor(boilerEfficiency());
    }

    @Override
    public float calculateAddedStressCapacity() {
        if (getBlockState().getValue(ArcaneTurbineBlock.HALF) != DoubleBlockHalf.LOWER) {
            return 0.0F;
        }
        return capacityFor(boilerEfficiency());
    }

    /** 与蒸汽引擎一致：效率越高，转速档位越高；最大应力固定为 147456 SU。 */
    private float generatedSpeedFor(float efficiency) {
        if (efficiency <= 0.0F) {
            return 0.0F;
        }
        int speedModifier = 1 + (int) Math.min(3.0D, Math.floor(2.0D + efficiency * 4.0D));
        return movementDirection * 16.0F * speedModifier;
    }

    private float capacityFor(float efficiency) {
        if (efficiency <= 0.0F) {
            return 0.0F;
        }
        int speedModifier = 1 + (int) Math.min(3.0D, Math.floor(2.0D + efficiency * 4.0D));
        // 单台满效率最大 147456 SU；多台时按蒸汽引擎的 heat / engines 效率衰减。
        return MAX_STRESS * efficiency / (16.0F * speedModifier);
    }

    /** 下半部分四个水平面贴着的锅炉，取最高效率。 */
    private float boilerEfficiency() {
        if (level == null
                || getBlockState().getValue(ArcaneTurbineBlock.HALF) != DoubleBlockHalf.LOWER) {
            return 0.0F;
        }
        float best = 0.0F;
        currentController = null;
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockEntity blockEntity = level.getBlockEntity(worldPosition.relative(direction));
            if (!(blockEntity instanceof FluidTankBlockEntity tank)) {
                continue;
            }
            FluidTankBlockEntity controller = tank.getControllerBE();
            if (controller == null || controller.boiler == null) {
                continue;
            }
            currentController = controller;
            // 普通流体储罐没有主动寻找引擎；由涡轮机触发一次锅炉评估，
            // BoilerDataMixin 会在这时把本涡轮机计入 attachedEngines。
            if (!level.isClientSide) {
                int turbines = Math.max(1, countNearbyTurbines(controller));
                if (controller.boiler.attachedEngines <= 0) {
                    controller.updateBoilerState();
                }
                if (controller.boiler.attachedEngines < turbines) {
                    controller.boiler.attachedEngines = turbines;
                }
            }
            if (!controller.boiler.isActive()) {
                continue;
            }
            float efficiency = controller.boiler.getEngineEfficiency(controller.getTotalTankSize());
            if (efficiency > best) {
                best = efficiency;
            }
        }
        // 炼狱锅炉成型后，外壳/底座会替换主体方块；直接按结构查找 controller。
        InfernalBoilerBlockEntity infernal = InfernalBoilerStructure.findController(level, worldPosition);
        if (infernal != null && infernal.boiler != null
                && infernal.boiler.activeHeat > 0
                && infernal.getTankInventory().getFluidAmount() > 0) {
            currentController = infernal;
            if (!level.isClientSide) {
                int turbines = Math.max(1, countNearbyTurbines(infernal));
                if (infernal.boiler.attachedEngines < turbines) {
                    infernal.boiler.attachedEngines = turbines;
                }
            }
            float efficiency = infernal.boiler.getEngineEfficiency(infernal.getTotalTankSize());
            if (efficiency <= 0.0F) {
                int engines = Math.max(1, infernal.boiler.attachedEngines);
                efficiency = Math.min(1.0F, infernal.boiler.activeHeat / (float) engines);
            }
            if (efficiency > best) {
                best = efficiency;
            }
        }
        return Math.min(1.0F, best);
    }

    /** 统计锅炉附近已放置的涡轮机下半部分。 */
    private int countNearbyTurbines(FluidTankBlockEntity controller) {
        if (level == null) {
            return 1;
        }
        BlockPos center = controller.getBlockPos();
        java.util.Set<BlockPos> found = new java.util.HashSet<>();
        for (BlockPos pos : BlockPos.betweenClosed(
                center.offset(-4, -1, -4), center.offset(4, 2, 4))) {
            BlockState state = level.getBlockState(pos);
            if (state.is(com.mysterydynamics.MysteryDynamics.ARCANE_TURBINE.get())
                    && state.getValue(ArcaneTurbineBlock.HALF) == DoubleBlockHalf.LOWER) {
                found.add(pos.immutable());
            }
        }
        return found.size();
    }

    /** 上半部分没有独立动力网络，护目镜信息直接读取下半部分。 */
    @Override
    public boolean addToGoggleTooltip(java.util.List<net.minecraft.network.chat.Component> tooltip,
                                      boolean isPlayerSneaking) {
        if (getBlockState().getValue(ArcaneTurbineBlock.HALF) == DoubleBlockHalf.UPPER
                && level != null
                && level.getBlockEntity(worldPosition.below()) instanceof ArcaneTurbineBlockEntity lower) {
            return lower.addToGoggleTooltip(tooltip, isPlayerSneaking);
        }
        return super.addToGoggleTooltip(tooltip, isPlayerSneaking);
    }

    public void toggleDirection() {
        movementDirection *= -1;
        lastEfficiency = -1.0F;
        lastGeneratedSpeed = 0.0F;
        lastCapacity = 0.0F;
        updateGeneratedRotation();
        if (hasNetwork()) {
            notifyStressCapacityChange(calculateAddedStressCapacity());
        }
        sendData();
    }

    public boolean isReversed() {
        return movementDirection < 0;
    }

    /** 上半部分跟随下半部分的转速；下半部分直接读取自身转速。 */
    public float getRenderAngle(float partialTick) {
        ArcaneTurbineBlockEntity source = this;
        if (getBlockState().getValue(ArcaneTurbineBlock.HALF) == DoubleBlockHalf.UPPER
                && level != null
                && level.getBlockEntity(worldPosition.below()) instanceof ArcaneTurbineBlockEntity lower) {
            source = lower;
        }
        float speed = source.getSpeed();
        if (source.level == null || Math.abs(speed) < 0.001F) {
            return 0.0F;
        }
        float time = AnimationTickHolder.getRenderTime(source.level) + partialTick;
        return (time * speed * 3.0F / 10.0F) % 360.0F;
    }

    @Override
    protected void write(net.minecraft.nbt.CompoundTag tag,
                         net.minecraft.core.HolderLookup.Provider registries,
                         boolean clientPacket) {
        tag.putInt("TurbineDirection", movementDirection);
        super.write(tag, registries, clientPacket);
    }

    @Override
    protected void read(net.minecraft.nbt.CompoundTag tag,
                        net.minecraft.core.HolderLookup.Provider registries,
                        boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        if (tag.contains("TurbineDirection")) {
            movementDirection = tag.getInt("TurbineDirection") < 0 ? -1 : 1;
        }
    }
}
