package com.miophas.singularity_iteration.core.api.capability;

import com.miophas.singularity_iteration.core.api.energy.ICableTier;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.ItemCapability;

/**
 * mio_icif capability system API.
 *
 * <p>This interface defines all capabilities that other mods can use. Add-on mods use the NeoForge
 * Capability system to interact with the blocks and items of this mod.
 *
 * <p>Example:
 * <pre>{@code
 * // Get the EU energy storage of a block.
 * IEUStorage storage = level.getCapability(IMioIcifCapabilities.EU_STORAGE_BLOCK, pos, direction);
 * if (storage != null) {
 *     long stored = storage.getStored();
 *     long capacity = storage.getCapacity();
 * }
 *
 * // Get the EU energy storage of an item.
 * IEUStorage itemStorage = stack.getCapability(IMioIcifCapabilities.EU_STORAGE_ITEM);
 * if (itemStorage != null) {
 *     long received = itemStorage.receiveEnergy(100, false);
 * }
 * }</pre>
 */
public interface IMioIcifCapabilities {

    // ========== Block capabilities ==========

    /**
     * EU energy storage capability (block).
     */
    BlockCapability<IEUStorage, Direction> EU_STORAGE_BLOCK = BlockCapability.create(
        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("mio_icif", "eu_storage"),
        IEUStorage.class,
        Direction.class
    );

    /**
     * Machine information capability (block).
     */
    BlockCapability<IMachineCapability, Direction> MACHINE_BLOCK = BlockCapability.create(
        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("mio_icif", "machine"),
        IMachineCapability.class,
        Direction.class
    );

    /**
     * Generator information capability (block).
     */
    BlockCapability<IGeneratorCapability, Direction> GENERATOR_BLOCK = BlockCapability.create(
        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("mio_icif", "generator"),
        IGeneratorCapability.class,
        Direction.class
    );

    /**
     * Heat storage capability (block).
     */
    BlockCapability<IHeatStorage, Direction> HEAT_STORAGE_BLOCK = BlockCapability.createSided(
        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("mio_icif", "api_heat_storage"),
        IHeatStorage.class
    );

    /**
     * Kinetic storage capability (block).
     */
    BlockCapability<IKineticStorage, Direction> KINETIC_STORAGE_BLOCK = BlockCapability.createSided(
        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("mio_icif", "api_kinetic_storage"),
        IKineticStorage.class
    );

    // ========== Item capabilities ==========

    /**
     * EU energy storage capability (item).
     */
    ItemCapability<IEUStorage, Void> EU_STORAGE_ITEM = ItemCapability.create(
        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("mio_icif", "eu_storage_item"),
        IEUStorage.class,
        Void.class
    );

    /**
     * Electric item capability.
     */
    ItemCapability<ICapabilityElectricItem, Void> ELECTRIC_ITEM = ItemCapability.create(
        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("mio_icif", "electric_item"),
        ICapabilityElectricItem.class,
        Void.class
    );

    /**
     * Upgrade item capability.
     */
    ItemCapability<IUpgradeItem, Void> UPGRADE_ITEM = ItemCapability.create(
        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("mio_icif", "upgrade_item"),
        IUpgradeItem.class,
        Void.class
    );

    /**
     * Reactor component capability.
     */
    ItemCapability<IReactorComponent, Void> REACTOR_COMPONENT = ItemCapability.create(
        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("mio_icif", "reactor_component"),
        IReactorComponent.class,
        Void.class
    );

    // ========== Additional block capabilities ==========

    /**
     * Transformer capability (block).
     */
    BlockCapability<ITransformerCapability, Direction> TRANSFORMER_BLOCK = BlockCapability.create(
        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("mio_icif", "transformer"),
        ITransformerCapability.class,
        Direction.class
    );

    /**
     * Pipe capability (block).
     */
    BlockCapability<IPipeCapability, Direction> PIPE_BLOCK = BlockCapability.create(
        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("mio_icif", "pipe"),
        IPipeCapability.class,
        Direction.class
    );

