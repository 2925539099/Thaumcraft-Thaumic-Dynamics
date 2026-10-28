package com.mysterydynamics.compat;

import com.mysterydynamics.MysteryDynamics;
import com.mysterydynamics.block.LargeCentrifugeControllerBlock;
import com.mysterydynamics.block.LargeCentrifugeControllerBlockEntity;
import com.simibubi.create.AllBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.common.registry.TCBlocks;

import java.util.concurrent.atomic.AtomicBoolean;

/** 3×3×3 大型奥法离心机结构。第三层中心为控制器，底层正面为朝向标记。 */
public final class LargeCentrifugeStructure {

    private static final AtomicBoolean RESTORING = new AtomicBoolean(false);

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

    public static boolean matches(Level level, BlockPos center, int rotation) {
        if (!level.isLoaded(center)) {
            return false;
        }
        // Bottom layer: 3×3 arcane bricks, center windmill bearing, front-center alchemical construct.
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                BlockPos pos = world(center, x, -1, z, rotation);
                BlockState state = level.getBlockState(pos);
                if (x == 0 && z == 0) {
                    if (!state.is(AllBlocks.MECHANICAL_BEARING.get())) {
                        return false;
                    }
                } else if (x == 0 && z == -1) {
                    if (!state.is(TCBlocks.ALCHEMICAL_CONSTRUCT.get())) {
                        return false;
                    }
                } else if (!state.is(TCBlocks.ARCANE_STONE_BRICKS.get())) {
                    return false;
                }
            }
        }
        // Middle layer: only the center construct; everything else air.
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                BlockPos pos = world(center, x, 0, z, rotation);
                if (x == 0 && z == 0) {
                    if (!level.getBlockState(pos).is(TCBlocks.ALCHEMICAL_CONSTRUCT.get())) {
                        return false;
                    }
                } else if (!level.getBlockState(pos).isAir()) {
                    return false;
                }
            }
        }
        // Top layer: center clutch; everything else air.
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                BlockPos pos = world(center, x, 1, z, rotation);
                if (x == 0 && z == 0) {
                    if (!level.getBlockState(pos).is(AllBlocks.CLUTCH.get())) {
                        return false;
                    }
                } else if (!level.getBlockState(pos).isAir()) {
                    return false;
                }
            }
        }
        return true;
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

    /** 填充块被破坏时，从周围 3×3×3 中寻找控制器，读其朝向再恢复整栋。 */
    public static void requestDismantleNear(Level level, BlockPos around) {
        if (level.isClientSide || isRestoring() || level.getServer() == null) {
            return;
        }
        for (int dy = -2; dy <= 2; dy++) {
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    BlockPos candidate = around.offset(dx, dy, dz);
                    BlockState state = level.getBlockState(candidate);
                    if (state.is(MysteryDynamics.LARGE_CENTRIFUGE_CONTROLLER.get())) {
                        int rotation = state.hasProperty(LargeCentrifugeControllerBlock.ROTATION)
                                ? state.getValue(LargeCentrifugeControllerBlock.ROTATION) : 0;
                        requestDismantle(level, candidate, rotation, around);
                        return;
                    }
                }
            }
        }
    }

    public static void requestDismantle(Level level, BlockPos center, int rotation, BlockPos brokenPos) {
        if (level.isClientSide || center.equals(BlockPos.ZERO) || isRestoring()
                || level.getServer() == null) {
            return;
        }
        level.getServer().execute(() -> dismantle(level, center, rotation, brokenPos));
    }

    public static void dismantle(Level level, BlockPos center, int rotation, BlockPos brokenPos) {
        if (center.equals(BlockPos.ZERO)) {
            return;
        }
        RESTORING.set(true);
        try {
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    BlockPos pos = world(center, x, -1, z, rotation);
                    if (pos.equals(brokenPos)) {
                        continue;
                    }
                    Block block;
                    if (x == 0 && z == 0) {
                        block = AllBlocks.MECHANICAL_BEARING.get();
                    } else if (x == 0 && z == -1) {
                        block = TCBlocks.ALCHEMICAL_CONSTRUCT.get();
                    } else {
                        block = TCBlocks.ARCANE_STONE_BRICKS.get();
                    }
                    level.setBlock(pos, block.defaultBlockState(), 3);
                }
            }
            if (!center.equals(brokenPos)) {
                level.removeBlockEntity(center);
                level.setBlock(center, TCBlocks.ALCHEMICAL_CONSTRUCT.get().defaultBlockState(), 3);
            }
            BlockPos top = world(center, 0, 1, 0, rotation);
            if (!top.equals(brokenPos)) {
                level.removeBlockEntity(top);
                level.setBlock(top, AllBlocks.CLUTCH.get().defaultBlockState(), 3);
            }
        } finally {
            RESTORING.set(false);
        }
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
