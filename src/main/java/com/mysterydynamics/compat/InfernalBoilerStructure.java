package com.mysterydynamics.compat;

import com.mysterydynamics.MysteryDynamics;
import com.mysterydynamics.block.InfernalBoilerBlock;
import com.mysterydynamics.block.InfernalBoilerBlockEntity;
import com.mysterydynamics.block.InfernalBoilerCasingBlockEntity;
import com.mysterydynamics.block.InfernalBoilerFaceBlock;
import com.simibubi.create.api.connectivity.ConnectivityHandler;
import com.simibubi.create.content.fluids.tank.FluidTankBlock;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.common.registry.TCBlocks;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;


/**
 * 炼狱锅炉结构。
 *
 * <p>以守卫者玻璃所在的面作为正面，支持四个水平朝向。
 * 局部坐标以 4×4 底座的西北角为原点，正面玻璃在局部 (1,1,3)。</p>
 */
public final class InfernalBoilerStructure {

    private InfernalBoilerStructure() {
    }

    private static final ThreadLocal<Boolean> RESTORING = ThreadLocal.withInitial(() -> false);

    private static final Block[][] LAYER_0 = {
            {Blocks.NETHER_BRICKS, Blocks.OBSIDIAN, Blocks.OBSIDIAN, Blocks.NETHER_BRICKS},
            {Blocks.OBSIDIAN, Blocks.OBSIDIAN, Blocks.OBSIDIAN, Blocks.OBSIDIAN},
            {Blocks.OBSIDIAN, Blocks.OBSIDIAN, Blocks.OBSIDIAN, Blocks.OBSIDIAN},
            {Blocks.NETHER_BRICKS, Blocks.OBSIDIAN, Blocks.OBSIDIAN, Blocks.NETHER_BRICKS}
    };

    private static final Block[][] LAYER_1 = {
            {Blocks.NETHER_BRICKS, Blocks.OBSIDIAN, Blocks.OBSIDIAN, Blocks.NETHER_BRICKS},
            {Blocks.OBSIDIAN, Blocks.LAVA, TCBlocks.ALCHEMICAL_CONSTRUCT.get(), Blocks.OBSIDIAN},
            {Blocks.OBSIDIAN, Blocks.LAVA, TCBlocks.ARCANE_FURNACE.get(), Blocks.OBSIDIAN},
            {Blocks.NETHER_BRICKS, TCBlocks.WARDED_GLASS.get(), Blocks.OBSIDIAN, Blocks.NETHER_BRICKS}
    };

    private static final Block[][] LAYER_2 = {
            {TCBlocks.THAUMIUM_BLOCK.get(), TCBlocks.THAUMIUM_BLOCK.get(), TCBlocks.THAUMIUM_BLOCK.get(), Blocks.NETHER_BRICKS},
            {TCBlocks.THAUMIUM_BLOCK.get(), TCBlocks.THAUMIUM_BLOCK.get(), TCBlocks.THAUMIUM_BLOCK.get(), Blocks.OBSIDIAN},
            {TCBlocks.THAUMIUM_BLOCK.get(), TCBlocks.THAUMIUM_BLOCK.get(), TCBlocks.THAUMIUM_BLOCK.get(), Blocks.OBSIDIAN},
            {Blocks.NETHER_BRICKS, Blocks.OBSIDIAN, Blocks.OBSIDIAN, Blocks.NETHER_BRICKS}
    };

    public record Match(BlockPos origin, BlockPos glass, int rotation, Direction facing) {
    }

    /** 结构中第一个不符合要求的位置，以及该位置应放置的方块名称。 */
    public record Mismatch(BlockPos pos, Component expected) {
    }

    /** 结构总共需要检查的方块数：底座三层 4×4 + 上两层 3×3。 */
    private static final int TOTAL_BLOCKS = 16 * 3 + 9 * 2;
    /** 点守卫者玻璃时，只要至少一半方块正确就给出诊断。 */
    private static final int DIAGNOSE_TOLERANCE_GLASS = TOTAL_BLOCKS / 2;
    /** 点黑曜石/下界砖等通用方块时要求结构几乎完成，避免其他多方块被误报。 */
    private static final int DIAGNOSE_TOLERANCE_OTHER = 6;

