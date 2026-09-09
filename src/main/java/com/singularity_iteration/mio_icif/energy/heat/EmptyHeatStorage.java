package com.singularity_iteration.mio_icif.energy.heat;

/**
 * 空的 HU 热能存储实现
 * 用于默认回退，不能存储、接收或提供热能
 * 
 * 使用示例：
 * <pre>{@code
 * IHeatStorage heatStorage = blockEntity.getCapability(HUCapabilities.HEAT_STORAGE, direction)
 *     .orElse(EmptyHeatStorage.INSTANCE);
 * // 使用 heatStorage 无需检查是否存续
 * }</pre>
 */
@SuppressWarnings("null")
public class EmptyHeatStorage implements IHeatStorage {
    
    public static final EmptyHeatStorage INSTANCE = new EmptyHeatStorage();
    
    protected EmptyHeatStorage() {}
    
    @Override
    public long receiveHeat(long maxReceive, boolean simulate) {
        return 0;
    }
    
    @Override
    public long extractHeat(long maxExtract, boolean simulate) {
        return 0;
    }
    
    @Override
    public long getHeatStored() {
        return 0;
    }
    
    @Override
    public long getMaxHeatStored() {
        return 0;
    }
    
    @Override
    public boolean canExtractHeat() {
        return false;
    }
    
    @Override
    public boolean canReceiveHeat() {
        return false;
    }
    
    @Override
    public int getTemperature() {
        return 20;
    }
    
    @Override
    public boolean isOverheated() {
        return false;
    }
    
    @Override
    public long getHeatLossPerTick() {
        return 0;
    }
}