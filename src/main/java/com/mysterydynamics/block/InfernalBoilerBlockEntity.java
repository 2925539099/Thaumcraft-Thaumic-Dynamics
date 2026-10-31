package com.mysterydynamics.block;

import com.mysterydynamics.MysteryDynamics;
import com.mysterydynamics.compat.InfernalBoilerCompat;
import com.mysterydynamics.compat.InfernalBoilerStructure;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import com.simibubi.create.content.kinetics.steamEngine.SteamEngineBlock;
import com.simibubi.create.content.kinetics.steamEngine.SteamEngineBlockEntity;

import java.lang.ref.WeakReference;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.fml.ModList;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IAspectContainer;
import thaumcraft.common.registry.TCBlocks;

/**
 * 炼狱锅炉主体方块实体。
 *
 * <p>储罐的成型、容量、流体能力继承 Create 的 {@link FluidTankBlockEntity}；
 * 锅炉温度、超频和应力由 {@link InfernalBoilerData} 接管。</p>
 */
public class InfernalBoilerBlockEntity extends FluidTankBlockEntity implements IAspectContainer {

    public static final int MAX_FIRE_ESSENTIA = 256;
    /** 与 Create 满级 3×3×32 储罐相同的容量：288 × 8000 mB。 */
    public static final int MAX_WATER_CAPACITY = 2_304_000;

    private static final Direction[] DIRECTIONS = Direction.values();

    private BlockPos boilerOrigin = BlockPos.ZERO;
    private BlockPos facePos = BlockPos.ZERO;
    private Direction frontDirection = Direction.SOUTH;
    private int rotation;
    private int fireEssentia;
    private int fireVisTicks;
    private int linkedEngineCount;

