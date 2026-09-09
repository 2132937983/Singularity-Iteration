package com.singularity_iteration.mio_icif.Blocks.entity.generator;

import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_Energy_Block;
import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_Energy_Generator;
import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_block_entities;
import com.singularity_iteration.mio_icif.Blocks.entity.slot.SlotLayout;
import com.singularity_iteration.mio_icif.energy.EnergyUnit.CableTier;
import com.singularity_iteration.mio_icif.energy.grid.*;
import com.singularity_iteration.mio_icif.energy.kinetic.IKineticStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * 动能发电机（MV 级，最低 MV，动态升压）
 * 通过接收动能 (KU) 来发电 (EU)
 * 转换比例：4 KU = 1 EU（对应 IC2 原版 euPerKu = 0.25）
 * 
 * IC2 原版行为：
 * - getSourceTier() = Math.max(getTierFromPower(maxProduction), 2)
 *   即最低 MV 级（tier 2 = 128 EU/t），随动能输入量动态升压
 * - getOfferedEnergy() = getEnergyAvailable() * euPerKu
 *   即按需转换，不缓冲 EU
 * - 1个手摇发电机(400 KU/click) → 100 EU/click → MV 级
 * - 蒸汽动能（可达数千 KU/t) → HV/EV 级
 */
@SuppressWarnings("null")
public class mio_icif_kinetic_generator extends mio_icif_Energy_Generator {
    
    private static final long ENERGY_CAPACITY = 100000L;

    private static final long MAX_RECEIVE = 0L;

    private static final long MAX_EXTRACT = 128L;

    private static final long ENERGY_GENERATION_RATE = 0L;

    private final KineticReceiverStorage kineticStorage;

    private static final int KU_TO_EU_RATIO = 4;


    private long lastEnergyOutput = 0L;

    private double lastProduction = 0.0D;

    private int lastKuInputRate = 0;
    
    /**
     * 构造函数（用于 BlockEntityType.Builder）
     */
    public mio_icif_kinetic_generator(BlockPos pos, BlockState state) {
        this(pos, state, null);
    }

    /**
     * 构造函数
     */
    public mio_icif_kinetic_generator(BlockPos pos, BlockState state, BlockEntityType<?> type) {
        super(pos, state, type != null ? type : mio_icif_block_entities.KINETIC_GENERATOR_ENTITY_TYPE.get(),
              SlotLayout.builder().extra(2).build(), ENERGY_GENERATION_RATE, ENERGY_CAPACITY, MAX_RECEIVE, MAX_EXTRACT, CableTier.MV);
        this.kineticStorage = new KineticReceiverStorage(10000, 1000);
    }
    
    /**
     * 每 tick 更新逻辑
     * 
     * 能量传输路径：
     * 1. convertKineticToEnergy() 将 KU 转 EU 存入 energyStorage
     * 2. 电网（PowerGrid）在 LevelTickEvent.Pre 阶段通过 extractPowerForConsumer()
 *    从 energyStorage 提取 EU 并推送给用电器（电力）
     * 3. 这里不再直接推送至电线（电线无能量存储），改为在 BE tick 结束后
     *    通过 distributeEnergyToGrid() 立即将本 tick 新转换的 EU 分发到电网
     */
    public static void tick(Level level, BlockPos pos, BlockState state, mio_icif_kinetic_generator blockEntity) {
        if (level.isClientSide()) {
            return;
        }
        
        mio_icif_Energy_Block.tick(level, pos, state, blockEntity);
        
        blockEntity.lastEnergyOutput = 0;
        blockEntity.lastProduction = 0;
        blockEntity.lastKuInputRate = (int) blockEntity.kineticStorage.getAndResetReceivedThisTick();
        
        long euBefore = blockEntity.getEnergyStorage().getAmount();
        blockEntity.convertKineticToEnergy();
        long euAfter = blockEntity.getEnergyStorage().getAmount();
        blockEntity.lastProduction = euAfter - euBefore;
        
        blockEntity.chargeItems();
        
        boolean shouldBeActive = blockEntity.isBurning();
        boolean isActive = state.getValue(com.singularity_iteration.mio_icif.Blocks.generator.mio_icif_Block_kinetic_generator.ACTIVE);
        if (shouldBeActive != isActive) {
            level.setBlock(pos, state.setValue(com.singularity_iteration.mio_icif.Blocks.generator.mio_icif_Block_kinetic_generator.ACTIVE, shouldBeActive), 3);
        }
        
        blockEntity.setChanged();
    }
    
    @Override
    protected boolean shouldDirectlyDistributeEnergy() {
        return false;
    }
    
    /**
     * 将动能转换为电能
     */
    private void convertKineticToEnergy() {
        long kineticStored = kineticStorage.getKineticStored();
        if (kineticStored <= 0) {
            return;
        }
        
        long energySpace = ENERGY_CAPACITY - getEnergyStorage().getAmount();
        if (energySpace <= 0) {
            return;
        }
        
        long maxEuFromKinetic = kineticStored / KU_TO_EU_RATIO;
        long euToGenerate = Math.min(maxEuFromKinetic, energySpace);
        
        if (euToGenerate > 0) {
            long kuToConsume = euToGenerate * KU_TO_EU_RATIO;
            kineticStorage.extractKinetic(kuToConsume, false);
            
            apiGenerateEnergy(euToGenerate, false);
        }
    }
    
