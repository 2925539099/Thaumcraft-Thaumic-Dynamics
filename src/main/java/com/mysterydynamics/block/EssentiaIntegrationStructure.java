package com.mysterydynamics.block;

import com.mysterydynamics.MysteryDynamics;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.level.BlockEvent;

/** 源质集成存储站：3×3×3 多方块结构，支持四个水平朝向。 */
public final class EssentiaIntegrationStructure {

    public static final int SIZE = 3;

    private EssentiaIntegrationStructure() {
    }

    /** 局部坐标 (x,y,z) 在 rotation 下的世界偏移。 */
    private static int rotateX(int x, int z, int rotation) {
        return switch (rotation & 3) {
            case 1 -> z;
            case 2 -> -x;
            case 3 -> -z;
            default -> x;
        };
    }

    private static int rotateZ(int x, int z, int rotation) {
        return switch (rotation & 3) {
            case 1 -> -x;
            case 2 -> -z;
            case 3 -> x;
            default -> z;
        };
    }

    private static BlockPos local(BlockPos origin, int x, int y, int z, int rotation) {
        return origin.offset(rotateX(x, z, rotation), y, rotateZ(x, z, rotation));
    }

    /**
     * 局部结构：
     * y=0 全工程构材；
     * y=1 左右两列为储罐，中间列为工程构材；
     * y=2 左右两列为储罐，中间列不限制（成型时会被填满）。
     */
    private static boolean matchesRotation(Level level, BlockPos origin, int rotation) {
        for (int y = 0; y < SIZE; y++) {
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    BlockPos pos = local(origin, x, y, z, rotation);
                    BlockState state = level.getBlockState(pos);
                    boolean engineering = state.is(MysteryDynamics.ENGINEERING_CONSTRUCT.get());
                    boolean tank = state.is(MysteryDynamics.REINFORCED_ESSENTIA_TANK.get());
                    if (y == 0) {
                        if (!engineering) return false;
                    } else if (y == 1) {
                        if (x == 0 ? !engineering : !tank) return false;
                    } else {
                        if (x != 0 && !tank) return false;
                    }
                }
            }
        }
        return true;
    }

    /** 返回结构朝向 0~3；不匹配返回 -1。 */
    public static int findRotation(Level level, BlockPos origin) {
        if (level == null) return -1;
        for (int rotation = 0; rotation < 4; rotation++) {
            if (matchesRotation(level, origin, rotation)) return rotation;
        }
        return -1;
    }

    public static boolean isValid(Level level, BlockPos origin) {
        return findRotation(level, origin) >= 0;
    }

    /** 从点击的任意结构方块反推底层中心。 */
    public static BlockPos findOrigin(Level level, BlockPos clicked) {
        if (level == null) return null;
        for (int dy = 0; dy >= -2; dy--) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    BlockPos origin = clicked.offset(dx, dy, dz);
                    if (origin.getY() < level.getMinBuildHeight()) continue;
                    if (isValid(level, origin)) return origin.immutable();
                }
            }
        }
        return null;
    }

    /** 成型：整个 3×3×3 全部替换成源质集成存储站方块。 */
    public static boolean form(Level level, BlockPos origin) {
        int rotation = findRotation(level, origin);
        if (rotation < 0) return false;
        for (int y = 0; y < SIZE; y++) {
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    level.setBlock(local(origin, x, y, z, rotation),
                            MysteryDynamics.ESSENTIA_INTEGRATION_STATION.get().defaultBlockState(),
                            Block.UPDATE_ALL);
                }
            }
        }
        if (level.getBlockEntity(origin) instanceof EssentiaIntegrationBlockEntity station) {
            station.setRotation(rotation);
            station.setFormed(true);
        }
        return true;
    }

    /** 从任意已成型部件找到控制器位置。 */
    public static BlockPos findFormedController(Level level, BlockPos around) {
        for (int dy = -2; dy <= 2; dy++) {
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    BlockPos pos = around.offset(dx, dy, dz);
                    if (level.getBlockEntity(pos) instanceof EssentiaIntegrationBlockEntity station
                            && station.isFormed()) {
                        return pos.immutable();
                    }
                }
            }
        }
        return null;
    }

    /** 成型结构被破坏时直接取消这次破坏，并原地还原整座结构。 */
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (event.getLevel().isClientSide()) return;
        if (!(event.getLevel() instanceof Level level)) return;
        BlockState state = event.getState();
        if (!(state.is(MysteryDynamics.ESSENTIA_INTEGRATION_STATION.get())
                || state.is(MysteryDynamics.ENGINEERING_CONSTRUCT.get())
                || state.is(MysteryDynamics.REINFORCED_ESSENTIA_TANK.get()))) {
            return;
        }
        BlockPos controller = findFormedController(level, event.getPos());
        if (controller != null) {
            event.setCanceled(true);
            dismantle(level, controller);
        }
    }

    public static void dismantleAround(Level level, BlockPos around) {
        BlockPos controller = findFormedController(level, around);
        if (controller != null) {
            dismantle(level, controller);
        }
    }

    /** 拆解：按原朝向恢复工程构材/加固源质储罐。 */
    public static void dismantle(Level level, BlockPos origin) {
        if (level == null) return;
        int rotation = 0;
        if (level.getBlockEntity(origin) instanceof EssentiaIntegrationBlockEntity station) {
            rotation = station.getRotation();
        }
        for (int y = 0; y < SIZE; y++) {
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    BlockState restored;
                    if (y == 0 || (y == 1 && x == 0)) {
                        restored = MysteryDynamics.ENGINEERING_CONSTRUCT.get().defaultBlockState();
                    } else if (x != 0) {
                        restored = MysteryDynamics.REINFORCED_ESSENTIA_TANK.get().defaultBlockState();
                    } else {
                        restored = Blocks.AIR.defaultBlockState();
                    }
                    level.setBlock(local(origin, x, y, z, rotation), restored, Block.UPDATE_ALL);
                }
            }
        }
    }
}
