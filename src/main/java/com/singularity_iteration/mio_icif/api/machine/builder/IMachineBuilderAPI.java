package com.singularity_iteration.mio_icif.api.machine.builder;

import com.singularity_iteration.mio_icif.api.energy.ICableTier;
import com.singularity_iteration.mio_icif.api.machine.ISlotType;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * 机器构建 API
 *
 * <p>提供工厂方法，让附属模组可以方便地创建符合 mio_icif 风格的机器。
 * 这是附属模组开发的核心 API，封装了机器创建的所有复杂逻辑。
 *
 * <p>使用示例：
 * <pre>{@code
 * // 获取 API
 * IMachineBuilderAPI builder = MioIcifAPI.instance().getMachineBuilderAPI();
 *
 * // 创建一个简单的电力机器
 * CustomMachineBlock myMachine = builder.createElectricMachineBuilder()
 *     .setName("my_machine")
 *     .setEnergyCapacity(10000)
 *     .setEnergyPerTick(32)
 *     .setProcessTime(200)
 *     .addInputSlot(0, 64)
 *     .addOutputSlot(1, 64)
 *     .addBatterySlot(2)
 *     .setCableTier(ICableTier tier)
 *     .setRecipeType(MioIcifAPI.instance().getRecipeAPI().getCompressorRecipeType())
 *     .build();
 *
 * // 注册机器
 * builder.registerMachine("mymod", myMachine);
 * }</pre>
 */
public interface IMachineBuilderAPI {

    /**
     * 创建电力机器构建器
     *
     * @return 新的电力机器构建器实例
     */
    IElectricMachineBuilder createElectricMachineBuilder();

    /**
     * 创建发电机构建器
     *
     * @return 新的发电机构建器实例
     */
    IGeneratorBuilder createGeneratorBuilder();

    /**
     * 创建热能机器构建器
     *
     * @return 新的热能机器构建器实例
     */
    IHeatMachineBuilder createHeatMachineBuilder();

    /**
     * 创建动能机器构建器
     *
     * @return 新的动能机器构建器实例
     */
    IKineticMachineBuilder createKineticMachineBuilder();

    /**
     * 创建储能方块构建器
     *
     * @return 新的储能方块构建器实例
     */
    IEnergyContainerBuilder createEnergyContainerBuilder();

    /**
     * 注册自定义机器到系统
     *
     * <p><strong>注意：此方法仅将机器定义存入内部注册表，不会注册到 NeoForge 的任何注册表中。</strong>
     * 附属模组需要自行注册 Block、BlockEntity 和 BlockEntityType。
     * 此方法主要用于：
     * <ul>
     *   <li>让机器可以通过 API 查询（如 getMachineDefinition）</li>
     *   <li>让机器可以使用升级系统</li>
     *   <li>让机器可以使用能量网络</li>
     * </ul>
     *
     * @param modId 附属模组 ID
     * @param machine 机器定义
     * @return 注册后的 BlockEntityType（可能为 null）
     */
    BlockEntityType<?> registerMachine(String modId, IMachineDefinition machine);

    /**
     * 注册自定义机器到系统（带 BlockEntityType）
     *
     * <p>由于 NeoForge 要求 BlockEntityType 在模组初始化阶段注册，
     * 附属模组需要先创建自己的 BlockEntityType，然后通过此方法注册。
     *
     * @param modId 附属模组 ID
     * @param machine 机器定义
     * @param blockEntityType 已注册的 BlockEntityType
     * @return 传入的 BlockEntityType
     */
    default BlockEntityType<?> registerMachine(String modId, IMachineDefinition machine, BlockEntityType<?> blockEntityType) {
        // 创建一个带有 BlockEntityType 的新定义
        IMachineDefinition definitionWithEntity = new IMachineDefinition() {
            @Override
            public String getId() { return machine.getId(); }
            @Override
            public String getModId() { return modId; }
            @Override
            public String getTranslationKey() { return machine.getTranslationKey(); }
            @Override
            public BlockEntityType<?> getBlockEntityType() { return blockEntityType; }
            @Override
            public IMachineProperties getProperties() { return machine.getProperties(); }
            @Override
            public MachineConfiguration getConfiguration() { return machine.getConfiguration(); }
        };
        return registerMachine(modId, definitionWithEntity);
    }

    /**
     * 获取已注册的机器定义
     *
     * @param modId 模组 ID
     * @param machineId 机器 ID
     * @return 机器定义，如果不存在则返回 null
     */
    IMachineDefinition getMachineDefinition(String modId, String machineId);

    /**
     * 检查机器 ID 是否已注册
     *
     * @param modId 模组 ID
     * @param machineId 机器 ID
     * @return true 如果已注册
     */
    boolean isMachineRegistered(String modId, String machineId);

