package com.miophas.singularity_iteration.core.api.machine.builder;

import com.miophas.singularity_iteration.core.api.machine.builder.IMachineBuilderAPI.IMachineDefinition;
import com.miophas.singularity_iteration.core.api.energy.ICableTier;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.state.BlockBehaviour;


/**
 * Builder for electric machines.
 *
 * <p>Use this builder to make electric machines in the mio_icif style.
 * The API is fluent. An addon mod sets the machine properties one step at a time.
 *
 * <p>Example:
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
 *         // Custom logic when work starts
 *     })
 *     .onWorkComplete((world, pos, output) -> {
 *         // Custom logic when work completes
 *     })
 *     .build();
 * }</pre>
 */
public interface IElectricMachineBuilder {

    // ========== Basic settings ==========

    /**
     * Sets the machine ID (the unique identifier).
     *
     * @param name the machine name
     * @return this builder, for chained calls
     */
    IElectricMachineBuilder setName(String name);

    /**
     * Sets the translation key.
     *
     * @param translationKey the translation key
     * @return this builder
     */
    IElectricMachineBuilder setTranslationKey(String translationKey);

    /**
     * Sets the block properties.
     *
     * @param properties the block properties
     * @return this builder
     */
    IElectricMachineBuilder setBlockProperties(BlockBehaviour.Properties properties);

    // ========== Energy settings ==========

    /**
     * Sets the energy capacity.
     *
     * @param capacity the capacity, in EU
     * @return this builder
     */
    IElectricMachineBuilder setEnergyCapacity(long capacity);

    /**
     * Sets the maximum input rate.
     *
     * @param maxReceive the maximum input, in EU/t
     * @return this builder
     */
    IElectricMachineBuilder setMaxReceive(long maxReceive);

    /**
     * Sets the maximum output rate.
     *
     * @param maxExtract the maximum output, in EU/t
     * @return this builder
     */
    IElectricMachineBuilder setMaxExtract(long maxExtract);

    /**
     * Sets the energy use per tick.
     *
     * @param energyPerTick the energy use, in EU/t
     * @return this builder
     */
    IElectricMachineBuilder setEnergyPerTick(long energyPerTick);

    /**
     * Sets the cable tier. This also sets the related energy parameters.
     *
     * @param tier the cable tier
     * @return this builder
     */
    IElectricMachineBuilder setCableTier(ICableTier tier);

    /**
     * Uses the default template for a tier. This sets the energy parameters from the tier.
     *
     * @param tier the cable tier
     * @return this builder
     */
    IElectricMachineBuilder useDefaultsForTier(ICableTier tier);

    // ========== Work settings ==========

    /**
     * Sets the process time.
     *
     * @param processTime the process time, in ticks
     * @return this builder
     */
    IElectricMachineBuilder setProcessTime(int processTime);

    /**
     * Sets the recipe type.
     *
     * @param recipeType the recipe type
     * @return this builder
     */
    IElectricMachineBuilder setRecipeType(RecipeType<?> recipeType);

    /**
     * Sets whether the machine accepts upgrades.
     *
     * @param supportsUpgrades true if the machine accepts upgrades
     * @return this builder
     */
    IElectricMachineBuilder setSupportsUpgrades(boolean supportsUpgrades);

    // ========== Slot settings ==========

    /**
     * Adds an input slot.
     *
     * @param index the slot index
     * @param maxStackSize the maximum stack size
     * @return this builder
     */
    IElectricMachineBuilder addInputSlot(int index, int maxStackSize);

    /**
     * Adds an output slot.
     *
     * @param index the slot index
     * @param maxStackSize the maximum stack size
     * @return this builder
     */
    IElectricMachineBuilder addOutputSlot(int index, int maxStackSize);

    /**
     * Adds a battery slot.
     *
     * @param index the slot index
     * @return this builder
     */
    IElectricMachineBuilder addBatterySlot(int index);

    /**
     * Adds upgrade slots.
     *
     * @param startIndex the index of the first slot
     * @param count the number of slots
     * @return this builder
     */
    IElectricMachineBuilder addUpgradeSlots(int startIndex, int count);

