package com.mysterydynamics.block;

import com.mojang.serialization.MapCodec;
import com.mysterydynamics.MysteryDynamics;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.wands.WandApi;

/** 加固源质储罐：可接受源质管道输入的容器方块，支持水平四向摆放。 */
public class ReinforcedEssentiaTankBlock extends HorizontalDirectionalBlock implements EntityBlock {

    public static final BooleanProperty FORMED = BooleanProperty.create("formed");

    public ReinforcedEssentiaTankBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(FORMED, false));
    }

    @Override
    public MapCodec<ReinforcedEssentiaTankBlock> codec() {
        return simpleCodec(ReinforcedEssentiaTankBlock::new);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, FORMED);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return state.getValue(FORMED) ? RenderShape.INVISIBLE : RenderShape.MODEL;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.setValue(FACING, mirror.mirror(state.getValue(FACING)));
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, net.minecraft.world.entity.player.Player player) {
        if (!level.isClientSide && state.getValue(FORMED)) {
            EssentiaIntegrationStructure.dismantleAround(level, pos);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        if (!WandApi.isWand(stack)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        BlockPos origin = EssentiaIntegrationStructure.findOrigin(level, pos);
        if (origin == null || !EssentiaIntegrationStructure.isValid(level, origin)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (level.isClientSide) {
            return ItemInteractionResult.SUCCESS;
        }
        AspectList cost = new AspectList()
                .add(Aspect.AIR, 75)
                .add(Aspect.FIRE, 75)
                .add(Aspect.WATER, 75)
                .add(Aspect.EARTH, 75)
                .add(Aspect.ENTROPY, 75)
                .add(Aspect.ORDER, 75);
        if (!WandApi.consumeVis(stack, player, cost, false)) {
            return ItemInteractionResult.FAIL;
        }
        return EssentiaIntegrationStructure.form(level, origin)
                ? ItemInteractionResult.SUCCESS
                : ItemInteractionResult.FAIL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ReinforcedEssentiaTankBlockEntity(
                MysteryDynamics.REINFORCED_ESSENTIA_TANK_BE.get(), pos, state);
    }
}
