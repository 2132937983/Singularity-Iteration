package com.singularity_iteration.mio_icif.api.capability;

import com.singularity_iteration.mio_icif.api.energy.ICableTier;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;

import java.lang.reflect.Method;

/**
 * mio_icif 能力系统 API 实现
 *
 * <p>提供所有能力接口的工厂方法，让附属模组可以创建自定义的能力实现。
 */
public class MioIcifCapabilitiesImpl implements IMioIcifCapabilities {

    /**
     * 创建 EU 能量存储实现
     *
     * @param capacity 能量容量
     * @param maxReceive 最大接收速率
     * @param maxExtract 最大提取速率
     * @param tier 电缆等级
     * @return EU 存储实例
     */
    @Override
    public IEUStorage createEUStorage(long capacity, long maxReceive, long maxExtract, ICableTier tier) {
        return new EUStorageImpl(capacity, maxReceive, maxExtract, tier);
    }

    /**
     * 创建可配置方向的 EU 能量存储实现
     *
     * @param capacity 能量容量
     * @param maxReceive 最大接收速率
     * @param maxExtract 最大提取速率
     * @param tier 电缆等级
     * @param inputSides 允许输入的面
     * @param outputSides 允许输出的面
     * @return EU 存储实例
     */
    @Override
    public IEUStorage createEUStorage(long capacity, long maxReceive, long maxExtract, ICableTier tier,
                                       Direction[] inputSides, Direction[] outputSides) {
        return new EUStorageImpl(capacity, maxReceive, maxExtract, tier, inputSides, outputSides);
    }

    /**
     * 创建热能存储实现
     *
     * @param capacity 热能容量
     * @param maxOutput 最大输出速率
     * @return 热能存储实例
     */
    @Override
    public IHeatStorage createHeatStorage(long capacity, long maxOutput) {
        return new HeatStorageImpl(capacity, maxOutput);
    }

    @Override
    public IHeatStorage createHeatStorage(long capacity, long maxReceive, long maxExtract,
                                           int baseTemp, int maxTemp, float lossFactor) {
        return new HeatStorageImpl(capacity, maxReceive, maxExtract, baseTemp, maxTemp, lossFactor);
    }

    /**
     * 创建动能存储实现
     *
     * @param capacity 动能容量
     * @param maxOutput 最大输出速率
     * @return 动能存储实例
     */
    @Override
    public IKineticStorage createKineticStorage(long capacity, long maxOutput) {
        return new KineticStorageImpl(capacity, maxOutput);
    }

    @Override
    public IKineticStorage createKineticStorage(long capacity, long maxReceive, long maxExtract,
                                                 int maxRPM, float frictionFactor) {
        return new KineticStorageImpl(capacity, maxReceive, maxExtract, maxRPM, frictionFactor);
    }

    /**
     * 创建电动物品实现
     *
     * @param maxCharge 最大电荷
     * @param transferLimit 传输限制
     * @param tier 等级
     * @param canProvide 是否可以提供能量
     * @return 电动物品实例
     */
    @Override
    public ICapabilityElectricItem createElectricItem(long maxCharge, long transferLimit, int tier, boolean canProvide) {
        return new ElectricItemImpl(maxCharge, transferLimit, tier, canProvide);
    }

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
    @Override
    public IUpgradeItem createUpgradeItem(String type, double speedMultiplier, double energyMultiplier,
                                           int extraStorage, boolean autoEject, boolean autoImport, int tierUpgrade) {
        return new UpgradeItemImpl(type, speedMultiplier, energyMultiplier, extraStorage, autoEject, autoImport, tierUpgrade);
    }

    // ========== 新增工厂方法 ==========

    @Override
    public ITransformerCapability createTransformer(boolean isStepUp, ICableTier lowTier, ICableTier highTier) {
        return new TransformerImpl(isStepUp, lowTier, highTier);
    }

    @Override
    public IPipeCapability createPipe(IPipeCapability.PipeType pipeType, int transferRate) {
        return new PipeImpl(pipeType, transferRate);
    }

    @Override
    public IEnergyConverterCapability createEnergyConverter(IEnergyConverterCapability.EnergyType inputType, IEnergyConverterCapability.EnergyType outputType, double conversionRatio) {
        return new EnergyConverterImpl(inputType, outputType, conversionRatio);
    }

    @Override
    public IWeaponCapability createWeapon(float damage, float range, long energyPerShot, boolean isRanged) {
        return new WeaponImpl(damage, range, energyPerShot, isRanged);
    }

    @Override
    public ISolarHelmetCapability createSolarHelmet(long generationRate, boolean requiresSky, boolean isDayOnly) {
        return new SolarHelmetImpl(generationRate, requiresSky, isDayOnly);
    }

    @Override
    public IJetpackCapability createJetpack(float thrust, long energyPerTickFlying) {
        return new JetpackImpl(thrust, energyPerTickFlying);
    }

