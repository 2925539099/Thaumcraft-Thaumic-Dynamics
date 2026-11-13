package com.mysterydynamics.block;

import com.mojang.serialization.MapCodec;
import com.mysterydynamics.MysteryDynamics;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.wands.WandApi;

/** 工程构材；也是源质集成存储站的控制器方块。 */
public class EngineeringConstructBlock extends HorizontalDirectionalBlock implements EntityBlock {

    public static final BooleanProperty FORMED = BooleanProperty.create("formed");
    private static final int VIS_COST = 75;

    public EngineeringConstructBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(FORMED, false));
    }

    @Override
    public MapCodec<EngineeringConstructBlock> codec() {
        return simpleCodec(EngineeringConstructBlock::new);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, FORMED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return state.getValue(FORMED) ? RenderShape.INVISIBLE : RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EssentiaIntegrationBlockEntity(
                MysteryDynamics.ESSENTIA_INTEGRATION_BE.get(), pos, state);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (!WandApi.isWand(stack)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        BlockPos origin = EssentiaIntegrationStructure.findOrigin(level, pos);
        if (origin == null) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        BlockEntity be = level.getBlockEntity(origin);
        if (!(be instanceof EssentiaIntegrationBlockEntity station)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (station.isFormed()) {
            return ItemInteractionResult.SUCCESS;
        }
        if (level.isClientSide) {
            return ItemInteractionResult.SUCCESS;
        }
        if (!EssentiaIntegrationStructure.isValid(level, origin)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        AspectList cost = new AspectList()
                .add(Aspect.AIR, VIS_COST)
                .add(Aspect.FIRE, VIS_COST)
                .add(Aspect.WATER, VIS_COST)
                .add(Aspect.EARTH, VIS_COST)
                .add(Aspect.ENTROPY, VIS_COST)
                .add(Aspect.ORDER, VIS_COST);
        if (!WandApi.consumeVis(stack, player, cost, false)) {
            return ItemInteractionResult.FAIL;
        }

        station.setFormed(true);
        EssentiaIntegrationStructure.setFormed(level, origin, true);
        return ItemInteractionResult.SUCCESS;
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, net.minecraft.world.entity.player.Player player) {
        if (!level.isClientSide && state.getValue(FORMED)) {
            EssentiaIntegrationStructure.dismantleAround(level, pos);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!level.isClientSide && state.is(this) && !newState.is(this)) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof EssentiaIntegrationBlockEntity station && station.isFormed()) {
                EssentiaIntegrationStructure.dismantle(level, pos);
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
