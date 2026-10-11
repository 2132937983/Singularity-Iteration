package com.miophas.singularity_iteration.core.api.machine;

import com.miophas.singularity_iteration.core.api.energy.ICableTier;
import net.minecraft.core.BlockPos;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Machine API (aggregate interface).
 *
 * <p>This interface is the single entry point for all machine sub-APIs.
 * It extends these sub-interfaces:
 * <ul>
 *   <li>{@link IMachineQueryAPI} - detect, query and bulk-find machines</li>
 *   <li>{@link IMachineControlAPI} - start, stop and control machine state</li>
 *   <li>{@link IMachineSlotAPI} - slot layout and item operations</li>
 *   <li>{@link IMachineUpgradeOpsAPI} - install and remove upgrades</li>
 *   <li>{@link IUpgradeStatsAPI} - query upgrade statistics</li>
 *   <li>{@link IGeneratorQueryAPI} - query generator fuel and burn state</li>
 *   <li>{@link IMultiblockReactorAPI} - manage reactor multiblock structures</li>
 *   <li>{@link IRedstoneAPI} - control redstone signals</li>
 * </ul>
 *
 * <p>Addon mods should use the specific sub-interface that they need.
 * Do not depend directly on this aggregate interface.
 *
 * <p>Example:
 * <pre>{@code
 * IMachineAPI machineAPI = MioIcifAPI.instance().getMachineAPI();
 *
 * // Check if a machine is at the position
 * if (machineAPI.isMachine(world, pos)) {
 *     IMachineInfo info = machineAPI.getMachineInfo(world, pos);
 *     boolean working = info.isWorking();
 *     int progress = info.getProgress();
 * }
 * }</pre>
 */
