package com.singularity_iteration.mio_icif.Blocks.entity.energy_converter;

import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_Energy_Block;
import com.singularity_iteration.mio_icif.Blocks.energy_converter.mio_icif_block_energy_converter;
import com.singularity_iteration.mio_icif.energy.EnergyUnit.CableTier;
import com.singularity_iteration.mio_icif.api.machine.IEnergyConverter;
import com.singularity_iteration.mio_icif.api.machine.IMachineAPI;
import com.singularity_iteration.mio_icif.api.energy.ICableTier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.jetbrains.annotations.Nullable;

/**
 * 能源转换器方块实体
 * 
 * 模式说明：
 * - EU_TO_FE (false): 正面输出EU，其余5面输入FE，1EU = 4FE
 * - FE_TO_EU (true): 正面输出FE，其余5面输入EU，4FE = 1EU
 */
@SuppressWarnings("null")
public class mio_icif_energy_converter_entity extends mio_icif_Energy_Block implements IEnergyConverter {
    
    // FE能量存储
    private final FEEnergyStorage feStorage;
    
    // 转换比例
    public static final int EU_TO_FE_RATE = 4; // 1 EU = 4 FE
    public static final int MAX_CONVERT_PER_TICK = 1000; // 最大转换量
    
    // 当前模式
    private boolean mode = false; // false = EU->FE, true = FE->EU
    
    public mio_icif_energy_converter_entity(BlockPos pos, BlockState state) {
        this(pos, state, com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_block_entities.ENERGY_CONVERTER_ENTITY_TYPE.get());
    }
    
    public mio_icif_energy_converter_entity(BlockPos pos, BlockState state, BlockEntityType<?> type) {
        super(pos, state, type, 100000, 2048, 2048, CableTier.HV);
        
        // FE存储：容量400k FE（对应100k EU * 4），最大输入/输出 8192 FE/t
        this.feStorage = new FEEnergyStorage(400000, 8192, 8192);
        
        // 默认作为EU用电器（EU->FE模式，接收EU），禁止FE面接收FE
        setAsConsumer();
        feStorage.setCanReceive(false);
    }
    