    @Override
    public IFluidCellCapability createFluidCell(int capacity) {
        return new FluidCellImpl(capacity);
    }

    @Override
    public IHeatStorage adaptHeatStorage(Object blockEntity) {
        if (blockEntity == null) return null;
        return HeatCapabilityAdapter.wrap(blockEntity);
    }

    @Override
    public IKineticStorage adaptKineticStorage(Object blockEntity) {
        if (blockEntity == null) return null;
        return KineticCapabilityAdapter.wrap(blockEntity);
    }

    // ========== EU 存储实现 ==========

    private static class EUStorageImpl implements IEUStorage {
        private static final int MAX_CAS_RETRIES = 16;
        private final java.util.concurrent.atomic.AtomicLong stored = new java.util.concurrent.atomic.AtomicLong();
        private final long capacity;
        private final long maxReceive;
        private final long maxExtract;
        private final ICableTier tier;
        private final Direction[] inputSides;
        private final Direction[] outputSides;

        public EUStorageImpl(long capacity, long maxReceive, long maxExtract, ICableTier tier) {
            this(capacity, maxReceive, maxExtract, tier, null, null);
        }

        public EUStorageImpl(long capacity, long maxReceive, long maxExtract, ICableTier tier,
                             Direction[] inputSides, Direction[] outputSides) {
            if (capacity <= 0) throw new IllegalArgumentException("capacity must be positive");
            if (maxReceive < 0) throw new IllegalArgumentException("maxReceive cannot be negative");
            if (maxExtract < 0) throw new IllegalArgumentException("maxExtract cannot be negative");
            if (tier == null) throw new IllegalArgumentException("tier cannot be null");

            this.capacity = capacity;
            this.maxReceive = maxReceive;
            this.maxExtract = maxExtract;
            this.tier = tier;
            this.inputSides = inputSides;
            this.outputSides = outputSides;
        }

        @Override
        public long getStored() {
            return stored.get();
        }

        @Override
        public long getCapacity() {
            return capacity;
        }

        @Override
        public long receiveEnergy(long maxReceive, boolean simulate) {
            if (maxReceive <= 0) return 0;
            long maxAllowed = Math.min(this.maxReceive, maxReceive);
            for (int i = 0; i < MAX_CAS_RETRIES; i++) {
                long current = stored.get();
                long canReceive = Math.min(maxAllowed, capacity - current);
                if (canReceive <= 0) return 0;
                if (simulate) return canReceive;
                if (stored.compareAndSet(current, current + canReceive)) return canReceive;
            }
            synchronized (this) {
                long current = stored.get();
                long canReceive = Math.min(maxAllowed, capacity - current);
                if (canReceive <= 0) return 0;
                if (!simulate) stored.set(current + canReceive);
                return canReceive;
            }
        }

        @Override
        public long extractEnergy(long maxExtract, boolean simulate) {
            if (maxExtract <= 0) return 0;
            long maxAllowed = Math.min(this.maxExtract, maxExtract);
            for (int i = 0; i < MAX_CAS_RETRIES; i++) {
                long current = stored.get();
                long canExtract = Math.min(maxAllowed, current);
                if (canExtract <= 0) return 0;
                if (simulate) return canExtract;
                if (stored.compareAndSet(current, current - canExtract)) return canExtract;
            }
            synchronized (this) {
                long current = stored.get();
                long canExtract = Math.min(maxAllowed, current);
                if (canExtract <= 0) return 0;
                if (!simulate) stored.set(current - canExtract);
                return canExtract;
            }
        }

        @Override
        public long getMaxReceive() {
            return maxReceive;
        }

        @Override
        public long getMaxExtract() {
            return maxExtract;
        }

        @Override
        public ICableTier getTier() {
            return tier;
        }

        @Override
        public boolean canReceiveFrom(Direction direction) {
            if (inputSides == null || inputSides.length == 0) return true;
            for (Direction side : inputSides) {
                if (side == direction) return true;
            }
            return false;
        }

        @Override
        public boolean canExtractTo(Direction direction) {
            if (outputSides == null || outputSides.length == 0) return true;
            for (Direction side : outputSides) {
                if (side == direction) return true;
            }
            return false;
        }

        /**
         * 内部设置存储能量（用于机器内部逻辑）
         */
        @Override
        public void setStored(long amount) {
            this.stored.set(Math.max(0, Math.min(amount, capacity)));
        }

        @Override
        public void setCapacity(long capacity) {
            // 不支持动态修改容量
            throw new UnsupportedOperationException("setCapacity not supported");
        }
    }

    // ========== 热能存储实现 ==========

    private static class HeatStorageImpl implements IHeatStorage {
        private static final int MAX_CAS_RETRIES = 16;
        private final java.util.concurrent.atomic.AtomicLong stored = new java.util.concurrent.atomic.AtomicLong();
        private volatile long capacity;
        private final long maxReceive;
        private final long maxExtract;
        private final int baseTemp;
        private final int maxTemp;
        private final float lossFactor;

