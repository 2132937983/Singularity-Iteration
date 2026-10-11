package com.miophas.singularity_iteration.core.api.machine.builder;

import com.miophas.singularity_iteration.core.api.machine.builder.IMachineBuilderAPI.IMachineDefinition;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * Builder for heat machines.
 *
 * <p>Use it to create heat machines (HU system) in the mio_icif style.
 *
 * <p>Example:
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

    // ========== Basic configuration ==========

    IHeatMachineBuilder setName(String name);

    IHeatMachineBuilder setTranslationKey(String translationKey);

    IHeatMachineBuilder setBlockProperties(BlockBehaviour.Properties properties);

    // ========== Heat configuration ==========

    /**
     * Sets the heat capacity.
     */
    IHeatMachineBuilder setHeatCapacity(long capacity);

    /**
     * Sets the heat output (HU/t).
     */
    IHeatMachineBuilder setHeatOutput(long heatOutput);

    /**
     * Sets the heat transfer rate.
     */
    IHeatMachineBuilder setHeatTransferRate(long transferRate);

    /**
     * Sets the heat input rate.
     */
    IHeatMachineBuilder setHeatInputRate(long inputRate);

    /**
     * Sets the operating temperature.
     */
    IHeatMachineBuilder setOperatingTemperature(long temperature);

    /**
     * Sets the maximum temperature.
     */
    IHeatMachineBuilder setMaxTemperature(long maxTemperature);

    // ========== Work configuration ==========

    /**
     * Sets if the machine needs fuel.
     */
    IHeatMachineBuilder setRequiresFuel(boolean requiresFuel);

    /**
     * Sets if the machine needs fluid cooling.
     */
    IHeatMachineBuilder setRequiresCooling(boolean requiresCooling);

    /**
     * Sets the coolant type.
     */
    IHeatMachineBuilder setCoolantType(String fluidId);

    // ========== Slot configuration ==========

    IHeatMachineBuilder addInputSlot(int index, int maxStackSize);

    IHeatMachineBuilder addOutputSlot(int index, int maxStackSize);

    IHeatMachineBuilder addFuelSlot(int index);

    IHeatMachineBuilder addFluidTank(int capacity);

    /**
     * Sets the machine type.
     */
    IHeatMachineBuilder setMachineType(com.miophas.singularity_iteration.core.api.machine.IMachineAPI.MachineType machineType);

    // ========== Event callbacks ==========

    IHeatMachineBuilder onHeatGenerate(HeatCallback callback);

    IHeatMachineBuilder onOverheat(OverheatCallback callback);

    // ========== Build ==========

    /**
     * Sets the registered BlockEntityType.
     *
     * <p>An addon mod creates the BlockEntityType during its own mod initialization.
     * Then it calls this method to attach the type to the builder, so that the definition from {@link #build()} contains a valid entityType.
     *
     * @param entityType the registered BlockEntityType
     * @return this builder
     */
    IHeatMachineBuilder withEntityType(net.minecraft.world.level.block.entity.BlockEntityType<?> entityType);

    IMachineDefinition build();

    /**
     * Builds and registers the heat machine.
     *
     * <p>If you set the BlockEntityType with {@link #withEntityType(BlockEntityType)},
     * the returned definition contains that entityType and is ready to use.
     *
     * @param modId the mod ID
     * @return the machine definition
     */
    IMachineDefinition buildAndRegister(String modId);

    /**
     * Gets the machine configuration record.
     *
     * <p>It gives all the parameters necessary to create the machine, so that addon mods can register their own BlockEntityType.
     *
     * @return the machine configuration record
     */
    IMachineBuilderAPI.MachineConfiguration getConfiguration();

    // ========== Callback interfaces ==========

    @FunctionalInterface
    interface HeatCallback {
        void onHeatGenerate(net.minecraft.world.level.Level world, net.minecraft.core.BlockPos pos, long amount);
    }

    @FunctionalInterface
    interface OverheatCallback {
        void onOverheat(net.minecraft.world.level.Level world, net.minecraft.core.BlockPos pos, long currentHeat, long maxHeat);
    }
}