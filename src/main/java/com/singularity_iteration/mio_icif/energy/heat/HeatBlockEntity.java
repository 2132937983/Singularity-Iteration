package com.singularity_iteration.mio_icif.energy.heat;

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
import org.jetbrains.annotations.Nullable;

/**
 * HU 热能方块实体基类
 * 提供基础的热能存储和传输功能
 * 
 * �?Energy_Block 的区别：
 * - 使用 HU 而不�?FE
 * - 热能会自然散�? * - 有温度概�? * - 不能像电力那样远距离传输
 */
@SuppressWarnings("null")
public abstract class HeatBlockEntity extends BlockEntity implements MenuProvider {
    
    protected final HeatStorage heatStorage;
    
    /**
     * 构造函数
 * @param type 方块实体类型
     * @param pos 位置
     * @param state 方块状态
 * @param capacity 热能容量
     * @param maxReceive 最大接收速率
     * @param maxExtract 最大提取速率
     * @param baseTemp 基础温度
     * @param maxTemp 最高温�
 * @param lossFactor 热损失系�
 */
    public HeatBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state,
                           int capacity, int maxReceive, int maxExtract,
                           int baseTemp, int maxTemp, float lossFactor) {
        super(type, pos, state);
        this.heatStorage = new HeatStorage(capacity, maxReceive, maxExtract, 
                                           baseTemp, maxTemp, lossFactor);
    }
    
    /**
     * 简化构造函数（使用默认温度参数据
 */
    public HeatBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state,
                           int capacity, int maxReceive, int maxExtract) {
        this(type, pos, state, capacity, maxReceive, maxExtract, 20, 1000, 0.01f);
    }
    
    /**
     * 获取热能存储
     */
    public HeatStorage getHeatStorage() {
        return heatStorage;
    }
    
    /**
     * �?tick 更新逻辑
     */
    public static void tick(Level level, BlockPos pos, BlockState state, HeatBlockEntity blockEntity) {
        if (level.isClientSide()) {
            return;
        }
        
        // 应用热损坏
    blockEntity.applyHeatLoss();
        
        // 传导热能到相邻方法
    blockEntity.distributeHeat();
    }
    
    /**
     * 应用热损坏
 */
    protected void applyHeatLoss() {
        long loss = heatStorage.applyHeatLoss();
        if (loss > 0) {
            setChanged();
        }
    }
    
    /**
     * 向相邻方块传导热�
 * 热能传导需要温度差，且效率受介质影�
 */
    protected void distributeHeat() {
        if (heatStorage.getHeatStored() <= 0) {
            return;
        }
        
        int myTemp = heatStorage.getTemperature();
        
        for (Direction direction : Direction.values()) {
            BlockPos adjacentPos = worldPosition.relative(direction);
            
            // 获取相邻方块的热能存�
        IHeatStorage adjacentHeat = level.getCapability(
                HUCapabilities.HeatStorage.BLOCK, adjacentPos, direction.getOpposite());
            
            if (adjacentHeat != null && adjacentHeat.canReceiveHeat()) {
                int adjacentTemp = adjacentHeat.getTemperature();
                
                // 需要温度差才能传导
                if (myTemp > adjacentTemp) {
                    int tempDiff = myTemp - adjacentTemp;
                    
                    // 传导速率取决于温度差和最大提取速率
                    long maxTransfer = Math.min(heatStorage.getMaxExtract(), 
                                               adjacentHeat.getMaxHeatStored() - adjacentHeat.getHeatStored());
                    long heatToTransfer = Math.min(maxTransfer, tempDiff / 10);
                    
                    if (heatToTransfer > 0) {
                        long extracted = heatStorage.extractHeat(heatToTransfer, false);
                        if (extracted > 0) {
                            long received = adjacentHeat.receiveHeat(extracted, false);
                            if (received < extracted) {
                                heatStorage.receiveHeat(extracted - received, false);
                            }
                            setChanged();
                        }
                    }
                }
            }
        }
    }
    
    /**
     * 获取热能存储能力
     * 子类可以重写以提供方向特定的行为
     */
    @Nullable
    public IHeatStorage getHeatStorageCapability(@Nullable Direction side) {
        return heatStorage;
    }
    
    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLong("heat", heatStorage.getHeatStored());
    }
    
    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("heat", net.minecraft.nbt.Tag.TAG_INT)) {
            heatStorage.setHeat(tag.getInt("heat"));
        }
    }
    
    @Override
    public Component getDisplayName() {
        return Component.translatable("container.mio_icif.heat_block");
    }
    
    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        // 基础热能方块不提供菜单，子类应该覆盖此方法
    return null;
    }
}