public interface IMachineAPI
        extends IMachineQueryAPI, IMachineControlAPI, IMachineSlotAPI,
                IMachineUpgradeOpsAPI, IUpgradeStatsAPI, IGeneratorQueryAPI,
                IMultiblockReactorAPI, IRedstoneAPI {

    // ========== Nested types ==========

    /**
     * Machine type.
     */
    enum MachineType {
        GENERATOR,      // Generator
        PROCESSOR,      // Processing machine (compressor, extractor, and other)
        SMELTER,        // Electric furnace
        MACERATOR,      // Macerator
        COMPRESSOR,     // Compressor
        EXTRACTOR,      // Extractor
        CENTRIFUGE,     // Centrifuge
        WASHER,         // Ore washer
        RECYCLER,       // Recycler
        METAL_FORMER,   // Metal former
        ELECTROLYZER,   // Electrolyzer
        BLENDER,        // Blender
        CUTTER,         // Cutter
        WELDER,         // Welder
        LATHE,          // Lathe
        ROLLING,        // Rolling machine
        EXTRUDER,       // Extruder
        FERMENTER,      // Fermenter
        CANNER,         // Canner
        FLUID_SOLID,    // Fluid/solid canner
        THERMAL_CENTRIFUGE, // Thermal centrifuge
        LASER_ENGRAVER, // Laser engraver
        PRECISION_ASSEMBLER, // Precision assembler
        VACUUM_FREEZER, // Vacuum freezer
        PLASMA_FURNACE, // Plasma furnace
        FUSION_REACTOR, // Fusion reactor
        MATTER_FABRICATOR, // Matter fabricator
        REPLICATOR,     // Replicator
        SCANNER,        // Scanner
        TELEPORTER,     // Teleporter
        PUMP,           // Pump
        MINER,          // Miner
        INDUCTION,      // Induction furnace
        CONDENSER,      // Condenser
        TERRAFORMER,    // Terraformer
        HARVESTER,      // Harvester
        MATRON,         // Matter replicator
        MAGNETIZER,     // Magnetizer
        TESLA_COIL,     // Tesla coil
        ADVANCED_MINER, // Advanced miner
        TRANSFORMER,    // Transformer
        PIPE,           // Pipe
        ENERGY_CONVERTER, // Energy converter
        NEUTRON_POLYMERIZER, // Particle polymerization generator
        CUSTOM          // Custom type
    }

    /**
     * Redstone mode.
     */
    enum RedstoneMode {
        /** No redstone control. */
        NONE(0),
        /** Emits a signal only when the energy is exactly full (100%). */
        FULL(1),
        /** Emits a signal when the energy is in the middle range. */
        PARTIAL(2),
        /** Emits a signal when the energy is not full. */
        NOT_FULL(3),
        /** Emits a signal only when the energy is exactly empty (0%). */
        EMPTY(4),
        /** Stops output when the block gets a redstone signal. */
        INVERTED(5),
        /** Stops output when the block gets a redstone signal. Continues output when the energy is exactly full. */
        CONDITIONAL(6),
        /** Outputs energy only when the block gets a redstone signal. */
        REDSTONE_ON(7);

        private final int id;

        RedstoneMode(int id) {
            this.id = id;
        }

        public int getId() {
            return id;
        }

        public static RedstoneMode fromId(int id) {
            for (RedstoneMode mode : values()) {
                if (mode.id == id) return mode;
            }
            return NONE;
        }
    }

    enum MetalFormerMode {
        ROLLING(0),
        CUTTING(1),
        EXTRUDING(2);

        private final int id;

        MetalFormerMode(int id) {
            this.id = id;
        }

        public int getId() {
            return id;
        }

        public static MetalFormerMode fromId(int id) {
            for (MetalFormerMode mode : values()) {
                if (mode.id == id) return mode;
            }
            return ROLLING;
        }
    }

    /**
     * Slot type.
     */
    enum SlotType implements ISlotType {
        INPUT,
        OUTPUT,
        UPGRADE,
        TOOL,
        SCANNER,
        MEMORY,
        FLUID_INPUT,
        FLUID_OUTPUT,
        FUEL,
        BATTERY,
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
        HAMMER_TOOL,
        HAMMER_INPUT,
        HAMMER_OUTPUT,
        CUTTER_TOOL,
        CUTTER_INPUT,
        CUTTER_OUTPUT,
        RTG_PELLET,
        CUSTOM;

        @Override
        public String getName() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }

        @Override
        public boolean isInput() {
            return this == INPUT || this == FLUID_INPUT || this == HAMMER_INPUT || this == CUTTER_INPUT;
        }

        @Override
        public boolean isOutput() {
            return this == OUTPUT || this == FLUID_OUTPUT || this == HAMMER_OUTPUT || this == CUTTER_OUTPUT;
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
    }

    /**
     * Information about a multiblock structure.
     */
    interface IMultiblockInfo {
        boolean isStructureValid();
        int getStructureBlockCount();
        java.util.Set<BlockPos> getStructureBlocks();
        long getFormationTime();
        java.util.Map<String, Object> getStructureData();
        java.util.Set<BlockPos> getRedstonePorts();
    }

    /**
     * Machine configuration.
     */
    interface IMachineConfiguration {
        MachineType getMachineType();
        ICableTier getCableTier();
        long getEnergyCapacity();
        long getMaxReceive();
        long getMaxExtract();
        int getBaseProgress();
        long getBaseEnergyPerTick();
        int getSlotCount();
    }

    // ========== Extensible machine type registration ==========

    /**
     * Metadata record for a machine type.
     *
     * <p>Use this when {@link MachineType#CUSTOM} cannot fully describe a custom machine.
     * Register the type with its metadata through {@link MachineTypeRegistry#register(MachineTypeInfo)}.
     *
     * @param id          unique type identifier (recommended format: {@code modid:type_name})
     * @param displayName display name (for the HUD and logs)
     * @param parentType  parent type (for categories and compatibility checks)
     * @param metadata    additional metadata
     */
    record MachineTypeInfo(
        String id,
        String displayName,
        MachineType parentType,
        Map<String, Object> metadata
    ) {
        /**
         * Short constructor. It uses an empty metadata map.
         */
        public MachineTypeInfo(String id, String displayName, MachineType parentType) {
            this(id, displayName, parentType, Collections.emptyMap());
        }
    }

    /**
     * Registry for custom machine types.
     *
     * <p>Use it to register and query custom machine types at runtime.
     * The {@link MachineType} enum cannot be extended, so this registry adds that function.
     * The registry is thread-safe.
     *
     * <h3>Example</h3>
     * <pre>{@code
     * // Register a custom type
     * MachineTypeRegistry.register(new IMachineAPI.MachineTypeInfo(
     *     "mymod:quantum_assembler",
     *     "Quantum Assembler",
     *     IMachineAPI.MachineType.PROCESSOR
     * ));
     *
     * // Query the type information
     * Optional<IMachineAPI.MachineTypeInfo> info = MachineTypeRegistry.get("mymod:quantum_assembler");
     * }</pre>
     */
    final class MachineTypeRegistry {
        private static final Map<String, MachineTypeInfo> REGISTRY = new ConcurrentHashMap<>();

        private MachineTypeRegistry() {
        }

        /**
         * Registers a custom machine type.
         * If a type with the same ID exists, this method replaces it.
         *
         * @param info type metadata (must not be null)
         * @return the previous value for this ID, or null if no previous value exists
         */
        public static MachineTypeInfo register(MachineTypeInfo info) {
            return REGISTRY.put(info.id(), info);
        }

        /**
         * Gets the registered type information for an ID.
         *
         * @param id type ID
         * @return the type information, or an empty Optional if the ID is not registered
         */
        public static java.util.Optional<MachineTypeInfo> get(String id) {
            return java.util.Optional.ofNullable(REGISTRY.get(id));
        }

        /**
         * Gets all registered custom types.
         *
         * @return a read-only view of the registry (not a copy; it shows later registrations)
         */
        public static Map<String, MachineTypeInfo> getAll() {
            return Collections.unmodifiableMap(REGISTRY);
        }

        /**
         * Checks if a type with the given ID is registered.
         */
        public static boolean isRegistered(String id) {
            return REGISTRY.containsKey(id);
        }
    }
}