package com.singularity_iteration.mio_icif.Blocks.entity.transformer;

import com.singularity_iteration.mio_icif.Menu.Storage.TransformerMenu;
import com.singularity_iteration.mio_icif.energy.EnergyUnit.CableTier;
import com.singularity_iteration.mio_icif.energy.EnergyUnit.IEUEnergyStorage;
import com.singularity_iteration.mio_icif.energy.grid.*;
import com.singularity_iteration.mio_icif.api.MioIcifAPI;
import com.singularity_iteration.mio_icif.api.machine.ITransformerBlock;
import com.singularity_iteration.mio_icif.api.tool.IWrenchable;
import com.singularity_iteration.mio_icif.api.machine.IMachineAPI;
import com.singularity_iteration.mio_icif.api.energy.ICableTier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.Nullable;

/**
 * 变压器方块实体：IC2 风格 ExtraNode 跨电网架构
 * 变压器只改变电压等级，不限制传输速率
 * 原理：
 * - 正面（Front）= 高压侧（单孔），其他 5 面 = 低压侧（五孔）
 * - 三种模式：STEP_DOWN（降压）/ STEP_UP（聚合升压）/ REDSTONE_CONTROL（红石控制）
 * - 降压模式：单孔（高压侧）输入 → 五孔（低压侧）输出，每tick发4个包
 * - 聚合升压模式：五孔（低压侧）输入 → 单孔（高压侧）输出，不限流，吃多少输出多少
 * - 通过 ExtraNode 机制，Source node（输出侧）和 Sink node（输入侧）
 *   分别位于两个完全独立的电网中，消除回环递归风险
 */