        public HeatStorageImpl(long capacity, long maxOutput) {
            this(capacity, maxOutput, maxOutput, 20, 1000, 0.01f);
        }

        public HeatStorageImpl(long capacity, long maxReceive, long maxExtract,
                               int baseTemp, int maxTemp, float lossFactor) {
            if (capacity <= 0) throw new IllegalArgumentException("capacity must be positive");
            if (maxReceive < 0) throw new IllegalArgumentException("maxReceive cannot be negative");
            if (maxExtract < 0) throw new IllegalArgumentException("maxExtract cannot be negative");

            this.capacity = capacity;
            this.maxReceive = maxReceive;
            this.maxExtract = maxExtract;
            this.baseTemp = baseTemp;
            this.maxTemp = maxTemp;
            this.lossFactor = lossFactor;
        }

        @Override
        public long getHeatStored() {
            return stored.get();
        }

        @Override
        public long getMaxHeatStored() {
            return capacity;
        }

        @Override
        public long getMaxReceive() {
            return maxReceive;
        }

        @Override
        public long getMaxExtract() {
            return maxExtract;
        }

        @Override
        public long receiveHeat(long maxReceive, boolean simulate) {
            if (maxReceive <= 0 || !canReceiveHeat()) return 0;
            long maxAllowed = Math.min(this.maxReceive, maxReceive);
            for (int i = 0; i < MAX_CAS_RETRIES; i++) {
                long current = stored.get();
                long canReceive = Math.min(maxAllowed, capacity - current);
                if (canReceive <= 0) return 0;
                if (simulate) return canReceive;
                if (stored.compareAndSet(current, current + canReceive)) return canReceive;
            }
            synchronized (this) {
                long current = stored.get();
                long canReceive = Math.min(maxAllowed, capacity - current);
                if (canReceive <= 0) return 0;
                if (!simulate) stored.set(current + canReceive);
                return canReceive;
            }
        }

        @Override
        public long extractHeat(long maxExtract, boolean simulate) {
            if (maxExtract <= 0 || !canExtractHeat()) return 0;
            long maxAllowed = Math.min(this.maxExtract, maxExtract);
            for (int i = 0; i < MAX_CAS_RETRIES; i++) {
                long current = stored.get();
                long canExtract = Math.min(maxAllowed, current);
                if (canExtract <= 0) return 0;
                if (simulate) return canExtract;
                if (stored.compareAndSet(current, current - canExtract)) return canExtract;
            }
            synchronized (this) {
                long current = stored.get();
                long canExtract = Math.min(maxAllowed, current);
                if (canExtract <= 0) return 0;
                if (!simulate) stored.set(current - canExtract);
                return canExtract;
            }
        }

        @Override
        public boolean canExtractHeat() {
            return maxExtract > 0 && stored.get() > 0;
        }

        @Override
        public boolean canReceiveHeat() {
            return maxReceive > 0 && stored.get() < capacity;
        }

        @Override
        public int getTemperature() {
            long cap = capacity;
            if (cap == 0) return baseTemp;
            long heatPercent = (stored.get() * 100) / cap;
            return baseTemp + (int)((heatPercent * (maxTemp - baseTemp)) / 100);
        }

        @Override
        public boolean isOverheated() {
            return getTemperature() >= 800;
        }

        @Override
        public long getHeatLossPerTick() {
            int temp = getTemperature();
            if (temp <= baseTemp) return 0;
            return (long) ((temp - baseTemp) * lossFactor);
        }

        @Override
        public void setHeat(long heat) {
            long cap = capacity;
            stored.set(Math.max(0, Math.min(cap, heat)));
        }

        @Override
        public void setCapacity(long newCapacity) {
            long cap = Math.max(0, newCapacity);
            capacity = cap;
            long current = stored.get();
            if (current > cap) {
                stored.set(cap);
            }
        }
    }

    // ========== 动能存储实现 ==========

    private static class KineticStorageImpl implements IKineticStorage {
        private static final int MAX_CAS_RETRIES = 16;
        private final java.util.concurrent.atomic.AtomicLong stored = new java.util.concurrent.atomic.AtomicLong();
        private final long capacity;
        private final long maxReceiveRate;
        private final long maxExtractRate;
        private final int maxRPM;
        private final float frictionFactor;

        public KineticStorageImpl(long capacity, long maxOutput) {
            this(capacity, maxOutput, maxOutput, 10000, 0.005f);
        }

        public KineticStorageImpl(long capacity, long maxReceive, long maxExtract,
                                   int maxRPM, float frictionFactor) {
            if (capacity < 0) throw new IllegalArgumentException("capacity cannot be negative");
            if (maxReceive < 0) throw new IllegalArgumentException("maxReceive cannot be negative");
            if (maxExtract < 0) throw new IllegalArgumentException("maxExtract cannot be negative");

            this.capacity = capacity;
            this.maxReceiveRate = maxReceive;
            this.maxExtractRate = maxExtract;
            this.maxRPM = maxRPM;
            this.frictionFactor = frictionFactor;
        }

