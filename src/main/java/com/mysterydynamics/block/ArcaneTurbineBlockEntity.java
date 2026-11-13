package com.mysterydynamics.block;

import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
import com.mysterydynamics.MysteryDynamics;
import com.mysterydynamics.compat.InfernalBoilerStructure;

import net.createmod.catnip.animation.AnimationTickHolder;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

/** 奥法涡轮机动力端：贴近锅炉产生应力，从底面输出。 */
public class ArcaneTurbineBlockEntity extends GeneratingKineticBlockEntity {

    public static final float MAX_STRESS = 147_456.0F;
    /** 满热锅炉的总应力；多台涡轮机共享这个上限。 */
    public static final float BOILER_TOTAL_STRESS = 294_912.0F;
    /** 锅炉状态的扫描间隔（tick）。扫描要查询大量方块实体，不能每 tick 做。 */
    private static final int REFRESH_INTERVAL = 10;

    private int movementDirection = 1;
    private float lastEfficiency = -1.0F;
    private float lastGeneratedSpeed = 0.0F;
    private float lastCapacity = 0.0F;
    private int refreshCooldown;

    // ---- 最近一次扫描的缓存；getGeneratedSpeed / 容量 / 护目镜都只读这些 ----
    private float cachedEfficiency;
    private int currentEngineCount = 1;
    private float currentBoilerTotalStress = BOILER_TOTAL_STRESS;
    private boolean infernalAttached;
    private boolean infernalOverclock;

    public ArcaneTurbineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    private boolean isLowerHalf() {
        return getBlockState().getValue(ArcaneTurbineBlock.HALF) == DoubleBlockHalf.LOWER;
    }

    @Override
    public void initialize() {
        // Create 在 initialize() 里按 lastCapacityProvided 减去旧网络容量。
        // 若此时 cachedEfficiency 仍为 0，getGeneratedSpeed() 会返回 0，
        // 旧容量减不掉，随后新容量再加一次，造成读档后容量翻倍。
        // 优先使用 NBT 中恢复的缓存；老存档没有缓存时再现场扫描。
        if (level != null && !level.isClientSide && cachedEfficiency <= 0.0F) {
            refreshBoilerState();
        }
        if (level != null && !level.isClientSide) {
            repairStaleNetworkCapacity();
        }
        super.initialize();
    }

    @Override
    public void tick() {
        boolean lower = isLowerHalf();
        // 双端都刷新缓存（客户端用于护目镜显示），但只按间隔扫描。
        // 放在 super.tick() 之前，保证区块加载后第一次校验转速时缓存已就绪。
        if (lower && level != null && --refreshCooldown <= 0) {
            refreshCooldown = REFRESH_INTERVAL;
            refreshBoilerState();
        }
        super.tick();
        // 客户端不能调用 updateGeneratedRotation / notifyStressCapacityChange，
        // 否则会在客户端凭空建出动力网络。
        if (!lower || level == null || level.isClientSide) {
            return;
        }
        float speed = getGeneratedSpeed();
        float capacity = calculateAddedStressCapacity();
        if (Math.abs(cachedEfficiency - lastEfficiency) > 0.001F
                || Math.abs(speed - lastGeneratedSpeed) > 0.001F
                || Math.abs(capacity - lastCapacity) > 0.001F) {
            lastEfficiency = cachedEfficiency;
            updateGeneratedRotation();
            // 自动对齐可能翻转方向，按更新后的值记录
            lastGeneratedSpeed = getGeneratedSpeed();
            lastCapacity = calculateAddedStressCapacity();
            if (hasNetwork()) {
                notifyStressCapacityChange(lastCapacity);
            }
        }
    }

    @Override
    public float getGeneratedSpeed() {
        if (!isLowerHalf()) {
            return 0.0F;
        }
        return generatedSpeedFor(cachedEfficiency);
    }

    @Override
    public float calculateAddedStressCapacity() {
        float capacity = isLowerHalf() ? capacityFor(cachedEfficiency) : 0.0F;
        // Create 会把它存进 NBT，区块重新加载时按这个值把容量加回网络
        lastCapacityProvided = capacity;
        return capacity;
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
        float perEngineMax = Math.min(MAX_STRESS,
                currentBoilerTotalStress / Math.max(1, currentEngineCount));
        return perEngineMax / (16.0F * speedModifierFor(efficiency));
    }

