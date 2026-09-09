package com.singularity_iteration.mio_icif.api.machine.builder;

import com.singularity_iteration.mio_icif.api.machine.builder.IMachineBuilderAPI.IMachineDefinition;
import com.singularity_iteration.mio_icif.api.energy.ICableTier;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.state.BlockBehaviour;


/**
 * 电力机器构建器
 *
 * <p>使用 Builder 模式创建符合 mio_icif 风格的电力机器。
 * 提供流畅的 API，让附属模组可以一步步配置机器属性。
 *
 * <p>使用示例：
 * <pre>{@code
 * CustomMachineBlock machine = builder
 *     .setName("advanced_compressor")
 *     .setTranslationKey("block.mymod.advanced_compressor")
 *     .setEnergyCapacity(50000)
 *     .setEnergyPerTick(128)
 *     .setProcessTime(100)
 *     .addInputSlot(0, 64)
 *     .addOutputSlot(1, 64)
 *     .addBatterySlot(2)
 *     .addUpgradeSlots(3, 4)
 *     .setCableTier(ICableTier tier)
 *     .setRecipeType(MioIcifAPI.instance().getRecipeAPI().getCompressorRecipeType())
 *     .setBlockProperties(BlockBehaviour.Properties.of().strength(3.5f))
 *     .onWorkStart((world, pos) -> {
 *         // 自定义工作开始逻辑
 *     })
 *     .onWorkComplete((world, pos, output) -> {
 *         // 自定义工作完成逻辑
 *     })
 *     .build();
 * }</pre>
 */
public interface IElectricMachineBuilder {

    // ========== 基础配置 ==========

    /**
     * 设置机器 ID（唯一标识符）
     *
     * @param name 机器名称
     * @return 构建器自身，用于链式调用
     */
    IElectricMachineBuilder setName(String name);

    /**
     * 设置翻译键
     *
     * @param translationKey 翻译键
     * @return 构建器自身
     */
    IElectricMachineBuilder setTranslationKey(String translationKey);

    /**
     * 设置方块属性
     *
     * @param properties 方块属性
     * @return 构建器自身
     */
    IElectricMachineBuilder setBlockProperties(BlockBehaviour.Properties properties);

    // ========== 能量配置 ==========

    /**
     * 设置能量容量
     *
     * @param capacity 容量 (EU)
     * @return 构建器自身
     */
    IElectricMachineBuilder setEnergyCapacity(long capacity);

    /**
     * 设置最大接收速率
     *
     * @param maxReceive 最大接收 (EU/t)
     * @return 构建器自身
     */
    IElectricMachineBuilder setMaxReceive(long maxReceive);

    /**
     * 设置最大输出速率
     *
     * @param maxExtract 最大输出 (EU/t)
     * @return 构建器自身
     */
    IElectricMachineBuilder setMaxExtract(long maxExtract);

    /**
     * 设置每 tick 能耗
     *
     * @param energyPerTick 能耗 (EU/t)
     * @return 构建器自身
     */
    IElectricMachineBuilder setEnergyPerTick(long energyPerTick);

    /**
     * 设置电缆等级（自动配置相关能量参数）
     *
     * @param tier 电缆等级
     * @return 构建器自身
     */
    IElectricMachineBuilder setCableTier(ICableTier tier);

    /**
     * 使用默认模板配置（根据等级自动设置能量参数）
     *
     * @param tier 电缆等级
     * @return 构建器自身
     */
    IElectricMachineBuilder useDefaultsForTier(ICableTier tier);

    // ========== 工作配置 ==========

    /**
     * 设置处理时间
     *
     * @param processTime 处理时间（刻）
     * @return 构建器自身
     */
    IElectricMachineBuilder setProcessTime(int processTime);

    /**
     * 设置配方类型
     *
     * @param recipeType 配方类型
     * @return 构建器自身
     */
    IElectricMachineBuilder setRecipeType(RecipeType<?> recipeType);

    /**
     * 设置是否支持升级
     *
     * @param supportsUpgrades true 支持升级
     * @return 构建器自身
     */
    IElectricMachineBuilder setSupportsUpgrades(boolean supportsUpgrades);

    // ========== 槽位配置 ==========

    /**
     * 添加输入槽
     *
     * @param index 槽位索引
     * @param maxStackSize 最大堆叠数
     * @return 构建器自身
     */
    IElectricMachineBuilder addInputSlot(int index, int maxStackSize);

