package com.mysterydynamics.block;

import com.mysterydynamics.MysteryDynamics;
import com.mysterydynamics.compat.InfernalBoilerCompat;
import com.mysterydynamics.compat.InfernalBoilerStructure;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import com.simibubi.create.content.kinetics.steamEngine.SteamEngineBlock;
import com.simibubi.create.content.kinetics.steamEngine.SteamEngineBlockEntity;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.Block;
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

    /** 是否已记录成型数据。不能用 origin == BlockPos.ZERO 判断，(0,0,0) 也是合法坐标。 */
    private boolean formed;
    private BlockPos boilerOrigin = BlockPos.ZERO;
    private BlockPos facePos = BlockPos.ZERO;
    private Direction frontDirection = Direction.SOUTH;
    private int rotation;
    /** 成型前被替换、且与默认恢复方块不同的原始方块（高级构材、带朝向的熔炉等）；只存服务端。 */
    private Map<BlockPos, BlockState> originalStates = Map.of();
    private int fireEssentia;
    private int fireVisTicks;
    private int linkedEngineCount;
    private int linkedSteamEngines;
    private int linkedTurbines;

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
        if (!isController() || !formed || level == null) {
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
        BlockPos origin = formed ? boilerOrigin : getBlockPos().offset(-1, -3, -1);
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

        if (!formed && getBlockState().getValue(InfernalBoilerBlock.FORMED)) {
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
            if (fireVisTicks == 0) {
                // 魔力加热结束时立即同步，护目镜不必等下一次采样才更新。
                essentiaChanged = true;
            }
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
        if (!formed || level == null) {
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
                        if (blockEntity instanceof InfernalBoilerCasingBlockEntity casing) {
                            // 旧存档的 casing 还没有记录所属锅炉，补上。
                            if (casing.ownerOrigin() == null) {
                                casing.setOwnerOrigin(boilerOrigin);
                            }
                        } else {
                            InfernalBoilerCasingBlockEntity casing = new InfernalBoilerCasingBlockEntity(
                                    MysteryDynamics.INFERNAL_BOILER_CASING_BE.get(), pos, state);
                            casing.setOwnerOrigin(boilerOrigin);
                            level.setBlockEntity(casing);
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
        if (!formed || level == null) {
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
        int turbines = countShellTurbines();
        int count = engines.size() + turbines;
        boolean split = engines.size() != linkedSteamEngines || turbines != linkedTurbines;
        this.linkedSteamEngines = engines.size();
        this.linkedTurbines = turbines;
        if (boiler != null && (boiler.attachedEngines != count || split)) {
            boiler.attachedEngines = count;
            // 让客户端护目镜拿到同一个引擎数，避免显示值来回闪。
            notifyUpdate();
        }
        this.linkedEngineCount = count;
    }

    public int linkedEngineCount() {
        return linkedEngineCount;
    }

    /** 蒸汽引擎数量（同步到客户端，供护目镜计算超频应力）。 */
    public int linkedSteamEngines() {
        return linkedSteamEngines;
    }

    /** 奥法涡轮机数量（同步到客户端，供护目镜计算超频应力）。 */
    public int linkedTurbines() {
        return linkedTurbines;
    }

    /** 精确统计贴在炼狱锅炉外壳任意一面上的涡轮机下半部分。 */
    private int countShellTurbines() {
        java.util.Set<BlockPos> turbines = new java.util.HashSet<>();
        for (int y = 0; y <= 4; y++) {
            for (int x = 0; x <= 3; x++) {
                for (int z = 0; z <= 3; z++) {
                    BlockPos shellPos = InfernalBoilerStructure.world(boilerOrigin, x, y, z, rotation);
                    // 4×4×5 包围盒顶部有空位；只统计贴着真实锅炉方块的涡轮机，
                    // 与 ArcaneTurbineBlockEntity 的供能判定保持一致。
                    if (!isBoilerPart(level.getBlockState(shellPos))) {
                        continue;
                    }
                    for (Direction direction : Direction.Plane.HORIZONTAL) {
                        BlockPos neighbor = shellPos.relative(direction);
                        BlockState state = level.getBlockState(neighbor);
                        if (state.is(MysteryDynamics.ARCANE_TURBINE.get())
                                && state.getValue(ArcaneTurbineBlock.HALF)
                                == net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER) {
                            turbines.add(neighbor);
                        }
                    }
                }
            }
        }
        return turbines.size();
    }

    private static boolean isBoilerPart(BlockState state) {
        return state.is(MysteryDynamics.INFERNAL_BOILER.get())
                || state.is(MysteryDynamics.INFERNAL_BOILER_CASING.get())
                || state.is(MysteryDynamics.INFERNAL_BOILER_SHELL.get())
                || state.is(MysteryDynamics.INFERNAL_BOILER_FACE.get());
    }

    private void updateShellState() {
        if (!formed || level == null || !level.isLoaded(boilerOrigin)) {
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
        if (!formed || level == null || !level.isLoaded(facePos)) {
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

    public void setFormation(BlockPos origin, BlockPos face, Direction front, int rotation,
                             Map<BlockPos, BlockState> originals) {
        this.formed = true;
        this.boilerOrigin = origin;
        this.facePos = face;
        this.frontDirection = front;
        this.rotation = rotation;
        this.originalStates = Map.copyOf(originals);
        setChanged();
    }

    /** 是否属于一座已成型的锅炉（origin/rotation 有效）。 */
    public boolean hasFormation() {
        return formed;
    }

    public BlockPos boilerOrigin() {
        return boilerOrigin;
    }

    /** 拆除时需要还原的原始方块；旧存档为空，拆除时退回默认方块。 */
    public Map<BlockPos, BlockState> originalStates() {
        return originalStates;
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
            sendData();
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
        sendData();
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
        // 负数会反向“增加”源质，0 也没有意义，一律视为无操作。
        if (aspect != Aspect.FIRE || amount <= 0 || fireEssentia < amount) {
            return false;
        }
        fireEssentia -= amount;
        setChanged();
        sendData();
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
        // 护目镜要显示魔力加热剩余时间，开始加热时立即同步一次。
        notifyUpdate();
    }

    @Override
    public void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putBoolean("Formed", formed);
        tag.putLong("BoilerOrigin", boilerOrigin.asLong());
        tag.putLong("FacePos", facePos.asLong());
        tag.putString("Front", frontDirection.getSerializedName());
        tag.putInt("Rotation", rotation);
        tag.putInt("FireEssentia", fireEssentia);
        tag.putInt("FireVisTicks", fireVisTicks);
        tag.putInt("SteamEngines", linkedSteamEngines);
        tag.putInt("Turbines", linkedTurbines);
        if (!clientPacket && !originalStates.isEmpty()) {
            ListTag list = new ListTag();
            for (Map.Entry<BlockPos, BlockState> entry : originalStates.entrySet()) {
                CompoundTag element = new CompoundTag();
                element.putLong("Pos", entry.getKey().asLong());
                element.put("State", NbtUtils.writeBlockState(entry.getValue()));
                list.add(element);
            }
            tag.put("OriginalStates", list);
        }
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        boilerOrigin = BlockPos.of(tag.getLong("BoilerOrigin"));
        // 旧存档没有 Formed 字段：沿用以前“origin 非零即成型”的判断。
        formed = tag.contains("Formed") ? tag.getBoolean("Formed")
                : !boilerOrigin.equals(BlockPos.ZERO);
        facePos = BlockPos.of(tag.getLong("FacePos"));
        Direction loadedFront = Direction.byName(tag.getString("Front"));
        frontDirection = loadedFront == null || loadedFront.getAxis().isVertical()
                ? Direction.SOUTH : loadedFront;
        rotation = Math.max(0, Math.min(3, tag.getInt("Rotation")));
        fireEssentia = Math.max(0, Math.min(MAX_FIRE_ESSENTIA, tag.getInt("FireEssentia")));
        fireVisTicks = Math.max(0, Math.min(400, tag.getInt("FireVisTicks")));
        linkedSteamEngines = Math.max(0, tag.getInt("SteamEngines"));
        linkedTurbines = Math.max(0, tag.getInt("Turbines"));
        if (!clientPacket) {
            Map<BlockPos, BlockState> loaded = new HashMap<>();
            HolderGetter<Block> blocks = registries.lookupOrThrow(Registries.BLOCK);
            ListTag list = tag.getList("OriginalStates", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag element = list.getCompound(i);
                BlockState state = NbtUtils.readBlockState(blocks, element.getCompound("State"));
                // 方块已被移除（卸载了模组）时读出来是空气，交给默认恢复逻辑。
                if (!state.isAir()) {
                    loaded.put(BlockPos.of(element.getLong("Pos")), state);
                }
            }
            originalStates = Map.copyOf(loaded);
        }
    }
}
