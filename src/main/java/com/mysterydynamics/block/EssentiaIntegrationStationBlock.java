package com.mysterydynamics.block;

import com.mojang.serialization.MapCodec;
import com.mysterydynamics.MysteryDynamics;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** 源质集成存储站控制器：成型后替代底层中心的工程构材。 */
public class EssentiaIntegrationStationBlock extends Block implements EntityBlock {

    public EssentiaIntegrationStationBlock(Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<EssentiaIntegrationStationBlock> codec() {
        return simpleCodec(EssentiaIntegrationStationBlock::new);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EssentiaIntegrationBlockEntity(
                MysteryDynamics.ESSENTIA_INTEGRATION_BE.get(), pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        return (lvl, pos, st, be) -> {
            if (be instanceof EssentiaIntegrationBlockEntity station) {
                EssentiaIntegrationBlockEntity.serverTick(lvl, pos, st, station);
            }
        };
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state,
                                        net.minecraft.world.entity.player.Player player) {
        if (!level.isClientSide) {
            EssentiaIntegrationStructure.dismantleAround(level, pos);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }
}
