package com.mysterydynamics.compat;

import com.mysterydynamics.MysteryDynamics;
import com.mysterydynamics.block.LargeCentrifugeControllerBlock;
import com.mysterydynamics.block.LargeCentrifugeControllerBlockEntity;
import com.simibubi.create.AllBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.common.registry.TCBlocks;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.Nullable;

/** 3×3×3 大型奥法离心机结构。第三层中心为控制器，底层正面为朝向标记。 */
public final class LargeCentrifugeStructure {

    private static final AtomicBoolean RESTORING = new AtomicBoolean(false);
    private static final Set<GlobalPos> PENDING = ConcurrentHashMap.newKeySet();
    /** 法杖诊断的容错：最多 6 格不符时才认为玩家在搭离心机。 */
    private static final int DIAGNOSE_TOLERANCE = 6;

    private LargeCentrifugeStructure() {
    }

    public record Match(BlockPos center, int rotation, Direction front) {
    }

    public static Match find(Level level, BlockPos clicked) {
        for (int dy = -2; dy <= 2; dy++) {
            for (int dx = -3; dx <= 3; dx++) {
                for (int dz = -3; dz <= 3; dz++) {
                    BlockPos center = clicked.offset(dx, dy, dz);
                    for (int rotation = 0; rotation < 4; rotation++) {
                        if (matches(level, center, rotation)) {
                            return new Match(center, rotation, facingFor(rotation));
                        }
                    }
                }
            }
        }
        return null;
    }

    /** 结构中第一个不符合要求的位置，以及该位置应放置的方块名称。 */
    public record Mismatch(BlockPos pos, Component expected) {
    }

    public static boolean matches(Level level, BlockPos center, int rotation) {
        return level.isLoaded(center) && firstMismatch(level, center, rotation) == null;
    }

