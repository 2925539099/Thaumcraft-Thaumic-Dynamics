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

    /** 成型时记录的控制器坐标；null 表示未知（旧存档）。 */
    @javax.annotation.Nullable
    private BlockPos center;
    private int rotation;

    public LargeCentrifugeCasingInfoBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public void setFormation(BlockPos center, int rotation) {
        this.center = center.immutable();
        this.rotation = rotation & 3;
        setChanged();
    }

    @javax.annotation.Nullable
    public BlockPos center() {
        return center;
    }

    private LargeCentrifugeControllerBlockEntity controller() {
        if (level == null) {
            return null;
        }
        if (center != null) {
            return level.isLoaded(center)
                    && level.getBlockEntity(center) instanceof LargeCentrifugeControllerBlockEntity c ? c : null;
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
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag, registries);
        return tag;
    }

    @Override
    public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (center != null) {
            tag.putLong("Center", center.asLong());
        }
        tag.putInt("Rotation", rotation);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        // 旧存档用 BlockPos.ZERO 表示“未知”，这里统一转成 null。
        BlockPos loaded = tag.contains("Center") ? BlockPos.of(tag.getLong("Center")) : null;
        center = loaded == null || loaded.equals(BlockPos.ZERO) ? null : loaded;
        rotation = tag.getInt("Rotation") & 3;
    }
}