    /** 从任意一个结构方块位置找出对应的正面玻璃位置。 */
    public static BlockPos findGlass(Level level, BlockPos clicked) {
        for (int dx = -5; dx <= 5; dx++) {
            for (int dy = -4; dy <= 4; dy++) {
                for (int dz = -5; dz <= 5; dz++) {
                    BlockPos candidate = clicked.offset(dx, dy, dz);
                    if (level.getBlockState(candidate).is(TCBlocks.WARDED_GLASS.get())
                            && matches(level, candidate)) {
                        return candidate;
                    }
                }
            }
        }
        return null;
    }

    public static boolean matches(Level level, BlockPos glassPos) {
        return findMatch(level, glassPos) != null;
    }

    private static Block expectedAt(int x, int y, int z) {
        if (y == 0) {
            return LAYER_0[z][x];
        }
        if (y == 1) {
            return LAYER_1[z][x];
        }
        if (y == 2) {
            return LAYER_2[z][x];
        }
        if (y <= 4 && x <= 2 && z <= 2) {
            return TCBlocks.THAUMIUM_BLOCK.get();
        }
        return null;
    }

    /** 依次尝试四个朝向，返回第一个匹配的结构。 */
    public static Match findMatch(Level level, BlockPos glassPos) {
        if (!level.getBlockState(glassPos).is(TCBlocks.WARDED_GLASS.get())) {
            return null;
        }
        for (int rotation = 0; rotation < 4; rotation++) {
            int gx = rotateX(1, 3, rotation);
            int gz = rotateZ(1, 3, rotation);
            BlockPos origin = glassPos.offset(-gx, -1, -gz);
            if (matchesAll(level, origin, rotation)) {
                return new Match(origin, glassPos, rotation, facingFor(rotation));
            }
        }
        return null;
    }

    private static boolean matchesAll(Level level, BlockPos origin, int rotation) {
        return matchesLayer(level, origin, rotation, 0, LAYER_0)
                && matchesLayer(level, origin, rotation, 1, LAYER_1)
                && matchesLayer(level, origin, rotation, 2, LAYER_2)
                && matchesBody(level, origin, rotation);
    }

    private static boolean matchesLayer(Level level, BlockPos origin, int rotation, int y, Block[][] pattern) {
        for (int x = 0; x < 4; x++) {
            for (int z = 0; z < 4; z++) {
                Block expected = pattern[z][x];
                BlockState actual = level.getBlockState(world(origin, x, y, z, rotation));
                if (accepted(expected, actual)) {
                    continue;
                }
                return false;
            }
        }
        return true;
    }

    private static boolean accepted(Block expected, BlockState actual) {
        if (expected == Blocks.LAVA) {
            // 只接受岩浆源：流动岩浆拆除时会被还原成源方块，等于白送岩浆。
            return actual.is(Blocks.LAVA) && actual.getFluidState().isSource();
        }
        if (actual.is(expected)) {
            return true;
        }
        // 神秘时代里“炼金炉”实际注册名是 alchemical_furnace；
        // 同时兼容奥术熔炉核心、炼金炉外壳等不同摆放形式。
        if (expected == TCBlocks.ARCANE_FURNACE.get()
                && (actual.is(TCBlocks.ALCHEMICAL_FURNACE.get())
                || actual.is(TCBlocks.ADVANCED_ALCHEMICAL_FURNACE.get())
                || actual.is(TCBlocks.ARCANE_FURNACE.get())
                || actual.is(TCBlocks.ARCANE_FURNACE_CASING.get()))) {
            return true;
        }
        // 允许使用高级炼金构材。
        return expected == TCBlocks.ALCHEMICAL_CONSTRUCT.get()
                && actual.is(TCBlocks.ADVANCED_ALCHEMICAL_CONSTRUCT.get());
    }

