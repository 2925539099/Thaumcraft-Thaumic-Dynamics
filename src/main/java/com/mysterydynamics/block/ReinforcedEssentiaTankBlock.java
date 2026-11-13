package com.mysterydynamics.block;

import com.mysterydynamics.MysteryDynamics;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** 加固源质储罐：可接受源质管道输入的容器方块。 */
public class ReinforcedEssentiaTankBlock extends Block implements EntityBlock {

    public ReinforcedEssentiaTankBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ReinforcedEssentiaTankBlockEntity(
                MysteryDynamics.REINFORCED_ESSENTIA_TANK_BE.get(), pos, state);
    }
}
