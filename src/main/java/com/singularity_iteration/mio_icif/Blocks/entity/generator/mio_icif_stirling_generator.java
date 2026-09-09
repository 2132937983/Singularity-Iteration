package com.singularity_iteration.mio_icif.Blocks.entity.generator;

import com.singularity_iteration.mio_icif.energy.CustomEUEnergyStorage;
import com.singularity_iteration.mio_icif.Blocks.entity.HUEntity.mio_icif_HeatU_Block;
import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_block_entities;
import com.singularity_iteration.mio_icif.energy.EnergyUnit.CableTier;
import com.singularity_iteration.mio_icif.energy.EnergyUnit.EUApi;
import com.singularity_iteration.mio_icif.energy.EnergyUnit.IEUEnergyStorage;
import com.singularity_iteration.mio_icif.energy.grid.*;
import com.singularity_iteration.mio_icif.api.capability.IMioIcifCapabilities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

/**
 * 斯特林发电机方块实体
 * 继承自热能方块，将 HU 热能转换为 EU 电能
 *
 * 特点：
 * - 从外部接收 HU 热能（像普通用热能设备）
 * - 将 HU 热能转换为 EU 电能
 * - 转换率：2 HU = 1 EU（即 1 HU = 0.5 EU，对应 IC2 原版 productionpeerheat = 0.5）
 * - 输出等级：最高 MV（对应 IC2 原版 getSourceTier = max(getTierFromPower(maxProduction), 2)）
 * - 可以存储一定量的热能用于发电
 */
@SuppressWarnings("null")
public class mio_icif_stirling_generator extends mio_icif_HeatU_Block implements IEnergySource, com.singularity_iteration.mio_icif.api.machine.IGeneratorBlock {

    private boolean registered = false;

    // 能量存储（EU）?
    private final CustomEUEnergyStorage energyStorage;

    // 热能容量
    private static final int HEAT_CAPACITY = 20000;
    // 最大热能接收速率
    private static final int MAX_HEAT_RECEIVE = 1000;
    // 最大热能提取速率（斯特林发电机需要提取热能来发电）
    private static final int MAX_HEAT_EXTRACT = 1000;
    // 基础温度
    private static final int BASE_TEMP = 20;
    // 最高温度
    private static final int MAX_TEMP = 1000;
    // 热损失系数
    private static final float LOSS_FACTOR = 0.01f;

    // 电能容量 - MV 级
    private static final long ENERGY_CAPACITY = 200000;
    // 最大电能输出速率 - MV 级 128 EU/t
    private static final long MAX_ENERGY_EXTRACT = 128;
    // 最大电能接收速率（斯特林发电机不能从外部接收电能）
    private static final long MAX_ENERGY_RECEIVE = 0;

    // 转换率：2 HU = 1 EU（IC2 原版 productionpeerheat = 0.5）
    private static final int HU_PER_CONVERSION = 2;
    private static final int EU_PER_CONVERSION = 1;

 // 当前存的热能（用于转换）
    private long heatBuffer = 0;

    // 是否正在工作
    private boolean isWorking = false;

    // 电缆等级 - MV 级
    private static final CableTier CABLE_TIER = CableTier.MV;

    /**
     * 构造函数
     */
    public mio_icif_stirling_generator(BlockPos pos, BlockState state) {
        super(mio_icif_block_entities.STIRLING_GENERATOR_ENTITY_TYPE.get(), pos, state, HEAT_CAPACITY, MAX_HEAT_RECEIVE, MAX_HEAT_EXTRACT,
                BASE_TEMP, MAX_TEMP, LOSS_FACTOR);

        // 初始化电能存储（EU）
        this.energyStorage = new CustomEUEnergyStorage(ENERGY_CAPACITY, MAX_ENERGY_RECEIVE, MAX_ENERGY_EXTRACT, CABLE_TIER);
    }

