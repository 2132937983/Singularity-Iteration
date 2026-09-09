package com.singularity_iteration.mio_icif.api.capability;

import com.singularity_iteration.mio_icif.api.energy.ICableTier;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.ItemCapability;

/**
 * mio_icif 能力系统 API
 *
 * <p>定义所有可供外部使用的能力接口，让附属模组可以通过 NeoForge 的 Capability 系统
 * 与本模组的方块和物品交互。
 *
 * <p>使用示例：
 * <pre>{@code
 * // 获取方块的 EU 能量存储
 * IEUStorage storage = level.getCapability(IMioIcifCapabilities.EU_STORAGE_BLOCK, pos, direction);
 * if (storage != null) {
 *     long stored = storage.getStored();
 *     long capacity = storage.getCapacity();
 * }
 *
 * // 获取物品的 EU 能量存储
 * IEUStorage itemStorage = stack.getCapability(IMioIcifCapabilities.EU_STORAGE_ITEM);
 * if (itemStorage != null) {
 *     long received = itemStorage.receiveEnergy(100, false);
 * }
 * }</pre>
 */
public interface IMioIcifCapabilities {

    // ========== 方块能力 ==========

    /**
     * EU 能量存储能力（方块）
     */
    BlockCapability<IEUStorage, Direction> EU_STORAGE_BLOCK = BlockCapability.create(
        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("mio_icif", "eu_storage"),
        IEUStorage.class,
        Direction.class
    );

    /**
     * 机器信息能力（方块）
     */
    BlockCapability<IMachineCapability, Direction> MACHINE_BLOCK = BlockCapability.create(
        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("mio_icif", "machine"),
        IMachineCapability.class,
        Direction.class
    );

    /**
     * 发电机信息能力（方块）
     */
    BlockCapability<IGeneratorCapability, Direction> GENERATOR_BLOCK = BlockCapability.create(
        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("mio_icif", "generator"),
        IGeneratorCapability.class,
        Direction.class
    );

    /**
     * 热能存储能力（方块）
     */
    BlockCapability<IHeatStorage, Direction> HEAT_STORAGE_BLOCK = BlockCapability.createSided(
        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("mio_icif", "api_heat_storage"),
        IHeatStorage.class
    );

    /**
     * 动能存储能力（方块）
     */
    BlockCapability<IKineticStorage, Direction> KINETIC_STORAGE_BLOCK = BlockCapability.createSided(
        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("mio_icif", "api_kinetic_storage"),
        IKineticStorage.class
    );

    // ========== 物品能力 ==========

    /**
     * EU 能量存储能力（物品）
     */
    ItemCapability<IEUStorage, Void> EU_STORAGE_ITEM = ItemCapability.create(
        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("mio_icif", "eu_storage_item"),
        IEUStorage.class,
        Void.class
    );

    /**
     * 电动物品能力
     */
    ItemCapability<ICapabilityElectricItem, Void> ELECTRIC_ITEM = ItemCapability.create(
        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("mio_icif", "electric_item"),
        ICapabilityElectricItem.class,
        Void.class
    );

    /**
     * 升级物品能力
     */
    ItemCapability<IUpgradeItem, Void> UPGRADE_ITEM = ItemCapability.create(
        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("mio_icif", "upgrade_item"),
        IUpgradeItem.class,
        Void.class
    );

    /**
     * 反应堆组件能力
     */
    ItemCapability<IReactorComponent, Void> REACTOR_COMPONENT = ItemCapability.create(
        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("mio_icif", "reactor_component"),
        IReactorComponent.class,
        Void.class
    );

    // ========== 新增方块能力 ==========

    /**
     * 变压器能力（方块）
     */
    BlockCapability<ITransformerCapability, Direction> TRANSFORMER_BLOCK = BlockCapability.create(
        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("mio_icif", "transformer"),
        ITransformerCapability.class,
        Direction.class
    );

    /**
     * 管道能力（方块）
     */
    BlockCapability<IPipeCapability, Direction> PIPE_BLOCK = BlockCapability.create(
        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("mio_icif", "pipe"),
        IPipeCapability.class,
        Direction.class
    );