        @Override
        public long getKineticStored() {
            return stored.get();
        }

        @Override
        public long getMaxKineticStored() {
            return capacity;
        }

        @Override
        public long receiveKinetic(long maxReceive, boolean simulate) {
            if (maxReceive <= 0 || !canReceiveKinetic()) return 0;
            long maxAllowed = Math.min(this.maxReceiveRate, maxReceive);
            for (int i = 0; i < MAX_CAS_RETRIES; i++) {
                long current = stored.get();
                long canReceive = Math.min(maxAllowed, capacity - current);
                if (canReceive <= 0) return 0;
                if (simulate) return canReceive;
                if (stored.compareAndSet(current, current + canReceive)) return canReceive;
            }
            synchronized (this) {
                long current = stored.get();
                long canReceive = Math.min(maxAllowed, capacity - current);
                if (canReceive <= 0) return 0;
                if (!simulate) stored.set(current + canReceive);
                return canReceive;
            }
        }

        @Override
        public long extractKinetic(long maxExtract, boolean simulate) {
            if (maxExtract <= 0 || !canExtractKinetic()) return 0;
            long maxAllowed = Math.min(this.maxExtractRate, maxExtract);
            for (int i = 0; i < MAX_CAS_RETRIES; i++) {
                long current = stored.get();
                long canExtract = Math.min(maxAllowed, current);
                if (canExtract <= 0) return 0;
                if (simulate) return canExtract;
                if (stored.compareAndSet(current, current - canExtract)) return canExtract;
            }
            synchronized (this) {
                long current = stored.get();
                long canExtract = Math.min(maxAllowed, current);
                if (canExtract <= 0) return 0;
                if (!simulate) stored.set(current - canExtract);
                return canExtract;
            }
        }

        @Override
        public boolean canExtractKinetic() {
            return maxExtractRate > 0 && stored.get() > 0;
        }

        @Override
        public boolean canReceiveKinetic() {
            return maxReceiveRate > 0 && stored.get() < capacity;
        }

        @Override
        public int getRPM() {
            if (getMaxKineticStored() == 0) return 0;
            long kineticPercent = (getKineticStored() * 100) / getMaxKineticStored();
            return (int)((kineticPercent * maxRPM) / 100);
        }

        @Override
        public boolean isOverspeed() {
            return getRPM() >= (maxRPM * 80 / 100);
        }

        @Override
        public long getKineticLossPerTick() {
            long k = getKineticStored();
            if (k <= 0) return 0;
            long loss = (long)(k * frictionFactor);
            if (loss < 1 && k > 0) loss = 1;
            return loss;
        }

        @Override
        public long getMaxReceive() {
            return maxReceiveRate;
        }

        @Override
        public long getMaxExtract() {
            return maxExtractRate;
        }

        @Override
        public void setKinetic(long kinetic) {
            stored.set(Math.max(0, Math.min(capacity, kinetic)));
        }

        @Override
        public void applyFrictionLoss() {
            if (stored.get() > 0) {
                long loss = (long)(stored.get() * frictionFactor);
                if (loss < 1 && stored.get() > 0) loss = 1;
                extractKinetic(loss, false);
            }
        }
    }

    // ========== 电动物品实现 ==========

    private static class ElectricItemImpl implements ICapabilityElectricItem {
        private final long maxCharge;
        private final long transferLimit;
        private final int tier;
        private final boolean canProvide;

        public ElectricItemImpl(long maxCharge, long transferLimit, int tier, boolean canProvide) {
            if (maxCharge <= 0) throw new IllegalArgumentException("maxCharge must be positive");
            if (transferLimit <= 0) throw new IllegalArgumentException("transferLimit must be positive");
            if (tier < 0) throw new IllegalArgumentException("tier cannot be negative");

            this.maxCharge = maxCharge;
            this.transferLimit = transferLimit;
            this.tier = tier;
            this.canProvide = canProvide;
        }

        @Override
        public long getMaxCharge(ItemStack stack) {
            return maxCharge;
        }

        @Override
        public long getTransferLimit(ItemStack stack) {
            return transferLimit;
        }

        @Override
        public int getTier(ItemStack stack) {
            return tier;
        }

        @Override
        public boolean canProvideEnergy(ItemStack stack) {
            return canProvide;
        }

        @Override
        public long charge(ItemStack stack, long amount, int tier, boolean ignoreTransferLimit, boolean simulate) {
            if (amount <= 0 || stack.isEmpty()) return 0;
            if (tier > this.tier) return 0;

            long currentCharge = getCharge(stack);
            long canCharge = Math.min(maxCharge - currentCharge, amount);
            if (!ignoreTransferLimit) {
                canCharge = Math.min(canCharge, transferLimit);
            }
            if (canCharge <= 0) return 0;

            if (!simulate) {
                setCharge(stack, currentCharge + canCharge);
            }
            return canCharge;
        }