    public InfernalBoilerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        this.boiler = new InfernalBoilerData();
    }

    @Override
    public void applyFluidTankSize(int blocks) {
        if (tankInventory != null) {
            tankInventory.setCapacity(MAX_WATER_CAPACITY);
            forceFluidLevelUpdate = true;
        }
    }

    @Override
    public int getTotalTankSize() {
        // 尺寸按 Create 满级 3×3×32 锅炉显示；实际结构与容量仍按 3×3×3。
        return 288;
    }

    /**
     * 只有真正成型、且 origin 处还有 shell 锚点的 controller 才渲染整栋模型。
     * 单独放置/拆除后残留的锅炉方块不会各自渲染完整多方块。
     */
    public boolean isFormedMultiBlock() {
        if (!isController() || boilerOrigin.equals(BlockPos.ZERO) || level == null) {
            return false;
        }
        if (!level.getBlockState(boilerOrigin).is(MysteryDynamics.INFERNAL_BOILER_SHELL.get())) {
            return false;
        }
        // Create 的 controller 不一定在正中心。只要同一 origin 下有多个 controller，
        // 选位置最小的那个作为唯一渲染者，避免每个 controller 都画一遍整栋模型。
        for (int y = 2; y <= 4; y++) {
            for (int x = 0; x <= 2; x++) {
                for (int z = 0; z <= 2; z++) {
                    BlockPos pos = InfernalBoilerStructure.world(
                            boilerOrigin, x, y, z, rotation);
                    if (pos.equals(worldPosition)) {
                        continue;
                    }
                    if (level.getBlockEntity(pos) instanceof InfernalBoilerBlockEntity other
                            && other.isController()
                            && boilerOrigin.equals(other.boilerOrigin())
                            && comparePos(pos, worldPosition) < 0) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private static int comparePos(BlockPos a, BlockPos b) {
        int byY = Integer.compare(a.getY(), b.getY());
        if (byY != 0) {
            return byY;
        }
        int byX = Integer.compare(a.getX(), b.getX());
        return byX != 0 ? byX : Integer.compare(a.getZ(), b.getZ());
    }

    @Override
    public AABB getRenderBoundingBox() {
        BlockPos origin = boilerOrigin.equals(BlockPos.ZERO)
                ? getBlockPos().offset(-1, -3, -1)
                : boilerOrigin;
        // 整栋模型是 4×4×5，必须把渲染裁剪盒扩大到整栋，
        // 否则视角稍偏、controller 离开视锥时模型会整栋消失。
        return new AABB(origin.getX(), origin.getY(), origin.getZ(),
                origin.getX() + 4.0D, origin.getY() + 5.0D, origin.getZ() + 4.0D)
                .inflate(0.5D);
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide || !isController()) {
            return;
        }

        if (boilerOrigin.equals(BlockPos.ZERO)
                && getBlockState().getValue(InfernalBoilerBlock.FORMED)) {
            // 拆除中断/旧存档残留的孤立锅炉：没有 origin 就无法组装成整栋模型。
            if (level.getGameTime() % 20L == 0L) {
                level.setBlock(worldPosition, TCBlocks.THAUMIUM_BLOCK.get().defaultBlockState(), 3);
            }
            return;
        }

        boolean changed = false;
        boolean essentiaChanged = false;
        if (fireEssentia > 0 && isWorking() && level.getGameTime() % 20L == 0L) {
            fireEssentia--;
            changed = true;
            essentiaChanged = true;
        }

        if (ModList.get().isLoaded("thaumcraft")) {
            InfernalBoilerCompat.tickVis(this);
            if (level.getGameTime() % 20L == 0L) {
                InfernalBoilerCompat.pullFireEssentiaFromPort(this);
                cleanupCasingEntities();
            }
        }
        if (fireVisTicks > 0) {
            fireVisTicks--;
            changed = true;
        }

        updateFaceState();
        updateShellState();
        if (level.getGameTime() % 5L == 0L) {
            linkNearbySteamEngines();
        }
        if (changed) {
            setChanged();
        }
        if (essentiaChanged) {
            notifyUpdate();
        }
    }

    private void cleanupCasingEntities() {
        if (boilerOrigin.equals(BlockPos.ZERO) || level == null) {
            return;
        }
        for (int y = 0; y <= 2; y++) {
            for (int x = 0; x <= 3; x++) {
                for (int z = 0; z <= 3; z++) {
                    BlockPos pos = InfernalBoilerStructure.world(
                            boilerOrigin, x, y, z, rotation);
                    BlockState state = level.getBlockState(pos);
                    var blockEntity = level.getBlockEntity(pos);
                    if (state.is(MysteryDynamics.INFERNAL_BOILER_CASING.get())) {
                        if (!(blockEntity instanceof InfernalBoilerCasingBlockEntity)) {
                            level.setBlockEntity(new InfernalBoilerCasingBlockEntity(
                                    MysteryDynamics.INFERNAL_BOILER_CASING_BE.get(), pos, state));
                        }
                    } else if (blockEntity instanceof InfernalBoilerCasingBlockEntity) {
                        // 拆除/修复后残留在黑曜石、下界砖、外壳上的旧 casing BE。
                        level.removeBlockEntity(pos);
                    }
                }
            }
        }
    }

    /**
     * 只有 3×3×3 主体能带动蒸汽引擎；底座、外壳、正面都不参与动力。
     * 引擎背面必须正对主体，避免把旁边朝向错误或贴在底座上的引擎算进去。
     */
    private void linkNearbySteamEngines() {
        if (boilerOrigin.equals(BlockPos.ZERO) || level == null) {
            return;
        }
        // 只有 3×3×3 主体能带动蒸汽引擎；底座 casing 不参与动力。
        java.util.Set<BlockPos> engines = new java.util.HashSet<>();
        for (int y = 2; y <= 4; y++) {
            for (int x = 0; x <= 2; x++) {
                for (int z = 0; z <= 2; z++) {
                    BlockPos bodyPos = InfernalBoilerStructure.world(
                            boilerOrigin, x, y, z, rotation);
                    for (Direction direction : DIRECTIONS) {
                        BlockPos enginePos = bodyPos.relative(direction);
                        if (!level.isLoaded(enginePos)) {
                            continue;
                        }
                        BlockState engineState = level.getBlockState(enginePos);
                        if (!(engineState.getBlock() instanceof SteamEngineBlock)) {
                            continue;
                        }
                        Direction engineBack = SteamEngineBlock.getConnectedDirection(engineState).getOpposite();
                        if (!enginePos.relative(engineBack).equals(bodyPos)
                                || !engines.add(enginePos)) {
                            continue;
                        }
                        if (level.getBlockEntity(enginePos) instanceof SteamEngineBlockEntity engine) {
                            engine.source = new WeakReference<>(this);
                        }
                    }
                }
            }
        }
        int count = engines.size() + countShellTurbines();
        if (boiler != null && boiler.attachedEngines != count) {
            boiler.attachedEngines = count;
            // 让客户端护目镜拿到同一个引擎数，避免显示值来回闪。
            setChanged();
            sendData();
            notifyUpdate();
        }
        this.linkedEngineCount = count;
    }

    public int linkedEngineCount() {
        return linkedEngineCount;
    }

    /** 精确统计贴在炼狱锅炉外壳任意一面上的涡轮机下半部分。 */
    private int countShellTurbines() {
        java.util.Set<BlockPos> turbines = new java.util.HashSet<>();
        for (int y = 0; y <= 4; y++) {
            for (int x = 0; x <= 3; x++) {
                for (int z = 0; z <= 3; z++) {
                    BlockPos shellPos = InfernalBoilerStructure.world(boilerOrigin, x, y, z, rotation);
                    for (Direction direction : Direction.Plane.HORIZONTAL) {
                        BlockPos neighbor = shellPos.relative(direction);
                        BlockState state = level.getBlockState(neighbor);
                        if (state.is(MysteryDynamics.ARCANE_TURBINE.get())
                                && state.getValue(ArcaneTurbineBlock.HALF)
                                == net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER
                                && neighbor.relative(state.getValue(ArcaneTurbineBlock.FACING).getOpposite())
                                .equals(shellPos)) {
                            turbines.add(neighbor);
                        }
                    }
                }
            }
        }
        return turbines.size();
    }

    private void updateShellState() {
        if (boilerOrigin.equals(BlockPos.ZERO) || level == null || !level.isLoaded(boilerOrigin)) {
            return;
        }
        BlockState state = level.getBlockState(boilerOrigin);
        if (!state.is(MysteryDynamics.INFERNAL_BOILER_SHELL.get())) {
            return;
        }
        boolean overclock = isOverclock();
        if (state.getValue(InfernalBoilerShellBlock.OVERCLOCK) != overclock) {
            level.setBlock(boilerOrigin,
                    state.setValue(InfernalBoilerShellBlock.OVERCLOCK, overclock), 3);
        }
    }

    private void updateFaceState() {
        if (facePos.equals(BlockPos.ZERO) || level == null || !level.isLoaded(facePos)) {
            return;
        }
        BlockState state = level.getBlockState(facePos);
        if (!state.is(MysteryDynamics.INFERNAL_BOILER_FACE.get())) {
            return;
        }
        boolean overclock = isOverclock();
        if (state.getValue(InfernalBoilerFaceBlock.OVERCLOCK) != overclock) {
            level.setBlock(facePos, state.setValue(InfernalBoilerFaceBlock.OVERCLOCK, overclock), 3);
        }
    }

    public void setFormation(BlockPos origin, BlockPos face, Direction front, int rotation) {
        this.boilerOrigin = origin;
        this.facePos = face;
        this.frontDirection = front;
        this.rotation = rotation;
        setChanged();
    }

    public BlockPos boilerOrigin() {
        return boilerOrigin;
    }

    public Direction frontDirection() {
        return frontDirection;
    }

    public int rotation() {
        return rotation;
    }

    public int fireEssentia() {
        return fireEssentia;
    }

    public int addFireEssentia(int amount) {
        if (amount <= 0) {
            return 0;
        }
        int accepted = Math.min(amount, MAX_FIRE_ESSENTIA - fireEssentia);
        if (accepted > 0) {
            fireEssentia += accepted;
            setChanged();
            notifyUpdate();
        }
        return accepted;
    }

    public boolean isOverclock() {
        return fireEssentia > 0;
    }

    /** 有蒸汽引擎且在产热时才算工作，空闲不消耗火源质。 */
    public boolean isWorking() {
        return boiler != null && boiler.attachedEngines > 0 && boiler.activeHeat > 0;
    }

    @Override
    public AspectList getAspects() {
        AspectList list = new AspectList();
        if (fireEssentia > 0) {
            list.add(Aspect.FIRE, fireEssentia);
        }
        return list;
    }

    @Override
    public void setAspects(AspectList aspects) {
        fireEssentia = aspects == null ? 0
                : Math.max(0, Math.min(MAX_FIRE_ESSENTIA, aspects.getAmount(Aspect.FIRE)));
        setChanged();
        notifyUpdate();
    }

    @Override
    public boolean doesContainerAccept(Aspect aspect) {
        return aspect == Aspect.FIRE && fireEssentia < MAX_FIRE_ESSENTIA;
    }

    @Override
    public int addToContainer(Aspect aspect, int amount) {
        return aspect == Aspect.FIRE ? addFireEssentia(amount) : 0;
    }

    @Override
    public boolean takeFromContainer(Aspect aspect, int amount) {
        if (aspect != Aspect.FIRE || fireEssentia < amount) {
            return false;
        }
        fireEssentia -= amount;
        setChanged();
        notifyUpdate();
        return true;
    }

    @Override
    public boolean doesContainerContainAmount(Aspect aspect, int amount) {
        return aspect == Aspect.FIRE && fireEssentia >= amount;
    }

    @Override
    public int containerContains(Aspect aspect) {
        return aspect == Aspect.FIRE ? fireEssentia : 0;
    }

    public int fireVisTicks() {
        return fireVisTicks;
    }

    public void setFireVisTicks(int ticks) {
        this.fireVisTicks = Math.max(0, Math.min(400, ticks));
        setChanged();
    }

    @Override
    public void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putLong("BoilerOrigin", boilerOrigin.asLong());
        tag.putLong("FacePos", facePos.asLong());
        tag.putString("Front", frontDirection.getSerializedName());
        tag.putInt("Rotation", rotation);
        tag.putInt("FireEssentia", fireEssentia);
        tag.putInt("FireVisTicks", fireVisTicks);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        boilerOrigin = BlockPos.of(tag.getLong("BoilerOrigin"));
        facePos = BlockPos.of(tag.getLong("FacePos"));
        Direction loadedFront = Direction.byName(tag.getString("Front"));
        frontDirection = loadedFront == null || loadedFront.getAxis().isVertical()
                ? Direction.SOUTH : loadedFront;
        rotation = Math.max(0, Math.min(3, tag.getInt("Rotation")));
        fireEssentia = tag.getInt("FireEssentia");
        fireVisTicks = tag.getInt("FireVisTicks");
    }
}