    /**
     * 每 tick 更新逻辑
     * 
     * 注意：发电机不再直接向相邻方块输出能量（distributeEnergy已移除）
     * 能量分配完全通过电网机制进行：
     * - 发电机产生能量存入energyStorage
     * - 电网通过extractPowerForConsumer从发电机提取能量
 * - 电力消费者通过电网requestEnergy机制获取能量
     * 
     * 这种设计确保能量分配遵循电网的公平分配和过载保护机制
     */
    public static void tick(Level level, BlockPos pos, BlockState state, mio_icif_stirling_generator blockEntity) {
        if (level.isClientSide()) {
            return;
        }

        // 调用父类的 tick 方法（处理热损失和热传导）
        mio_icif_HeatU_Block.tick(level, pos, state, blockEntity);

        // 将热能转换为电能
        blockEntity.convertHeatToEnergy();

        // 注意：不再直接向相邻方块输出能量
        // 能量分配完全由电网机制处理
        // 电网通过 IPowerSource.extractPowerForConsumer 从发电机的energyStorage提取能量

        // 更新工作状态
        boolean wasWorking = blockEntity.isWorking;
        blockEntity.isWorking = blockEntity.heatBuffer >= HU_PER_CONVERSION ||
                blockEntity.heatStorage.getHeatStored() >= HU_PER_CONVERSION;

        // 如果工作状态改变，更新方块状态
        if (wasWorking != blockEntity.isWorking) {
            blockEntity.updateBlockState(blockEntity.isWorking);
        }
    }

    /**
     * 将热能转换为电能
     */
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

    private void convertHeatToEnergy() {
        long heatInStorage = heatStorage.getHeatStored();
        long maxExtract = heatStorage.getMaxExtract();

        if (heatInStorage > 0) {
            long heatToBuffer = Math.min(heatInStorage, maxExtract);
            if (heatToBuffer > 0) {
                long extracted = heatStorage.extractHeat(heatToBuffer, false);
                heatBuffer += extracted;
                setChanged();
            }
        }

        // 检查是否有足够的热能进行转换
        if (heatBuffer >= HU_PER_CONVERSION) {
            // 检查电能存储是否已满
            long energySpace = energyStorage.getCapacity() - energyStorage.getAmount();
            if (energySpace >= EU_PER_CONVERSION) {
                // 进行转换
                long conversions = Math.min(heatBuffer / HU_PER_CONVERSION, energySpace / EU_PER_CONVERSION);
                long heatToConvert = conversions * HU_PER_CONVERSION;
                long energyToGenerate = conversions * EU_PER_CONVERSION;

                heatBuffer -= heatToConvert;
                energyStorage.generateEnergyInternal(energyToGenerate, false);
                setChanged();
            }
        }
    }

    /**
     * 向相邻方块输出电能
     * 注意：不在正面（接收热能的一面）输出电能
     */
    @SuppressWarnings("unused")
    private void distributeEnergy() {
        if (energyStorage.getAmount() <= 0) {
            return;
        }

        Direction facing = getBlockState().getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING);
        Direction front = facing.getOpposite();