    /**
     * 添加输出槽
     *
     * @param index 槽位索引
     * @param maxStackSize 最大堆叠数
     * @return 构建器自身
     */
    IElectricMachineBuilder addOutputSlot(int index, int maxStackSize);

    /**
     * 添加电池槽
     *
     * @param index 槽位索引
     * @return 构建器自身
     */
    IElectricMachineBuilder addBatterySlot(int index);

    /**
     * 添加升级槽
     *
     * @param startIndex 起始索引
     * @param count 数量
     * @return 构建器自身
     */
    IElectricMachineBuilder addUpgradeSlots(int startIndex, int count);

    /**
     * 使用标准槽位布局
     *
     * @param inputSlots 输入槽数量
     * @param outputSlots 输出槽数量
     * @param hasBattery 是否有电池槽
     * @param upgradeSlots 升级槽数量
     * @return 构建器自身
     */
    IElectricMachineBuilder useStandardLayout(int inputSlots, int outputSlots, boolean hasBattery, int upgradeSlots);

    // ========== 流体配置 ==========

    /**
     * 设置是否支持流体
     *
     * @param supportsFluids true 支持流体
     * @return 构建器自身
     */
    IElectricMachineBuilder setSupportsFluids(boolean supportsFluids);

    /**
     * 添加流体槽
     *
     * @param capacity 容量 (mB)
     * @return 构建器自身
     */
    IElectricMachineBuilder addFluidTank(int capacity);

    /**
     * 设置机器类型
     *
     * @param machineType 机器类型
     * @return 构建器自身
     */
    IElectricMachineBuilder setMachineType(com.singularity_iteration.mio_icif.api.machine.IMachineAPI.MachineType machineType);

    // ========== 事件回调 ==========

    /**
     * 设置工作开始回调
     *
     * @param callback 回调函数
     * @return 构建器自身
     */
    IElectricMachineBuilder onWorkStart(MachineWorkCallback callback);

    /**
     * 设置工作完成回调
     *
     * @param callback 回调函数
     * @return 构建器自身
     */
    IElectricMachineBuilder onWorkComplete(MachineWorkCompleteCallback callback);

    /**
     * 设置每 tick 更新回调
     *
     * @param callback 回调函数
     * @return 构建器自身
     */
    IElectricMachineBuilder onTick(MachineTickCallback callback);

    // ========== 构建 ==========

    /**
     * 设置已注册的 BlockEntityType
     *
     * <p>附属模组在自己的模组初始化阶段创建 BlockEntityType 后，
     * 通过此方法将其关联到构建器，使 {@link #build()} 返回的定义包含有效的 entityType。
     *
     * <p>使用示例：
     * <pre>{@code
     * // 在模组初始化阶段
     * public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GenericMachineBlockEntity>> MY_MACHINE =
     *     BLOCK_ENTITIES.register("my_machine", () ->
     *         BlockEntityType.Builder.of(GenericMachineBlockEntity::new, ModBlocks.MY_MACHINE.get()).build(null));
     *
     * var definition = builder.createElectricMachineBuilder()
     *     .setName("my_machine")
     *     .setEnergyCapacity(10000)
     *     .withEntityType(MY_MACHINE.get())  // 关联已注册的 BlockEntityType
     *     .build();
     * }</pre>
     *
     * @param entityType 已注册的 BlockEntityType
     * @return 构建器自身
     */
    IElectricMachineBuilder withEntityType(net.minecraft.world.level.block.entity.BlockEntityType<?> entityType);

    /**
     * 构建机器定义
     *
     * @return 机器定义
     */
    IMachineDefinition build();

    /**
     * 构建并注册机器
     *
     * <p>如果已通过 {@link #withEntityType(BlockEntityType)} 设置了 BlockEntityType，
     * 返回的定义将包含该 entityType，可直接使用。
     * 否则返回的定义中 entityType 为 null，附属模组需自行注册。
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
    interface MachineWorkCallback {
        void onWorkStart(net.minecraft.world.level.Level world, net.minecraft.core.BlockPos pos);
    }

    @FunctionalInterface
    interface MachineWorkCompleteCallback {
        void onWorkComplete(net.minecraft.world.level.Level world, net.minecraft.core.BlockPos pos, net.minecraft.world.item.ItemStack output);
    }

    @FunctionalInterface
    interface MachineTickCallback {
        void onTick(net.minecraft.world.level.Level world, net.minecraft.core.BlockPos pos, int progress, int maxProgress);
    }
}