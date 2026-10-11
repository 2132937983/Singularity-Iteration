package com.miophas.singularity_iteration.core.api.machine.builder;

import com.miophas.singularity_iteration.core.api.machine.builder.IMachineBuilderAPI.IMachineDefinition;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * Builder for kinetic machines.
 *
 * <p>Use it to create kinetic machines (KU system) in the mio_icif style.
 *
 * <p>Example:
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

    // ========== Basic configuration ==========

    IKineticMachineBuilder setName(String name);

    IKineticMachineBuilder setTranslationKey(String translationKey);

    IKineticMachineBuilder setBlockProperties(BlockBehaviour.Properties properties);

    // ========== Kinetic configuration ==========

    /**
     * Sets the kinetic capacity (KU).
     */
    IKineticMachineBuilder setKineticCapacity(long capacity);

    /**
     * Sets the kinetic output (KU/t).
     */
    IKineticMachineBuilder setKineticOutput(long kineticOutput);

    /**
     * Sets the kinetic transfer rate (KU/t).
     */
    IKineticMachineBuilder setKineticTransferRate(long transferRate);

    /**
     * Sets the kinetic input rate (KU/t).
     */
    IKineticMachineBuilder setKineticInputRate(long inputRate);

    // ========== Rotor configuration ==========

    /**
     * Sets whether the machine needs a rotor.
     */
    IKineticMachineBuilder setRequiresRotor(boolean requiresRotor);

    /**
     * Sets the rotor type.
     */
    IKineticMachineBuilder setRotorType(RotorType rotorType);

    /**
     * Sets the rotor wear rate.
     */
    IKineticMachineBuilder setRotorWearRate(int wearRate);

    // ========== Slot configuration ==========

    IKineticMachineBuilder addRotorSlot(int index);

    IKineticMachineBuilder addBatterySlot(int index);

    IKineticMachineBuilder addInputSlot(int index, int maxStackSize);

    IKineticMachineBuilder addOutputSlot(int index, int maxStackSize);

    /**
     * Sets the machine type.
     */
    IKineticMachineBuilder setMachineType(com.miophas.singularity_iteration.core.api.machine.IMachineAPI.MachineType machineType);

    // ========== Event callbacks ==========

    IKineticMachineBuilder onKineticGenerate(KineticCallback callback);

    IKineticMachineBuilder onRotorBreak(RotorBreakCallback callback);

    // ========== Build ==========

    /**
     * Sets the registered BlockEntityType.
     *
     * <p>After an addon mod creates the BlockEntityType during its own mod initialization,
     * it uses this method to link the type to the builder. The definition that {@link #build()} returns then contains a valid entityType.
     *
     * @param entityType the registered BlockEntityType
     * @return this builder
     */
    IKineticMachineBuilder withEntityType(net.minecraft.world.level.block.entity.BlockEntityType<?> entityType);

    IMachineDefinition build();

    /**
     * Builds and registers the kinetic machine.
     *
     * <p>If a BlockEntityType was set through {@link #withEntityType(BlockEntityType)},
     * the returned definition contains that entityType and is ready to use.
     *
     * @param modId the mod ID
     * @return the machine definition
     */
    IMachineDefinition buildAndRegister(String modId);

    /**
     * Returns the machine configuration record.
     *
     * <p>It supplies all parameters that are necessary to create the machine, so that addon mods can register the BlockEntityType themselves.
     *
     * @return the machine configuration record
     */
    IMachineBuilderAPI.MachineConfiguration getConfiguration();

    // ========== Enums and callbacks ==========

    enum RotorType {
        WIND,       // Wind rotor
        WATER,      // Water rotor
        STEAM,      // Steam rotor
        MANUAL,     // Manual rotor
        STIRLING    // Stirling rotor
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