    /**
     * 扫描下半部分四个水平面贴着的锅炉，取效率最高的那一台；
     * 引擎数与总应力也取自这台锅炉，而不是最后检查到的那台。
     */
    private void refreshBoilerState() {
        float best = 0.0F;
        int engines = 1;
        float totalStress = BOILER_TOTAL_STRESS;
        boolean infernalFound = false;
        boolean overclock = false;
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos neighborPos = worldPosition.relative(direction);
            BlockEntity blockEntity = level.getBlockEntity(neighborPos);

            // 炼狱锅炉：只认紧贴着的结构方块，不再按大范围搜索，
            // 避免离锅炉几格远的涡轮机也能白拿满额输出。
            InfernalBoilerBlockEntity infernal = adjacentInfernalBoiler(neighborPos, blockEntity);
            if (infernal != null) {
                infernalFound = true;
                overclock = infernal.isOverclock();
                float efficiency = infernalEfficiency(infernal);
                if (efficiency > best) {
                    best = efficiency;
                    engines = Math.max(1, infernal.boiler.attachedEngines);
                    totalStress = infernal.isOverclock()
                            ? BOILER_TOTAL_STRESS * 2.0F : BOILER_TOTAL_STRESS;
                }
                continue;
            }

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
            if (!controller.boiler.isActive()) {
                continue;
            }
            float efficiency = controller.boiler.getEngineEfficiency(controller.getTotalTankSize());
            if (efficiency > best) {
                int heat = Math.min(controller.boiler.activeHeat,
                        Math.min(controller.boiler.getMaxHeatLevelForBoilerSize(
                                        controller.getTotalTankSize()),
                                controller.boiler.getMaxHeatLevelForWaterSupply()));
                best = efficiency;
                engines = Math.max(1, controller.boiler.attachedEngines);
                totalStress = Math.max(0, heat) * 16_384.0F;
            }
        }
        cachedEfficiency = Math.min(1.0F, best);
        currentEngineCount = engines;
        currentBoilerTotalStress = totalStress;
        infernalAttached = infernalFound;
        infernalOverclock = overclock;
    }

    /**
     * 相邻位置属于某台成型炼狱锅炉时返回其 controller。
     * 主体/底座方块本身带储罐方块实体；外壳锚点和正面没有方块实体，
     * 需要按结构查找，并确认该位置确实在这台锅炉的 4×4×5 范围内。
     */
    private InfernalBoilerBlockEntity adjacentInfernalBoiler(BlockPos neighborPos, BlockEntity blockEntity) {
        if (blockEntity instanceof FluidTankBlockEntity tank) {
            return tank.getControllerBE() instanceof InfernalBoilerBlockEntity infernal ? infernal : null;
        }
        BlockState state = level.getBlockState(neighborPos);
        if (!state.is(MysteryDynamics.INFERNAL_BOILER_SHELL.get())
                && !state.is(MysteryDynamics.INFERNAL_BOILER_FACE.get())) {
            return null;
        }
        InfernalBoilerBlockEntity infernal = InfernalBoilerStructure.findController(level, neighborPos);
        return infernal != null && structureContains(infernal, neighborPos) ? infernal : null;
    }

    private static boolean structureContains(InfernalBoilerBlockEntity boiler, BlockPos pos) {
        if (!boiler.hasFormation()) {
            return false;
        }
        BlockPos origin = boiler.boilerOrigin();
        BlockPos a = InfernalBoilerStructure.world(origin, 0, 0, 0, boiler.rotation());
        BlockPos b = InfernalBoilerStructure.world(origin, 3, 4, 3, boiler.rotation());
        return pos.getX() >= Math.min(a.getX(), b.getX()) && pos.getX() <= Math.max(a.getX(), b.getX())
                && pos.getY() >= Math.min(a.getY(), b.getY()) && pos.getY() <= Math.max(a.getY(), b.getY())
                && pos.getZ() >= Math.min(a.getZ(), b.getZ()) && pos.getZ() <= Math.max(a.getZ(), b.getZ());
    }

    /** 炼狱锅炉效率：必须有热量且有水（储水或管道供水），不接受岩浆等其他流体。 */
    private static float infernalEfficiency(InfernalBoilerBlockEntity infernal) {
        if (infernal.boiler == null || infernal.boiler.activeHeat <= 0 || !hasWater(infernal)) {
            return 0.0F;
        }
        float efficiency = infernal.boiler.getEngineEfficiency(infernal.getTotalTankSize());
        if (efficiency <= 0.0F) {
            // 锅炉还没把本涡轮机计入引擎数（刚放下 / 客户端尚未同步）时按热量估算
            int engines = Math.max(1, infernal.boiler.attachedEngines);
            efficiency = infernal.boiler.activeHeat / (float) engines;
        }
        return Math.min(1.0F, efficiency);
    }

    private static boolean hasWater(InfernalBoilerBlockEntity infernal) {
        var fluid = infernal.getTankInventory().getFluid();
        boolean storedWater = !fluid.isEmpty() && fluid.getFluid().is(FluidTags.WATER);
        // 锅炉的管道输入只接受水，供水量 > 0 也算有水
        return storedWater || infernal.boiler.waterSupply > 0.0F;
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
    public boolean addToGoggleTooltip(java.util.List<Component> tooltip,
                                      boolean isPlayerSneaking) {
        if (getBlockState().getValue(ArcaneTurbineBlock.HALF) == DoubleBlockHalf.UPPER
                && level != null
                && level.getBlockEntity(worldPosition.below()) instanceof ArcaneTurbineBlockEntity lower) {
            return lower.addToGoggleTooltip(tooltip, isPlayerSneaking);
        }
        boolean added = super.addToGoggleTooltip(tooltip, isPlayerSneaking);
        if (infernalAttached) {
            tooltip.add(Component.literal("    ").append(Component.translatable(infernalOverclock
                            ? "mystery_dynamics.goggles.arcane_turbine.overclocked"
                            : "mystery_dynamics.goggles.arcane_turbine.not_overclocked"))
                    .withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.literal("    ").append(Component.translatable(
                            "mystery_dynamics.goggles.arcane_turbine.total_stress_cap",
                            String.format("%,.0f", infernalOverclock
                                    ? BOILER_TOTAL_STRESS * 2.0F : BOILER_TOTAL_STRESS)))
                    .withStyle(ChatFormatting.GRAY));
            added = true;
        }
        if (cachedEfficiency <= 0.0F) {
            tooltip.add(Component.literal("    ").append(Component.translatable(
                            "mystery_dynamics.goggles.arcane_turbine.no_boiler"))
                    .withStyle(ChatFormatting.DARK_GRAY));
            added = true;
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

    /**
     * 扳手翻转输出方向。被网络里其他动力源带动时，自动对齐会立刻翻回来，
     * 此时返回 false，由方块提示“方向由网络锁定”。
     */
    public boolean toggleDirection() {
        boolean before = isReversed();
        movementDirection *= -1;
        lastEfficiency = -1.0F;
        lastGeneratedSpeed = 0.0F;
        lastCapacity = 0.0F;
        updateGeneratedRotation();
        if (hasNetwork()) {
            notifyStressCapacityChange(calculateAddedStressCapacity());
        }
        setChanged();
        sendData();
        return isReversed() != before;
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
        tag.putFloat("CachedEfficiency", cachedEfficiency);
        tag.putInt("EngineCount", currentEngineCount);
        tag.putFloat("BoilerTotalStress", currentBoilerTotalStress);
        tag.putBoolean("InfernalAttached", infernalAttached);
        tag.putBoolean("InfernalOverclock", infernalOverclock);
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
        if (tag.contains("CachedEfficiency")) {
            cachedEfficiency = tag.getFloat("CachedEfficiency");
        }
        if (tag.contains("EngineCount")) {
            currentEngineCount = Math.max(1, tag.getInt("EngineCount"));
        }
        if (tag.contains("BoilerTotalStress")) {
            currentBoilerTotalStress = tag.getFloat("BoilerTotalStress");
        }
        if (tag.contains("InfernalAttached")) {
            infernalAttached = tag.getBoolean("InfernalAttached");
        }
        if (tag.contains("InfernalOverclock")) {
            infernalOverclock = tag.getBoolean("InfernalOverclock");
        }
        if (!clientPacket) {
            repairStaleNetworkCapacity();
        }
    }

    /**
     * 旧版本读档时 cachedEfficiency 还是 0，Create 在 initialize() 中
     * 用 0 转速减旧容量，导致旧容量残留；这里在初始化前把本方块记录的
     * AddedCapacity 提升到存档网络容量，确保旧容量能被完整扣除一次。
     * 正常的存档不会触发这个分支。
     */
    private void repairStaleNetworkCapacity() {
        float speed = Math.abs(getGeneratedSpeed());
        if (speed > 0.001F && capacity > lastCapacityProvided * speed + 1.0F) {
            lastCapacityProvided = capacity / speed;
        }
    }
}
