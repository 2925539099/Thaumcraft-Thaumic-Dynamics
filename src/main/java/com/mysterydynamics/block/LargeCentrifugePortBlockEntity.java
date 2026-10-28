package com.mysterydynamics.block;

import com.mysterydynamics.compat.LargeCentrifugeStructure;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.IEssentiaTransport;

/** 大型奥法离心机源质端口 BE：西面输入、东面输出，直接实现源质接口。 */
public class LargeCentrifugePortBlockEntity extends BlockEntity implements IEssentiaTransport {

    private final boolean input;
    private BlockPos center = BlockPos.ZERO;
    private int rotation;

    public LargeCentrifugePortBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state,
                                          boolean input) {
        super(type, pos, state);
        this.input = input;
    }

    public void setFormation(BlockPos center, int rotation) {
        this.center = center.immutable();
        this.rotation = rotation & 3;
        setChanged();
    }

    private LargeCentrifugeControllerBlockEntity controller() {
        if (level == null || center.equals(BlockPos.ZERO)) {
            return null;
        }
        return level.getBlockEntity(center) instanceof LargeCentrifugeControllerBlockEntity c ? c : null;
    }

    private Direction outward() {
        return LargeCentrifugeStructure.directionForLocal(input ? -1 : 1, 0, rotation);
    }

    private boolean portSide(Direction side) {
        return side == outward() || side == outward().getOpposite();
    }

    @Override
    public boolean isConnectable(Direction side) {
        return portSide(side);
    }

    @Override
    public boolean canInputFrom(Direction side) {
        LargeCentrifugeControllerBlockEntity c = controller();
        return input && portSide(side) && c != null && c.canInputEssentia();
    }

    @Override
    public boolean canOutputTo(Direction side) {
        LargeCentrifugeControllerBlockEntity c = controller();
        return !input && portSide(side) && c != null && c.canOutputEssentia();
    }

    @Override
    public void setSuction(Aspect aspect, int amount) {
    }

    @Override
    public Aspect getSuctionType(Direction side) {
        LargeCentrifugeControllerBlockEntity c = controller();
        if (c == null || !portSide(side)) {
            return null;
        }
        if (!input) {
            return c.outputAspect();
        }
        // 输入口为空时不绑定类型，由管道/过滤器决定送什么；
        // 已有内容时只吸附同种源质。
        return c.inputAspect();
    }

    @Override
    public int getSuctionAmount(Direction side) {
        LargeCentrifugeControllerBlockEntity c = controller();
        if (c == null || !portSide(side)) {
            return 0;
        }
        return input ? (c.canInputEssentia() ? 64 : 0) : 64;
    }

    @Override
    public int takeEssentia(Aspect aspect, int amount, Direction side) {
        LargeCentrifugeControllerBlockEntity c = controller();
        if (input || c == null || !portSide(side) || !c.canOutputEssentia()) {
            return 0;
        }
        return c.takeEssentia(aspect, amount);
    }

    @Override
    public int addEssentia(Aspect aspect, int amount, Direction side) {
        LargeCentrifugeControllerBlockEntity c = controller();
        if (!input || c == null || !portSide(side) || !c.canInputEssentia()) {
            return 0;
        }
        return c.addEssentia(aspect, amount);
    }

    @Override
    public Aspect getEssentiaType(Direction side) {
        LargeCentrifugeControllerBlockEntity c = controller();
        return !input && c != null && portSide(side) ? c.outputAspect() : null;
    }

    @Override
    public int getEssentiaAmount(Direction side) {
        LargeCentrifugeControllerBlockEntity c = controller();
        return !input && c != null && portSide(side) ? c.storedOutput() : 0;
    }

    @Override
    public int getMinimumSuction() {
        return 0;
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
