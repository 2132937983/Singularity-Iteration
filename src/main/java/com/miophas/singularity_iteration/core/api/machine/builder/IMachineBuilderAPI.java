package com.miophas.singularity_iteration.core.api.machine.builder;

import com.miophas.singularity_iteration.core.api.energy.ICableTier;
import com.miophas.singularity_iteration.core.api.machine.ISlotType;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Machine builder API.
 *
 * <p>This API gives factory methods for addon mods. Use them to make machines in the mio_icif style.
 * This is the core API for addon development. It contains all the complex logic of machine creation.
 *
 * <p>Example:
 * <pre>{@code
 * // Get the API
 * IMachineBuilderAPI builder = MioIcifAPI.instance().getMachineBuilderAPI();
 *
 * // Make a simple electric machine
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
 * // Register the machine
 * builder.registerMachine("mymod", myMachine);
 * }</pre>
 */
public interface IMachineBuilderAPI {

    /**
     * Makes a new electric machine builder.
     *
     * @return a new electric machine builder instance
     */
    IElectricMachineBuilder createElectricMachineBuilder();

    /**
     * Makes a new generator builder.
     *
     * @return a new generator builder instance
     */
    IGeneratorBuilder createGeneratorBuilder();

    /**
     * Makes a new heat machine builder.
     *
     * @return a new heat machine builder instance
     */
    IHeatMachineBuilder createHeatMachineBuilder();

    /**
     * Makes a new kinetic machine builder.
     *
     * @return a new kinetic machine builder instance
     */
    IKineticMachineBuilder createKineticMachineBuilder();

    /**
     * Makes a new energy storage block builder.
     *
     * @return a new energy storage block builder instance
     */
    IEnergyContainerBuilder createEnergyContainerBuilder();

    /**
     * Registers a custom machine in the system.
     *
     * <p><strong>Note: this method only puts the machine definition in an internal registry.
     * It does not register anything in a NeoForge registry.</strong>
     * The addon mod must register its Block, BlockEntity and BlockEntityType itself.
     * Use this method to:
     * <ul>
     *   <li>Let the API find the machine (for example, with getMachineDefinition)</li>
     *   <li>Let the machine use the upgrade system</li>
     *   <li>Let the machine use the energy grid</li>
     * </ul>
     *
     * @param modId the addon mod ID
     * @param machine the machine definition
     * @return the registered BlockEntityType (can be null)
     */
    BlockEntityType<?> registerMachine(String modId, IMachineDefinition machine);