    /**
     * Uses the standard slot layout.
     *
     * @param inputSlots the number of input slots
     * @param outputSlots the number of output slots
     * @param hasBattery true to add a battery slot
     * @param upgradeSlots the number of upgrade slots
     * @return this builder
     */
    IElectricMachineBuilder useStandardLayout(int inputSlots, int outputSlots, boolean hasBattery, int upgradeSlots);

    // ========== Fluid settings ==========

    /**
     * Sets whether the machine accepts fluids.
     *
     * @param supportsFluids true if the machine accepts fluids
     * @return this builder
     */
    IElectricMachineBuilder setSupportsFluids(boolean supportsFluids);

    /**
     * Adds a fluid tank.
     *
     * @param capacity the capacity, in mB
     * @return this builder
     */
    IElectricMachineBuilder addFluidTank(int capacity);

    /**
     * Sets the machine type.
     *
     * @param machineType the machine type
     * @return this builder
     */
    IElectricMachineBuilder setMachineType(com.miophas.singularity_iteration.core.api.machine.IMachineAPI.MachineType machineType);

    // ========== Event callbacks ==========

    /**
     * Sets the callback for work start.
     *
     * @param callback the callback
     * @return this builder
     */
    IElectricMachineBuilder onWorkStart(MachineWorkCallback callback);

    /**
     * Sets the callback for work completion.
     *
     * @param callback the callback
     * @return this builder
     */
    IElectricMachineBuilder onWorkComplete(MachineWorkCompleteCallback callback);

    /**
     * Sets the callback for each tick update.
     *
     * @param callback the callback
     * @return this builder
     */
    IElectricMachineBuilder onTick(MachineTickCallback callback);

    // ========== Build ==========

    /**
     * Sets the registered BlockEntityType.
     *
     * <p>An addon mod makes the BlockEntityType during its own mod initialization.
     * Then it calls this method to link the type to the builder.
     * The definition from {@link #build()} then has a valid entityType.
     *
     * <p>Example:
     * <pre>{@code
     * // During mod initialization
     * public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GenericMachineBlockEntity>> MY_MACHINE =
     *     BLOCK_ENTITIES.register("my_machine", () ->
     *         BlockEntityType.Builder.of(GenericMachineBlockEntity::new, ModBlocks.MY_MACHINE.get()).build(null));
     *
     * var definition = builder.createElectricMachineBuilder()
     *     .setName("my_machine")
     *     .setEnergyCapacity(10000)
     *     .withEntityType(MY_MACHINE.get())  // Link the registered BlockEntityType
     *     .build();
     * }</pre>
     *
     * @param entityType the registered BlockEntityType
     * @return this builder
     */
    IElectricMachineBuilder withEntityType(net.minecraft.world.level.block.entity.BlockEntityType<?> entityType);

    /**
     * Builds the machine definition.
     *
     * @return the machine definition
     */
    IMachineDefinition build();

    /**
     * Builds and registers the machine.
     *
     * <p>If you set a BlockEntityType with {@link #withEntityType(BlockEntityType)},
     * the returned definition contains that entityType. You can use it directly.
     * If not, the entityType in the returned definition is null. The addon mod must register it.
     *
     * @param modId the mod ID
     * @return the machine definition
     */
    IMachineDefinition buildAndRegister(String modId);

    /**
     * Gets the machine configuration record.
     *
     * <p>The record contains all parameters to make the machine.
     * An addon mod uses it to register its own BlockEntityType.
     *
     * @return the machine configuration record
     */
    IMachineBuilderAPI.MachineConfiguration getConfiguration();

    // ========== Callback interfaces ==========

    /** Called when the machine starts work. Runs on the logical server. */
    @FunctionalInterface
    interface MachineWorkCallback {
        void onWorkStart(net.minecraft.world.level.Level world, net.minecraft.core.BlockPos pos);
    }

    /** Called when the machine completes one operation and makes {@code output}. */
    @FunctionalInterface
    interface MachineWorkCompleteCallback {
        void onWorkComplete(net.minecraft.world.level.Level world, net.minecraft.core.BlockPos pos, net.minecraft.world.item.ItemStack output);
    }

    /** Called each tick. {@code progress} and {@code maxProgress} are in ticks. */
    @FunctionalInterface
    interface MachineTickCallback {
        void onTick(net.minecraft.world.level.Level world, net.minecraft.core.BlockPos pos, int progress, int maxProgress);
    }
}
