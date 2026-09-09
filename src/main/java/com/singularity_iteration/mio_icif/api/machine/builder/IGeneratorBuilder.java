package com.singularity_iteration.mio_icif.api.machine.builder;

import com.singularity_iteration.mio_icif.api.machine.builder.IMachineBuilderAPI.IMachineDefinition;
import com.singularity_iteration.mio_icif.api.energy.ICableTier;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * 发电机构建器
 *
 * <p>用于创建符合 mio_icif 风格的发电机。
 *
 * <p>使用示例：
 * <pre>{@code
 * CustomGeneratorBlock generator = builder
 *     .setName("advanced_solar")
 *     .setGenerationRate(128)
 *     .setEnergyCapacity(100000)
 *     .setCableTier(ICableTier tier)
 *     .setGeneratorType(GeneratorType.SOLAR)
 *     .setRequiresSky(true)
 *     .setDayOnly(true)
 *     .addBatterySlot(0)
 *     .setBlockProperties(BlockBehaviour.Properties.of().strength(2.0f))
 *     .build();
 * }</pre>
 */
public interface IGeneratorBuilder {

    // ========== 基础配置 ==========

    IGeneratorBuilder setName(String name);

    IGeneratorBuilder setTranslationKey(String translationKey);

    IGeneratorBuilder setBlockProperties(BlockBehaviour.Properties properties);

    // ========== 能量配置 ==========

    /**
     * 设置发电速率 (EU/t)
     */
    IGeneratorBuilder setGenerationRate(long generationRate);

    /**
     * 设置能量容量
     */
    IGeneratorBuilder setEnergyCapacity(long capacity);

    /**
     * 设置最大输出速率
     */
    IGeneratorBuilder setMaxExtract(long maxExtract);

    /**
     * 设置电缆等级
     */
    IGeneratorBuilder setCableTier(ICableTier tier);

    /**
     * 使用默认模板配置
     */
    IGeneratorBuilder useDefaultsForTier(ICableTier tier);

    // ========== 发电机类型配置 ==========

    /**
     * 设置发电机类型
     */
    IGeneratorBuilder setGeneratorType(GeneratorType type);

    /**
     * 设置是否需要天空（太阳能等）
     */
    IGeneratorBuilder setRequiresSky(boolean requiresSky);

    /**
     * 设置是否只在白天工作
     */
    IGeneratorBuilder setDayOnly(boolean dayOnly);

    /**
     * 设置是否只在夜晚工作
     */
    IGeneratorBuilder setNightOnly(boolean nightOnly);

    /**
     * 设置是否受天气影响
     */
    IGeneratorBuilder setWeatherDependent(boolean weatherDependent);

    /**
     * 设置是否需要燃料
     */
    IGeneratorBuilder setRequiresFuel(boolean requiresFuel);

    /**
     * 设置是否需要流体燃料
     */
    IGeneratorBuilder setRequiresFluidFuel(boolean requiresFluidFuel);

    /**
     * 设置是否需要转子（风力/水力）
     */
    IGeneratorBuilder setRequiresRotor(boolean requiresRotor);

    // ========== 槽位配置 ==========

    /**
     * 添加燃料槽
     */
    IGeneratorBuilder addFuelSlot(int index);

    /**
     * 添加电池槽（输出充电）
     */
    IGeneratorBuilder addBatterySlot(int index);

    /**
     * 添加流体槽
     */
    IGeneratorBuilder addFluidTank(int capacity);

    // ========== 效率配置 ==========

    /**
     * 设置基础效率
     */
    IGeneratorBuilder setBaseEfficiency(double efficiency);

    /**
     * 设置效率随高度变化（风力发电机）
     */
    IGeneratorBuilder setHeightEfficiency(boolean heightEfficiency);

    /**
     * 设置效率随生物群系变化
     */
    IGeneratorBuilder setBiomeEfficiency(boolean biomeEfficiency);

    /**
     * 设置机器类型
     */
    IGeneratorBuilder setMachineType(com.singularity_iteration.mio_icif.api.machine.IMachineAPI.MachineType machineType);

    // ========== 事件回调 ==========

    /**
     * 设置发电开始回调
     */
    IGeneratorBuilder onGenerationStart(GenerationCallback callback);

    /**
     * 设置每 tick 发电回调
     */
    IGeneratorBuilder onGenerate(GenerationCallback callback);

    /**
     * 设置燃料消耗回调
     */
    IGeneratorBuilder onFuelConsume(FuelConsumeCallback callback);

    // ========== 构建 ==========

    /**
     * 设置已注册的 BlockEntityType
     *
     * <p>附属模组在自己的模组初始化阶段创建 BlockEntityType 后，
     * 通过此方法将其关联到构建器，使 {@link #build()} 返回的定义包含有效的 entityType。
     *
     * @param entityType 已注册的 BlockEntityType
     * @return 构建器自身
     */
    IGeneratorBuilder withEntityType(net.minecraft.world.level.block.entity.BlockEntityType<?> entityType);

    IMachineDefinition build();

    /**
     * 构建并注册发电机
     *
     * <p>如果已通过 {@link #withEntityType(BlockEntityType)} 设置了 BlockEntityType，
     * 返回的定义将包含该 entityType，可直接使用。
     *
     * @param modId 模组 ID
     * @return 机器定义
     */
    IMachineDefinition buildAndRegister(String modId);

    /**
     * 获取机器配置记录
     *
     * <p>提供创建机器所需的所有参数，供附属模组自行注册 BlockEntityType。
     *
     * @return 机器配置记录
     */
    IMachineBuilderAPI.MachineConfiguration getConfiguration();

    // ========== 枚举和回调 ==========

    enum GeneratorType {
        SOLAR,          // 太阳能
        WIND,           // 风力
        WATER,          // 水力
        THERMAL,        // 热力
        KINETIC,        // 动能
        NUCLEAR,        // 核能
        SEMI_FLUID,     // 半流体
        GEO,            // 地热
        RTG,            // 放射性同位素
        FUSION,         // 聚变
        CUSTOM          // 自定义
    }

    @FunctionalInterface
    interface GenerationCallback {
        void onGenerate(net.minecraft.world.level.Level world, net.minecraft.core.BlockPos pos, long amount);
    }

    @FunctionalInterface
    interface FuelConsumeCallback {
        void onFuelConsume(net.minecraft.world.level.Level world, net.minecraft.core.BlockPos pos, net.minecraft.world.item.ItemStack fuel, int burnTime);
    }
}