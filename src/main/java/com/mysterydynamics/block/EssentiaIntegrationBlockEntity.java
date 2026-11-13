package com.mysterydynamics.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IAspectContainer;

/**
 * 源质集成存储站控制器/端口。
 * 底层两侧（局部 x=±1, y=0）为端口，所有端口共享控制器 3500 点容量。
 */
public class EssentiaIntegrationBlockEntity extends com.simibubi.create.foundation.blockEntity.CachedRenderBBBlockEntity
        implements IAspectContainer {

    public static final int CAPACITY = 3500;
    private static final AspectList EMPTY = new AspectList();

    private final AspectList aspects = new AspectList();
    private boolean formed;
    private int rotation;
    private BlockPos owner = BlockPos.ZERO;
    private boolean controller;

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

    public void setFormed(boolean formed) {
        this.formed = formed;
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
        }
    }

    /** 所有 27 个存储站方块都记录同一个 origin。 */
    public void setOwner(BlockPos origin) {
        this.owner = origin.immutable();
        this.controller = worldPosition.equals(owner);
        setChanged();
    }

    private EssentiaIntegrationBlockEntity controllerBE() {
        if (controller) return this;
        if (level == null || owner == null || owner.equals(BlockPos.ZERO)) return null;
        return level.getBlockEntity(owner) instanceof EssentiaIntegrationBlockEntity be ? be : null;
    }

    /** 端口位于底层两侧：局部 x=±1、y=0。 */
    private boolean isPort() {
        if (controller || owner == null || owner.equals(BlockPos.ZERO)) return false;
        int dx = worldPosition.getX() - owner.getX();
        int dz = worldPosition.getZ() - owner.getZ();
        if (worldPosition.getY() != owner.getY()) return false;
        int lx, lz;
        switch (rotation & 3) {
            case 1 -> { lx = -dz; lz = dx; }
            case 2 -> { lx = -dx; lz = -dz; }
            case 3 -> { lx = dz; lz = -dx; }
            default -> { lx = dx; lz = dz; }
        }
        return Math.abs(lx) == 1;
    }

    @Override
    public AABB getRenderBoundingBox() {
        BlockPos pos = getBlockPos();
        return new AABB(pos.getX() - 1.0D, pos.getY(), pos.getZ() - 1.0D,
                pos.getX() + 2.0D, pos.getY() + 3.0D, pos.getZ() + 2.0D).inflate(0.5D);
    }

    @Override
    public AspectList getAspects() {
        if (controller) return aspects;
        EssentiaIntegrationBlockEntity be = controllerBE();
        return be != null && isPort() ? be.aspects : EMPTY;
    }

    @Override
    public void setAspects(AspectList list) {
        if (controller) {
            aspects.aspects.clear();
            if (list != null) aspects.add(list);
            setChanged();
        } else if (isPort()) {
            EssentiaIntegrationBlockEntity be = controllerBE();
            if (be != null) be.setAspects(list);
        }
    }

    @Override
    public boolean doesContainerAccept(Aspect aspect) {
        if (!controller && !isPort()) return false;
        EssentiaIntegrationBlockEntity be = controllerBE();
        return be != null && be.aspects.totalAmount() < CAPACITY;
    }

    @Override
    public int addToContainer(Aspect aspect, int amount) {
        if (!controller && !isPort()) return 0;
        EssentiaIntegrationBlockEntity be = controllerBE();
        if (be == null || aspect == null || amount <= 0) return 0;
        int accepted = Math.min(amount, CAPACITY - be.aspects.totalAmount());
        if (accepted <= 0) return 0;
        be.aspects.add(aspect, accepted);
        be.setChanged();
        return accepted;
    }

    @Override
    public boolean takeFromContainer(Aspect aspect, int amount) {
        if (!controller && !isPort()) return false;
        EssentiaIntegrationBlockEntity be = controllerBE();
        if (be == null || aspect == null || amount <= 0 || be.aspects.amount(aspect) < amount) {
            return false;
        }
        be.aspects.remove(aspect, amount);
        be.setChanged();
        return true;
    }

    @Override
    public boolean doesContainerContainAmount(Aspect aspect, int amount) {
        if (!controller && !isPort()) return false;
        EssentiaIntegrationBlockEntity be = controllerBE();
        return be != null && aspect != null && be.aspects.amount(aspect) >= amount;
    }

    @Override
    public int containerContains(Aspect aspect) {
        if (!controller && !isPort()) return 0;
        EssentiaIntegrationBlockEntity be = controllerBE();
        return be == null || aspect == null ? 0 : be.aspects.amount(aspect);
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
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("Formed", formed);
        tag.putInt("Rotation", rotation);
        tag.putLong("Owner", owner.asLong());
        aspects.writeToNBT(tag);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        formed = tag.getBoolean("Formed");
        rotation = tag.getInt("Rotation") & 3;
        owner = BlockPos.of(tag.getLong("Owner"));
        controller = worldPosition.equals(owner);
        if (controller && owner.equals(BlockPos.ZERO)) {
            owner = worldPosition.immutable();
            controller = true;
        }
        aspects.readFromNBT(tag);
    }
}
