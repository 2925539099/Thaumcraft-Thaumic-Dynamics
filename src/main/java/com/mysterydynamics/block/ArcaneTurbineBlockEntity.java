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
    /** 满热锅炉的总应力；多台涡轮机共享这个上限。 */
    public static final float BOILER_TOTAL_STRESS = 294_912.0F;

    private int movementDirection = 1;
    private float lastEfficiency = -1.0F;
    private float lastGeneratedSpeed = 0.0F;
    private float lastCapacity = 0.0F;
    private int currentEngineCount = 1;
    private float currentBoilerTotalStress = BOILER_TOTAL_STRESS;
    private float currentPerTurbineMax = MAX_STRESS;

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
        return movementDirection * 16.0F * speedModifierFor(efficiency);
    }

    /** 与 Create PoweredShaftBlockEntity.getSpeedModifier 完全一致。 */
    private int speedModifierFor(float efficiency) {
        if (efficiency >= 1.0F) {
            return 4;
        }
        return 1 + (int) Math.min(2.0D, Math.floor(efficiency * 4.0F));
    }

    private float capacityFor(float efficiency) {
        if (efficiency <= 0.0F) {
            return 0.0F;
        }
        float perEngineMax = Math.min(currentPerTurbineMax,
                currentBoilerTotalStress / Math.max(1, currentEngineCount));
        return perEngineMax * efficiency / (16.0F * speedModifierFor(efficiency));
    }

    /** 下半部分四个水平面贴着的锅炉，取最高效率。 */
    private float boilerEfficiency() {
        if (level == null
                || getBlockState().getValue(ArcaneTurbineBlock.HALF) != DoubleBlockHalf.LOWER) {
            return 0.0F;
        }
        float best = 0.0F;
        currentEngineCount = 1;
        currentBoilerTotalStress = BOILER_TOTAL_STRESS;
        currentPerTurbineMax = MAX_STRESS;
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockEntity blockEntity = level.getBlockEntity(worldPosition.relative(direction));
            if (!(blockEntity instanceof FluidTankBlockEntity tank)) {
                continue;
            }
            FluidTankBlockEntity controller = tank.getControllerBE();
            if (controller == null || controller.boiler == null) {
                continue;
            }
            if (!level.isClientSide && controller.boiler.attachedEngines <= 0) {
                controller.updateBoilerState();
            }
            if (!level.isClientSide) {
                int turbines = countTurbinesForTank(controller);
                if (controller.boiler.attachedEngines < turbines) {
                    controller.boiler.attachedEngines = turbines;
                    controller.setChanged();
                    controller.sendData();
                }
            }
            currentEngineCount = Math.max(1, controller.boiler.attachedEngines);
            if (controller.boiler.isActive()) {
                best = Math.max(best,
                        controller.boiler.getEngineEfficiency(controller.getTotalTankSize()));
            }
        }
        // 炼狱锅炉成型后，外壳/底座会替换主体方块；按结构查找 controller。
        InfernalBoilerBlockEntity infernal = InfernalBoilerStructure.findController(level, worldPosition);
        if (infernal != null && infernal.boiler != null
                && infernal.boiler.activeHeat > 0
                && infernal.getTankInventory().getFluidAmount() > 0) {
            currentBoilerTotalStress = infernal.isOverclock()
                    ? BOILER_TOTAL_STRESS * 2.0F : BOILER_TOTAL_STRESS;
            currentPerTurbineMax = infernal.isOverclock()
                    ? MAX_STRESS * 2.0F : MAX_STRESS;
            currentEngineCount = Math.max(1, infernal.boiler.attachedEngines);
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

    public static int countTurbinesForTank(FluidTankBlockEntity tank) {
        if (tank == null || tank.getLevel() == null) {
            return 0;
        }
        var level = tank.getLevel();
        BlockPos origin = tank.getBlockPos();
        java.util.Set<BlockPos> found = new java.util.HashSet<>();
        for (int y = 0; y < tank.getHeight(); y++) {
            for (int x = 0; x < tank.getWidth(); x++) {
                for (int z = 0; z < tank.getWidth(); z++) {
                    BlockPos tankPos = origin.offset(x, y, z);
                    if (!(level.getBlockEntity(tankPos) instanceof FluidTankBlockEntity)) {
                        continue;
                    }
                    for (Direction direction : Direction.Plane.HORIZONTAL) {
                        BlockPos neighbor = tankPos.relative(direction);
                        BlockState state = level.getBlockState(neighbor);
                        if (state.is(com.mysterydynamics.MysteryDynamics.ARCANE_TURBINE.get())
                                && state.getValue(ArcaneTurbineBlock.HALF) == DoubleBlockHalf.LOWER) {
                            found.add(neighbor.immutable());
                        }
                    }
                }
            }
        }
        return found.size();
    }

    public static int countTurbines(net.minecraft.world.level.Level level, BlockPos center, int radius) {
        if (level == null) {
            return 0;
        }
        java.util.Set<BlockPos> found = new java.util.HashSet<>();
        for (BlockPos pos : BlockPos.betweenClosed(
                center.offset(-radius, -1, -radius), center.offset(radius, 2, radius))) {
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
        boolean added = super.addToGoggleTooltip(tooltip, isPlayerSneaking);
        if (level != null) {
            InfernalBoilerBlockEntity infernal = InfernalBoilerStructure.findController(level, worldPosition);
            if (infernal != null) {
                tooltip.add(net.minecraft.network.chat.Component.literal(
                        "超频判定: " + (infernal.isOverclock() ? "是" : "否")));
                tooltip.add(net.minecraft.network.chat.Component.literal(
                        "总应力上限: " + (infernal.isOverclock() ? "589,824" : "294,912") + " SU"));
            }
        }
        return added;
    }

    /** 与蒸汽引擎一致：转向冲突时自动调整输出方向，避免被 Create 顶成掉落物。 */
    @Override
    public void updateGeneratedRotation() {
        float generated = getGeneratedSpeed();
        float current = getSpeed();
        if (hasSource() && Math.abs(current) > 0.001F && Math.abs(generated) > 0.001F
                && Math.signum(current) != Math.signum(generated)) {
            movementDirection = current > 0.0F ? 1 : -1;
        }
        super.updateGeneratedRotation();
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
