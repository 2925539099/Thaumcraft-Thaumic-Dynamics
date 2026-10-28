package com.mysterydynamics.compat;

import com.mysterydynamics.MysteryDynamics;
import com.mysterydynamics.block.LargeCentrifugeControllerBlock;
import com.mysterydynamics.block.LargeCentrifugeControllerBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.IEssentiaTransport;

/** 底座两侧的源质输入/输出口。输入口在模型西面，输出口在东面。 */
public final class LargeCentrifugeTransport implements IEssentiaTransport {

    private final LargeCentrifugeControllerBlockEntity controller;
    private final Direction portDirection;
    private final boolean input;

    private LargeCentrifugeTransport(LargeCentrifugeControllerBlockEntity controller,
                                     Direction portDirection, boolean input) {
        this.controller = controller;
        this.portDirection = portDirection;
        this.input = input;
    }

    public static IEssentiaTransport get(Level level, BlockPos pos, Direction side) {
        if (level == null || side == null) {
            return null;
        }
        for (int dy = -2; dy <= 2; dy++) {
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    BlockPos center = pos.offset(dx, dy, dz);
                    BlockState state = level.getBlockState(center);
                    if (!state.is(MysteryDynamics.LARGE_CENTRIFUGE_CONTROLLER.get())) {
                        continue;
                    }
                    int rotation = state.hasProperty(LargeCentrifugeControllerBlock.ROTATION)
                            ? state.getValue(LargeCentrifugeControllerBlock.ROTATION) : 0;
                    if (!(level.getBlockEntity(center)
                            instanceof LargeCentrifugeControllerBlockEntity controller)) {
                        continue;
                    }
                    BlockPos inputPos = LargeCentrifugeStructure.world(center, -1, -1, 0, rotation);
                    BlockPos outputPos = LargeCentrifugeStructure.world(center, 1, -1, 0, rotation);
                    if (pos.equals(inputPos)
                            && side == LargeCentrifugeStructure.directionForLocal(-1, 0, rotation)) {
                        return new LargeCentrifugeTransport(controller, side, true);
                    }
                    if (pos.equals(outputPos)
                            && side == LargeCentrifugeStructure.directionForLocal(1, 0, rotation)) {
                        return new LargeCentrifugeTransport(controller, side, false);
                    }
                }
            }
        }
        return null;
    }

    @Override
    public boolean isConnectable(Direction side) {
        return side == portDirection;
    }

    @Override
    public boolean canInputFrom(Direction side) {
        return input && side == portDirection && controller.canInputEssentia();
    }

    @Override
    public boolean canOutputTo(Direction side) {
        return !input && side == portDirection && controller.canOutputEssentia();
    }

    @Override
    public void setSuction(Aspect aspect, int amount) {
    }

    @Override
    public Aspect getSuctionType(Direction side) {
        return input ? null : controller.outputAspect();
    }

    @Override
    public int getSuctionAmount(Direction side) {
        return side == portDirection ? 64 : 0;
    }

    @Override
    public int takeEssentia(Aspect aspect, int amount, Direction side) {
        if (!canOutputTo(side)) {
            return 0;
        }
        return controller.takeEssentia(aspect, amount);
    }

    @Override
    public int addEssentia(Aspect aspect, int amount, Direction side) {
        if (!canInputFrom(side)) {
            return 0;
        }
        return controller.addEssentia(aspect, amount);
    }

    @Override
    public Aspect getEssentiaType(Direction side) {
        return input ? null : controller.outputAspect();
    }

    @Override
    public int getEssentiaAmount(Direction side) {
        return input ? 0 : controller.storedOutput();
    }

    @Override
    public int getMinimumSuction() {
        return 0;
    }
}
