package com.mysterydynamics.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IAspectContainer;

/** 源质集成存储站的控制器方块实体，容量 12×256＝3072。 */
public class EssentiaIntegrationBlockEntity extends BlockEntity implements IAspectContainer {

    public static final int CAPACITY = 3072;

    private final AspectList aspects = new AspectList();
    private boolean formed;

    public EssentiaIntegrationBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public boolean isFormed() {
        return formed;
    }

    public void setFormed(boolean formed) {
        this.formed = formed;
        setChanged();
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
        aspects.writeToNBT(tag);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        formed = tag.getBoolean("Formed");
        aspects.readFromNBT(tag);
    }
}