    /**
     * 设置模式
     */
    public void setMode(boolean mode) {
        this.mode = mode;
        // 根据模式设置电源/用电器状态
        // EU->FE 模式：转换器作为EU用电器，接收EU并转换为FE输出
        // FE->EU 模式：转换器作为EU电源，接收FE并转换为EU输出
        if (!mode) {
            // EU->FE 模式：作为EU用电器（接收EU），禁止FE面接收FE
            setAsConsumer();
            feStorage.setCanReceive(false);
        } else {
            // FE->EU 模式：作为EU电源（输出EU），允许FE面接收FE
            setAsPowerSource(2048); // HV 等级输出
            feStorage.setCanReceive(true);
        }
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            // 刷新电网注册以更新连接
            refreshRegistration();
        }
    }
    
    /**
     * 获取当前模式
     */
    public boolean getMode() {
        return mode;
    }
    
    /**
     * 获取正面方向
     */
    public Direction getFrontFacing() {
        return getBlockState().getValue(mio_icif_block_energy_converter.FACING);
    }
    
    /**
     * 主 tick 逻辑
     */
    public void tick() {
        if (level == null || level.isClientSide) return;
        
        Direction front = getFrontFacing();
        
        if (mode) {
            // FE -> EU 模式
            convertFEtoEU(front);
        } else {
            // EU -> FE 模式
            convertEUtoFE(front);
        }
    }
    
    /**
     * EU -> FE 转换
     * 从EU存储提取EU，转换为FE，然后从正面输出FE
     * 其余5面可以接收EU补充到EU存储
     */
    private void convertEUtoFE(Direction front) {
        // 1. 从EU存储提取EU，转换为FE存入FE存储
        // 注意：energyStorage.getAmount() 返回 EU 单位，getEnergyStored() 返回 FE 单位
        long euAvailable = energyStorage.getAmount();  // 使用 getAmount() 获取 EU
        long euToConvert = Math.min(euAvailable, MAX_CONVERT_PER_TICK);
        int feToAdd = (int) (euToConvert * EU_TO_FE_RATE);
        
        if (feToAdd > 0) {
            // 使用内部方法存入FE（不受canReceive限制）
            int actualAdded = feStorage.receiveEnergyInternal(feToAdd, false);
            if (actualAdded > 0) {
                // 根据实际转换的FE量计算消耗的EU
                int actualEUConsumed = actualAdded / EU_TO_FE_RATE;
                // 使用 extract() 方法直接提取EU（不是 extractEnergy，后者期望FE单位）
                energyStorage.extract(actualEUConsumed, false);
            }
        }
        
        // 2. 从正面输出FE到连接的FE设备
        BlockPos outputPos = worldPosition.relative(front);
        net.minecraft.world.level.block.entity.BlockEntity be = level.getBlockEntity(outputPos);
        if (be != null) {
            IEnergyStorage feHandler = level.getCapability(Capabilities.EnergyStorage.BLOCK, outputPos,
                                                              be.getBlockState(), be, front.getOpposite());
            if (feHandler != null && feHandler.canReceive()) {
                int feToOutput = Math.min(feStorage.getEnergyStored(), feStorage.maxExtract);
                if (feToOutput > 0) {
                    int accepted = feHandler.receiveEnergy(feToOutput, false);
                    if (accepted > 0) {
                        feStorage.extractEnergy(accepted, false);
                    }
                }
            }
        }
    }
    
    /**
     * FE -> EU 转换
     * 从5个输入面接收FE，转换为EU存入EU存储
     * EU存储通过电网系统输出EU
     */
    private void convertFEtoEU(Direction front) {
        // 1. 从其他方块接收FE（5个输入面，跳过正面）
        for (Direction dir : Direction.values()) {
            if (dir == front) continue; // 正面是输出，跳过
            
            BlockPos inputPos = worldPosition.relative(dir);
            net.minecraft.world.level.block.entity.BlockEntity be = level.getBlockEntity(inputPos);
            if (be == null) continue;
            
            IEnergyStorage feHandler = level.getCapability(Capabilities.EnergyStorage.BLOCK, inputPos, 
                                                             be.getBlockState(), be, dir.getOpposite());
            if (feHandler == null || !feHandler.canExtract()) continue;
            
            // 尝试提取FE
            int canReceive = feStorage.receiveEnergy(MAX_CONVERT_PER_TICK, true);
            if (canReceive > 0) {
                int extracted = feHandler.extractEnergy(canReceive, false);
                if (extracted > 0) {
                    feStorage.receiveEnergy(extracted, false);
                }
            }
        }
        
        // 2. 将FE存储中的FE转换为EU存入EU存储
        int feAvailable = feStorage.getEnergyStored();
        int feToConvert = Math.min(feAvailable, MAX_CONVERT_PER_TICK * EU_TO_FE_RATE);
        int euToAdd = feToConvert / EU_TO_FE_RATE;
        
        if (euToAdd > 0) {
            // 使用 receive() 方法直接存入EU（不是 receiveEnergy，后者期望FE单位）
            long actualAdded = energyStorage.receive(euToAdd, false);
            if (actualAdded > 0) {
                // 根据实际存入的EU量计算消耗的FE
                int actualFEConsumed = (int) (actualAdded * EU_TO_FE_RATE);
                feStorage.extractEnergy(actualFEConsumed, false);
            }
        }
    }
    
    @Override
    public void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("Mode", mode);
        tag.putInt("FEStored", feStorage.getEnergyStored());
    }
    
    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        mode = tag.getBoolean("Mode");
        feStorage.setEnergy(tag.getInt("FEStored"));
        // 根据加载的模式设置电源/用电器状态
        if (!mode) {
            setAsConsumer();
        } else {
            setAsPowerSource(2048);
        }
    }
    
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        saveAdditional(tag, registries);
        return tag;
    }
    
    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
    
    /**
     * 获取FE存储
     */
    public FEEnergyStorage getFEStorage() {
        return feStorage;
    }
    
    /**
     * 内部FE能量存储类
     */
    public static class FEEnergyStorage implements IEnergyStorage {
        private int energy;
        private final int capacity;
        private final int maxReceive;
        private final int maxExtract;
        private boolean canReceive = true; // 是否允许接收FE
        private boolean internalReceive = false; // 是否允许内部转换存入
        
        public FEEnergyStorage(int capacity, int maxReceive, int maxExtract) {
            this.capacity = capacity;
            this.maxReceive = maxReceive;
            this.maxExtract = maxExtract;
            this.energy = 0;
        }
        
        public void setCanReceive(boolean canReceive) {
            this.canReceive = canReceive;
        }
        
        /**
         * 内部转换存入FE（不受canReceive限制）
         */
        public int receiveEnergyInternal(int maxReceive, boolean simulate) {
            int received = Math.min(capacity - energy, Math.min(this.maxReceive, maxReceive));
            if (!simulate) {
                energy += received;
            }
            return received;
        }
        
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            if (!canReceive && !internalReceive) return 0; // EU->FE模式下禁止接收FE
            int received = Math.min(capacity - energy, Math.min(this.maxReceive, maxReceive));
            if (!simulate) {
                energy += received;
            }
            return received;
        }
        
        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            int extracted = Math.min(energy, Math.min(this.maxExtract, maxExtract));
            if (!simulate) {
                energy -= extracted;
            }
            return extracted;
        }
        
        @Override
        public int getEnergyStored() {
            return energy;
        }
        
        @Override
        public int getMaxEnergyStored() {
            return capacity;
        }
        
        @Override
        public boolean canExtract() {
            return maxExtract > 0;
        }
        
        @Override
        public boolean canReceive() {
            return canReceive && maxReceive > 0;
        }
        
        public void setEnergy(int energy) {
            this.energy = Math.min(energy, capacity);
        }
    }

    // ==================== IEnergyConverter API ====================

    @Override
    public EnergyType getInputType() {
        return mode ? EnergyType.FE : EnergyType.EU;
    }

    @Override
    public EnergyType getOutputType() {
        return mode ? EnergyType.EU : EnergyType.FE;
    }

    @Override
    public double getConversionRatio() {
        return mode ? (1.0 / EU_TO_FE_RATE) : EU_TO_FE_RATE;
    }

    @Override
    public ICableTier getInputCableTier() {
        return CableTier.HV;
    }

    @Override
    public ICableTier getOutputCableTier() {
        return CableTier.HV;
    }

    @Override
    public long getBufferAmount() {
        return mode ? feStorage.getEnergyStored() : energyStorage.getAmount();
    }

    @Override
    public long getBufferCapacity() {
        return mode ? feStorage.getMaxEnergyStored() : energyStorage.getCapacity();
    }

    @Override
    public IMachineAPI.MachineType getMachineType() {
        return IMachineAPI.MachineType.ENERGY_CONVERTER;
    }
}