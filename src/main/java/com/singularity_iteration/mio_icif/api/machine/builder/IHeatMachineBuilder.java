package com.singularity_iteration.mio_icif.api.machine.builder;

import com.singularity_iteration.mio_icif.api.machine.builder.IMachineBuilderAPI.IMachineDefinition;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * 热能机器构建器
 *
 * <p>用于创建符合 mio_icif 风格的热能机器（HU系统）。
 *
 * <p>使用示例：
 * <pre>{@code
 * CustomHeatMachine machine = builder
 *     .setName("advanced_heat_exchanger")
 *     .setHeatCapacity(10000)
 *     .setHeatOutput(100)
 *     .setHeatTransferRate(50)
 *     .addInputSlot(0, 64)
 *     .addOutputSlot(1, 64)
 *     .setBlockProperties(BlockBehaviour.Properties.of().strength(3.0f))
 *     .build();
 * }</pre>
 */
public interface IHeatMachineBuilder {

    // ========== 基础配置 ==========

    IHeatMachineBuilder setName(String name);

    IHeatMachineBuilder setTranslationKey(String translationKey);

    IHeatMachineBuilder setBlockProperties(BlockBehaviour.Properties properties);

    // ========== 热能配置 ==========

    /**
     * 设置热量容量
     */
    IHeatMachineBuilder setHeatCapacity(long capacity);

    /**
     * 设置热量输出 (HU/t)
     */
    IHeatMachineBuilder setHeatOutput(long heatOutput);

    /**
     * 设置热量传输速率
     */
    IHeatMachineBuilder setHeatTransferRate(long transferRate);

    /**
     * 设置热量输入速率
     */
    IHeatMachineBuilder setHeatInputRate(long inputRate);

    /**
     * 设置工作温度
     */
    IHeatMachineBuilder setOperatingTemperature(long temperature);

    /**
     * 设置最大温度
     */
    IHeatMachineBuilder setMaxTemperature(long maxTemperature);

    // ========== 工作配置 ==========

    /**
     * 设置是否需要燃料
     */
    IHeatMachineBuilder setRequiresFuel(boolean requiresFuel);

    /**
     * 设置是否需要流体冷却
     */
    IHeatMachineBuilder setRequiresCooling(boolean requiresCooling);

    /**
     * 设置冷却液类型
     */
    IHeatMachineBuilder setCoolantType(String fluidId);

    // ========== 槽位配置 ==========

    IHeatMachineBuilder addInputSlot(int index, int maxStackSize);

    IHeatMachineBuilder addOutputSlot(int index, int maxStackSize);

    IHeatMachineBuilder addFuelSlot(int index);

    IHeatMachineBuilder addFluidTank(int capacity);

    /**
     * 设置机器类型
     */
    IHeatMachineBuilder setMachineType(com.singularity_iteration.mio_icif.api.machine.IMachineAPI.MachineType machineType);

    // ========== 事件回调 ==========

    IHeatMachineBuilder onHeatGenerate(HeatCallback callback);

    IHeatMachineBuilder onOverheat(OverheatCallback callback);

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
    IHeatMachineBuilder withEntityType(net.minecraft.world.level.block.entity.BlockEntityType<?> entityType);

    IMachineDefinition build();

    /**
     * 构建并注册热能机器
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

    // ========== 回调接口 ==========

    @FunctionalInterface
    interface HeatCallback {
        void onHeatGenerate(net.minecraft.world.level.Level world, net.minecraft.core.BlockPos pos, long amount);
    }

    @FunctionalInterface
    interface OverheatCallback {
        void onOverheat(net.minecraft.world.level.Level world, net.minecraft.core.BlockPos pos, long currentHeat, long maxHeat);
    }
}