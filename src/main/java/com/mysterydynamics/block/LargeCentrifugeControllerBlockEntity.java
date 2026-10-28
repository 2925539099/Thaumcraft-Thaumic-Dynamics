package com.mysterydynamics.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;

import java.util.List;

/**
 * 大型奥法离心机控制器。
 *
 * <p>顶部输入口接入 Create 动力网络；低于 32 RPM 不旋转、不处理。
 * 内部最多存储 24 点源质，存满或收到红石信号后开始离心，输出口把处理后的
 * 源质交给管道，输出清空前不接受新源质。</p>
 */
public class LargeCentrifugeControllerBlockEntity extends BlockEntity {

    public static final int CAPACITY = 24;
    public static final int MIN_RPM = 32;
    public static final int MAX_RPM = 256;
    public static final int PROCESS_TIME = 39;

    private float spin;
    private float spinRate;

    private AspectList input = new AspectList();
    private AspectList processing = new AspectList();
    private AspectList output = new AspectList();
    private boolean processingActive;
    private float progress;

    public LargeCentrifugeControllerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public void setFormation(int rotation) {
        this.spin = 0.0F;
        this.spinRate = 0.0F;
        this.progress = 0.0F;
        this.processingActive = false;
        setChanged();
    }

    public int rotation() {
        BlockState state = getBlockState();
        return state.hasProperty(LargeCentrifugeControllerBlock.ROTATION)
                ? state.getValue(LargeCentrifugeControllerBlock.ROTATION) : 0;
    }

    public float spin(float partialTick) {
        return spin + spinRate * partialTick;
    }

    public void tick() {
        if (level == null) {
            return;
        }
        if (level.isClientSide) {
            float speed = inputSpeed();
            spinRate = speed >= MIN_RPM ? speed * 0.3F : 0.0F;
            spin = (spin + spinRate) % 360.0F;
            return;
        }
        serverTick();
    }

    private void serverTick() {
        float speed = inputSpeed();
        if (!processingActive && speed >= MIN_RPM && input.totalAmount() > 0) {
            boolean redstone = level.hasNeighborSignal(worldPosition);
            if (input.totalAmount() >= CAPACITY || redstone) {
                startProcessing();
            }
        }
        if (processingActive) {
            process(speed);
        }
    }

    private void startProcessing() {
        if (input.isEmpty()) {
            return;
        }
        processing = input.copy();
        input = new AspectList();
        processingActive = true;
        progress = 0.0F;
        setChanged();
    }

    private void process(float speed) {
        if (speed < MIN_RPM) {
            return;
        }
        if (processing.isEmpty()) {
            processingActive = false;
            setChanged();
            return;
        }
        if (speed >= MAX_RPM) {
            while (!processing.isEmpty()) {
                processOne();
            }
            processingActive = false;
            setChanged();
            return;
        }
        progress += speed / (float) MIN_RPM;
        while (progress >= PROCESS_TIME && !processing.isEmpty()) {
            progress -= PROCESS_TIME;
            processOne();
        }
        if (processing.isEmpty()) {
            processingActive = false;
        }
        setChanged();
    }

    private void processOne() {
        for (Aspect aspect : processing.aspects()) {
            if (processing.amount(aspect) <= 0) {
                continue;
            }
            processing.reduce(aspect, 1);
            List<Aspect> components = aspect.components();
            if (!components.isEmpty()) {
                Aspect component = components.get(level.random.nextInt(components.size()));
                output.add(component, 1);
            }
            return;
        }
    }

    private float inputSpeed() {
        if (level == null) {
            return 0.0F;
        }
        BlockEntity above = level.getBlockEntity(worldPosition.above());
        return above instanceof LargeCentrifugeInputBlockEntity input
                ? Math.abs(input.getSpeed()) : 0.0F;
    }

    // ---- essentia storage / transport ----

    public boolean canInputEssentia() {
        return !processingActive && output.isEmpty() && input.totalAmount() < CAPACITY;
    }

    public int addEssentia(Aspect aspect, int amount) {
        if (!canInputEssentia() || aspect == null || amount <= 0) {
            return 0;
        }
        int accepted = Math.min(amount, CAPACITY - input.totalAmount());
        if (accepted > 0) {
            input.add(aspect, accepted);
            setChanged();
        }
        return accepted;
    }

    public boolean canOutputEssentia() {
        return !output.isEmpty();
    }

    public Aspect outputAspect() {
        for (Aspect aspect : output.aspects()) {
            if (output.amount(aspect) > 0) {
                return aspect;
            }
        }
        return null;
    }

    public int outputAmount(Aspect aspect) {
        return aspect == null ? 0 : output.amount(aspect);
    }

    public int takeEssentia(Aspect aspect, int amount) {
        if (aspect == null || amount <= 0 || !canOutputEssentia()) {
            return 0;
        }
        int taken = Math.min(amount, output.amount(aspect));
        if (taken > 0) {
            output.remove(aspect, taken);
            setChanged();
        }
        return taken;
    }

    public boolean isProcessing() {
        return processingActive;
    }

    public int storedInput() {
        return input.totalAmount();
    }

    public int storedOutput() {
        return output.totalAmount();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Input", input.save());
        tag.put("Processing", processing.save());
        tag.put("Output", output.save());
        tag.putBoolean("ProcessingActive", processingActive);
        tag.putFloat("Progress", progress);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        input = tag.contains("Input") ? AspectList.load(tag.getCompound("Input")) : new AspectList();
        processing = tag.contains("Processing")
                ? AspectList.load(tag.getCompound("Processing")) : new AspectList();
        output = tag.contains("Output") ? AspectList.load(tag.getCompound("Output")) : new AspectList();
        processingActive = tag.getBoolean("ProcessingActive");
        progress = tag.getFloat("Progress");
    }
}