    /**
     * Energy converter capability (block).
     */
    BlockCapability<IEnergyConverterCapability, Direction> ENERGY_CONVERTER_BLOCK = BlockCapability.create(
        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("mio_icif", "energy_converter"),
        IEnergyConverterCapability.class,
        Direction.class
    );

    // ========== Additional item capabilities ==========

    /**
     * Weapon item capability.
     */
    ItemCapability<IWeaponCapability, Void> WEAPON_ITEM = ItemCapability.create(
        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("mio_icif", "weapon"),
        IWeaponCapability.class,
        Void.class
    );

    /**
     * Solar helmet capability.
     */
    ItemCapability<ISolarHelmetCapability, Void> SOLAR_HELMET_ITEM = ItemCapability.create(
        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("mio_icif", "solar_helmet"),
        ISolarHelmetCapability.class,
        Void.class
    );

    /**
     * Jetpack capability.
     */
    ItemCapability<IJetpackCapability, Void> JETPACK_ITEM = ItemCapability.create(
        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("mio_icif", "jetpack"),
        IJetpackCapability.class,
        Void.class
    );

    /**
     * Fluid cell capability.
     */
    ItemCapability<IFluidCellCapability, Void> FLUID_CELL_ITEM = ItemCapability.create(
        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("mio_icif", "fluid_cell"),
        IFluidCellCapability.class,
        Void.class
    );

    // ========== Factory methods ==========

    /**
     * Creates an EU energy storage.
     *
     * @param capacity energy capacity (EU)
     * @param maxReceive maximum receive rate (EU/t)
     * @param maxExtract maximum extract rate (EU/t)
     * @param tier cable tier
     * @return a new EU storage
     */
    IEUStorage createEUStorage(long capacity, long maxReceive, long maxExtract, ICableTier tier);

    /**
     * Creates an EU energy storage with configurable sides.
     *
     * @param capacity energy capacity (EU)
     * @param maxReceive maximum receive rate (EU/t)
     * @param maxExtract maximum extract rate (EU/t)
     * @param tier cable tier
     * @param inputSides the sides that can receive energy
     * @param outputSides the sides that can output energy
     * @return a new EU storage
     */
    IEUStorage createEUStorage(long capacity, long maxReceive, long maxExtract, ICableTier tier,
                                Direction[] inputSides, Direction[] outputSides);

    /**
     * Creates a heat storage (basic parameters).
     *
     * @param capacity heat capacity
     * @param maxOutput maximum output and input rate
     * @return a new heat storage
     */
    IHeatStorage createHeatStorage(long capacity, long maxOutput);

    /**
     * Creates a heat storage (all parameters).
     *
     * @param capacity heat capacity
     * @param maxReceive maximum receive rate
     * @param maxExtract maximum extract rate
     * @param baseTemp base temperature (degrees Celsius)
     * @param maxTemp maximum temperature (degrees Celsius)
     * @param lossFactor heat loss factor
     * @return a new heat storage
     */
    IHeatStorage createHeatStorage(long capacity, long maxReceive, long maxExtract,
                                   int baseTemp, int maxTemp, float lossFactor);

    /**
     * Creates a kinetic storage (basic parameters).
     *
     * @param capacity kinetic energy capacity
     * @param maxOutput maximum output and input rate
     * @return a new kinetic storage
     */
    IKineticStorage createKineticStorage(long capacity, long maxOutput);

    /**
     * Creates a kinetic storage (all parameters).
     *
     * @param capacity kinetic energy capacity
     * @param maxReceive maximum receive rate
     * @param maxExtract maximum extract rate
     * @param maxRPM maximum rotation speed (RPM)
     * @param frictionFactor friction factor
     * @return a new kinetic storage
     */
    IKineticStorage createKineticStorage(long capacity, long maxReceive, long maxExtract,
                                         int maxRPM, float frictionFactor);