        @Override
        public long discharge(ItemStack stack, long amount, int tier, boolean ignoreTransferLimit, boolean externally, boolean simulate) {
            if (amount <= 0 || stack.isEmpty()) return 0;
            if (!canProvide && externally) return 0;

            long currentCharge = getCharge(stack);
            long canDischarge = Math.min(currentCharge, amount);
            if (!ignoreTransferLimit) {
                canDischarge = Math.min(canDischarge, transferLimit);
            }
            if (canDischarge <= 0) return 0;

            if (!simulate) {
                setCharge(stack, currentCharge - canDischarge);
            }
            return canDischarge;
        }

        /**
         * 获取物品当前电荷
         */
        private long getCharge(ItemStack stack) {
            return BatteryBridge.getCharge(stack);
        }

        /**
         * 设置物品电荷
         */
        private void setCharge(ItemStack stack, long charge) {
            BatteryBridge.setCharge(stack, charge, maxCharge);
        }
    }

    // ========== 升级物品实现 ==========

    private static class UpgradeItemImpl implements IUpgradeItem {
        private final String type;
        private final double speedMultiplier;
        private final double energyMultiplier;
        private final int extraEnergyStorage;
        private final boolean autoEject;
        private final boolean autoImport;
        private final int tierUpgrade;

        public UpgradeItemImpl(String type, double speedMultiplier, double energyMultiplier,
                               int extraEnergyStorage, boolean autoEject, boolean autoImport, int tierUpgrade) {
            if (type == null || type.isEmpty()) throw new IllegalArgumentException("type cannot be null or empty");
            if (speedMultiplier < 0) throw new IllegalArgumentException("speedMultiplier cannot be negative");
            if (energyMultiplier < 0) throw new IllegalArgumentException("energyMultiplier cannot be negative");
            if (extraEnergyStorage < 0) throw new IllegalArgumentException("extraEnergyStorage cannot be negative");
            if (tierUpgrade < 0) throw new IllegalArgumentException("tierUpgrade cannot be negative");

            this.type = type;
            this.speedMultiplier = speedMultiplier;
            this.energyMultiplier = energyMultiplier;
            this.extraEnergyStorage = extraEnergyStorage;
            this.autoEject = autoEject;
            this.autoImport = autoImport;
            this.tierUpgrade = tierUpgrade;
        }

        @Override
        public String getUpgradeType() {
            return type;
        }

        @Override
        public double getSpeedMultiplier() {
            return speedMultiplier;
        }

        @Override
        public double getEnergyMultiplier() {
            return energyMultiplier;
        }

        @Override
        public int getExtraEnergyStorage() {
            return extraEnergyStorage;
        }

        @Override
        public boolean hasAutoEject() {
            return autoEject;
        }

        @Override
        public boolean hasAutoImport() {
            return autoImport;
        }

        @Override
        public int getTierUpgrade() {
            return tierUpgrade;
        }
    }

    // ========== 变压器实现 ==========

    private static class TransformerImpl implements ITransformerCapability {
        private final boolean stepUp;
        private final ICableTier lowTier;
        private final ICableTier highTier;

        TransformerImpl(boolean stepUp, ICableTier lowTier, ICableTier highTier) {
            this.stepUp = stepUp;
            this.lowTier = lowTier;
            this.highTier = highTier;
        }

        @Override public boolean isStepUp() { return stepUp; }
        @Override public ICableTier getLowTier() { return lowTier; }
        @Override public ICableTier getHighTier() { return highTier; }
        @Override public ICableTier getInputTier() { return stepUp ? lowTier : highTier; }
        @Override public ICableTier getOutputTier() { return stepUp ? highTier : lowTier; }
        @Override public long getBufferAmount() { return 0; }
        @Override public long getBufferCapacity() { return lowTier.getPowerRating() * 10; }
    }

    // ========== 管道实现 ==========

    private static class PipeImpl implements IPipeCapability {
        private final PipeType pipeType;
        private final int transferRate;
        private final java.util.Set<Direction> connections = java.util.EnumSet.allOf(Direction.class);
        private final java.util.Set<Direction> extracting = java.util.EnumSet.noneOf(Direction.class);

        PipeImpl(PipeType pipeType, int transferRate) {
            this.pipeType = pipeType;
            this.transferRate = transferRate;
        }

        @Override public PipeType getPipeType() { return pipeType; }
        @Override public java.util.Set<Direction> getConnections() { return java.util.Set.copyOf(connections); }
        @Override public boolean isExtracting(Direction side) { return extracting.contains(side); }
        @Override public int getTransferRate() { return transferRate; }
    }

    // ========== 能量转换器实现 ==========

    private static class EnergyConverterImpl implements IEnergyConverterCapability {
        private final EnergyType inputType;
        private final EnergyType outputType;
        private final double conversionRatio;

