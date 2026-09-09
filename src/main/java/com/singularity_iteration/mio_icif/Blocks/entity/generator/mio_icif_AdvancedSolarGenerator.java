package com.singularity_iteration.mio_icif.Blocks.entity.generator;

import com.singularity_iteration.mio_icif.energy.EnergyUnit.CableTier;
import net.minecraft.core.BlockPos;
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
        this.dayPower = dayPower;
        this.nightPower = nightPower;
        this.tier = tier;
        // 重新初始化能量存储
        this.energyStorage.setCapacity(capacity);
        // 设置正确的最大输出速率（根据电压等级）
        this.energyStorage.setMaxExtract(getCableTierFromIndexStatic(tier - 1).powerRating);
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
        // 检查是否在主世界
        if (!isOverworld(level)) {
            return false;
        }

        // 检查上方是否有非透明方块阻挡
        if (!hasClearSky(level, pos)) {
            return false;
        }

        // 检查天气（下雨或下雪时只能夜间发电）
        boolean isRaining = level.isRaining() || level.isThundering();
        
        // 检查时间
        long timeOfDay = level.getDayTime() % 24000;
        boolean isDay = timeOfDay >= GENERATION_START_TIME && timeOfDay <= GENERATION_END_TIME;

        if (isDay && !isRaining) {
            generationState = GenerationState.DAY;
        } else {
            generationState = GenerationState.NIGHT;
        }

        return true;
    }

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

                long energyExtracted = getEnergyStorageInternal().extract(energyToCharge, false);

                api.chargeBattery(chargeStack, energyExtracted, false);
                setChanged();
            }
        }
    }
}