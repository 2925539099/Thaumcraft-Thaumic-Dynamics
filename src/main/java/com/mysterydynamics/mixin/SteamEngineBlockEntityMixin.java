package com.mysterydynamics.mixin;

import com.mysterydynamics.MysteryDynamics;
import com.simibubi.create.content.kinetics.steamEngine.SteamEngineBlock;
import com.simibubi.create.content.kinetics.steamEngine.SteamEngineBlockEntity;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Create 的 SteamEngineBlockEntity.isValid() 只认 AllBlocks.FLUID_TANK 这个精确方块。
 * 炼狱锅炉的 3×3×3 主体不是 Create 的 FLUID_TANK，所以需要单独放行。
 * 底座、外壳和正面不在这里放行，避免底座也能带动引擎。
 */
@Mixin(value = SteamEngineBlockEntity.class, remap = false)
public abstract class SteamEngineBlockEntityMixin {

    @Inject(method = "isValid", at = @At("HEAD"), cancellable = true, remap = false)
    private void mysterydynamics$acceptInfernalBoiler(CallbackInfoReturnable<Boolean> cir) {
        SteamEngineBlockEntity engine = (SteamEngineBlockEntity) (Object) this;
        if (engine.getLevel() == null) {
            return;
        }
        Direction back = SteamEngineBlock.getConnectedDirection(engine.getBlockState()).getOpposite();
        BlockState behind = engine.getLevel().getBlockState(engine.getBlockPos().relative(back));
        if (behind.is(MysteryDynamics.INFERNAL_BOILER.get())) {
            cir.setReturnValue(true);
        }
    }

    // 以前这里还有一个 @Redirect 把 tick 里的 Mth.clamp 上限放宽到 2.0，
    // 但 InfernalBoilerData.getEngineEfficiency 永远 ≤ 1（超频只翻倍涡轮机总应力），
    // 那个重定向对炼狱锅炉是死代码，却会改变所有锅炉/其他模组的蒸汽引擎行为，因此已移除。
}