    private static boolean matchesBody(Level level, BlockPos origin, int rotation) {
        for (int y = 3; y <= 4; y++) {
            for (int x = 0; x <= 2; x++) {
                for (int z = 0; z <= 2; z++) {
                    if (!level.getBlockState(world(origin, x, y, z, rotation))
                            .is(TCBlocks.THAUMIUM_BLOCK.get())) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    /** origin/rotation 下从底层到顶层第一个不匹配的位置；完全匹配时返回 null。 */
    @Nullable
    private static Mismatch firstMismatch(Level level, BlockPos origin, int rotation) {
        for (int y = 0; y <= 4; y++) {
            for (int x = 0; x <= 3; x++) {
                for (int z = 0; z <= 3; z++) {
                    Block expected = expectedAt(x, y, z);
                    if (expected == null) {
                        continue;
                    }
                    BlockPos pos = world(origin, x, y, z, rotation);
                    if (!accepted(expected, level.getBlockState(pos))) {
                        return new Mismatch(pos, defaultRestoreState(expected).getBlock().getName());
                    }
                }
            }
        }
        return null;
    }

    private static int mismatchCount(Level level, BlockPos origin, int rotation) {
        int count = 0;
        for (int y = 0; y <= 4; y++) {
            for (int x = 0; x <= 3; x++) {
                for (int z = 0; z <= 3; z++) {
                    Block expected = expectedAt(x, y, z);
                    if (expected != null
                            && !accepted(expected, level.getBlockState(world(origin, x, y, z, rotation)))) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    /**
     * 结构不完整时，找附近守卫者玻璃对应的“最接近”的朝向，返回第一个缺失位置。
     * 只有点击位置落在该候选结构范围内、且大部分方块已就位时才诊断；
     * 黑曜石/下界砖等通用方块要求几乎完成，避免误报其他多方块。
     */
    @Nullable
    public static Mismatch diagnose(Level level, BlockPos clicked) {
        boolean clickedGlass = level.getBlockState(clicked).is(TCBlocks.WARDED_GLASS.get());
        int tolerance = clickedGlass ? DIAGNOSE_TOLERANCE_GLASS : DIAGNOSE_TOLERANCE_OTHER;
        Mismatch best = null;
        int bestScore = Integer.MAX_VALUE;
        for (int dx = -5; dx <= 5; dx++) {
            for (int dy = -4; dy <= 4; dy++) {
                for (int dz = -5; dz <= 5; dz++) {
                    BlockPos glass = clicked.offset(dx, dy, dz);
                    if (!level.getBlockState(glass).is(TCBlocks.WARDED_GLASS.get())) {
                        continue;
                    }
                    for (int rotation = 0; rotation < 4; rotation++) {
                        BlockPos origin = glass.offset(-rotateX(1, 3, rotation), -1,
                                -rotateZ(1, 3, rotation));
                        if (!footprintContains(origin, clicked, 0)) {
                            continue;
                        }
                        int score = mismatchCount(level, origin, rotation);
                        if (score > 0 && score < bestScore) {
                            bestScore = score;
                            best = firstMismatch(level, origin, rotation);
                        }
                    }
                }
            }
        }
        return bestScore <= tolerance ? best : null;
    }

    /** 拆除时某个局部位置默认恢复成的方块（旧存档没有记录原始方块时使用）。 */
    private static BlockState defaultRestoreState(Block expected) {
        return expected == TCBlocks.ARCANE_FURNACE.get()
                ? TCBlocks.ALCHEMICAL_FURNACE.get().defaultBlockState()
                : expected.defaultBlockState();
    }

    public static boolean form(Level level, BlockPos glassPos) {
        Match match = findMatch(level, glassPos);
        if (match == null) {
            return false;
        }
        BlockPos origin = match.origin();
        int rotation = match.rotation();
        BlockPos center = world(origin, 1, 3, 1, rotation);
        BlockPos face = match.glass();

        // 先记下与默认恢复方块不同的原始方块（高级构材、奥术熔炉外壳、带朝向的熔炉等），
        // 拆除时原样还给玩家，避免以次充好或吞物品。
        Map<BlockPos, BlockState> originals = new HashMap<>();
        for (int y = 0; y <= 2; y++) {
            for (int x = 0; x <= 3; x++) {
                for (int z = 0; z <= 3; z++) {
                    Block expected = expectedAt(x, y, z);
                    BlockPos pos = world(origin, x, y, z, rotation);
                    BlockState actual = level.getBlockState(pos);
                    if (expected != null && !actual.equals(defaultRestoreState(expected))) {
                        originals.put(pos.immutable(), actual);
                    }
                }
            }
        }

        RESTORING.set(true);
        try {
            // 成型后整个 4×4×2 底座换成不可见填充块，由 shell 模型绘制。
            for (int y = 0; y <= 1; y++) {
                for (int x = 0; x <= 3; x++) {
                    for (int z = 0; z <= 3; z++) {
                        placeCasing(level, world(origin, x, y, z, rotation), origin);
                    }
                }
            }

            // 第三层的 4×4 外圈也是多结构的一部分，同样交给 shell；
            // 中心 3×3 随后会被锅炉主体覆盖。
            for (int x = 0; x <= 3; x++) {
                for (int z = 0; z <= 3; z++) {
                    placeCasing(level, world(origin, x, 2, z, rotation), origin);
                }
            }

            // 先放中心，让 Create 的连通性把 controller 选在中心附近
            level.setBlock(center, MysteryDynamics.INFERNAL_BOILER.get().defaultBlockState()
                    .setValue(InfernalBoilerBlock.FORMED, true), 3);
            for (int y = 2; y <= 4; y++) {
                for (int x = 0; x <= 2; x++) {
                    for (int z = 0; z <= 2; z++) {
                        if (x == 1 && y == 3 && z == 1) {
                            continue;
                        }
                        level.setBlock(world(origin, x, y, z, rotation),
                                MysteryDynamics.INFERNAL_BOILER.get().defaultBlockState()
                                        .setValue(InfernalBoilerBlock.FORMED, true), 3);
                    }
                }
            }

            // 整栋模型承载方块放在底座西北角，模型从该位置向外延伸 4×4×5。
            level.setBlock(origin, MysteryDynamics.INFERNAL_BOILER_SHELL.get().defaultBlockState(), 3);

            BlockState faceState = MysteryDynamics.INFERNAL_BOILER_FACE.get().defaultBlockState()
                    .setValue(InfernalBoilerFaceBlock.FACING, match.facing());
            level.setBlock(face, faceState, 3);

            if (level.getBlockEntity(center) instanceof InfernalBoilerBlockEntity centerBoiler) {
                FluidTankBlockEntity controller = centerBoiler.getControllerBE();
                if (controller == null) {
                    ConnectivityHandler.formMulti(centerBoiler);
                    controller = centerBoiler.getControllerBE();
                }
                if (controller instanceof InfernalBoilerBlockEntity boiler) {
                    boiler.setFormation(origin, face, match.facing(), rotation, originals);
                } else {
                    centerBoiler.setFormation(origin, face, match.facing(), rotation, originals);
                }
            }

            // 每个主体方块都保存 origin/rotation 和原始方块。这样即使先拆掉 controller，
            // 其他主体方块仍能触发整栋 restore，不会退化成多个独立锅炉。
            for (int y = 2; y <= 4; y++) {
                for (int x = 0; x <= 2; x++) {
                    for (int z = 0; z <= 2; z++) {
                        if (level.getBlockEntity(world(origin, x, y, z, rotation))
                                instanceof InfernalBoilerBlockEntity part) {
                            part.setFormation(origin, face, match.facing(), rotation, originals);
                        }
                    }
                }
            }

            BlockState centerState = level.getBlockState(center);
            FluidTankBlock.updateBoilerState(centerState, level, center);
            level.updateNeighborsAt(face, MysteryDynamics.INFERNAL_BOILER_FACE.get());
        } finally {
            // 任何异常都不能让 RESTORING 卡在 true，否则之后所有拆除都会被静默忽略。
            RESTORING.set(false);
        }
        return true;
    }

    private static void placeCasing(Level level, BlockPos pos, BlockPos origin) {
        level.setBlock(pos, MysteryDynamics.INFERNAL_BOILER_CASING.get().defaultBlockState(), 3);
        if (level.getBlockEntity(pos) instanceof InfernalBoilerCasingBlockEntity casing) {
            casing.setOwnerOrigin(origin);
        }
    }

    /**
     * 破坏成型锅炉时恢复成建造时的五层方块。
     *
     * @param originals 成型时记录的原始方块；没有记录的位置按默认方块恢复（兼容旧存档）
     */
    public static void dismantle(Level level, BlockPos origin, int rotation,
                                 Map<BlockPos, BlockState> originals) {
        if (level.isClientSide || isRestoring()) {
            return;
        }
        RESTORING.set(true);
        try {
            for (int y = 0; y <= 2; y++) {
                for (int x = 0; x <= 3; x++) {
                    for (int z = 0; z <= 3; z++) {
                        BlockPos pos = world(origin, x, y, z, rotation);
                        BlockState restored = originals.get(pos);
                        if (restored == null) {
                            restored = defaultRestoreState(expectedAt(x, y, z));
                        }
                        restore(level, pos, restored);
                    }
                }
            }
            for (int y = 3; y <= 4; y++) {
                for (int x = 0; x <= 2; x++) {
                    for (int z = 0; z <= 2; z++) {
                        restore(level, world(origin, x, y, z, rotation),
                                TCBlocks.THAUMIUM_BLOCK.get().defaultBlockState());
                    }
                }
            }
        } finally {
            RESTORING.set(false);
        }
    }

    /**
     * 只覆盖仍是锅炉部件或已经空了的位置（玩家刚拆掉的那格、被装置带走的部件）。
     * 别的方块已经占了这个位置时不覆盖，避免吞掉无关方块；重复的拆除请求也因此是幂等的。
     */
    private static void restore(Level level, BlockPos pos, BlockState restored) {
        BlockState current = level.getBlockState(pos);
        if (!current.isAir() && !isBoilerPart(current)) {
            return;
        }
        if (level.getBlockEntity(pos) != null) {
            // 先移除锅炉 BE：避免 Create 储罐 onRemove 做 splitMulti，也避免 BE 残留在恢复后的方块上。
            level.removeBlockEntity(pos);
        }
        level.setBlock(pos, restored, 3);
    }

    private static boolean isBoilerPart(BlockState state) {
        return state.is(MysteryDynamics.INFERNAL_BOILER.get())
                || state.is(MysteryDynamics.INFERNAL_BOILER_CASING.get())
                || state.is(MysteryDynamics.INFERNAL_BOILER_SHELL.get())
                || state.is(MysteryDynamics.INFERNAL_BOILER_FACE.get());
    }

    public static boolean isRestoring() {
        return RESTORING.get();
    }

    /** 在任意已成型方块被破坏时请求恢复整栋结构。 */
    public static void requestDismantle(Level level, BlockPos around) {
        if (level.isClientSide || isRestoring() || level.getServer() == null) {
            return;
        }
        InfernalBoilerBlockEntity formed = findFormedPart(level, around);
        if (formed == null) {
            return;
        }
        // 立刻取出数据：延迟执行时被拆的部件 BE 已经不存在了。
        BlockPos origin = formed.boilerOrigin();
        int rotation = formed.rotation();
        Map<BlockPos, BlockState> originals = formed.originalStates();
        level.getServer().execute(() -> dismantle(level, origin, rotation, originals));
    }

    /**
     * 查找 around 所属成型锅炉的 controller。around 可以在 4×4×5 结构内，
     * 也可以水平紧贴结构外侧（例如贴在外壳上的奥法涡轮机）。
     */
    public static InfernalBoilerBlockEntity findController(Level level, BlockPos around) {
        InfernalBoilerBlockEntity part = findBoiler(level, around, 1);
        return part == null ? null : controllerOf(level, part);
    }

    /** 查找 around 所属锅炉中任意一个仍有 origin 的成型部件，controller 被拆掉时也能恢复整栋。 */
    private static InfernalBoilerBlockEntity findFormedPart(Level level, BlockPos around) {
        return findBoiler(level, around, 0);
    }

    /**
     * 只接受 4×4×5 范围（origin + 旋转后的占地）包含 around 的锅炉，
     * 两座锅炉相邻时不会把 A 的方块认成 B 的。margin 为允许的水平外扩格数，
     * 结构内部的精确匹配总是优先于外扩匹配。
     */
    private static InfernalBoilerBlockEntity findBoiler(Level level, BlockPos around, int margin) {
        // 1) 自己就是主体方块。
        if (level.getBlockEntity(around) instanceof InfernalBoilerBlockEntity self
                && self.hasFormation() && footprintContains(self.boilerOrigin(), around, 0)) {
            return self;
        }
        // 2) 底座 casing 记录了所属锅炉的 origin，直接定位。
        if (level.getBlockEntity(around) instanceof InfernalBoilerCasingBlockEntity casing
                && casing.ownerOrigin() != null) {
            InfernalBoilerBlockEntity part = partOf(level, casing.ownerOrigin());
            if (part != null) {
                return part;
            }
        }
        // 3) shell/正面/旧存档/相邻方块：在附近扫描主体方块。
        // 主体在 origin 上方 2~4 层，origin 在 around 下方 0~4 层，所以 dy 取 -2..4。
        InfernalBoilerBlockEntity nearby = null;
        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                if (!level.isLoaded(around.offset(dx, 0, dz))) {
                    continue;
                }
                for (int dy = -2; dy <= 4; dy++) {
                    BlockPos pos = around.offset(dx, dy, dz);
                    if (!level.getBlockState(pos).is(MysteryDynamics.INFERNAL_BOILER.get())
                            || !(level.getBlockEntity(pos) instanceof InfernalBoilerBlockEntity part)
                            || !part.hasFormation()) {
                        continue;
                    }
                    if (footprintContains(part.boilerOrigin(), around, 0)) {
                        return part;
                    }
                    if (nearby == null && margin > 0
                            && footprintContains(part.boilerOrigin(), around, margin)) {
                        nearby = part;
                    }
                }
            }
        }
        return nearby;
    }

    /** 已知 origin 时，在主体所在的 4×4×3 范围内找一个属于它的锅炉部件。 */
    private static InfernalBoilerBlockEntity partOf(Level level, BlockPos origin) {
        for (int y = 2; y <= 4; y++) {
            for (int x = 0; x <= 3; x++) {
                for (int z = 0; z <= 3; z++) {
                    if (level.getBlockEntity(origin.offset(x, y, z)) instanceof InfernalBoilerBlockEntity part
                            && part.hasFormation() && origin.equals(part.boilerOrigin())) {
                        return part;
                    }
                }
            }
        }
        return null;
    }

    private static InfernalBoilerBlockEntity controllerOf(Level level, InfernalBoilerBlockEntity part) {
        if (part.isController()) {
            return part;
        }
        if (part.getControllerBE() instanceof InfernalBoilerBlockEntity controller
                && controller.hasFormation()
                && part.boilerOrigin().equals(controller.boilerOrigin())) {
            return controller;
        }
        return null;
    }

    /** 4×4 底座在任意旋转下都覆盖 origin..origin+3，因此占地与 rotation 无关。 */
    private static boolean footprintContains(BlockPos origin, BlockPos pos, int margin) {
        return pos.getX() >= origin.getX() - margin && pos.getX() <= origin.getX() + 3 + margin
                && pos.getZ() >= origin.getZ() - margin && pos.getZ() <= origin.getZ() + 3 + margin
                && pos.getY() >= origin.getY() && pos.getY() <= origin.getY() + 4;
    }

    /** 局部坐标 (x,y,z) 旋转到世界坐标。rotation 0 为正面朝南。 */
    public static BlockPos world(BlockPos origin, int x, int y, int z, int rotation) {
        return origin.offset(rotateX(x, z, rotation), y, rotateZ(x, z, rotation));
    }

    private static int rotateX(int x, int z, int rotation) {
        return switch (rotation) {
            case 1 -> z;
            case 2 -> 3 - x;
            case 3 -> 3 - z;
            default -> x;
        };
    }

    private static int rotateZ(int x, int z, int rotation) {
        return switch (rotation) {
            case 1 -> 3 - x;
            case 2 -> 3 - z;
            case 3 -> x;
            default -> z;
        };
    }

    public static Direction facingFor(int rotation) {
        return switch (rotation) {
            case 1 -> Direction.EAST;
            case 2 -> Direction.NORTH;
            case 3 -> Direction.WEST;
            default -> Direction.SOUTH;
        };
    }
}