    /**
     * 能量转换器能力（方块）
     */
    BlockCapability<IEnergyConverterCapability, Direction> ENERGY_CONVERTER_BLOCK = BlockCapability.create(
        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("mio_icif", "energy_converter"),
        IEnergyConverterCapability.class,
        Direction.class
    );

    // ========== 新增物品能力 ==========

    /**
     * 武器物品能力
     */
    ItemCapability<IWeaponCapability, Void> WEAPON_ITEM = ItemCapability.create(
        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("mio_icif", "weapon"),
        IWeaponCapability.class,
        Void.class
    );

    /**
     * 太阳能头盔能力
     */
    ItemCapability<ISolarHelmetCapability, Void> SOLAR_HELMET_ITEM = ItemCapability.create(
        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("mio_icif", "solar_helmet"),
        ISolarHelmetCapability.class,
        Void.class
    );

    /**
     * 喷气背包能力
     */
    ItemCapability<IJetpackCapability, Void> JETPACK_ITEM = ItemCapability.create(
        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("mio_icif", "jetpack"),
        IJetpackCapability.class,
        Void.class
    );

    /**
     * 流体单元能力
     */
    ItemCapability<IFluidCellCapability, Void> FLUID_CELL_ITEM = ItemCapability.create(
        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("mio_icif", "fluid_cell"),
        IFluidCellCapability.class,
        Void.class
    );

    // ========== 工厂方法 ==========

    /**
     * 创建 EU 能量存储实现
     *
     * @param capacity 能量容量
     * @param maxReceive 最大接收速率
     * @param maxExtract 最大提取速率
     * @param tier 电缆等级
     * @return EU 存储实例
     */
    IEUStorage createEUStorage(long capacity, long maxReceive, long maxExtract, ICableTier tier);

    /**
     * 创建可配置方向的 EU 能量存储实现
     *
     * @param capacity 能量容量
     * @param maxReceive 最大接收速率
     * @param maxExtract 最大提取速率
     * @param tier 电缆等级
     * @param inputSides 允许输入的方向
     * @param outputSides 允许输出的方向
     * @return EU 存储实例
     */
    IEUStorage createEUStorage(long capacity, long maxReceive, long maxExtract, ICableTier tier,
                                Direction[] inputSides, Direction[] outputSides);

    /**
     * 创建热能存储实现（基础参数）
     *
     * @param capacity 热能容量
     * @param maxOutput 最大输出/输入速率
     * @return 热能存储实例
     */
    IHeatStorage createHeatStorage(long capacity, long maxOutput);

    /**
     * 创建热能存储实现（完整参数）
     *
     * @param capacity 热能容量
     * @param maxReceive 最大接收速率
     * @param maxExtract 最大提取速率
     * @param baseTemp 基础温度（摄氏度）
     * @param maxTemp 最高温度（摄氏度）
     * @param lossFactor 热损失系数
     * @return 热能存储实例
     */
    IHeatStorage createHeatStorage(long capacity, long maxReceive, long maxExtract,
                                   int baseTemp, int maxTemp, float lossFactor);

    /**
     * 创建动能存储实现（基础参数）
     *
     * @param capacity 动能容量
     * @param maxOutput 最大输出/输入速率
     * @return 动能存储实例
     */
    IKineticStorage createKineticStorage(long capacity, long maxOutput);

    /**
     * 创建动能存储实现（完整参数）
     *
     * @param capacity 动能容量
     * @param maxReceive 最大接收速率
     * @param maxExtract 最大提取速率
     * @param maxRPM 最大转速
     * @param frictionFactor 摩擦系数
     * @return 动能存储实例
     */
    IKineticStorage createKineticStorage(long capacity, long maxReceive, long maxExtract,
                                         int maxRPM, float frictionFactor);

    /**
     * 创建电动物品实现
     *
     * @param maxCharge 最大电荷
     * @param transferLimit 传输限制
     * @param tier 等级
     * @param canProvide 是否可以提供能量
     * @return 电动物品实例
     */
    ICapabilityElectricItem createElectricItem(long maxCharge, long transferLimit, int tier, boolean canProvide);

    /**
     * 创建升级物品实现
     *
     * @param type 升级类型
     * @param speedMultiplier 速度倍率
     * @param energyMultiplier 能量倍率
     * @param extraStorage 额外存储
     * @param autoEject 自动弹出
     * @param autoImport 自动导入
     * @param tierUpgrade 等级提升
     * @return 升级物品实例
     */
    IUpgradeItem createUpgradeItem(String type, double speedMultiplier, double energyMultiplier,
                                    int extraStorage, boolean autoEject, boolean autoImport, int tierUpgrade);