        EnergyConverterImpl(EnergyType inputType, EnergyType outputType, double conversionRatio) {
            this.inputType = inputType;
            this.outputType = outputType;
            this.conversionRatio = conversionRatio;
        }

        @Override public EnergyType getInputType() { return inputType; }
        @Override public EnergyType getOutputType() { return outputType; }
        @Override public double getConversionRatio() { return conversionRatio; }
        @Override public ICableTier getInputCableTier() { return com.singularity_iteration.mio_icif.api.energy.tile.IEnergyConductor.LV_TIER; }
        @Override public ICableTier getOutputCableTier() { return com.singularity_iteration.mio_icif.api.energy.tile.IEnergyConductor.LV_TIER; }
        @Override public long getBufferAmount() { return 0; }
        @Override public long getBufferCapacity() { return 0; }
    }

    // ========== 武器实现 ==========

    private static class WeaponImpl implements IWeaponCapability {
        private final float damage;
        private final float range;
        private final long energyPerShot;
        private final boolean isRanged;

        WeaponImpl(float damage, float range, long energyPerShot, boolean isRanged) {
            this.damage = damage;
            this.range = range;
            this.energyPerShot = energyPerShot;
            this.isRanged = isRanged;
        }

        @Override public float getDamage() { return damage; }
        @Override public float getRange() { return range; }
        @Override public long getEnergyPerShot() { return energyPerShot; }
        @Override public boolean isRanged() { return isRanged; }
    }

    // ========== 太阳能头盔实现 ==========

    private static class SolarHelmetImpl implements ISolarHelmetCapability {
        private final long generationRate;
        private final boolean requiresSky;
        private final boolean isDayOnly;

        SolarHelmetImpl(long generationRate, boolean requiresSky, boolean isDayOnly) {
            this.generationRate = generationRate;
            this.requiresSky = requiresSky;
            this.isDayOnly = isDayOnly;
        }

        @Override public long getGenerationRate() { return generationRate; }
        @Override public boolean requiresSky() { return requiresSky; }
        @Override public boolean isDayOnly() { return isDayOnly; }
    }

    // ========== 喷气背包实现 ==========

    private static class JetpackImpl implements IJetpackCapability {
        private final float thrust;
        private final long energyPerTickFlying;

        JetpackImpl(float thrust, long energyPerTickFlying) {
            this.thrust = thrust;
            this.energyPerTickFlying = energyPerTickFlying;
        }

        @Override public float getThrust() { return thrust; }
        @Override public long getEnergyPerTickFlying() { return energyPerTickFlying; }
        @Override public JetpackMode getMode() { return JetpackMode.OFF; }
    }

    // ========== 流体单元实现 ==========

    private static class FluidCellImpl implements IFluidCellCapability {
        private final int capacity;
        private net.neoforged.neoforge.fluids.FluidStack fluid = net.neoforged.neoforge.fluids.FluidStack.EMPTY;

        FluidCellImpl(int capacity) {
            this.capacity = capacity;
        }

        @Override public int getCapacity() { return capacity; }
        @Override public net.neoforged.neoforge.fluids.FluidStack getFluid() { return fluid; }
        @Override public boolean canHoldFluid(net.minecraft.world.level.material.Fluid f) { return true; }

        @Override
        public int fill(net.neoforged.neoforge.fluids.FluidStack fillStack, boolean simulate) {
            if (fillStack.isEmpty() || !canHoldFluid(fillStack.getFluid())) return 0;
            if (!fluid.isEmpty() && fluid.getFluid() != fillStack.getFluid()) return 0;
            int space = capacity - fluid.getAmount();
            int accepted = Math.min(space, fillStack.getAmount());
            if (!simulate && accepted > 0) {
                if (fluid.isEmpty()) {
                    fluid = new net.neoforged.neoforge.fluids.FluidStack(fillStack.getFluid(), accepted);
                } else {
                    fluid.grow(accepted);
                }
            }
            return accepted;
        }

        @Override
        public net.neoforged.neoforge.fluids.FluidStack drain(int drainAmount, boolean simulate) {
            if (fluid.isEmpty() || drainAmount <= 0) return net.neoforged.neoforge.fluids.FluidStack.EMPTY;
            int drained = Math.min(fluid.getAmount(), drainAmount);
            net.neoforged.neoforge.fluids.FluidStack result = new net.neoforged.neoforge.fluids.FluidStack(fluid.getFluid(), drained);
            if (!simulate) {
                fluid.shrink(drained);
                if (fluid.isEmpty()) fluid = net.neoforged.neoforge.fluids.FluidStack.EMPTY;
            }
            return result;
        }
    }

    // ========== 热能存储适配器 ==========

