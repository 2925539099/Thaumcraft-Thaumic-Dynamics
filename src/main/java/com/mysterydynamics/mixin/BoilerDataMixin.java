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
import org.spongepowered.asm.mixin.Unique;
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

    @Shadow
    public int attachedWhistles;

    @Unique
    private int mysterydynamics$whistlesBefore;

    /** evaluate 开始前的引擎数（含上次统计的涡轮机），用于在 RETURN 处重新判断是否变化。 */
    @Unique
    private int mysterydynamics$enginesBefore;

    @Inject(method = "evaluate", at = @At("HEAD"))
    private void mysterydynamics$rememberEngines(FluidTankBlockEntity tank,
                                                 CallbackInfoReturnable<Boolean> cir) {
        mysterydynamics$enginesBefore = attachedEngines;
        mysterydynamics$whistlesBefore = attachedWhistles;
    }

    /**
     * Create 在 evaluate 末尾用“只含蒸汽引擎”的数量算出返回值，之后才轮到这里补上涡轮机。
     * 所以只增减涡轮机时原返回值是 false，锅炉不会刷新；这里按最终数量补报变化。
     */
    @Inject(method = "evaluate", at = @At("RETURN"), cancellable = true)
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
        // 与 Create 原逻辑相同的判定，但引擎数含涡轮机：
        // 原返回值拿“含涡轮机的旧值”比“不含涡轮机的新值”，有涡轮机时永远为 true，导致每次都刷新。
        cir.setReturnValue(attachedEngines != mysterydynamics$enginesBefore
                || attachedWhistles != mysterydynamics$whistlesBefore);
    }
}