    // ========== 能力接口定义 ==========

    /**
     * EU 能量存储接口
     */
    interface IEUStorage {
        long getStored();
        long getCapacity();
        long receiveEnergy(long maxReceive, boolean simulate);
        long extractEnergy(long maxExtract, boolean simulate);

        /**
         * 消耗能量（内部做功），不受 maxExtract 限制
         *
         * <p>与 {@link #extractEnergy} 的区别：
         * <ul>
         *   <li>{@code extractEnergy} — 对外输出，受 maxExtract 限制</li>
         *   <li>{@code useEnergy} — 内部消耗，不受 maxExtract 限制</li>
         * </ul>
         *
         * @param amount 消耗量 (EU)
         * @param simulate 如果为true，仅模拟而不实际消耗
         * @return 实际消耗的能量量
         */
        default long useEnergy(long amount, boolean simulate) {
            long toUse = Math.min(amount, getStored());
            if (!simulate && toUse > 0) {
                setStored(getStored() - toUse);
            }
            return toUse;
        }

        /**
         * 生成能量（内部发电），不受 maxReceive 限制
         *
         * <p>与 {@link #receiveEnergy} 的区别：
         * <ul>
         *   <li>{@code receiveEnergy} — 从外部充入，受 maxReceive 限制</li>
         *   <li>{@code generateEnergy} — 内部生成，不受 maxReceive 限制</li>
         * </ul>
         *
         * @param amount 生成量 (EU)
         * @param simulate 如果为true，仅模拟而不实际生成
         * @return 实际生成的能量量
         */
        default long generateEnergy(long amount, boolean simulate) {
            long toGenerate = Math.min(amount, getCapacity() - getStored());
            if (!simulate && toGenerate > 0) {
                setStored(getStored() + toGenerate);
            }
            return toGenerate;
        }

        long getMaxReceive();
        long getMaxExtract();
        ICableTier getTier();
        boolean canReceiveFrom(Direction direction);
        boolean canExtractTo(Direction direction);

        /**
         * 设置存储的能量（可选操作）
         * 默认实现抛出 UnsupportedOperationException，实现类可以选择覆盖此方法
         */
        default void setStored(long amount) {
            throw new UnsupportedOperationException("setStored not supported by this implementation");
        }

        /**
         * 设置能量容量（可选操作）
         * 默认实现抛出 UnsupportedOperationException，实现类可以选择覆盖此方法
         */
        default void setCapacity(long capacity) {
            throw new UnsupportedOperationException("setCapacity not supported by this implementation");
        }
    }

    /**
     * 机器能力接口
     */
    interface IMachineCapability {
        boolean isWorking();
        int getProgress();
        int getMaxProgress();
        double getProgressPercent();
        long getEnergyPerTick();
        ICableTier getCableTier();
        boolean supportsUpgrades();
        int getUpgradeSlotCount();
        ItemStack getUpgradeInSlot(int slot);
    }

    /**
     * 发电机能力接口
     */
    interface IGeneratorCapability {
        boolean isGenerating();
        long getGenerationRate();
        long getCurrentOutput();
        double getEfficiency();
        ICableTier getCableTier();
    }

    /**
     * 热能存储接口
     */
    interface IHeatStorage {
        long getHeatStored();
        long getMaxHeatStored();
        long receiveHeat(long maxReceive, boolean simulate);
        long extractHeat(long maxExtract, boolean simulate);
        boolean canExtractHeat();
        boolean canReceiveHeat();
        int getTemperature();
        boolean isOverheated();
        long getHeatLossPerTick();
        long getMaxReceive();
        long getMaxExtract();

        default void setHeat(long heat) {
            throw new UnsupportedOperationException("setHeat not supported by this implementation");
        }

        default void setCapacity(long capacity) {
            throw new UnsupportedOperationException("setCapacity not supported by this implementation");
        }