    /**
     * Creates an electric item.
     *
     * @param maxCharge maximum charge (EU)
     * @param transferLimit transfer limit (EU per operation)
     * @param tier tier
     * @param canProvide true if the item can supply energy to other items
     * @return a new electric item
     */
    ICapabilityElectricItem createElectricItem(long maxCharge, long transferLimit, int tier, boolean canProvide);

    /**
     * Creates an upgrade item.
     *
     * @param type upgrade type
     * @param speedMultiplier speed multiplier
     * @param energyMultiplier energy multiplier
     * @param extraStorage extra energy storage (EU)
     * @param autoEject true to eject output automatically
     * @param autoImport true to import input automatically
     * @param tierUpgrade number of tiers to add
     * @return a new upgrade item
     */
    IUpgradeItem createUpgradeItem(String type, double speedMultiplier, double energyMultiplier,
                                    int extraStorage, boolean autoEject, boolean autoImport, int tierUpgrade);

    // ========== Capability interface definitions ==========

    /**
     * EU energy storage.
     */
    interface IEUStorage {
        long getStored();
        long getCapacity();
        long receiveEnergy(long maxReceive, boolean simulate);
        long extractEnergy(long maxExtract, boolean simulate);

        /**
         * Uses energy for internal work. The maxExtract limit does not apply.
         *
         * <p>Difference from {@link #extractEnergy}:
         * <ul>
         *   <li>{@code extractEnergy} — output to other blocks. The maxExtract limit applies.</li>
         *   <li>{@code useEnergy} — internal use. The maxExtract limit does not apply.</li>
         * </ul>
         *
         * @param amount amount to use (EU)
         * @param simulate if true, only simulate. The stored energy does not change.
         * @return the energy that was used (EU)
         */
        default long useEnergy(long amount, boolean simulate) {
            long toUse = Math.min(amount, getStored());
            if (!simulate && toUse > 0) {
                setStored(getStored() - toUse);
            }
            return toUse;
        }

        /**
         * Generates energy internally. The maxReceive limit does not apply.
         *
         * <p>Difference from {@link #receiveEnergy}:
         * <ul>
         *   <li>{@code receiveEnergy} — input from outside. The maxReceive limit applies.</li>
         *   <li>{@code generateEnergy} — internal generation. The maxReceive limit does not apply.</li>
         * </ul>
         *
         * @param amount amount to generate (EU)
         * @param simulate if true, only simulate. The stored energy does not change.
         * @return the energy that was generated (EU)
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
         * Sets the stored energy (optional operation).
         * The default implementation throws UnsupportedOperationException. An implementation can override this method.
         */
        default void setStored(long amount) {
            throw new UnsupportedOperationException("setStored not supported by this implementation");
        }

        /**
         * Sets the energy capacity (optional operation).
         * The default implementation throws UnsupportedOperationException. An implementation can override this method.
         */
        default void setCapacity(long capacity) {
            throw new UnsupportedOperationException("setCapacity not supported by this implementation");
        }
    }

    /**
     * Machine capability.
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
     * Generator capability.
     */
    interface IGeneratorCapability {
        boolean isGenerating();
        long getGenerationRate();
        long getCurrentOutput();
        double getEfficiency();
        ICableTier getCableTier();
    }

    /**
     * Heat storage.
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
     * Kinetic storage.
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
     * Electric item capability.
     * <p>
     * This interface is for the NeoForge Capability system. It is different from {@code api.item.electric.IElectricItem}.
     * That interface is the full electric item interface. It also has advanced functions, for example the item manager.
     * This interface is a simplified capability version. It has only charge, discharge and basic property queries.
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
     * Upgrade item.
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
     * Reactor component.
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

    // ========== Additional capability interfaces ==========

    /**
     * Transformer capability.
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
     * Pipe capability.
     */
    interface IPipeCapability {
        enum PipeType { ITEM, FLUID, CABLE, UNIVERSAL }
        PipeType getPipeType();
        java.util.Set<Direction> getConnections();
        boolean isExtracting(Direction side);
        int getTransferRate();
    }