    /**
     * 获取动能存储（用于 capability）
     */
    public IKineticStorage getKineticStorage() {
        return kineticStorage;
    }

    /**
     * 根据方向获取动能存储能力
     * 只有正面才能接收动能
     * @param side 相对于方块的方向
     * @return 如果是正面则返回动能存储，否则返回 null
     */
    public IKineticStorage getKineticStorageCapability(@Nullable Direction side) {
        if (side == null) {
            return null;
        }
        // 获取方块朝向
        Direction facing = getBlockState().getValue(
            com.singularity_iteration.mio_icif.Blocks.mio_icif_entity_block.FACING);
        // 只有正面（facing 方向）才能接收动能
        // side 是相对于动能发电机的方向，当外部方块在发电机正面时，side == facing
        if (side == facing) {
            return kineticStorage;
        }
        return null;
    }
    
    @Override
    public int getFuelBurnTime(ItemStack fuel) {
        // 动能发电机不需要燃料
        return 0;
    }
    
    @Override
    public boolean isBurning() {
        // 当有动能输入时视为运行中
        return kineticStorage.getKineticStored() > 0 || getEnergyStorage().getAmount() > 0;
    }
    
    /**
     * 获取上一 tick 输出到电网的电量（用于 GUI 显示）
     * @return 上一 tick 输出的 EU 数量
     */
    public long getLastEnergyOutput() {
        return lastEnergyOutput;
    }

    @Override
    public long getPowerOutput() {
        return (long) Math.min(getEnergyStorage().getAmount(), EnergyNetGlobal.getPowerFromTier(getSourceTier()));
    }

    @Override
    public int getSourceTier() {
        double potentialEuPerTick = lastKuInputRate / (double) KU_TO_EU_RATIO;
        return Math.max(EnergyNetGlobal.getTierFromPower(potentialEuPerTick), 2);
    }

    @Override
    public void drawEnergy(double amount) {
        if (amount > 0.0D) {
            apiUseEnergy((long) amount, false);
        }
    }

    public double getLastProduction() {
        return lastProduction;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.mio_icif.kinetic_generator");
    }
    
    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new com.singularity_iteration.mio_icif.Menu.Generator.KineticGeneratorMenu(containerId, playerInventory, this, this.getItemHandler(), null);
    }
    
    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        kineticStorage.saveToNBT(tag);
    }
    
    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        kineticStorage.loadFromNBT(tag);
    }
    
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        kineticStorage.saveToNBT(tag);
        return tag;
    }
    
    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        super.handleUpdateTag(tag, registries);
        kineticStorage.loadFromNBT(tag);
    }
    
    // ==================== 内部类：动能接收存储 ====================
    
    /**
     * 动能接收存储
     * 专门用于接收外部输入的动能
     */
    private class KineticReceiverStorage implements IKineticStorage {
        private long kineticStored = 0;
        private final long maxKinetic;
        private final long maxReceive;
        private long receivedThisTick = 0;
        
        public KineticReceiverStorage(long maxKinetic, long maxReceive) {
            this.maxKinetic = maxKinetic;
            this.maxReceive = maxReceive;
        }
        
        @Override
        public long receiveKinetic(long amount, boolean simulate) {
            long space = maxKinetic - kineticStored;
            long toReceive = Math.min(amount, Math.min(space, maxReceive));
            if (!simulate) {
                kineticStored += toReceive;
                receivedThisTick += toReceive;
                mio_icif_kinetic_generator.this.setChanged();
            }
            return toReceive;
        }
        
        public long getAndResetReceivedThisTick() {
            long received = receivedThisTick;
            receivedThisTick = 0;
            return received;
        }
        
        @Override
        public long extractKinetic(long amount, boolean simulate) {
            long toExtract = Math.min(amount, kineticStored);
            if (!simulate) {
                kineticStored -= toExtract;
                mio_icif_kinetic_generator.this.setChanged();
            }
            return toExtract;
        }
        
        @Override
        public long getKineticStored() {
            return kineticStored;
        }
        
        @Override
        public long getMaxKineticStored() {
            return maxKinetic;
        }
        
        @Override
        public boolean canReceiveKinetic() {
            return kineticStored < maxKinetic;
        }
        
        @Override
        public boolean canExtractKinetic() {
            return false;
        }
        
        @Override
        public long getMaxReceive() {
            return maxReceive;
        }
        
        @Override
        public long getMaxExtract() {
            return 0;
        }
        
        @Override
        public int getRPM() {
            // 动能发电机作为接收方，转速应该低于手摇发电机，确保能接收动能
            // 手摇发电机 MAX_RPM = 5000，这里返回 0 表示可以接受任何转速的输入
            return 0;
        }
        
    public void saveToNBT(CompoundTag tag) {
        tag.putLong("KineticStored", kineticStored);
    }
    
    public void loadFromNBT(CompoundTag tag) {
        kineticStored = tag.getLong("KineticStored");
    }
    }
}