        default long applyHeatLoss() {
            long loss = getHeatLossPerTick();
            if (loss > 0 && getHeatStored() > 0) {
                long actualLoss = Math.min(loss, getHeatStored());
                extractHeat(actualLoss, false);
                return actualLoss;
            }
            return 0;
        }

        default long consumeHeatInternal(long amount, boolean simulate) {
            long heatConsumed = Math.min(getHeatStored(), amount);
            if (!simulate && heatConsumed > 0) {
                setHeat(getHeatStored() - heatConsumed);
            }
            return heatConsumed;
        }

        default long generateHeatInternal(long amount, boolean simulate) {
            long heatGenerated = Math.min(getMaxHeatStored() - getHeatStored(), amount);
            if (!simulate && heatGenerated > 0) {
                setHeat(getHeatStored() + heatGenerated);
            }
            return heatGenerated;
        }
    }

    /**
     * 动能存储接口
     */
    interface IKineticStorage {
        long getKineticStored();
        long getMaxKineticStored();
        long receiveKinetic(long maxReceive, boolean simulate);
        long extractKinetic(long maxExtract, boolean simulate);
        boolean canExtractKinetic();
        boolean canReceiveKinetic();
        int getRPM();
        boolean isOverspeed();
        long getKineticLossPerTick();
        long getMaxReceive();
        long getMaxExtract();

        default void setKinetic(long kinetic) {
            throw new UnsupportedOperationException("setKinetic not supported by this implementation");
        }

        default void applyFrictionLoss() {
            if (getKineticStored() > 0) {
                long loss = getKineticLossPerTick();
                if (loss < 1 && getKineticStored() > 0) {
                    loss = 1;
                }
                extractKinetic(loss, false);
            }
        }

        default long generateKineticInternal(long amount, boolean simulate) {
            long kineticGenerated = Math.min(getMaxKineticStored() - getKineticStored(), amount);
            if (!simulate && kineticGenerated > 0) {
                setKinetic(getKineticStored() + kineticGenerated);
            }
            return kineticGenerated;
        }
    }

    /**
     * 电动物品能力接口。
     * <p>
     * 此接口用于 NeoForge Capability 系统，与 {@code api.item.electric.IElectricItem} 不同。
     * 后者是完整的电力物品接口，包含物品管理器等高级功能；
     * 此接口是简化的能力版本，仅包含充放电和基本属性查询。
     */
    interface ICapabilityElectricItem {
        long getMaxCharge(ItemStack stack);
        long getTransferLimit(ItemStack stack);
        int getTier(ItemStack stack);
        boolean canProvideEnergy(ItemStack stack);
        long charge(ItemStack stack, long amount, int tier, boolean ignoreTransferLimit, boolean simulate);
        long discharge(ItemStack stack, long amount, int tier, boolean ignoreTransferLimit, boolean externally, boolean simulate);
    }

    /**
     * 升级物品接口
     */
    interface IUpgradeItem {
        String getUpgradeType();
        double getSpeedMultiplier();
        double getEnergyMultiplier();
        int getExtraEnergyStorage();
        boolean hasAutoEject();
        boolean hasAutoImport();
        int getTierUpgrade();
    }

    /**
     * 反应堆组件接口
     */
    interface IReactorComponent {
        int getMaxDurability();
        int getCurrentDurability();
        int getHeatOutput();
        int getNeutronPulse();
        boolean isFuelRod();
        boolean isMoxFuel();
        boolean isHeatSink();
        boolean isReflector();
    }

    // ========== 新增能力接口 ==========

    /**
     * 变压器能力接口
     */
    interface ITransformerCapability {
        boolean isStepUp();
        ICableTier getLowTier();
        ICableTier getHighTier();
        ICableTier getInputTier();
        ICableTier getOutputTier();
        long getBufferAmount();
        long getBufferCapacity();
    }

    /**
     * 管道能力接口
     */
    interface IPipeCapability {
        enum PipeType { ITEM, FLUID, CABLE, UNIVERSAL }
        PipeType getPipeType();
        java.util.Set<Direction> getConnections();
        boolean isExtracting(Direction side);
        int getTransferRate();
    }

    /**
     * 能量转换器能力接口
     */
    interface IEnergyConverterCapability {
        enum EnergyType { EU, FE, KU, HE, CUSTOM }
        EnergyType getInputType();
        EnergyType getOutputType();
        double getConversionRatio();
        ICableTier getInputCableTier();
        ICableTier getOutputCableTier();
        long getBufferAmount();
        long getBufferCapacity();
    }

