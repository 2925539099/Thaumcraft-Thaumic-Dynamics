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

/** 容量 256 的加固源质储罐，可通过源质管道输入/抽取。 */
public class ReinforcedEssentiaTankBlockEntity extends BlockEntity implements IAspectContainer {

    public static final int CAPACITY = 256;

    private final AspectList aspects = new AspectList();

    public ReinforcedEssentiaTankBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public AspectList getAspects() {
        return aspects;
    }

    @Override
    public void setAspects(AspectList list) {
        aspects.aspects.clear();
        if (list != null) {
            aspects.add(list);
        }
        setChanged();
    }

    @Override
    public boolean doesContainerAccept(Aspect aspect) {
        return aspects.totalAmount() < CAPACITY;
    }

    @Override
    public int addToContainer(Aspect aspect, int amount) {
        if (aspect == null || amount <= 0) {
            return 0;
        }
        int accepted = Math.min(amount, CAPACITY - aspects.totalAmount());
        if (accepted <= 0) {
            return 0;
        }
        aspects.add(aspect, accepted);
        setChanged();
        return accepted;
    }

    @Override
    public boolean takeFromContainer(Aspect aspect, int amount) {
        if (aspect == null || amount <= 0 || aspects.amount(aspect) < amount) {
            return false;
        }
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
        aspects.writeToNBT(tag);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        aspects.readFromNBT(tag);
    }
}
