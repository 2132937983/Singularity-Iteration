package com.miophas.singularity_iteration.common.blockentity.generator;

import com.miophas.singularity_iteration.core.api.energy.storage.CableTier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import com.miophas.singularity_iteration.common.energy.SolarGeneratorProfile;
import com.miophas.singularity_iteration.core.runtime.energy.SolarItemCharging;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 高级太阳能发电机
 * 继承自太阳能发电机，但具有更高的发电效率和能量存储
 * 日夜均可发电，白天发电效率高，夜晚发电效率较低
 */
@SuppressWarnings("null")
public class mio_icif_AdvancedSolarGenerator extends mio_icif_solar_generator {

    // 配置参数
    protected final int dayPower;      // 白天发电功率 (EU/t)
    protected final int nightPower;    // 夜晚发电功率 (EU/t)
    protected final int tier;          // 电压等级
    private final SolarGeneratorProfile scexProfile;

    // 发电状态
    protected GenerationState generationState = GenerationState.NONE;

    public enum GenerationState {
        NONE, NIGHT, DAY
    }

    /**
     * 构造函数
     * @param dayPower 白天发电功率 (EU/t)
     * @param nightPower 夜晚发电功率 (EU/t)
     * @param capacity 能量容量 (EU)
     * @param tier 电压等级
     */
    public mio_icif_AdvancedSolarGenerator(BlockPos pos, BlockState state, 
                                           int dayPower, int nightPower, 
                                           long capacity, int tier) {
        this(pos, state, dayPower, nightPower, capacity, tier, null);
    }

    /**
     * 构造函数（带 BlockEntityType）
     * @param dayPower 白天发电功率 (EU/t)
     * @param nightPower 夜晚发电功率 (EU/t)
     * @param capacity 能量容量 (EU)
     * @param tier 电压等级
     * @param type 方块实体类型
     */
    public mio_icif_AdvancedSolarGenerator(BlockPos pos, BlockState state, 
                                           int dayPower, int nightPower, 
                                           long capacity, int tier,
                                           BlockEntityType<?> type) {
        super(pos, state, type, getCableTierFromIndexStatic(tier - 1));
        this.scexProfile = energyStorage.scexNetworkControlled()
            ? SolarGeneratorProfile.find(BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString())
                .filter(SolarGeneratorProfile::discrete).orElseThrow() : null;
        this.dayPower = scexProfile != null ? scexProfile.dayPower() : dayPower;
        this.nightPower = scexProfile != null ? scexProfile.nightPower() : nightPower;
        this.tier = tier;
        // 重新初始化能量存储
        this.energyStorage.setCapacity(scexProfile != null ? scexProfile.capacity() : capacity);
        // 设置正确的最大输出速率（根据电压等级）
        this.energyStorage.setMaxExtract(scexProfile != null ? scexProfile.outputPacket() : getCableTierFromIndexStatic(tier - 1).powerRating);
        if (scexProfile != null) setAsPowerSource(this.dayPower);
    }

    /**
     * 静态方法获取 CableTier（用于构造函数）
     */
    private static CableTier getCableTierFromIndexStatic(int index) {
        return switch (index) {
            case 0 -> CableTier.LV;
            case 1 -> CableTier.MV;
            case 2 -> CableTier.HV;
            case 3 -> CableTier.EV;
            case 4 -> CableTier.IV;
            case 5 -> CableTier.LuV;
            case 6 -> CableTier.ZPMV;
            case 7 -> CableTier.UV;
            case 8 -> CableTier.UHV;
            case 9 -> CableTier.UEV;
            case 10 -> CableTier.UIV;
            case 11 -> CableTier.UXV;
            case 12 -> CableTier.OpV;
            case 13 -> CableTier.MAX;
            default -> CableTier.LV;
        };
    }

        /**
     * 根据索引获取 CableTier
     */
    protected CableTier getCableTierFromIndex(int index) {
        return switch (index) {
            case 0 -> CableTier.LV;
            case 1 -> CableTier.MV;
            case 2 -> CableTier.HV;
            case 3 -> CableTier.EV;
            case 4 -> CableTier.IV;
            case 5 -> CableTier.LuV;
            case 6 -> CableTier.ZPMV;
            case 7 -> CableTier.UV;
            case 8 -> CableTier.UHV;
            case 9 -> CableTier.UEV;
            case 10 -> CableTier.UIV;
            case 11 -> CableTier.UXV;
            case 12 -> CableTier.OpV;
            case 13 -> CableTier.MAX;
            default -> CableTier.LV;
        };
    }

    /**
     * 检查是否可以发电（高级版本支持日夜发电）
     */
    @Override
    public boolean canGenerate(Level level, BlockPos pos) {
        generationState = GenerationState.NONE;
        // 检查是否在主世界
        if (!hasSkyLight(level)) {
            return false;
        }

        // 检查上方是否有非透明方块阻挡
        if (!hasClearSky(level, pos)) {
            return false;
        }

        // 检查天气（下雨或下雪时只能夜间发电）
        boolean isRaining = level.isRaining() || level.isThundering();
        
        // 检查时间
        // Dawn/dusk cutoffs retain the SI candidate policy until the full-day batch.
        long timeOfDay = Math.floorMod(level.getDayTime(), 24000L);
        boolean isDay = timeOfDay >= GENERATION_START_TIME && timeOfDay <= GENERATION_END_TIME;

        if (isDay && !isRaining) {
            generationState = GenerationState.DAY;
        } else {
            generationState = GenerationState.NIGHT;
        }

        return true;
    }