    /**
     * 武器物品能力接口
     */
    interface IWeaponCapability {
        float getDamage();
        float getRange();
        long getEnergyPerShot();
        boolean isRanged();
    }

    /**
     * 太阳能头盔能力接口
     */
    interface ISolarHelmetCapability {
        long getGenerationRate();
        boolean requiresSky();
        boolean isDayOnly();
    }

    /**
     * 喷气背包能力接口
     */
    interface IJetpackCapability {
        float getThrust();
        long getEnergyPerTickFlying();
        enum JetpackMode { OFF, NORMAL, HOVER, FLIGHT }
        JetpackMode getMode();
    }

    /**
     * 流体单元能力接口
     */
    interface IFluidCellCapability {
        int getCapacity();
        net.neoforged.neoforge.fluids.FluidStack getFluid();
        boolean canHoldFluid(net.minecraft.world.level.material.Fluid fluid);
        int fill(net.neoforged.neoforge.fluids.FluidStack fluid, boolean simulate);
        net.neoforged.neoforge.fluids.FluidStack drain(int amount, boolean simulate);
    }

    // ========== 新增工厂方法 ==========

    /**
     * 创建变压器能力实现
     *
     * @param isStepUp 是否升压模式
     * @param lowTier 低压侧等级
     * @param highTier 高压侧等级
     * @return 变压器能力实例
     */
    ITransformerCapability createTransformer(boolean isStepUp, ICableTier lowTier, ICableTier highTier);

    /**
     * 创建管道能力实现
     *
     * @param pipeType 管道类型
     * @param transferRate 传输速率
     * @return 管道能力实例
     */
    IPipeCapability createPipe(IPipeCapability.PipeType pipeType, int transferRate);

    /**
     * 创建能量转换器能力实现
     *
     * @param inputType 输入能量类型
     * @param outputType 输出能量类型
     * @param conversionRatio 转换比率
     * @return 能量转换器能力实例
     */
    IEnergyConverterCapability createEnergyConverter(IEnergyConverterCapability.EnergyType inputType, IEnergyConverterCapability.EnergyType outputType, double conversionRatio);

    /**
     * 创建武器能力实现
     *
     * @param damage 伤害值
     * @param range 射程
     * @param energyPerShot 每次射击能耗
     * @param isRanged 是否远程武器
     * @return 武器能力实例
     */
    IWeaponCapability createWeapon(float damage, float range, long energyPerShot, boolean isRanged);

    /**
     * 创建太阳能头盔能力实现
     *
     * @param generationRate 发电速率
     * @param requiresSky 是否需要天空
     * @param isDayOnly 是否仅白天
     * @return 太阳能头盔能力实例
     */
    ISolarHelmetCapability createSolarHelmet(long generationRate, boolean requiresSky, boolean isDayOnly);

    /**
     * 创建喷气背包能力实现
     *
     * @param thrust 推力
     * @param energyPerTickFlying 每 tick 飞行能耗
     * @return 喷气背包能力实例
     */
    IJetpackCapability createJetpack(float thrust, long energyPerTickFlying);

    /**
     * 创建流体单元能力实现
     *
     * @param capacity 容量（mB）
     * @return 流体单元能力实例
     */
    IFluidCellCapability createFluidCell(int capacity);

    /**
     * 为方块实体创建热能存储适配器。
     * 当方块实体实现了内部的 IHeatStorage 接口时，此方法返回一个通过 API 接口暴露的包装器。
     * 若方块不支持热能存储则返回 null。
     *
     * @param blockEntity 目标方块实体
     * @return API 层的热能存储接口，或 null 如果方块不支持
     */
    IHeatStorage adaptHeatStorage(Object blockEntity);

    /**
     * 为方块实体创建动能存储适配器。
     * 当方块实体实现了内部的 IKineticStorage 接口时，此方法返回一个通过 API 接口暴露的包装器。
     * 若方块不支持动能存储则返回 null。
     *
     * @param blockEntity 目标方块实体
     * @return API 层的动能存储接口，或 null 如果方块不支持
     */
    IKineticStorage adaptKineticStorage(Object blockEntity);
}