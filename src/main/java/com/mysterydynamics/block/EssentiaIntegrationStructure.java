package com.mysterydynamics.block;

import com.mysterydynamics.MysteryDynamics;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** 源质集成存储站：3×3×3 多方块结构。 */
public final class EssentiaIntegrationStructure {

    public static final int SIZE = 3;

    private EssentiaIntegrationStructure() {
    }

    /**
     * origin 是底层中心方块。结构：
     * y=0 全工程构材；
     * y=1 左右两列为储罐，中间列为工程构材；
     * y=2 左右两列为储罐，中间列为空气。
     */
    public static boolean isValid(Level level, BlockPos origin) {
        if (level == null) return false;
        for (int y = 0; y < SIZE; y++) {
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    BlockPos pos = origin.offset(x, y, z);
                    BlockState state = level.getBlockState(pos);
                    boolean engineering = state.is(MysteryDynamics.ENGINEERING_CONSTRUCT.get());
                    boolean tank = state.is(MysteryDynamics.REINFORCED_ESSENTIA_TANK.get());
                    if (y == 0) {
                        if (!engineering) return false;
                    } else if (y == 1) {
                        if (x == 0 ? !engineering : !tank) return false;
                    } else {
                        // 顶层中心列成型时会被填满，不要求预先留空。
                        if (x != 0 && !tank) return false;
                    }
                }
            }
        }
        return true;
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
        if (!isValid(level, origin)) return false;
        for (int y = 0; y < SIZE; y++) {
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    BlockPos pos = origin.offset(x, y, z);
                    level.setBlock(pos,
                            MysteryDynamics.ESSENTIA_INTEGRATION_STATION.get().defaultBlockState(),
                            Block.UPDATE_ALL);
                }
            }
        }
        if (level.getBlockEntity(origin) instanceof EssentiaIntegrationBlockEntity station) {
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

    public static void dismantleAround(Level level, BlockPos around) {
        BlockPos controller = findFormedController(level, around);
        if (controller != null) {
            dismantle(level, controller);
        }
    }

    /** 拆解：整个 3×3×3 原地恢复为工程构材/加固源质储罐，顶层中心恢复空气。 */
    public static void dismantle(Level level, BlockPos origin) {
        if (level == null) return;
        for (int y = 0; y < SIZE; y++) {
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    BlockPos pos = origin.offset(x, y, z);
                    BlockState restored;
                    if (y == 0 || (y == 1 && x == 0)) {
                        restored = MysteryDynamics.ENGINEERING_CONSTRUCT.get().defaultBlockState();
                    } else if (x != 0) {
                        restored = MysteryDynamics.REINFORCED_ESSENTIA_TANK.get().defaultBlockState();
                    } else {
                        // 顶层中心列原本是空气。
                        restored = net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
                    }
                    level.setBlock(pos, restored, Block.UPDATE_ALL);
                }
            }
        }
    }
}