    /**
     * 注册一个命名自定义机器类型。
     *
     * <p><b>注意：</b>{@link com.singularity_iteration.mio_icif.api.machine.IMachineAPI.MachineType}
     * 是固定枚举（含 {@code CUSTOM}），无法在运行时新增枚举常量。因此此方法仅将
     * {@code modId:name} 登记到全局自定义类型名称注册表，并返回 {@code CUSTOM}。
     * 附属模组可通过 {@link #getRegisteredCustomMachineTypeName} 取回登记的名称，
     * 用于在 UI/日志中展示带名称的自定义类型。</p>
     *
     * @param modId 附属模组 ID
     * @param name 自定义类型名称
     * @return 始终为 {@link com.singularity_iteration.mio_icif.api.machine.IMachineAPI.MachineType#CUSTOM}
     */
    com.singularity_iteration.mio_icif.api.machine.IMachineAPI.MachineType registerCustomMachineType(String modId, String name);

    /**
     * 查询已登记的自定义机器类型名称。
     *
     * @param modId 附属模组 ID
     * @param name 自定义类型名称
     * @return 登记的名称，未登记则返回空 Optional
     */
    Optional<String> getRegisteredCustomMachineTypeName(String modId, String name);

    /**
     * 创建标准槽位布局（输入+输出+电池+升级）
     *
     * @param inputSlots 输入槽数量
     * @param outputSlots 输出槽数量
     * @param hasBatterySlot 是否有电池槽
     * @param upgradeSlots 升级槽数量
     * @return 槽位布局定义
     */
    ISlotLayout createStandardLayout(int inputSlots, int outputSlots, boolean hasBatterySlot, int upgradeSlots);

    /**
     * 创建带特殊槽位的布局
     *
     * @param inputSlots 输入槽数量
     * @param outputSlots 输出槽数量
     * @param hasBatterySlot 是否有电池槽
     * @param upgradeSlots 升级槽数量
     * @param specialSlots 特殊槽位配置
     * @return 槽位布局定义
     */
    ISlotLayout createLayoutWithSpecialSlots(int inputSlots, int outputSlots, boolean hasBatterySlot, int upgradeSlots, ISlotLayout.SlotType[] specialSlots);

    /**
     * 创建流体机器槽位布局（输入+输出+电池+升级+流体）
     *
     * @param inputSlots 输入槽数量
     * @param outputSlots 输出槽数量
     * @param hasBatterySlot 是否有电池槽
     * @param upgradeSlots 升级槽数量
     * @param fluidTanks 流体槽数量
     * @param tankCapacity 每个流体槽容量 (mB)
     * @return 槽位布局定义
     */
    ISlotLayout createFluidLayout(int inputSlots, int outputSlots, boolean hasBatterySlot, int upgradeSlots, int fluidTanks, int tankCapacity);

    /**
     * 获取机器默认属性模板
     *
     * @param tier 电缆等级
     * @return 默认属性
     */
    IMachineDefaults getDefaultsForTier(ICableTier tier);

    /**
     * 机器定义接口 - 描述一个完整的机器
     */
    interface IMachineDefinition {
        String getId();
        String getModId();
        String getTranslationKey();

        /**
         * 获取机器的 BlockEntityType。
         *
         * <p>注意：通过 {@link IElectricMachineBuilder#build()} 等方法创建的机器定义，
         * 此方法可能返回 {@code null}。附属模组需要自己创建 BlockEntityType 并调用
         * {@link IMachineBuilderAPI#registerMachine(String, IMachineDefinition, BlockEntityType)}
         * 来完成注册。
         *
         * <p>使用示例：
         * <pre>{@code
         * // 在模组初始化阶段创建 BlockEntityType
         * public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GenericMachineBlockEntity>> MY_MACHINE =
         *     BLOCK_ENTITIES.register("my_machine", () ->
         *         BlockEntityType.Builder.of(GenericMachineBlockEntity::new, ModBlocks.MY_MACHINE.get()).build(null));
         *
         * // 创建机器定义
         * IMachineDefinition definition = builder.createElectricMachineBuilder()
         *     .setName("my_machine")
         *     .setEnergyCapacity(10000)
         *     .build();
         *
         * // 注册机器（传入 BlockEntityType）
         * builder.registerMachine("mymod", definition, MY_MACHINE.get());
         * }</pre>
         *
         * @return BlockEntityType，如果未提供则返回 null
         */
        @Nullable
        BlockEntityType<?> getBlockEntityType();
        IMachineProperties getProperties();

        /**
         * 获取创建此机器时使用的完整配置记录。
         *
         * <p>由于 NeoForge 要求 BlockEntityType 在模组初始化阶段注册，
         * {@link #getBlockEntityType()} 可能为 null。附属模组可通过此方法
         * 获取配置后自行创建 BlockEntityType。
         *
         * @return 机器配置记录
         */
        MachineConfiguration getConfiguration();
    }