    private static final class HeatCapabilityAdapter implements IHeatStorage {
        private static final Class<?> I_HEAT_STORAGE;
        private static final Method M_GET_HEAT_STORED;
        private static final Method M_GET_MAX_HEAT_STORED;
        private static final Method M_RECEIVE_HEAT;
        private static final Method M_EXTRACT_HEAT;
        private static final Method M_CAN_EXTRACT_HEAT;
        private static final Method M_CAN_RECEIVE_HEAT;
        private static final Method M_GET_TEMPERATURE;
        private static final Method M_IS_OVERHEATED;
        private static final Method M_GET_HEAT_LOSS_PER_TICK;
        private static final Method M_GET_MAX_RECEIVE;
        private static final Method M_GET_MAX_EXTRACT;

        static {
            ClassLoader cl = MioIcifCapabilitiesImpl.class.getClassLoader();
            Class<?> storage = null;
            Method getHeatStored = null, getMaxHeatStored = null, receiveHeat = null, extractHeat = null;
            Method canExtractHeat = null, canReceiveHeat = null, getTemperature = null, isOverheated = null, getHeatLossPerTick = null;
            Method getMaxReceive = null, getMaxExtract = null;
            try {
                storage = cl.loadClass("com.singularity_iteration.mio_icif.energy.heat.IHeatStorage");
                getHeatStored = storage.getMethod("getHeatStored");
                getMaxHeatStored = storage.getMethod("getMaxHeatStored");
                receiveHeat = storage.getMethod("receiveHeat", long.class, boolean.class);
                extractHeat = storage.getMethod("extractHeat", long.class, boolean.class);
                canExtractHeat = storage.getMethod("canExtractHeat");
                canReceiveHeat = storage.getMethod("canReceiveHeat");
                getTemperature = storage.getMethod("getTemperature");
                isOverheated = storage.getMethod("isOverheated");
                getHeatLossPerTick = storage.getMethod("getHeatLossPerTick");
                getMaxReceive = storage.getMethod("getMaxReceive");
                getMaxExtract = storage.getMethod("getMaxExtract");
            } catch (Throwable ignored) {
                storage = null;
            }
            I_HEAT_STORAGE = storage;
            M_GET_HEAT_STORED = getHeatStored;
            M_GET_MAX_HEAT_STORED = getMaxHeatStored;
            M_RECEIVE_HEAT = receiveHeat;
            M_EXTRACT_HEAT = extractHeat;
            M_CAN_EXTRACT_HEAT = canExtractHeat;
            M_CAN_RECEIVE_HEAT = canReceiveHeat;
            M_GET_TEMPERATURE = getTemperature;
            M_IS_OVERHEATED = isOverheated;
            M_GET_HEAT_LOSS_PER_TICK = getHeatLossPerTick;
            M_GET_MAX_RECEIVE = getMaxReceive;
            M_GET_MAX_EXTRACT = getMaxExtract;
        }

        static IHeatStorage wrap(Object target) {
            if (I_HEAT_STORAGE == null || !I_HEAT_STORAGE.isInstance(target)) return null;
            return new HeatCapabilityAdapter(target);
        }

        private final Object target;

        private HeatCapabilityAdapter(Object target) { this.target = target; }

        private long invokeLong(Method m, Object... args) {
            try { Object v = m.invoke(target, args); return v instanceof Number n ? n.longValue() : 0; }
            catch (Throwable t) { return 0; }
        }
        private int invokeInt(Method m, Object... args) {
            try { Object v = m.invoke(target, args); return v instanceof Number n ? n.intValue() : 0; }
            catch (Throwable t) { return 0; }
        }
        private boolean invokeBool(Method m, Object... args) {
            try { Object v = m.invoke(target, args); return v instanceof Boolean b ? b : false; }
            catch (Throwable t) { return false; }
        }

        @Override public long getHeatStored() { return invokeLong(M_GET_HEAT_STORED); }
        @Override public long getMaxHeatStored() { return invokeLong(M_GET_MAX_HEAT_STORED); }
        @Override public long receiveHeat(long maxReceive, boolean simulate) { return invokeLong(M_RECEIVE_HEAT, maxReceive, simulate); }
        @Override public long extractHeat(long maxExtract, boolean simulate) { return invokeLong(M_EXTRACT_HEAT, maxExtract, simulate); }
        @Override public boolean canExtractHeat() { return invokeBool(M_CAN_EXTRACT_HEAT); }
        @Override public boolean canReceiveHeat() { return invokeBool(M_CAN_RECEIVE_HEAT); }
        @Override public int getTemperature() { return invokeInt(M_GET_TEMPERATURE); }
        @Override public boolean isOverheated() { return invokeBool(M_IS_OVERHEATED); }
        @Override public long getHeatLossPerTick() { return invokeLong(M_GET_HEAT_LOSS_PER_TICK); }
        @Override public long getMaxReceive() { return invokeLong(M_GET_MAX_RECEIVE); }
        @Override public long getMaxExtract() { return invokeLong(M_GET_MAX_EXTRACT); }
    }

    // ========== 动能存储适配器 ==========

