package com.mysterydynamics.block;

import com.mysterydynamics.MysteryDynamics;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import com.simibubi.create.content.kinetics.base.KineticBlock;
import com.simibubi.create.foundation.block.IBE;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/** 奥法涡轮机：两格高、可接入 Create 应力网络的旋转方块。 */
public class ArcaneTurbineBlock extends KineticBlock
        implements EntityBlock, IBE<ArcaneTurbineBlockEntity> {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;

    public ArcaneTurbineBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(HALF, DoubleBlockHalf.LOWER));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, HALF);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        Level level = context.getLevel();
        if (pos.getY() >= level.getMaxBuildHeight() - 1
                || !level.getBlockState(pos.above()).canBeReplaced(context)) {
            return null;
        }
        return defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite())
                .setValue(HALF, DoubleBlockHalf.LOWER);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state,
                            LivingEntity placer, ItemStack stack) {
        level.setBlock(pos.above(), state.setValue(HALF, DoubleBlockHalf.UPPER), Block.UPDATE_ALL);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighbor,
                                  LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (state.getValue(HALF) == DoubleBlockHalf.LOWER && direction == Direction.UP) {
            return neighbor.is(this) && neighbor.getValue(HALF) == DoubleBlockHalf.UPPER
                    ? state : Blocks.AIR.defaultBlockState();
        }
        if (state.getValue(HALF) == DoubleBlockHalf.UPPER && direction == Direction.DOWN) {
            return neighbor.is(this) && neighbor.getValue(HALF) == DoubleBlockHalf.LOWER
                    ? state : Blocks.AIR.defaultBlockState();
        }
        return state;
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide) {
            BlockPos otherPos = state.getValue(HALF) == DoubleBlockHalf.LOWER ? pos.above() : pos.below();
            if (level.getBlockState(otherPos).is(this)) {
                level.setBlock(otherPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public void playerDestroy(Level level, Player player, BlockPos pos, BlockState state,
                              BlockEntity blockEntity, ItemStack tool) {
        if (!player.isCreative()) {
            Block.popResource(level, pos, new ItemStack(asItem()));
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState,
                         boolean movedByPiston) {
        if (!level.isClientSide && !newState.is(this)) {
            boolean anyTurbineLeft = ArcaneTurbineBlockEntity.countTurbines(level, pos, 8) > 0;
            // 涡轮机移除后让相邻储罐重新评估锅炉状态，流体储罐会恢复原状。
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                if (level.getBlockEntity(pos.relative(direction)) instanceof FluidTankBlockEntity tank) {
                    FluidTankBlockEntity controller = tank.getControllerBE();
                    if (controller != null) {
                        if (!anyTurbineLeft) {
                            controller.boiler.attachedEngines = 0;
                            controller.boiler.needsHeatLevelUpdate = true;
                        }
                        controller.updateBoilerState();
                        controller.updateBoilerTemperature();
                        controller.setChanged();
                        controller.sendData();
                    }
                }
            }
            var infernal = com.mysterydynamics.compat.InfernalBoilerStructure.findController(level, pos);
            if (infernal != null) {
                if (!anyTurbineLeft) {
                    infernal.boiler.attachedEngines = 0;
                    infernal.boiler.needsHeatLevelUpdate = true;
                }
                infernal.updateBoilerState();
                infernal.updateBoilerTemperature();
                infernal.setChanged();
                infernal.sendData();
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        Level level = context.getLevel();
        if (!level.isClientSide) {
            BlockPos pos = context.getClickedPos();
            if (state.getValue(HALF) == DoubleBlockHalf.UPPER) {
                pos = pos.below();
            }
            if (level.getBlockEntity(pos) instanceof ArcaneTurbineBlockEntity turbine) {
                turbine.toggleDirection();
                IWrenchable.playRotateSound(level, pos);
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult onSneakWrenched(BlockState state, UseOnContext context) {
        Level level = context.getLevel();
        if (!(level instanceof ServerLevel)) {
            return InteractionResult.SUCCESS;
        }
        BlockPos pos = context.getClickedPos();
        BlockPos otherPos = state.getValue(HALF) == DoubleBlockHalf.LOWER ? pos.above() : pos.below();
        if (level.getBlockState(otherPos).is(this)) {
            level.setBlock(otherPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
        level.removeBlock(pos, false);
        Player player = context.getPlayer();
        if (player != null && !player.isCreative()) {
            player.getInventory().placeItemBackInInventory(new ItemStack(asItem()));
        }
        IWrenchable.playRemoveSound(level, pos);
        return InteractionResult.SUCCESS;
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return Direction.Axis.Y;
    }

    @Override
    public boolean hasShaftTowards(LevelReader level, BlockPos pos, BlockState state, Direction face) {
        // 仅下半部分底面输出应力。
        return state.getValue(HALF) == DoubleBlockHalf.LOWER && face == Direction.DOWN;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ArcaneTurbineBlockEntity(MysteryDynamics.ARCANE_TURBINE_BE.get(), pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                  BlockEntityType<T> type) {
        return (tickLevel, pos, tickState, blockEntity) -> {
            if (blockEntity instanceof ArcaneTurbineBlockEntity turbine) {
                turbine.tick();
            }
        };
    }

    @Override
    public Class<ArcaneTurbineBlockEntity> getBlockEntityClass() {
        return ArcaneTurbineBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends ArcaneTurbineBlockEntity> getBlockEntityType() {
        return MysteryDynamics.ARCANE_TURBINE_BE.get();
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }
}