    /**
     * 机器属性接口
     */
    interface IMachineProperties {
        long getEnergyCapacity();
        long getMaxReceive();
        long getMaxExtract();
        long getEnergyPerTick();
        int getProcessTime();
        ICableTier getCableTier();
        ISlotLayout getSlotLayout();
        boolean supportsUpgrades();
        boolean supportsFluids();
    }

    /**
     * 槽位布局接口（构建器专用）
     *
     * <p>继承 {@link com.singularity_iteration.mio_icif.api.machine.ISlotLayout}，添加构建器所需的额外方法。
     * 通过 default 方法桥接两套 API，使构建器布局可直接传给查询 API。
     */
    interface ISlotLayout extends com.singularity_iteration.mio_icif.api.machine.ISlotLayout {

        // ========== Builder-side methods (implementor MUST implement) ==========
        // These are the "source" methods; the query-side methods below delegate to them.

        int getTotalSlots();
        int getInputSlotCount();
        int getOutputSlotCount();
        int getUpgradeSlotCount();
        int getBatterySlotIndex();
        int getUpgradeSlotStart();
        boolean hasBatterySlot();
        boolean hasFluidTanks();
        int getFluidTankCount();
        int getFluidTankCapacity();
        int getStart(SlotType type);
        boolean isType(int slot, SlotType type);
        SlotType getSlotType(int slot);
        
        int getCountByBuilderType(SlotType type);
        int[] getSlotsOfBuilderType(SlotType type);

        // ========== Bridge: parent query methods → builder implementation (one-way) ==========
        // Only this direction is default; no reverse delegation to avoid infinite recursion.

        @Override
        default int getTotalCount() { return getTotalSlots(); }
        @Override
        default int getInputCount() { return getInputSlotCount(); }
        @Override
        default int getOutputCount() { return getOutputSlotCount(); }
        @Override
        default int getUpgradeCount() { return getUpgradeSlotCount(); }
        @Override
        default int getBatteryCount() { return hasBatterySlot() ? 1 : 0; }

        @Override
        default com.singularity_iteration.mio_icif.api.machine.ISlotType getType(int slotIndex) {
            return getSlotType(slotIndex);
        }

        @Override
        default int getCount(com.singularity_iteration.mio_icif.api.machine.ISlotType type) {
            if (type instanceof SlotType st) return getCountByBuilderType(st);
            return 0;
        }

        @Override
        default int[] getSlotsOfType(com.singularity_iteration.mio_icif.api.machine.ISlotType type) {
            if (type instanceof SlotType st) return getSlotsOfBuilderType(st);
            return new int[0];
        }

        @Override
        default int[] getInputSlots() { return getSlotsOfBuilderType(SlotType.INPUT); }
        @Override
        default int[] getOutputSlots() { return getSlotsOfBuilderType(SlotType.OUTPUT); }
        @Override
        default int[] getBatterySlots() { return getSlotsOfBuilderType(SlotType.BATTERY); }
        @Override
        default int[] getUpgradeSlots() { return getSlotsOfBuilderType(SlotType.UPGRADE); }

        /**
         * 槽位类型枚举
         * 
         * <p>构建器专用槽位类型，包含更多 specialized 的槽位类型
         * （如 HAMMER_HEAD, CUTTER_HEAD 等）。
         * 
         * <p>与 {@link com.singularity_iteration.mio_icif.api.machine.IMachineAPI.SlotType} 的映射：
         * <ul>
         *   <li>HAMMER_HEAD → HAMMER_TOOL, HAMMER_PLATE → HAMMER_INPUT, HAMMER_HANDLE → HAMMER_OUTPUT</li>
         *   <li>CUTTER_HEAD → CUTTER_TOOL, CUTTER_PLATE → CUTTER_INPUT, CUTTER_HANDLE → CUTTER_OUTPUT</li>
         *   <li>其他类型名称相同</li>
         * </ul>
         */
        enum SlotType implements ISlotType {
            INPUT,
            OUTPUT,
            UPGRADE,
            BATTERY,
            TOOL,
            SCANNER,
            MEMORY,
            FLUID_INPUT,
            FLUID_OUTPUT,
            FUEL,
            EXTRA,
            ROTOR,
            TURBINE,
            REACTOR,
            COIL,
            HEATING,
            NUCLEAR,
            DRILL,
            MINING_PIPE,
            FILTER,
            HEAT_CONDUCTOR,
            HAMMER_HEAD,
            HAMMER_PLATE,
            HAMMER_HANDLE,
            CUTTER_HEAD,
            CUTTER_PLATE,
            CUTTER_HANDLE,
            RTG_PELLET,
            CUSTOM;

            @Override
            public String getName() {
                return name().toLowerCase();
            }