    private static final class KineticCapabilityAdapter implements IKineticStorage {
        private static final Class<?> I_KINETIC_STORAGE;
        private static final Method M_GET_KINETIC_STORED;
        private static final Method M_GET_MAX_KINETIC_STORED;
        private static final Method M_RECEIVE_KINETIC;
        private static final Method M_EXTRACT_KINETIC;
        private static final Method M_CAN_EXTRACT_KINETIC;
        private static final Method M_CAN_RECEIVE_KINETIC;
        private static final Method M_GET_RPM;
        private static final Method M_IS_OVERSPEED;
        private static final Method M_GET_KINETIC_LOSS_PER_TICK;
        private static final Method M_GET_MAX_RECEIVE;
        private static final Method M_GET_MAX_EXTRACT;

        static {
            ClassLoader cl = MioIcifCapabilitiesImpl.class.getClassLoader();
            Class<?> storage = null;
            Method getKineticStored = null, getMaxKineticStored = null, receiveKinetic = null, extractKinetic = null;
            Method canExtractKinetic = null, canReceiveKinetic = null, getRPM = null, isOverspeed = null, getKineticLossPerTick = null;
            Method getMaxReceive = null, getMaxExtract = null;
            try {
                storage = cl.loadClass("com.singularity_iteration.mio_icif.energy.kinetic.IKineticStorage");
                getKineticStored = storage.getMethod("getKineticStored");
                getMaxKineticStored = storage.getMethod("getMaxKineticStored");
                receiveKinetic = storage.getMethod("receiveKinetic", long.class, boolean.class);
                extractKinetic = storage.getMethod("extractKinetic", long.class, boolean.class);
                canExtractKinetic = storage.getMethod("canExtractKinetic");
                canReceiveKinetic = storage.getMethod("canReceiveKinetic");
                getRPM = storage.getMethod("getRPM");
                isOverspeed = storage.getMethod("isOverspeed");
                getKineticLossPerTick = storage.getMethod("getKineticLossPerTick");
                getMaxReceive = storage.getMethod("getMaxReceive");
                getMaxExtract = storage.getMethod("getMaxExtract");
            } catch (Throwable ignored) {
                storage = null;
            }
            I_KINETIC_STORAGE = storage;
            M_GET_KINETIC_STORED = getKineticStored;
            M_GET_MAX_KINETIC_STORED = getMaxKineticStored;
            M_RECEIVE_KINETIC = receiveKinetic;
            M_EXTRACT_KINETIC = extractKinetic;
            M_CAN_EXTRACT_KINETIC = canExtractKinetic;
            M_CAN_RECEIVE_KINETIC = canReceiveKinetic;
            M_GET_RPM = getRPM;
            M_IS_OVERSPEED = isOverspeed;
            M_GET_KINETIC_LOSS_PER_TICK = getKineticLossPerTick;
            M_GET_MAX_RECEIVE = getMaxReceive;
            M_GET_MAX_EXTRACT = getMaxExtract;
        }

        static IKineticStorage wrap(Object target) {
            if (I_KINETIC_STORAGE == null || !I_KINETIC_STORAGE.isInstance(target)) return null;
            return new KineticCapabilityAdapter(target);
        }

        private final Object target;

        private KineticCapabilityAdapter(Object target) { this.target = target; }

        private long invokeLong(Method m, Object... args) {
            try { Object v = m.invoke(target, args); return v instanceof Number n ? n.longValue() : 0; }
            catch (Throwable t) { return 0; }
        }
        private int invokeInt(Method m, Object... args) {
            try { Object v = m.invoke(target, args); return v instanceof Number n ? n.intValue() : 0; }
            catch (Throwable t) { return 0; }
        }
        private boolean invokeBool(Method m, Object... args) {
            try { Object v = m.invoke(target, args); return v instanceof Boolean b ? b : false; }
            catch (Throwable t) { return false; }
        }

        @Override public long getKineticStored() { return invokeLong(M_GET_KINETIC_STORED); }
        @Override public long getMaxKineticStored() { return invokeLong(M_GET_MAX_KINETIC_STORED); }
        @Override public long receiveKinetic(long maxReceive, boolean simulate) { return invokeLong(M_RECEIVE_KINETIC, maxReceive, simulate); }
        @Override public long extractKinetic(long maxExtract, boolean simulate) { return invokeLong(M_EXTRACT_KINETIC, maxExtract, simulate); }
        @Override public boolean canExtractKinetic() { return invokeBool(M_CAN_EXTRACT_KINETIC); }
        @Override public boolean canReceiveKinetic() { return invokeBool(M_CAN_RECEIVE_KINETIC); }
        @Override public int getRPM() { return invokeInt(M_GET_RPM); }
        @Override public boolean isOverspeed() { return invokeBool(M_IS_OVERSPEED); }
        @Override public long getKineticLossPerTick() { return invokeLong(M_GET_KINETIC_LOSS_PER_TICK); }
        @Override public long getMaxReceive() { return invokeLong(M_GET_MAX_RECEIVE); }
        @Override public long getMaxExtract() { return invokeLong(M_GET_MAX_EXTRACT); }
    }
}