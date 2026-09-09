package com.singularity_iteration.mio_icif.api.machine.builder;

import com.singularity_iteration.mio_icif.api.machine.builder.IMachineBuilderAPI.IMachineDefinition;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * 动能机器构建器
 *
 * <p>用于创建符合 mio_icif 风格的动能机器（KU系统）。
 *
 * <p>使用示例：
 * <pre>{@code
 * CustomKineticMachine machine = builder
 *     .setName("advanced_kinetic_generator")
 *     .setKineticCapacity(10000)
 *     .setKineticOutput(100)
 *     .setRequiresRotor(true)
 *     .setRotorType(RotorType.WIND)
 *     .addRotorSlot(0)
 *     .addBatterySlot(1)
 *     .setBlockProperties(BlockBehaviour.Properties.of().strength(3.0f))
 *     .build();
 * }</pre>
 */
public interface IKineticMachineBuilder {

    // ========== 基础配置 ==========

    IKineticMachineBuilder setName(String name);

    IKineticMachineBuilder setTranslationKey(String translationKey);

    IKineticMachineBuilder setBlockProperties(BlockBehaviour.Properties properties);

    // ========== 动能配置 ==========

    /**
     * 设置动能容量
     */
    IKineticMachineBuilder setKineticCapacity(long capacity);

    /**
     * 设置动能输出 (KU/t)
     */
    IKineticMachineBuilder setKineticOutput(long kineticOutput);

    /**
     * 设置动能传输速率
     */
    IKineticMachineBuilder setKineticTransferRate(long transferRate);

    /**
     * 设置动能输入速率
     */
    IKineticMachineBuilder setKineticInputRate(long inputRate);

    // ========== 转子配置 ==========

    /**
     * 设置是否需要转子
     */
    IKineticMachineBuilder setRequiresRotor(boolean requiresRotor);

    /**
     * 设置转子类型
     */
    IKineticMachineBuilder setRotorType(RotorType rotorType);

    /**
     * 设置转子损耗速率
     */
    IKineticMachineBuilder setRotorWearRate(int wearRate);

    // ========== 槽位配置 ==========

    IKineticMachineBuilder addRotorSlot(int index);

    IKineticMachineBuilder addBatterySlot(int index);

    IKineticMachineBuilder addInputSlot(int index, int maxStackSize);

    IKineticMachineBuilder addOutputSlot(int index, int maxStackSize);

    /**
     * 设置机器类型
     */
    IKineticMachineBuilder setMachineType(com.singularity_iteration.mio_icif.api.machine.IMachineAPI.MachineType machineType);

    // ========== 事件回调 ==========

    IKineticMachineBuilder onKineticGenerate(KineticCallback callback);

    IKineticMachineBuilder onRotorBreak(RotorBreakCallback callback);

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
    IKineticMachineBuilder withEntityType(net.minecraft.world.level.block.entity.BlockEntityType<?> entityType);

    IMachineDefinition build();

    /**
     * 构建并注册动能机器
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

    enum RotorType {
        WIND,       // 风力转子
        WATER,      // 水力转子
        STEAM,      // 蒸汽转子
        MANUAL,     // 手动转子
        STIRLING    // 斯特林转子
    }

    @FunctionalInterface
    interface KineticCallback {
        void onKineticGenerate(net.minecraft.world.level.Level world, net.minecraft.core.BlockPos pos, long amount);
    }

    @FunctionalInterface
    interface RotorBreakCallback {
        void onRotorBreak(net.minecraft.world.level.Level world, net.minecraft.core.BlockPos pos, net.minecraft.world.item.ItemStack rotor);
    }
}