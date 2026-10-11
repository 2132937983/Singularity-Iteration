package com.miophas.singularity_iteration.core.api.machine.builder;

import com.miophas.singularity_iteration.core.api.machine.builder.IMachineBuilderAPI.IMachineDefinition;
import com.miophas.singularity_iteration.core.api.energy.ICableTier;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * Generator builder.
 *
 * <p>Use it to make generators in the mio_icif style.
 *
 * <p>Example:
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

    // ========== Basic configuration ==========

    IGeneratorBuilder setName(String name);

    IGeneratorBuilder setTranslationKey(String translationKey);

    IGeneratorBuilder setBlockProperties(BlockBehaviour.Properties properties);

    // ========== Energy configuration ==========

    /**
     * Sets the generation rate in EU/t.
     */
    IGeneratorBuilder setGenerationRate(long generationRate);

    /**
     * Sets the energy capacity in EU.
     */
    IGeneratorBuilder setEnergyCapacity(long capacity);

    /**
     * Sets the maximum output rate in EU/t.
     */
    IGeneratorBuilder setMaxExtract(long maxExtract);

    /**
     * Sets the cable tier.
     */
    IGeneratorBuilder setCableTier(ICableTier tier);

    /**
     * Applies the default template values for the given tier.
     */
    IGeneratorBuilder useDefaultsForTier(ICableTier tier);

    // ========== Generator type configuration ==========

    /**
     * Sets the generator type.
     */
    IGeneratorBuilder setGeneratorType(GeneratorType type);

    /**
     * Sets if the generator needs a clear view of the sky (for example solar).
     */
    IGeneratorBuilder setRequiresSky(boolean requiresSky);

    /**
     * Sets if the generator works only during the day.
     */
    IGeneratorBuilder setDayOnly(boolean dayOnly);

    /**
     * Sets if the generator works only during the night.
     */
    IGeneratorBuilder setNightOnly(boolean nightOnly);

    /**
     * Sets if the weather has an effect on the generator.
     */
    IGeneratorBuilder setWeatherDependent(boolean weatherDependent);

    /**
     * Sets if the generator needs fuel.
     */
    IGeneratorBuilder setRequiresFuel(boolean requiresFuel);

    /**
     * Sets if the generator needs fluid fuel.
     */
    IGeneratorBuilder setRequiresFluidFuel(boolean requiresFluidFuel);

    /**
     * Sets if the generator needs a rotor (wind or water).
     */
    IGeneratorBuilder setRequiresRotor(boolean requiresRotor);

    // ========== Slot configuration ==========

    /**
     * Adds a fuel slot at the given slot index.
     */
    IGeneratorBuilder addFuelSlot(int index);

    /**
     * Adds a battery slot at the given slot index. The generator charges the item in this slot.
     */
    IGeneratorBuilder addBatterySlot(int index);

    /**
     * Adds a fluid tank with the given capacity in mB.
     */
    IGeneratorBuilder addFluidTank(int capacity);

    // ========== Efficiency configuration ==========

    /**
     * Sets the base efficiency.
     */
    IGeneratorBuilder setBaseEfficiency(double efficiency);

    /**
     * Sets if the efficiency changes with height (wind generator).
     */
    IGeneratorBuilder setHeightEfficiency(boolean heightEfficiency);

    /**
     * Sets if the efficiency changes with the biome.
     */
    IGeneratorBuilder setBiomeEfficiency(boolean biomeEfficiency);

    /**
     * Sets the machine type.
     */
    IGeneratorBuilder setMachineType(com.miophas.singularity_iteration.core.api.machine.IMachineAPI.MachineType machineType);

    // ========== Event callbacks ==========

    /**
     * Sets the callback for the start of generation.
     */
    IGeneratorBuilder onGenerationStart(GenerationCallback callback);

    /**
     * Sets the callback that runs for each tick of generation.
     */
    IGeneratorBuilder onGenerate(GenerationCallback callback);

    /**
     * Sets the callback for fuel consumption.
     */
    IGeneratorBuilder onFuelConsume(FuelConsumeCallback callback);

    // ========== Build ==========

    /**
     * Sets the registered BlockEntityType.
     *
     * <p>An addon mod creates its BlockEntityType during its own mod initialization.
     * Then it calls this method to link the type to the builder.
     * After this, the definition that {@link #build()} returns contains a valid entityType.
     *
     * @param entityType the registered BlockEntityType
     * @return this builder
     */
    IGeneratorBuilder withEntityType(net.minecraft.world.level.block.entity.BlockEntityType<?> entityType);

    IMachineDefinition build();

    /**
     * Builds and registers the generator.
     *
     * <p>If you set a BlockEntityType with {@link #withEntityType(BlockEntityType)},
     * the returned definition contains that entityType. You can use it directly.
     *
     * @param modId the mod ID
     * @return the machine definition
     */
    IMachineDefinition buildAndRegister(String modId);

    /**
     * Gets the machine configuration record.
     *
     * <p>The record contains all parameters that are necessary to create the machine.
     * Addon mods use it to register their own BlockEntityType.
     *
     * @return the machine configuration record
     */
    IMachineBuilderAPI.MachineConfiguration getConfiguration();

    // ========== Enums and callbacks ==========

    enum GeneratorType {
        SOLAR,          // Solar
        WIND,           // Wind
        WATER,          // Water
        THERMAL,        // Thermal
        KINETIC,        // Kinetic
        NUCLEAR,        // Nuclear
        SEMI_FLUID,     // Semi-fluid
        GEO,            // Geothermal
        RTG,            // Radioisotope
        FUSION,         // Fusion
        CUSTOM          // Custom
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