        for (Direction direction : Direction.values()) {
            if (direction == front) {
                continue;
            }

            BlockPos adjacentPos = worldPosition.relative(direction);

            IEUEnergyStorage adjacentStorage = level.getCapability(
                    EUApi.SIDED, adjacentPos, direction.getOpposite());

            if (adjacentStorage != null) {
                long energyToTransfer = Math.min(energyStorage.getAmount(), energyStorage.getMaxExtract());
                long energyReceived = adjacentStorage.receive(energyToTransfer, false);
                if (energyReceived > 0) {
                    energyStorage.extract(energyReceived, false);
                    setChanged();
                }
                continue;
            }

            long energyToTransfer = Math.min(energyStorage.getAmount(), energyStorage.getMaxExtract());
            long pushed = com.singularity_iteration.mio_icif.integration.mi.EnergyBridge.pushToStorage(level, adjacentPos, direction, energyToTransfer);
            if (pushed > 0) {
                energyStorage.extract(pushed, false);
                setChanged();
            }
        }

    }

    /**
     * 更新方块状态
     */
    private void updateBlockState(boolean working) {
        if (level == null || level.isClientSide()) {
            return;
        }

        BlockState currentState = level.getBlockState(worldPosition);
        // 获取当前的 active 状态
        Boolean currentActive = currentState.getValue(com.singularity_iteration.mio_icif.Blocks.generator.mio_icif_block_stirling_generator.ACTIVE);
        // 只有当状态改变时才更新
        if (currentActive != working) {
            level.setBlock(worldPosition, currentState.setValue(
                com.singularity_iteration.mio_icif.Blocks.generator.mio_icif_block_stirling_generator.ACTIVE,
                working), 3);
        }
    }

    /**
     * 获取电能存储
     */
    public CustomEUEnergyStorage getEnergyStorage() {
        return energyStorage;
    }

    /**
     * 获取电能存储能力（用于 capability 系统）
     */
    public IEUEnergyStorage getEnergyStorageCapability(@Nullable Direction side) {
        return energyStorage;
    }

    /**
     * 获取热能存储
     */
    public IMioIcifCapabilities.IHeatStorage getHeatStorage() {
        return heatStorage;
    }

    /**
     * 获取当前能量输出（EU/tick）
     */
    public long getEnergyOutput() {
        if (!isWorking) {
            return 0;
        }
        // 计算当前可以转换多少热能
        long availableHeat = heatBuffer + heatStorage.getHeatStored();
        long conversions = availableHeat / HU_PER_CONVERSION;
        return (long) conversions * EU_PER_CONVERSION;
    }

    /**
     * 斯特林发电机不向外传导热能
     * 它只接收热能并转换为电能
     */
    @Override
    public double getOfferedEnergy() {
        long available = Math.min(energyStorage.getAmount(), energyStorage.getMaxExtract());
        return Math.min(available, CABLE_TIER.powerRating);
    }

    @Override
    public void drawEnergy(double amount) {
        if (amount > 0.0D) {
            long request = Math.min((long) amount, CABLE_TIER.powerRating);
            energyStorage.extract(request, false);
        }
    }

    @Override
    public int getSourceTier() {
        return EnergyNetGlobal.cableTierToSourceTier(CABLE_TIER);
    }

    /** 保留旧兼容方法 */
    public long extractPowerForConsumer(long amount, boolean simulate) {
        long request = Math.min(amount, CABLE_TIER.powerRating);
        return energyStorage.extract(request, simulate);
    }

    // ==================== IEnergyEmitter ====================

    @Override
    public boolean emitsEnergyTo(IEnergyAcceptor acceptor, Direction direction) {
        // 斯特林发电机可以向所有方向输出能量（除了接收热能的那一面）
        Direction facing = getBlockState().getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING);
        Direction front = facing.getOpposite();
        return direction != front;
    }

    @Override
    protected void distributeHeat() {
        // 不传导热能，只接收
    }

    /**
     * 获取热能存储能力（用于 capability 系统）
     * 斯特林发电机只从正面接收热能，不输出热能
     * 正面 = 方块朝向的相反方向（因为发热机是朝向前方输出热量）
     */
    @Override
    public IMioIcifCapabilities.IHeatStorage getHeatStorageCapability(@Nullable Direction side) {
        if (side == null) {
            return new HeatStorageWrapper(heatStorage, null);
        }

        // 获取方块朝向
        Direction facing = getBlockState().getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING);
        // 正面是朝向的反方向（发热机从前方输出热量，斯特林发电机从正面接收）
        Direction front = facing.getOpposite();

        // 只有从正面才能接收热能
        if (side == front) {
            return new HeatStorageWrapper(heatStorage, side);
        }

        // 其他方向不能接收热能
        return null;
    }

    /**
 * 获取热缓冲区
     */
    public long getHeatBuffer() {
        return heatBuffer;
    }

    /**
     * 获取当前电能
     */
    public long getEnergyStored() {
        return energyStorage.getAmount();
    }

    /**
     * 获取电能容量
     */
    public long getEnergyCapacity() {
        return energyStorage.getCapacity();
    }

    /**
     * 是否正在工作
     */
    public boolean isWorking() {
        return isWorking;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLong("HeatBuffer", heatBuffer);
        tag.putLong("Energy", energyStorage.getAmount());
        tag.putBoolean("IsWorking", isWorking);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("HeatBuffer", net.minecraft.nbt.Tag.TAG_LONG)) {
            heatBuffer = tag.getLong("HeatBuffer");
        } else if (tag.contains("HeatBuffer", net.minecraft.nbt.Tag.TAG_INT)) {
            heatBuffer = tag.getInt("HeatBuffer");
        }
        if (tag.contains("Energy", net.minecraft.nbt.Tag.TAG_LONG)) {
            energyStorage.setEnergy(tag.getLong("Energy"));
        } else if (tag.contains("Energy", net.minecraft.nbt.Tag.TAG_INT)) {
            // 兼容旧存档
            energyStorage.setEnergy(tag.getInt("Energy"));
        }
        if (tag.contains("IsWorking", net.minecraft.nbt.Tag.TAG_BYTE)) {
            isWorking = tag.getBoolean("IsWorking");
        }
    }

    @Override
    public net.minecraft.network.chat.Component getDisplayName() {
        return net.minecraft.network.chat.Component.translatable("container.mio_icif.stirling_generator");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new com.singularity_iteration.mio_icif.Menu.Generator.StirlingGeneratorMenu(containerId, playerInventory, this, this.getItemHandler(), null);
    }

    // ==================== IGeneratorBlock / IBurnControl API ====================

    @Override
    public com.singularity_iteration.mio_icif.api.energy.ICableTier getCableTier() {
        return CABLE_TIER;
    }

    @Override
    public boolean isBurning() {
        return isWorking;
    }

    @Override
    public int getBurnTime() {
        return 0;
    }

    @Override
    public int getMaxBurnTime() {
        return 0;
    }

    @Override
    public long getPowerOutput() {
        return (long) getOfferedEnergy();
    }

    @Override
    public ItemStack getFuelSlotItem() {
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack getChargeSlotItem() {
        return ItemStack.EMPTY;
    }

    @Override
    public IItemHandler getItemHandler() {
        return null;
    }

    @Override
    public int getDefaultBurnTime() {
        return 0;
    }

    @Override
    public void setBurnTime(int ticks) {
        // 斯特林发电机无燃烧机制，此方法无操作
    }

    /**
     * 热能存储包装类
     * 限制斯特林发电机只能接收热能，不能输出热能
     */
    @SuppressWarnings("unused")
    private class HeatStorageWrapper implements IMioIcifCapabilities.IHeatStorage {
        private final IMioIcifCapabilities.IHeatStorage internal;
        private final Direction side;

        public HeatStorageWrapper(IMioIcifCapabilities.IHeatStorage internal, Direction side) {
            this.internal = internal;
            this.side = side;
        }

        @Override
        public long receiveHeat(long maxReceive, boolean simulate) {
            return internal.receiveHeat(maxReceive, simulate);
        }

        @Override
        public long extractHeat(long maxExtract, boolean simulate) {
            return 0;
        }

        @Override
        public long getHeatStored() {
            return internal.getHeatStored();
        }

        @Override
        public long getMaxHeatStored() {
            return internal.getMaxHeatStored();
        }

        @Override
        public boolean canReceiveHeat() {
            return internal.canReceiveHeat() && energyStorage.getAmount() < energyStorage.getCapacity();
        }

        @Override
        public boolean canExtractHeat() {
            return false;
        }

        @Override
        public int getTemperature() {
            return internal.getTemperature();
        }

        @Override
        public boolean isOverheated() {
            return internal.isOverheated();
        }

        @Override
        public long getHeatLossPerTick() {
            return internal.getHeatLossPerTick();
        }

        @Override
        public long getMaxReceive() {
            return internal.getMaxReceive();
        }

        @Override
        public long getMaxExtract() {
            return 0;
        }
    }
}