package com.mysterydynamics.block;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/** 基座填充块的信息 BE：让九个底座方块都能显示控制器状态。 */
public class LargeCentrifugeCasingInfoBlockEntity extends BlockEntity implements IHaveGoggleInformation {

    private BlockPos center = BlockPos.ZERO;
    private int rotation;

    public LargeCentrifugeCasingInfoBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public void setFormation(BlockPos center, int rotation) {
        this.center = center.immutable();
        this.rotation = rotation & 3;
        setChanged();
    }

    private LargeCentrifugeControllerBlockEntity controller() {
        if (level == null) {
            return null;
        }
        if (!center.equals(BlockPos.ZERO)) {
            return level.getBlockEntity(center) instanceof LargeCentrifugeControllerBlockEntity c ? c : null;
        }
        for (int dy = -2; dy <= 2; dy++) {
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    if (level.getBlockEntity(worldPosition.offset(dx, dy, dz))
                            instanceof LargeCentrifugeControllerBlockEntity c) {
                        return c;
                    }
                }
            }
        }
        return null;
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        LargeCentrifugeControllerBlockEntity c = controller();
        return c != null && c.addToGoggleTooltip(tooltip, isPlayerSneaking);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLong("Center", center.asLong());
        tag.putInt("Rotation", rotation);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        center = BlockPos.of(tag.getLong("Center"));
        rotation = tag.getInt("Rotation") & 3;
    }
}
