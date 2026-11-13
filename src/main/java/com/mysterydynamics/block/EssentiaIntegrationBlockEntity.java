package com.mysterydynamics.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import com.simibubi.create.foundation.blockEntity.CachedRenderBBBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IAspectContainer;

/** 源质集成存储站的控制器方块实体，容量 12×256＝3072。 */
public class EssentiaIntegrationBlockEntity extends CachedRenderBBBlockEntity implements IAspectContainer {

    public static final int CAPACITY = 3072;

    private final AspectList aspects = new AspectList();
    private boolean formed;
    private int rotation;

    public EssentiaIntegrationBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public boolean isFormed() {
        return formed;
    }

    public int getRotation() {
        return rotation & 3;
    }

    public void setRotation(int rotation) {
        this.rotation = rotation & 3;
        setChanged();
    }

    @Override
    public AABB getRenderBoundingBox() {
        // 整座模型是 3×3×3；必须扩大渲染裁剪盒，
        // 否则视角偏移、控制器离开视锥时整座模型会消失。
        BlockPos pos = getBlockPos();
        return new AABB(pos.getX() - 1.0D, pos.getY(), pos.getZ() - 1.0D,
                pos.getX() + 2.0D, pos.getY() + 3.0D, pos.getZ() + 2.0D)
                .inflate(0.5D);
    }

    public void setFormed(boolean formed) {
        this.formed = formed;
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public AspectList getAspects() {
        return aspects;
    }

    @Override
    public void setAspects(AspectList list) {
        aspects.aspects.clear();
        if (list != null) aspects.add(list);
        setChanged();
    }

    @Override
    public boolean doesContainerAccept(Aspect aspect) {
        return aspects.totalAmount() < CAPACITY;
    }

    @Override
    public int addToContainer(Aspect aspect, int amount) {
        if (aspect == null || amount <= 0) return 0;
        int accepted = Math.min(amount, CAPACITY - aspects.totalAmount());
        if (accepted <= 0) return 0;
        aspects.add(aspect, accepted);
        setChanged();
        return accepted;
    }

    @Override
    public boolean takeFromContainer(Aspect aspect, int amount) {
        if (aspect == null || amount <= 0 || aspects.amount(aspect) < amount) return false;
        aspects.remove(aspect, amount);
        setChanged();
        return true;
    }

    @Override
    public boolean doesContainerContainAmount(Aspect aspect, int amount) {
        return aspect != null && aspects.amount(aspect) >= amount;
    }

    @Override
    public int containerContains(Aspect aspect) {
        return aspect == null ? 0 : aspects.amount(aspect);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("Formed", formed);
        tag.putInt("Rotation", rotation);
        aspects.writeToNBT(tag);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        formed = tag.getBoolean("Formed");
        rotation = tag.getInt("Rotation") & 3;
        aspects.readFromNBT(tag);
    }
}