    /**
     * Registers a custom machine in the system, with a BlockEntityType.
     *
     * <p>NeoForge requires registration of a BlockEntityType during mod initialization.
     * Thus, the addon mod must first make its own BlockEntityType. Then it registers it with this method.
     *
     * @param modId the addon mod ID
     * @param machine the machine definition
     * @param blockEntityType the BlockEntityType that is already registered
     * @return the same BlockEntityType that you gave
     */
    default BlockEntityType<?> registerMachine(String modId, IMachineDefinition machine, BlockEntityType<?> blockEntityType) {
        // Make a new definition that contains the BlockEntityType
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
     * Gets a registered machine definition.
     *
     * @param modId the mod ID
     * @param machineId the machine ID
     * @return the machine definition, or null if it does not exist
     */
    IMachineDefinition getMachineDefinition(String modId, String machineId);

    /**
     * Checks if a machine ID is registered.
     *
     * @param modId the mod ID
     * @param machineId the machine ID
     * @return true if the machine is registered
     */
    boolean isMachineRegistered(String modId, String machineId);

    /**
     * Registers a named custom machine type.
     *
     * <p><b>Note:</b> {@link com.miophas.singularity_iteration.core.api.machine.IMachineAPI.MachineType}
     * is a fixed enum (it contains {@code CUSTOM}). You cannot add enum constants at runtime. Thus, this method only
     * records {@code modId:name} in the global registry of custom type names, and returns {@code CUSTOM}.
     * Addon mods can get the recorded name back with {@link #getRegisteredCustomMachineTypeName}.
     * Use the name to show the custom type in the UI or in logs.</p>
     *
     * @param modId the addon mod ID
     * @param name the custom type name
     * @return always {@link com.miophas.singularity_iteration.core.api.machine.IMachineAPI.MachineType#CUSTOM}
     */
    com.miophas.singularity_iteration.core.api.machine.IMachineAPI.MachineType registerCustomMachineType(String modId, String name);

    /**
     * Finds a recorded custom machine type name.
     *
     * @param modId the addon mod ID
     * @param name the custom type name
     * @return the recorded name, or an empty Optional if it is not recorded
     */
    Optional<String> getRegisteredCustomMachineTypeName(String modId, String name);

    /**
     * Makes a standard slot layout (input + output + battery + upgrade).
     *
     * @param inputSlots the number of input slots
     * @param outputSlots the number of output slots
     * @param hasBatterySlot true if the layout has a battery slot
     * @param upgradeSlots the number of upgrade slots
     * @return the slot layout definition
     */
    ISlotLayout createStandardLayout(int inputSlots, int outputSlots, boolean hasBatterySlot, int upgradeSlots);

    /**
     * Makes a slot layout with special slots.
     *
     * @param inputSlots the number of input slots
     * @param outputSlots the number of output slots
     * @param hasBatterySlot true if the layout has a battery slot
     * @param upgradeSlots the number of upgrade slots
     * @param specialSlots the special slot configuration
     * @return the slot layout definition
     */
    ISlotLayout createLayoutWithSpecialSlots(int inputSlots, int outputSlots, boolean hasBatterySlot, int upgradeSlots, ISlotLayout.SlotType[] specialSlots);

    /**
     * Makes a slot layout for a fluid machine (input + output + battery + upgrade + fluid).
     *
     * @param inputSlots the number of input slots
     * @param outputSlots the number of output slots
     * @param hasBatterySlot true if the layout has a battery slot
     * @param upgradeSlots the number of upgrade slots
     * @param fluidTanks the number of fluid tanks
     * @param tankCapacity the capacity of each fluid tank (mB)
     * @return the slot layout definition
     */
    ISlotLayout createFluidLayout(int inputSlots, int outputSlots, boolean hasBatterySlot, int upgradeSlots, int fluidTanks, int tankCapacity);

    /**
     * Gets the default machine property template for a tier.
     *
     * @param tier the cable tier
     * @return the default properties
     */
    IMachineDefaults getDefaultsForTier(ICableTier tier);

    /**
     * Machine definition. It describes a complete machine.
     */
    interface IMachineDefinition {
        String getId();
        String getModId();
        String getTranslationKey();

        /**
         * Gets the BlockEntityType of the machine.
         *
         * <p>Note: for a definition made by {@link IElectricMachineBuilder#build()} or a similar method,
         * this method can return {@code null}. The addon mod must make its own BlockEntityType. Then it must call
         * {@link IMachineBuilderAPI#registerMachine(String, IMachineDefinition, BlockEntityType)}
         * to complete the registration.
         *
         * <p>Example:
         * <pre>{@code
         * // Make the BlockEntityType during mod initialization
         * public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GenericMachineBlockEntity>> MY_MACHINE =
         *     BLOCK_ENTITIES.register("my_machine", () ->
         *         BlockEntityType.Builder.of(GenericMachineBlockEntity::new, ModBlocks.MY_MACHINE.get()).build(null));
         *
         * // Make the machine definition
         * IMachineDefinition definition = builder.createElectricMachineBuilder()
         *     .setName("my_machine")
         *     .setEnergyCapacity(10000)
         *     .build();
         *
         * // Register the machine (give the BlockEntityType)
         * builder.registerMachine("mymod", definition, MY_MACHINE.get());
         * }</pre>
         *
         * @return the BlockEntityType, or null if none was given
         */
        @Nullable
        BlockEntityType<?> getBlockEntityType();
        IMachineProperties getProperties();

        /**
         * Gets the full configuration record that was used to make this machine.
         *
         * <p>NeoForge requires registration of a BlockEntityType during mod initialization.
         * Thus, {@link #getBlockEntityType()} can be null. Addon mods can get the configuration
         * with this method and then make the BlockEntityType themselves.
         *
         * @return the machine configuration record
         */
        MachineConfiguration getConfiguration();
    }

    /**
     * Machine properties.
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
     * Slot layout for builders only.
     *
     * <p>This interface extends {@link com.miophas.singularity_iteration.core.api.machine.ISlotLayout} and adds the methods that builders need.
     * Default methods connect the two APIs. Thus, you can give a builder layout directly to the query API.
     */
    interface ISlotLayout extends com.miophas.singularity_iteration.core.api.machine.ISlotLayout {

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
        default com.miophas.singularity_iteration.core.api.machine.ISlotType getType(int slotIndex) {
            return getSlotType(slotIndex);
        }

        @Override
        default int getCount(com.miophas.singularity_iteration.core.api.machine.ISlotType type) {
            if (type instanceof SlotType st) return getCountByBuilderType(st);
            return 0;
        }

        @Override
        default int[] getSlotsOfType(com.miophas.singularity_iteration.core.api.machine.ISlotType type) {
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
         * Slot type enum.
         * 
         * <p>Slot types for builders only. It contains more specialized slot types
         * (for example, HAMMER_HEAD and CUTTER_HEAD).
         * 
         * <p>Mapping to {@link com.miophas.singularity_iteration.core.api.machine.IMachineAPI.SlotType}:
         * <ul>
         *   <li>HAMMER_HEAD → HAMMER_TOOL, HAMMER_PLATE → HAMMER_INPUT, HAMMER_HANDLE → HAMMER_OUTPUT</li>
         *   <li>CUTTER_HEAD → CUTTER_TOOL, CUTTER_PLATE → CUTTER_INPUT, CUTTER_HANDLE → CUTTER_OUTPUT</li>
         *   <li>All other types use the same name</li>
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
                return name().toLowerCase(java.util.Locale.ROOT);
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
             * Maps this type to the SlotType of the general query API.
             * Types with no match map to {@code CUSTOM}.
             */
            public com.miophas.singularity_iteration.core.api.machine.IMachineAPI.SlotType toApiSlotType() {
                return switch (this) {
                    case HAMMER_HEAD -> com.miophas.singularity_iteration.core.api.machine.IMachineAPI.SlotType.HAMMER_TOOL;
                    case HAMMER_PLATE -> com.miophas.singularity_iteration.core.api.machine.IMachineAPI.SlotType.HAMMER_INPUT;
                    case HAMMER_HANDLE -> com.miophas.singularity_iteration.core.api.machine.IMachineAPI.SlotType.HAMMER_OUTPUT;
                    case CUTTER_HEAD -> com.miophas.singularity_iteration.core.api.machine.IMachineAPI.SlotType.CUTTER_TOOL;
                    case CUTTER_PLATE -> com.miophas.singularity_iteration.core.api.machine.IMachineAPI.SlotType.CUTTER_INPUT;
                    case CUTTER_HANDLE -> com.miophas.singularity_iteration.core.api.machine.IMachineAPI.SlotType.CUTTER_OUTPUT;
                    default -> {
                        try {
                            yield com.miophas.singularity_iteration.core.api.machine.IMachineAPI.SlotType.valueOf(this.name());
                        } catch (IllegalArgumentException e) {
                            yield com.miophas.singularity_iteration.core.api.machine.IMachineAPI.SlotType.CUSTOM;
                        }
                    }
                };
            }
        }
    }

    /**
     * Default machine properties.
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
     * Machine configuration record.
     *
     * <p>Contains all parameters to make a machine. When an addon mod uses a builder,
     * it can get the full machine configuration from this record. It then uses these parameters to make its own Block, BlockEntity and BlockEntityType.
     *
     * @param name the machine name
     * @param translationKey the translation key
     * @param blockProperties the block properties
     * @param energyCapacity the energy capacity (EU)
     * @param maxReceive the maximum input rate (EU/t)
     * @param maxExtract the maximum output rate (EU/t)
     * @param energyPerTick the energy use per tick (EU/t)
     * @param cableTier the cable tier. Note: this field is null for machines that are not electric (heat or kinetic)
     * @param processTime the process time (ticks)
     * @param recipeType the recipe type
     * @param supportsUpgrades true if the machine accepts upgrades
     * @param supportsFluids true if the machine accepts fluids
     * @param inputSlots the number of input slots
     * @param outputSlots the number of output slots
     * @param hasBatterySlot true if the layout has a battery slot
     * @param upgradeSlots the number of upgrade slots
     * @param fluidTanks the number of fluid tanks
     * @param tankCapacity the capacity of each fluid tank (mB)
     * @param machineType the machine type. Note: this field is null if the builder did not call setMachineType()
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
        @Nullable com.miophas.singularity_iteration.core.api.machine.IMachineAPI.MachineType machineType
    ) {
        /**
         * Gets the total number of item slots (input + output + battery + upgrade).
         */
        public int getTotalSlots() {
            return inputSlots + outputSlots + (hasBatterySlot ? 1 : 0) + upgradeSlots;
        }

        /**
         * Gets the battery slot index.
         *
         * @return the index, or -1 if there is no battery slot
         */
        public int getBatterySlotIndex() {
            return hasBatterySlot ? inputSlots + outputSlots : -1;
        }

        /**
         * Gets the index of the first upgrade slot.
         */
        public int getUpgradeSlotStart() {
            return inputSlots + outputSlots + (hasBatterySlot ? 1 : 0);
        }
    }
}