    @Override
    protected float scexIndependentRate(Level level, BlockPos pos) {
        var before = generationState;
        boolean clear = canGenerate(level, pos);
        long rate = scexProfile.discreteOutput(clear, generationState == GenerationState.DAY, false);
        if (before != generationState) level.sendBlockUpdated(pos, getBlockState(), getBlockState(), 3);
        return rate;
    }

    @Override
    protected int scexRefreshTicks() { return scexProfile.refreshTicks(); }

    @Override
    protected boolean scexClampBasicBuffer() { return false; }

    /**
     * 获取当前发电功率
     */
    protected long getCurrentGenerationRate() {
        return switch (generationState) {
            case DAY -> dayPower;
            case NIGHT -> nightPower;
            case NONE -> 0;
        };
    }

    /**
     * 基础太阳能机的构造函数把 energyGenerationRate 固定为 1 EU/t，四个高级太阳能面板都沿用了
     * 那个构造函数，因此 IGeneratorBlock.getPowerOutput() 一直返回 1。这里改为报告当前日夜状态下的
     * 真实发电功率（Jade「输出功率」与机器 API 的 getEnergyPerTick 读的都是这个方法）。
     */
    @Override
    public long getPowerOutput() {
        return getCurrentGenerationRate();
    }

    /**
     * 生成能量（使用当前发电功率）
     */
    @Override
    protected void generateEnergy() {
        long rate = getCurrentGenerationRate();
        if (rate > 0) {
            long energyGenerated = Math.min(rate, 
                energyStorage.getCapacity() - energyStorage.getAmount());
            if (energyGenerated > 0) {
                apiGenerateEnergy(energyGenerated, false);
            }
        }
    }

    /**
     * 获取电压等级
     */
    public int getTier() {
        return tier;
    }

    /**
     * 获取白天发电功率
     */
    public int getDayPower() {
        return dayPower;
    }

    /**
     * 获取夜晚发电功率
     */
    public int getNightPower() {
        return nightPower;
    }

    /**
     * 获取当前发电状态
     */
    public GenerationState getGenerationState() {
        return generationState;
    }

    /**
     * 给所有4个电池槽充电
     * 重写父类方法，支持多个充电槽
     */
    @Override
    protected void chargeItems() {
        if (scexProfile != null) {
            if (SolarItemCharging.charge(itemHandler, scexProfile.chargeSlots(), energyStorage, getItemAPI())) setChanged();
            return;
        }
        // 遍历所有4个槽位
        for (int i = 0; i < 4; i++) {
            ItemStack chargeStack = itemHandler.getStackInSlot(i);
            if (chargeStack.isEmpty()) {
                continue;
            }

            // 检查物品是否是电池
            if (getItemAPI().isBattery(chargeStack)) {
                var api = getItemAPI();
                long currentEnergy = api.getBatteryStored(chargeStack);
                long batteryMaxEnergy = api.getBatteryCapacity(chargeStack);
                long batteryChargeRate = api.getChargeRate(chargeStack);

                if (currentEnergy >= batteryMaxEnergy) {
                    continue;
                }

                long availableEnergy = getEnergyStorage().getAmount();
                if (availableEnergy <= 0) {
                    continue;
                }

                long energyToCharge = Math.min(batteryChargeRate, batteryMaxEnergy - currentEnergy);
                energyToCharge = Math.min(energyToCharge, availableEnergy);
                energyToCharge = Math.min(energyToCharge, Math.max(0, api.chargeBattery(chargeStack, energyToCharge, true)));
                long energyExtracted = energyToCharge > 0 ? getEnergyStorageInternal().extract(energyToCharge, false) : 0;
                long energyStored = energyExtracted > 0 ? api.chargeBattery(chargeStack, energyExtracted, false) : 0;
                if (energyStored < energyExtracted) getEnergyStorageInternal().generateEnergyInternal(energyExtracted - energyStored, false);
                setChanged();
            }
        }
    }

    @Override
    public int[] getSlotsForFace(Direction side) { return new int[]{0, 1, 2, 3}; }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @org.jetbrains.annotations.Nullable Direction side) {
        return slot >= 0 && slot < Math.min(4, itemHandler.getSlots()) && isBattery(stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot >= 0 && slot < Math.min(4, itemHandler.getSlots());
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("scex_solar_state", generationState.ordinal());
    }

    private void scexReadState(CompoundTag tag) {
        int value = tag.getInt("scex_solar_state");
        generationState = value >= 0 && value < GenerationState.values().length ? GenerationState.values()[value] : GenerationState.NONE;
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        scexReadState(tag);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        var tag = super.getUpdateTag(registries);
        tag.putInt("scex_solar_state", generationState.ordinal());
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        super.handleUpdateTag(tag, registries);
        scexReadState(tag);
    }
}