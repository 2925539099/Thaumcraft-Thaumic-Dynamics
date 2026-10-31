package com.mysterydynamics.mixin;

import com.mysterydynamics.MysteryDynamics;
import com.mysterydynamics.block.ArcaneTurbineBlock;

import com.simibubi.create.content.fluids.tank.BoilerData;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashSet;
import java.util.Set;

/** 让奥法涡轮机像蒸汽引擎一样被锅炉统计为动力端。 */
@Mixin(BoilerData.class)
public abstract class BoilerDataMixin {

    @Shadow
    public int attachedEngines;

    @Inject(method = "evaluate", at = @At("RETURN"))
    private void mysterydynamics$countTurbines(FluidTankBlockEntity tank,
                                               CallbackInfoReturnable<Boolean> cir) {
        Level level = tank.getLevel();
        if (level == null) {
            return;
        }
        BlockPos origin = tank.getBlockPos();
        Set<BlockPos> turbines = new HashSet<>();
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
                        if (state.is(MysteryDynamics.ARCANE_TURBINE.get())
                                && state.getValue(ArcaneTurbineBlock.HALF) == DoubleBlockHalf.LOWER) {
                            turbines.add(neighbor);
                        }
                    }
                }
            }
        }
        attachedEngines += turbines.size();
    }
}