    /** 从底层到顶层逐格检查，返回第一个不匹配的位置；完全匹配时返回 null。 */
    @Nullable
    public static Mismatch firstMismatch(Level level, BlockPos center, int rotation) {
        for (int y = -1; y <= 1; y++) {
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    BlockPos pos = world(center, x, y, z, rotation);
                    Block expected = expectedAt(x, y, z);
                    BlockState state = level.getBlockState(pos);
                    boolean ok = expected == null ? state.isAir() : state.is(expected);
                    if (!ok) {
                        return new Mismatch(pos, expected == null
                                ? Component.translatable("mystery_dynamics.structure.air")
                                : expected.getName());
                    }
                }
            }
        }
        return null;
    }

    /** 局部坐标处要求的原始方块；null 表示必须为空气。 */
    @Nullable
    private static Block expectedAt(int x, int y, int z) {
        if (y == -1) {
            if (x == 0 && z == 0) {
                return AllBlocks.MECHANICAL_BEARING.get();
            }
            if (x == 0 && z == -1) {
                return TCBlocks.ALCHEMICAL_CONSTRUCT.get();
            }
            return TCBlocks.ARCANE_STONE_BRICKS.get();
        }
        if (x != 0 || z != 0) {
            return null;
        }
        return y == 0 ? TCBlocks.ALCHEMICAL_CONSTRUCT.get() : AllBlocks.CLUTCH.get();
    }

    /**
     * 玩家用法杖点了离合器或动力轴承但结构不完整时，给出“最接近”的朝向下第一个缺失位置，
     * 方便排查。其它方块（奥术石砖、炼金构材）也会被别的多方块使用，不做诊断，避免误报。
     */
    @Nullable
    public static Mismatch diagnose(Level level, BlockPos clicked) {
        BlockState state = level.getBlockState(clicked);
        BlockPos center;
        if (state.is(AllBlocks.CLUTCH.get())) {
            center = clicked.below();
        } else if (state.is(AllBlocks.MECHANICAL_BEARING.get())) {
            center = clicked.above();
        } else {
            return null;
        }
        Mismatch best = null;
        int bestScore = Integer.MAX_VALUE;
        for (int rotation = 0; rotation < 4; rotation++) {
            int score = mismatchCount(level, center, rotation);
            if (score < bestScore) {
                bestScore = score;
                best = firstMismatch(level, center, rotation);
            }
        }
        // 27 格里错太多说明玩家并不是在搭离心机，不提示、也不吞掉这次点击。
        return bestScore <= DIAGNOSE_TOLERANCE ? best : null;
    }

    private static int mismatchCount(Level level, BlockPos center, int rotation) {
        int count = 0;
        for (int y = -1; y <= 1; y++) {
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    Block expected = expectedAt(x, y, z);
                    BlockState state = level.getBlockState(world(center, x, y, z, rotation));
                    if (expected == null ? !state.isAir() : !state.is(expected)) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    public static boolean form(Level level, Match match) {
        if (level.isClientSide) {
            return false;
        }
        BlockPos center = match.center();
        int rotation = match.rotation();
        RESTORING.set(true);
        try {
            // Bottom 3×3 becomes invisible casing; center wheel and front construct are visual-only.
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    BlockPos pos = world(center, x, -1, z, rotation);
                    var block = x == -1 && z == 0
                            ? MysteryDynamics.LARGE_CENTRIFUGE_INPUT_PORT.get()
                            : x == 1 && z == 0
                            ? MysteryDynamics.LARGE_CENTRIFUGE_OUTPUT_PORT.get()
                            : MysteryDynamics.LARGE_CENTRIFUGE_CASING.get();
                    level.setBlock(pos, block.defaultBlockState(), 3);
                    if (level.getBlockEntity(pos)
                            instanceof com.mysterydynamics.block.LargeCentrifugePortBlockEntity port) {
                        port.setFormation(center, rotation);
                    } else if (level.getBlockEntity(pos)
                            instanceof com.mysterydynamics.block.LargeCentrifugeCasingInfoBlockEntity casing) {
                        casing.setFormation(center, rotation);
                    }
                }
            }
            // Top center: kinetic input (accepts a shaft above).
            BlockPos top = world(center, 0, 1, 0, rotation);
            level.setBlock(top, MysteryDynamics.LARGE_CENTRIFUGE_INPUT.get().defaultBlockState(), 3);
            // Controller at the middle center.
            level.removeBlockEntity(center);
            level.setBlock(center,
                    MysteryDynamics.LARGE_CENTRIFUGE_CONTROLLER.get().defaultBlockState()
                            .setValue(LargeCentrifugeControllerBlock.ROTATION, rotation), 3);
            if (level.getBlockEntity(center)
                    instanceof LargeCentrifugeControllerBlockEntity controller) {
                controller.setFormation(rotation);
            }
            return true;
        } finally {
            RESTORING.set(false);
        }
    }

    public static boolean isRestoring() {
        return RESTORING.get();
    }

    /**
     * 结构部件被破坏时调用。优先使用部件里记录的控制器坐标；没有记录（旧存档）时
     * 才在附近搜索，并且只接受“确实包含该部件”的控制器，避免两台离心机相邻时
     * 拆错了邻居。
     */
    public static void requestDismantleNear(Level level, BlockPos around, @Nullable BlockPos knownCenter) {
        if (level.isClientSide || isRestoring() || level.getServer() == null) {
            return;
        }
        if (knownCenter != null) {
            BlockState state = level.getBlockState(knownCenter);
            if (state.is(MysteryDynamics.LARGE_CENTRIFUGE_CONTROLLER.get())) {
                requestDismantle(level, knownCenter, rotationOf(state));
                return;
            }
        }
        for (int dy = -2; dy <= 2; dy++) {
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    BlockPos candidate = around.offset(dx, dy, dz);
                    BlockState state = level.getBlockState(candidate);
                    if (state.is(MysteryDynamics.LARGE_CENTRIFUGE_CONTROLLER.get())) {
                        int rotation = rotationOf(state);
                        if (containsPart(candidate, rotation, around)) {
                            requestDismantle(level, candidate, rotation);
                            return;
                        }
                    }
                }
            }
        }
    }

    public static void requestDismantleNear(Level level, BlockPos around) {
        requestDismantleNear(level, around, null);
    }

    private static int rotationOf(BlockState state) {
        return state.hasProperty(LargeCentrifugeControllerBlock.ROTATION)
                ? state.getValue(LargeCentrifugeControllerBlock.ROTATION) : 0;
    }

    /** 判断 {@code pos} 是否属于以 {@code center} 为控制器的成型结构（底层 3×3 + 中心 + 顶部输入口）。 */
    public static boolean containsPart(BlockPos center, int rotation, BlockPos pos) {
        if (pos.equals(center) || pos.equals(world(center, 0, 1, 0, rotation))) {
            return true;
        }
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                if (pos.equals(world(center, x, -1, z, rotation))) {
                    return true;
                }
            }
        }
        return false;
    }

    public static void requestDismantle(Level level, BlockPos center, int rotation) {
        if (level.isClientSide || isRestoring() || level.getServer() == null) {
            return;
        }
        // 爆炸等一次性破坏多个部件时，同一台离心机只排队拆解一次。
        GlobalPos key = GlobalPos.of(level.dimension(), center.immutable());
        if (!PENDING.add(key)) {
            return;
        }
        level.getServer().execute(() -> {
            PENDING.remove(key);
            dismantle(level, center, rotation);
        });
    }

    private static boolean isStructurePart(BlockState state) {
        return state.is(MysteryDynamics.LARGE_CENTRIFUGE_CONTROLLER.get())
                || state.is(MysteryDynamics.LARGE_CENTRIFUGE_CASING.get())
                || state.is(MysteryDynamics.LARGE_CENTRIFUGE_INPUT.get())
                || state.is(MysteryDynamics.LARGE_CENTRIFUGE_INPUT_PORT.get())
                || state.is(MysteryDynamics.LARGE_CENTRIFUGE_OUTPUT_PORT.get());
    }

    /**
     * 把成型结构还原为原始方块。只还原“仍然是离心机部件”或“已被破坏成空气”的位置，
     * 不会覆盖玩家在排队期间放下的其它方块。
     */
    public static void dismantle(Level level, BlockPos center, int rotation) {
        if (!level.isLoaded(center)) {
            return;
        }
        RESTORING.set(true);
        try {
            boolean anyPart = isStructurePart(level.getBlockState(center))
                    || isStructurePart(level.getBlockState(world(center, 0, 1, 0, rotation)));
            for (int x = -1; x <= 1 && !anyPart; x++) {
                for (int z = -1; z <= 1 && !anyPart; z++) {
                    anyPart = isStructurePart(level.getBlockState(world(center, x, -1, z, rotation)));
                }
            }
            if (!anyPart) {
                return;
            }
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    BlockPos pos = world(center, x, -1, z, rotation);
                    Block block;
                    if (x == 0 && z == 0) {
                        block = AllBlocks.MECHANICAL_BEARING.get();
                    } else if (x == 0 && z == -1) {
                        block = TCBlocks.ALCHEMICAL_CONSTRUCT.get();
                    } else {
                        block = TCBlocks.ARCANE_STONE_BRICKS.get();
                    }
                    restore(level, pos, block.defaultBlockState());
                }
            }
            restore(level, center, TCBlocks.ALCHEMICAL_CONSTRUCT.get().defaultBlockState());
            restore(level, world(center, 0, 1, 0, rotation), AllBlocks.CLUTCH.get().defaultBlockState());
        } finally {
            RESTORING.set(false);
        }
    }

    private static void restore(Level level, BlockPos pos, BlockState original) {
        BlockState current = level.getBlockState(pos);
        if (!isStructurePart(current) && !current.isAir()) {
            return;
        }
        // 先移除旧的离心机 BE，避免它残留在区块里被还原后的方块复用。
        level.removeBlockEntity(pos);
        level.setBlock(pos, original, 3);
    }

    public static Direction facingFor(int rotation) {
        return switch (rotation & 3) {
            case 1 -> Direction.EAST;
            case 2 -> Direction.SOUTH;
            case 3 -> Direction.WEST;
            default -> Direction.NORTH;
        };
    }

    /** 局部坐标 → 世界坐标；rotation 0 的正面朝北（-Z）。 */
    public static BlockPos world(BlockPos center, int x, int y, int z, int rotation) {
        int rx;
        int rz;
        switch (rotation & 3) {
            case 1 -> {
                rx = -z;
                rz = x;
            }
            case 2 -> {
                rx = -x;
                rz = -z;
            }
            case 3 -> {
                rx = z;
                rz = -x;
            }
            default -> {
                rx = x;
                rz = z;
            }
        }
        return center.offset(rx, y, rz);
    }

    /** 局部水平方向 → 世界方向。 */
    public static Direction directionForLocal(int x, int z, int rotation) {
        int rx;
        int rz;
        switch (rotation & 3) {
            case 1 -> {
                rx = -z;
                rz = x;
            }
            case 2 -> {
                rx = -x;
                rz = -z;
            }
            case 3 -> {
                rx = z;
                rz = -x;
            }
            default -> {
                rx = x;
                rz = z;
            }
        }
        return Direction.getNearest(rx, 0, rz);
    }
}
