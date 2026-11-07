package com.mysterydynamics.block;

import com.mysterydynamics.compat.LargeCentrifugeStructure;
import com.mysterydynamics.damage.ModDamageTypes;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.kinetics.drill.DrillBlock;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.EssentiaCapabilities;
import thaumcraft.api.aspects.IEssentiaTransport;

import java.util.List;

/**
 * 大型奥法离心机控制器。
 *
 * <p>顶部输入口接入 Create 动力网络；低于 32 RPM 不旋转、不处理。
 * 内部最多存储 24 点源质，存满或收到红石信号后开始离心，输出口把处理后的
 * 源质交给管道，输出清空前不接受新源质。</p>
 */
public class LargeCentrifugeControllerBlockEntity extends BlockEntity implements IHaveGoggleInformation {

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
    private Aspect decision;
    private int transferCooldown;
    private boolean dirty;

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
        if (--transferCooldown <= 0) {
            transferCooldown = 2;
            pullFromInputPipe();
        }
        float speed = inputSpeed();
        if (!processingActive && speed >= MIN_RPM && input.totalAmount() > 0) {
            boolean redstone = redstonePowered();
            if (input.totalAmount() >= CAPACITY || redstone) {
                startProcessing();
            }
        }
        if (processingActive) {
            process(speed);
        }
        // 受伤无敌帧为 10 tick，每 2 tick 扫描一次足够，且减半实体查询开销。
        if ((level.getGameTime() & 1L) == 0L) {
            applySpinDamage(speed);
        }
        if (dirty && level.getGameTime() % 10L == 0L) {
            sync();
        }
    }

    /**
     * 与动力锯/动力钻头一致：服务端接触判定、跳过物品、按转速取伤害。
     * 旋转部件没有实体碰撞方块，所以在控制器 tick 里扫描旋转组 AABB。
     */
    private void applySpinDamage(float speed) {
        if (level == null || speed < MIN_RPM) {
            return;
        }
        float clamped = Math.min(speed, MAX_RPM);
        float t = (clamped - MIN_RPM) / (float) (MAX_RPM - MIN_RPM);
        // 动力锯/钻头公式最高 10 点，这里 ×4 得到最高 40 点。
        float damage = 4.0F * (float) DrillBlock.getDamage(Math.abs(speed));
        int knockbackLevel = 1 + Math.round(9.0F * t);

        AABB area = new AABB(
                worldPosition.getX() - 0.625D, worldPosition.getY() - 0.25D,
                worldPosition.getZ() - 0.625D,
                worldPosition.getX() + 1.625D, worldPosition.getY() + 1.75D,
                worldPosition.getZ() + 1.625D);
        // 以旋转区/中央多方块的几何中心作为击退原点。
        Vec3 center = area.getCenter();

        // getEntitiesOfClass 已包含玩家；旁观者没有碰撞，直接跳过。
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, area,
                entity -> entity.isAlive() && !entity.isSpectator());
        if (targets.isEmpty()) {
            return;
        }
        var source = ModDamageTypes.centrifuge(level);

        for (LivingEntity entity : targets) {
            if (!entity.hurt(source, damage)) {
                continue;
            }
            Vec3 offset = entity.position().subtract(center);
            double horizontal = Math.sqrt(offset.x * offset.x + offset.z * offset.z);
            double dx;
            double dz;
            if (horizontal < 1.0E-4D) {
                dx = level.random.nextDouble() - 0.5D;
                dz = level.random.nextDouble() - 0.5D;
                double len = Math.max(1.0E-4D, Math.sqrt(dx * dx + dz * dz));
                dx /= len;
                dz /= len;
            } else {
                dx = offset.x / horizontal;
                dz = offset.z / horizontal;
            }
            // 明确向外击退；直接覆盖水平速度，避免玩家原本的移动方向把击退带偏。
            double resistance = entity.getAttributeValue(
                    net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE);
            float factor = (float) Math.max(0.0D, 1.0D - resistance);
            double strength = (0.5D * knockbackLevel + 0.2D) * factor;
            Vec3 motion = entity.getDeltaMovement();
            entity.setDeltaMovement(
                    dx * strength,
                    Math.max(motion.y, 0.35D + 0.02D * knockbackLevel),
                    dz * strength);
            entity.hasImpulse = true;
            entity.hurtMarked = true;
        }
    }

    /** 与炼狱锅炉一致：主动从端口相邻管道抽取源质，避免只依赖管道推送。 */
    private void pullFromInputPipe() {
        if (level == null || !canInputEssentia()) {
            return;
        }
        int rotation = rotation();
        BlockPos port = LargeCentrifugeStructure.world(worldPosition, -1, -1, 0, rotation);
        // 只从端口朝外的一面抽取，与端口 isConnectable 的朝向保持一致，
        // 避免把端口上方/下方无关的源质罐也抽空。
        Direction dir = LargeCentrifugeStructure.directionForLocal(-1, 0, rotation);
        BlockPos pipePos = port.relative(dir);
        IEssentiaTransport pipe = EssentiaCapabilities.find(level, pipePos, dir.getOpposite());
        if (pipe == null) {
            return;
        }
        Aspect aspect = pipe.getEssentiaType(dir.getOpposite());
        if (aspect == null) {
            aspect = pipe.getSuctionType(dir.getOpposite());
        }
        if (aspect == null || !canAcceptAspect(aspect)) {
            return;
        }
        int taken = pipe.takeEssentia(aspect, 1, dir.getOpposite());
        if (taken == 0) {
            taken = pipe.takeEssentia(aspect, 1, dir);
        }
        if (taken > 0) {
            addEssentia(aspect, taken);
        }
    }

    private void startProcessing() {
        if (input.totalAmount() <= 0) {
            return;
        }
        decision = null;
        for (Aspect aspect : input.aspects()) {
            if (input.amount(aspect) > 0) {
                List<Aspect> components = aspect.components();
                if (!components.isEmpty()) {
                    decision = components.get(level.random.nextInt(components.size()));
                }
                break;
            }
        }
        processing = input.copy();
        input = new AspectList();
        processingActive = true;
        progress = 0.0F;
        sync();
    }

    private void process(float speed) {
        if (speed < MIN_RPM) {
            return;
        }
        if (processing.totalAmount() <= 0) {
            processing = new AspectList();
            processingActive = false;
            sync();
            return;
        }
        if (processing.isEmpty()) {
            processingActive = false;
            sync();
            return;
        }
        if (speed >= MAX_RPM) {
            // 最多处理容量个单位；即使旧数据里有 0 量条目也不会卡死服务端。
            for (int i = 0; i < CAPACITY && !processing.isEmpty(); i++) {
                processOne();
            }
            processingActive = false;
            sync();
            return;
        }
        progress += speed / (float) MIN_RPM;
        while (progress >= PROCESS_TIME && !processing.isEmpty()) {
            progress -= PROCESS_TIME;
            processOne();
        }
        if (processing.totalAmount() <= 0) {
            processing = new AspectList();
            processingActive = false;
            // 批次结束立即同步，客户端状态不再停留在“离心中”。
            sync();
            return;
        }
        setChanged();
        dirty = true;
    }

    private void processOne() {
        for (Aspect aspect : processing.aspects()) {
            if (processing.amount(aspect) <= 0) {
                continue;
            }
            // remove(...,1) 会在数量归零时移除键；reduce 会留下 amount=0 的条目，
            // 而 AspectList.isEmpty() 仍会认为它非空，旧实现会在此死循环。
            processing.remove(aspect, 1);
            if (decision != null) {
                output.add(decision, 1);
            }
            dirty = true;
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

    /** 强启只认模型名为“强启控制器”的那个底层方块（局部 x=1,z=-1）。 */
    private boolean redstonePowered() {
        if (level == null) {
            return false;
        }
        BlockPos control = LargeCentrifugeStructure.world(worldPosition, 1, -1, -1, rotation());
        return level.hasNeighborSignal(control);
    }

    // ---- essentia storage / transport ----

    public boolean canInputEssentia() {
        return !processingActive && output.isEmpty() && input.totalAmount() < CAPACITY;
    }

    /** 一次只能输入一种类型；已有类型后只能补同种源质。 */
    public boolean canAcceptAspect(Aspect aspect) {
        if (aspect == null || aspect.components().isEmpty()) {
            // 原初源质无法再分解；接收后只会被吞掉，直接拒收。
            return false;
        }
        return input.totalAmount() == 0 || input.amount(aspect) > 0;
    }

    public int addEssentia(Aspect aspect, int amount) {
        if (!canInputEssentia() || !canAcceptAspect(aspect) || amount <= 0) {
            return 0;
        }
        int accepted = Math.min(amount, CAPACITY - input.totalAmount());
        if (accepted > 0) {
            input.add(aspect, accepted);
            setChanged();
            dirty = true;
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
            dirty = true;
        }
        return taken;
    }

    public boolean isProcessing() {
        return processingActive;
    }

    public int storedInput() {
        return input.totalAmount();
    }

    public float speed() {
        return inputSpeed();
    }

    public Aspect inputAspect() {
        for (Aspect aspect : input.aspects()) {
            if (input.amount(aspect) > 0) {
                return aspect;
            }
        }
        return null;
    }

    public int storedOutput() {
        return output.totalAmount();
    }

    /** 当前单位的处理进度（0~1），用于护目镜显示。 */
    public float progressFraction() {
        if (!processingActive || processing.totalAmount() <= 0) {
            return 0.0F;
        }
        return Math.min(1.0F, progress / (float) PROCESS_TIME);
    }

    /** 当前状态的一行说明；客户端标签与护目镜共用。 */
    public Component statusComponent() {
        float speed = inputSpeed();
        if (speed < MIN_RPM) {
            return Component.translatable("mystery_dynamics.large_centrifuge.status.slow", MIN_RPM)
                    .withStyle(ChatFormatting.RED);
        }
        if (processingActive) {
            return Component.translatable("mystery_dynamics.large_centrifuge.status.processing",
                    processing.totalAmount()).withStyle(ChatFormatting.AQUA);
        }
        if (!output.isEmpty()) {
            return Component.translatable("mystery_dynamics.large_centrifuge.status.output",
                    output.totalAmount()).withStyle(ChatFormatting.GREEN);
        }
        if (input.totalAmount() > 0) {
            return Component.translatable("mystery_dynamics.large_centrifuge.status.filling",
                    CAPACITY - input.totalAmount()).withStyle(ChatFormatting.GRAY);
        }
        return Component.translatable("mystery_dynamics.large_centrifuge.status.idle")
                .withStyle(ChatFormatting.GRAY);
    }

    private static Component indent(Component line) {
        return Component.literal("    ").append(line);
    }

    @Override
    public boolean addToGoggleTooltip(java.util.List<Component> tooltip, boolean isPlayerSneaking) {
        tooltip.add(indent(Component.translatable("block.mystery_dynamics.large_centrifuge_controller")
                .withStyle(ChatFormatting.GOLD)));
        Aspect in = inputAspect();
        Component inputName = in == null
                ? Component.translatable("mystery_dynamics.large_centrifuge.empty")
                        .withStyle(ChatFormatting.DARK_GRAY)
                : Component.translatable("tc.aspect." + in.getTag())
                        .withStyle(style -> style.withColor(in.getColor() & 0xFFFFFF));
        tooltip.add(indent(Component.translatable("mystery_dynamics.large_centrifuge.stored",
                inputName, input.totalAmount(), CAPACITY).withStyle(ChatFormatting.GRAY)));
        tooltip.add(indent(statusComponent()));
        if (processingActive) {
            tooltip.add(indent(Component.translatable("mystery_dynamics.large_centrifuge.progress",
                    Math.round(progressFraction() * 100.0F)).withStyle(ChatFormatting.DARK_AQUA)));
        }
        for (Aspect aspect : output.aspects()) {
            int amount = output.amount(aspect);
            if (amount > 0) {
                tooltip.add(Component.literal("      ")
                        .append(Component.translatable("tc.aspect." + aspect.getTag())
                                .withStyle(style -> style.withColor(aspect.getColor() & 0xFFFFFF)))
                        .append(Component.literal(" × " + amount))
                        .withStyle(ChatFormatting.AQUA));
            }
        }
        tooltip.add(indent(Component.translatable("mystery_dynamics.large_centrifuge.speed",
                Math.round(inputSpeed()), MIN_RPM, MAX_RPM).withStyle(ChatFormatting.DARK_GRAY)));
        if (isPlayerSneaking) {
            tooltip.add(indent(Component.translatable("mystery_dynamics.large_centrifuge.hint.ports")
                    .withStyle(ChatFormatting.DARK_GRAY)));
            tooltip.add(indent(Component.translatable("mystery_dynamics.large_centrifuge.hint.primal")
                    .withStyle(ChatFormatting.DARK_GRAY)));
            tooltip.add(indent(Component.translatable("mystery_dynamics.large_centrifuge.hint.redstone")
                    .withStyle(ChatFormatting.DARK_GRAY)));
        } else {
            tooltip.add(indent(Component.translatable("mystery_dynamics.goggles.sneak_for_details")
                    .withStyle(ChatFormatting.DARK_GRAY)));
        }
        return true;
    }

    private void sync() {
        setChanged();
        dirty = false;
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag, registries);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Input", input.save());
        tag.put("Processing", processing.save());
        tag.put("Output", output.save());
        tag.putBoolean("ProcessingActive", processingActive);
        tag.putFloat("Progress", progress);
        tag.putString("Decision", decision == null ? "" : decision.getTag());
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
        decision = tag.getString("Decision").isEmpty() ? null : Aspect.get(tag.getString("Decision"));
    }
}