    /**
     * Energy converter capability.
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
     * Weapon item capability.
     */
    interface IWeaponCapability {
        float getDamage();
        float getRange();
        long getEnergyPerShot();
        boolean isRanged();
    }

    /**
     * Solar helmet capability.
     */
    interface ISolarHelmetCapability {
        long getGenerationRate();
        boolean requiresSky();
        boolean isDayOnly();
    }

    /**
     * Jetpack capability.
     */
    interface IJetpackCapability {
        float getThrust();
        long getEnergyPerTickFlying();
        enum JetpackMode { OFF, NORMAL, HOVER, FLIGHT }
        JetpackMode getMode();
    }

    /**
     * Fluid cell capability.
     */
    interface IFluidCellCapability {
        int getCapacity();
        net.neoforged.neoforge.fluids.FluidStack getFluid();
        boolean canHoldFluid(net.minecraft.world.level.material.Fluid fluid);
        int fill(net.neoforged.neoforge.fluids.FluidStack fluid, boolean simulate);
        net.neoforged.neoforge.fluids.FluidStack drain(int amount, boolean simulate);
    }

    // ========== Additional factory methods ==========

    /**
     * Creates a transformer capability.
     *
     * @param isStepUp true for step-up mode
     * @param lowTier tier of the low-voltage side
     * @param highTier tier of the high-voltage side
     * @return a new transformer capability
     */
    ITransformerCapability createTransformer(boolean isStepUp, ICableTier lowTier, ICableTier highTier);

    /**
     * Creates a pipe capability.
     *
     * @param pipeType pipe type
     * @param transferRate transfer rate
     * @return a new pipe capability
     */
    IPipeCapability createPipe(IPipeCapability.PipeType pipeType, int transferRate);

    /**
     * Creates an energy converter capability.
     *
     * @param inputType input energy type
     * @param outputType output energy type
     * @param conversionRatio conversion ratio
     * @return a new energy converter capability
     */
    IEnergyConverterCapability createEnergyConverter(IEnergyConverterCapability.EnergyType inputType, IEnergyConverterCapability.EnergyType outputType, double conversionRatio);

    /**
     * Creates a weapon capability.
     *
     * @param damage damage value
     * @param range range
     * @param energyPerShot energy for each shot (EU)
     * @param isRanged true for a ranged weapon
     * @return a new weapon capability
     */
    IWeaponCapability createWeapon(float damage, float range, long energyPerShot, boolean isRanged);

    /**
     * Creates a solar helmet capability.
     *
     * @param generationRate generation rate (EU/t)
     * @param requiresSky true if the helmet must see the sky
     * @param isDayOnly true if the helmet works only in daytime
     * @return a new solar helmet capability
     */
    ISolarHelmetCapability createSolarHelmet(long generationRate, boolean requiresSky, boolean isDayOnly);

    /**
     * Creates a jetpack capability.
     *
     * @param thrust thrust
     * @param energyPerTickFlying energy used for each tick of flight (EU)
     * @return a new jetpack capability
     */
    IJetpackCapability createJetpack(float thrust, long energyPerTickFlying);

    /**
     * Creates a fluid cell capability.
     *
     * @param capacity capacity (mB)
     * @return a new fluid cell capability
     */
    IFluidCellCapability createFluidCell(int capacity);

    /**
     * Creates a heat storage adapter for a block entity.
     * If the block entity implements the internal IHeatStorage interface, this method returns a wrapper that shows it through the API interface.
     * If the block does not support heat storage, this method returns null.
     *
     * @param blockEntity the target block entity
     * @return the API heat storage, or null if the block does not support it
     */
    IHeatStorage adaptHeatStorage(Object blockEntity);

    /**
     * Creates a kinetic storage adapter for a block entity.
     * If the block entity implements the internal IKineticStorage interface, this method returns a wrapper that shows it through the API interface.
     * If the block does not support kinetic storage, this method returns null.
     *
     * @param blockEntity the target block entity
     * @return the API kinetic storage, or null if the block does not support it
     */
    IKineticStorage adaptKineticStorage(Object blockEntity);
}