            @Override
            public boolean isInput() {
                return this == INPUT || this == FLUID_INPUT || this == HAMMER_PLATE || this == CUTTER_PLATE;
            }

            @Override
            public boolean isOutput() {
                return this == OUTPUT || this == FLUID_OUTPUT || this == HAMMER_HANDLE || this == CUTTER_HANDLE;
            }

            @Override
            public boolean isBattery() {
                return this == BATTERY;
            }

            @Override
            public boolean isUpgrade() {
                return this == UPGRADE;
            }

            @Override
            public boolean isExtra() {
                return this == EXTRA;
            }

            /**
             * 映射到通用查询 API 的 SlotType
             */
            public com.singularity_iteration.mio_icif.api.machine.IMachineAPI.SlotType toApiSlotType() {
                return switch (this) {
                    case HAMMER_HEAD -> com.singularity_iteration.mio_icif.api.machine.IMachineAPI.SlotType.HAMMER_TOOL;
                    case HAMMER_PLATE -> com.singularity_iteration.mio_icif.api.machine.IMachineAPI.SlotType.HAMMER_INPUT;
                    case HAMMER_HANDLE -> com.singularity_iteration.mio_icif.api.machine.IMachineAPI.SlotType.HAMMER_OUTPUT;
                    case CUTTER_HEAD -> com.singularity_iteration.mio_icif.api.machine.IMachineAPI.SlotType.CUTTER_TOOL;
                    case CUTTER_PLATE -> com.singularity_iteration.mio_icif.api.machine.IMachineAPI.SlotType.CUTTER_INPUT;
                    case CUTTER_HANDLE -> com.singularity_iteration.mio_icif.api.machine.IMachineAPI.SlotType.CUTTER_OUTPUT;
                    default -> {
                        try {
                            yield com.singularity_iteration.mio_icif.api.machine.IMachineAPI.SlotType.valueOf(this.name());
                        } catch (IllegalArgumentException e) {
                            yield com.singularity_iteration.mio_icif.api.machine.IMachineAPI.SlotType.CUSTOM;
                        }
                    }
                };
            }
        }
    }

    /**
     * 机器默认属性
     */
    interface IMachineDefaults {
        long getDefaultCapacity();
        long getDefaultMaxReceive();
        long getDefaultMaxExtract();
        long getDefaultEnergyPerTick();
        int getDefaultProcessTime();
        int getDefaultUpgradeSlots();
    }

    /**
     * 机器配置记录
     *
     * <p>包含创建机器所需的所有参数。当附属模组使用构建器时，
     * 可以通过此记录获取完整的机器配置，然后使用这些参数创建自己的 Block、BlockEntity 和 BlockEntityType。
     *
     * @param name 机器名称
     * @param translationKey 翻译键
     * @param blockProperties 方块属性
     * @param energyCapacity 能量容量
     * @param maxReceive 最大接收速率
     * @param maxExtract 最大提取速率
     * @param energyPerTick 每tick能耗
     * @param cableTier 电缆等级。注意：对于非电力机器（热能/动能），此字段为 null
     * @param processTime 处理时间（tick）
     * @param recipeType 配方类型
     * @param supportsUpgrades 是否支持升级
     * @param supportsFluids 是否支持流体
     * @param inputSlots 输入槽数量
     * @param outputSlots 输出槽数量
     * @param hasBatterySlot 是否有电池槽
     * @param upgradeSlots 升级槽数量
     * @param fluidTanks 流体槽数量
     * @param tankCapacity 流体槽容量
     * @param machineType 机器类型。注意：若构建器未调用 setMachineType() 则为此字段为 null
     */
    record MachineConfiguration(
        String name,
        String translationKey,
        net.minecraft.world.level.block.state.BlockBehaviour.Properties blockProperties,
        long energyCapacity,
        long maxReceive,
        long maxExtract,
        long energyPerTick,
        @Nullable ICableTier cableTier,
        int processTime,
        RecipeType<?> recipeType,
        boolean supportsUpgrades,
        boolean supportsFluids,
        int inputSlots,
        int outputSlots,
        boolean hasBatterySlot,
        int upgradeSlots,
        int fluidTanks,
        int tankCapacity,
        @Nullable com.singularity_iteration.mio_icif.api.machine.IMachineAPI.MachineType machineType
    ) {
        /**
         * 获取总槽位数
         */
        public int getTotalSlots() {
            return inputSlots + outputSlots + (hasBatterySlot ? 1 : 0) + upgradeSlots;
        }

        /**
         * 获取电池槽索引
         */
        public int getBatterySlotIndex() {
            return hasBatterySlot ? inputSlots + outputSlots : -1;
        }

        /**
         * 获取升级槽起始索引
         */
        public int getUpgradeSlotStart() {
            return inputSlots + outputSlots + (hasBatterySlot ? 1 : 0);
        }
    }
}