@SuppressWarnings("null")
public abstract class mio_icif_transformer extends BlockEntity implements
        IEnergySource, IEnergySink, MenuProvider, ITransformerBlock, IWrenchable {

    @SuppressWarnings("null")
public enum TransformerMode {
        STEP_UP,
        STEP_DOWN,
        REDSTONE_CONTROL
    }

    protected final ICableTier lowTier;
    protected final ICableTier highTier;

    protected TransformerMode mode = TransformerMode.STEP_DOWN;
    protected boolean hasRedstoneSignal = false;

    private double internalBuffer = 0;
    private final double bufferCapacity;
    private static final int STEP_DOWN_PACKETS = 4;

    private boolean registered = false;

    protected mio_icif_transformer(
            BlockEntityType<?> type,
            BlockPos pos,
            BlockState state,
            ICableTier lowTier,
            ICableTier highTier,
            TransformerMode initialMode
    ) {
        super(type, pos, state);
        this.lowTier = lowTier;
        this.highTier = highTier;
        this.mode = initialMode;
        this.bufferCapacity = lowTier.getPowerRating() * 8.0D;
    }

    // ==================== 电网注册/注销 ====================

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide && !registered) {
            NeoForge.EVENT_BUS.post(new EnergyTileLoadEvent(this, level));
            registered = true;
        }
    }

    @Override
    public void setRemoved() {
        if (level != null && !level.isClientSide && registered) {
            NeoForge.EVENT_BUS.post(new EnergyTileUnloadEvent(this, level));
            registered = false;
        }
        super.setRemoved();
    }

    @Override
    public void clearRemoved() {
        super.clearRemoved();
        if (level != null && !level.isClientSide && !registered) {
            NeoForge.EVENT_BUS.post(new EnergyTileLoadEvent(this, level));
            registered = true;
        }
    }

    // ==================== Tick 与能量中转 ====================

    public static void tick(Level level, BlockPos pos, BlockState state, mio_icif_transformer blockEntity) {
        if (level.isClientSide()) return;

        blockEntity.updateRedstoneSignal();

        // 更新方块激活状态（升压模式时active=true）
    boolean shouldBeActive = blockEntity.getEffectiveMode() == TransformerMode.STEP_UP;
        boolean currentActive = state.getValue(
                com.singularity_iteration.mio_icif.Blocks.Transformer.mio_icif_block_transformer.ACTIVE);
        if (shouldBeActive != currentActive) {
            level.setBlock(pos, state.setValue(
                    com.singularity_iteration.mio_icif.Blocks.Transformer.mio_icif_block_transformer.ACTIVE,
                    shouldBeActive), 3);
        }

        // 能量中转：作为Source 的输出量在上一轮EnergyCalculator 分配后被 drawEnergy() 消耗，
        // 而作为Sink 的注入量在新一轮分配中由injectEnergy() 填充
    // 无需额外操作，EnergyCalculator 通过 Grid 自动完成分配
}

    // ==================== IEnergySource（输出侧）====================

    /**
     * 输出侧可提供的能量
 * 与IC2原版行为一致（fullEnergy=true）：
     * 只有当buffer 中的能量 >= 输出侧一个完整packet 时才输出
 * 避免输出零碎的小能量包
 *
     * 降压模式：每tick发4个包，每个包大小 = 输出侧tier 功率
     * 聚合升压模式：每tick发1个包，buffer里有多少输出多少
     */
    @Override
    public double getOfferedEnergy() {
        if (internalBuffer <= 0) return 0.0D;
        ICableTier outputTier = getOutputTierInternal();
        if (getEffectiveMode() == TransformerMode.STEP_UP) {
            if (internalBuffer < outputTier.getPowerRating()) return 0.0D;
            return internalBuffer;
        }
        if (internalBuffer < outputTier.getPowerRating()) return 0.0D;
        double maxOutput = STEP_DOWN_PACKETS * outputTier.getPowerRating();
        return Math.min(internalBuffer, maxOutput);
    }

    /**
     * 电网从本变压器Source node 抽取能量时调用
 */
    @Override
    public void drawEnergy(double amount) {
        internalBuffer = Math.max(0, internalBuffer - amount);
    }

    /**
     * Source 的电压等级 = 输出侧额定电压
 */
    @Override
    public int getSourceTier() {
        return MioIcifAPI.instance().getEnergyNetAPI().cableTierToSourceTier(getOutputTierInternal());
    }

    /**
     * 每tick发送的packet数量
     * 与IC2原版一致：
     *   升压模式 setPacketOutput(1)：每tick发1个包
     *   降压模式 setPacketOutput(4)：每tick发4个包
     * 实际packet数 = min(设定值, floor(buffer / outputTierPower))
     */
    @Override
    public int getPacketCount() {
        ICableTier outputTier = getOutputTierInternal();
        int maxPackets = getEffectiveMode() == TransformerMode.STEP_UP ? 1 : STEP_DOWN_PACKETS;
        return Math.min(maxPackets, (int) Math.floor(internalBuffer / outputTier.getPowerRating()));
    }

    // ==================== IEnergySink（输入侧）====================

    /**
     * 输入侧需求的能量 = 内部 buffer 的剩余空间
 * 与IC2原版行为一致：只有当输出侧存在可达的Sink 时才接受能量
 * 避免在输出侧无连接时持续消耗输入侧电源的能量
 *
     * buffer容量 = lowTier.powerRating * 8（与IC2原版一致）
     */
    @Override
    public double getDemandedEnergy() {
        if (!hasOutputPath()) return 0.0D;
        return Math.max(0, bufferCapacity - internalBuffer);
    }

    /**
     * Sink 的电压等级 = 输入侧额定电压
 */
    @Override
    public int getSinkTier() {
        return MioIcifAPI.instance().getEnergyNetAPI().cableTierToSourceTier(getInputTierInternal());
    }

    @Override
    public double injectEnergy(Direction direction, double amount, double voltage) {
        if (amount <= 0) return amount;
        double space = bufferCapacity - internalBuffer;
        if (space <= 0) return amount;
        double accepted = Math.min(amount, space);
        internalBuffer += accepted;
        return amount - accepted;
    }

    // ==================== IEnergyEmitter / IEnergyAcceptor（面感知）====================

    /**
     * Source node 只从输出侧方向发射能量
 */
    @Override
    public boolean emitsEnergyTo(IEnergyAcceptor acceptor, Direction direction) {
        return canProvidePowerFromSide(direction);
    }

    /**
     * Sink node 只从输入侧方向接受能量
 */
    @Override
    public boolean acceptsEnergyFrom(IEnergyEmitter emitter, Direction direction) {
        return canConsumePowerFromSide(direction);
    }

    // ==================== 方向感知旧接口（保持兼容性） ====================

    /**
     * 检查输入侧是否有可用能量（电网或相邻源）
 */
    private boolean hasInputSource() {
        if (level == null) return false;
        TransformerMode effMode = getEffectiveMode();

        if (effMode == TransformerMode.STEP_DOWN) {
            // 输入 = 高压侧
        Direction inputSide = getHighSide();
            return hasEnergyOnSide(inputSide);
        } else {
            // 输入 = 任一低压侧
        for (Direction side : Direction.values()) {
                if (side == getHighSide()) continue;
                if (hasEnergyOnSide(side)) return true;
            }
            return false;
        }
    }

    private boolean hasEnergyOnSide(Direction side) {
        if (level == null) return false;
        // 检查新电网系统
        IEnergyTile tile = EnergyNetGlobal.getTile(level, worldPosition.relative(side));
        if (tile instanceof IEnergySource) return true;
        // 检查旧系统（兼容期）
    BlockEntity adj = level.getBlockEntity(worldPosition.relative(side));
        return adj instanceof IEnergySource;
    }

    /**
     * 本变压器能否从指定侧提供能量（Source 输出）
 */
    public boolean canProvidePowerFromSide(Direction side) {
        TransformerMode effMode = getEffectiveMode();
        return effMode == TransformerMode.STEP_DOWN ? !isHighSide(side) : isHighSide(side);
    }

    /**
     * 检查Source 节点（输出侧）是否有可达的Sink
 * 如果输出侧没有连接任何用电器，则不应从输入侧吸收能量
 * 否则输入侧电源会持续掉电而能量无处可去
 */
    private boolean hasOutputPath() {
        if (level == null || level.isClientSide) return false;
        try {
            return EnergyNetGlobal.sourceHasReachableSinks(this);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 本变压器能否从指定侧接收能量（Sink 输入）
 */
    public boolean canConsumePowerFromSide(Direction side) {
        TransformerMode effMode = getEffectiveMode();
        return effMode == TransformerMode.STEP_DOWN ? isHighSide(side) : !isHighSide(side);
    }

    /**
     * 实现 PowerGrid IPowerSource 接口（兼容）
     */
    @Override
    public long getPowerOutput() {
        return hasInputSource() ? getOutputTierInternal().getPowerRating() : 0;
    }

    /**
     * 实现 PowerGrid IPowerConsumer 接口（兼容）
     */
    public long getPowerRating() { return Long.MAX_VALUE; }

    // ==================== 过压爆炸 ====================

    public void triggerOverloadExplosion() {
        if (level == null || level.isClientSide) return;
        if (!com.singularity_iteration.mio_icif.energy.CustomEUEnergyStorage.shouldTileExplode(this)) return;
        float explosionPower = com.singularity_iteration.mio_icif.energy.CustomEUEnergyStorage.resolveTileExplosionPower(this, getOutputTierInternal().getTier(), 0.5F);
        if (explosionPower > 0.0F) {
            level.explode(null,
                    worldPosition.getX() + 0.5,
                    worldPosition.getY() + 0.5,
                    worldPosition.getZ() + 0.5,
                    explosionPower, Level.ExplosionInteraction.BLOCK);
        }
        level.removeBlock(worldPosition, false);
    }

    // ==================== 方向/模式判定 ====================

    protected Direction getHighSide() {
        if (getBlockState().hasProperty(com.singularity_iteration.mio_icif.Blocks.Transformer.mio_icif_block_transformer.FACING)) {
            return getBlockState().getValue(com.singularity_iteration.mio_icif.Blocks.Transformer.mio_icif_block_transformer.FACING);
        }
        return Direction.NORTH;
    }

    protected boolean isHighSide(Direction side) {
        return side == getHighSide();
    }

    protected TransformerMode getEffectiveMode() {
        if (mode == TransformerMode.REDSTONE_CONTROL) {
            return hasRedstoneSignal ? TransformerMode.STEP_UP : TransformerMode.STEP_DOWN;
        }
        return mode;
    }

    private ICableTier getInputTierInternal() {
        return getEffectiveMode() == TransformerMode.STEP_DOWN ? highTier : lowTier;
    }

    private ICableTier getOutputTierInternal() {
        return getEffectiveMode() == TransformerMode.STEP_DOWN ? lowTier : highTier;
    }

    /** 获取输入侧等级（API ICableTier 类型）*/
    @Override
    public ICableTier getInputTier() {
        return getInputTierInternal();
    }

    /** 获取输出侧等级（API ICableTier 类型）*/
    @Override
    public ICableTier getOutputTier() {
        return getOutputTierInternal();
    }

    // ==================== IEUEnergyStorage Capability（无存储）====================

    @Nullable
    public IEUEnergyStorage getEnergyStorage(@Nullable Direction direction) {
        return noopStorage;
    }

    private final IEUEnergyStorage noopStorage = new IEUEnergyStorage() {
        @Override public boolean canConnect(CableTier cableTier) { return true; }
        @Override public long receive(long maxReceive, boolean simulate) { return 0; }
        @Override public long extract(long maxExtract, boolean simulate) { return 0; }
        @Override public long getAmount() { return 0; }
        @Override public long getCapacity() { return 0; }
        @Override public boolean canExtract() { return false; }
        @Override public boolean canReceive() { return false; }
    };

    // ==================== 模式控制 ====================

    protected void updateRedstoneSignal() {
        if (level == null) return;
        boolean old = hasRedstoneSignal;
        hasRedstoneSignal = level.hasNeighborSignal(worldPosition);
        if (old != hasRedstoneSignal) {
            internalBuffer = 0;
            if (mode == TransformerMode.REDSTONE_CONTROL) {
                refreshRegistration();
            }
            setChanged();
        }
    }

    public void setMode(TransformerMode mode) {
        if (this.mode != mode) {
            this.mode = mode;
            internalBuffer = 0;
            refreshRegistration();
            setChanged();
        }
    }

    /**
     * 重新注册到电网（模式切换时刷新连接）
 * 先注销再重新注册，使电网重新读取acceptsEnergyFrom/emitsEnergyTo 并更新路径存
 * 与IC2原版行为一致：模式变化时先unload再load
     */
    private void refreshRegistration() {
        if (level != null && !level.isClientSide && registered) {
            NeoForge.EVENT_BUS.post(new EnergyTileUnloadEvent(this, level));
            NeoForge.EVENT_BUS.post(new EnergyTileLoadEvent(this, level));
        }
    }

    public TransformerMode getMode() { return mode; }

    public void cycleMode() {
        TransformerMode[] values = TransformerMode.values();
        int next = (mode.ordinal() + 1) % values.length;
        setMode(values[next]);
    }

    public long getInternalEnergy() { return (long) internalBuffer; }
    public long getBufferCapacity() {
        return (long) bufferCapacity;
    }
    public ICableTier getLowTier() { return lowTier; }
    public ICableTier getHighTier() { return highTier; }
    public long getLowSideLimit() { return lowTier.getPowerRating(); }
    public long getHighSideLimit() { return highTier.getPowerRating(); }
    public boolean hasRedstoneSignal() { return hasRedstoneSignal; }

    // ==================== NBT ====================

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("mode", mode.ordinal());
        tag.putBoolean("redstone_signal", hasRedstoneSignal);
        tag.putDouble("buffer", internalBuffer);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("mode")) {
            int modeIndex = tag.getInt("mode");
            if (modeIndex >= 0 && modeIndex < TransformerMode.values().length) {
                mode = TransformerMode.values()[modeIndex];
            }
        }
        if (tag.contains("redstone_signal")) {
            hasRedstoneSignal = tag.getBoolean("redstone_signal");
        }
        if (tag.contains("buffer")) {
            internalBuffer = tag.getDouble("buffer");
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.putInt("mode", mode.ordinal());
        return tag;
    }

    // ==================== MenuProvider ====================

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.mio_icif.transformer");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new TransformerMenu(containerId, playerInventory, this);
    }

    // ==================== ITransformerBlock API ====================

    @Override
    public ITransformerBlock.TransformerMode getTransformerMode() {
        TransformerMode eff = getEffectiveMode();
        return switch (eff) {
            case STEP_UP -> ITransformerBlock.TransformerMode.STEP_UP;
            case STEP_DOWN, REDSTONE_CONTROL -> ITransformerBlock.TransformerMode.STEP_DOWN;
        };
    }

    @Override
    public void setTransformerMode(ITransformerBlock.TransformerMode apiMode) {
        TransformerMode internal = switch (apiMode) {
            case STEP_UP -> TransformerMode.STEP_UP;
            case STEP_DOWN -> TransformerMode.STEP_DOWN;
        };
        setMode(internal);
    }

    @Override
    public long getInternalBuffer() {
        return (long) internalBuffer;
    }

    @Override
    public boolean isConnected(Direction side) {
        if (level == null) return false;
        TransformerMode effMode = getEffectiveMode();
        boolean isOutputSide = effMode == TransformerMode.STEP_DOWN ? !isHighSide(side) : isHighSide(side);
        if (isOutputSide) {
            return hasEnergyOnSide(side);
        } else {
            return effMode == TransformerMode.STEP_DOWN ? isHighSide(side) : !isHighSide(side);
        }
    }

    // ==================== IEnergyBlock 默认实现 ====================

    @Override
    public com.singularity_iteration.mio_icif.api.energy.IEnergyStorageAccess getEnergyStorage() {
        return noopApiStorage;
    }

    @Override
    public boolean isPowerSource() {
        return getEffectiveMode() == TransformerMode.STEP_UP;
    }

    @Override
    public ICableTier getEffectiveCableTier() {
        return getOutputTier();
    }

    @Override
    public long getEffectiveCapacity() {
        return (long) bufferCapacity;
    }

    @Override
    public long getEffectiveMaxReceive() {
        return getInputTier().getPowerRating();
    }

    @Override
    public long useEnergy(long amount, boolean simulate) {
        return 0;
    }

    @Override
    public long generateEnergy(long amount, boolean simulate) {
        return 0;
    }

    @Override
    public boolean canConnect(@Nullable Direction side) {
        if (side == null) return true;
        TransformerMode effMode = getEffectiveMode();
        return effMode == TransformerMode.STEP_DOWN ? !isHighSide(side) : isHighSide(side);
    }

    @Override
    public IMachineAPI.MachineType getMachineType() {
        return IMachineAPI.MachineType.TRANSFORMER;
    }

    private final com.singularity_iteration.mio_icif.api.energy.IEnergyStorageAccess noopApiStorage =
        new com.singularity_iteration.mio_icif.api.energy.IEnergyStorageAccess() {
            @Override public long getAmount() { return 0; }
            @Override public long getCapacity() { return 0; }
            @Override public long getMaxReceive() { return 0; }
            @Override public long getMaxExtract() { return 0; }
            @Override public ICableTier getCableTier() { return getOutputTier(); }
            @Override public boolean isPowerSource() { return isPowerSource(); }
            @Override public long getPowerOutput() { return mio_icif_transformer.this.getPowerOutput(); }
            @Override public boolean isOutputEnabled() { return true; }
            @Override public long getPowerRating() { return getOutputTier().getPowerRating(); }
            @Override public boolean isOverloaded(long gridPower) { return false; }
            @Override public long useEnergy(long amount, boolean simulate) { return 0; }
            @Override public long generateEnergy(long amount, boolean simulate) { return 0; }
        };
}