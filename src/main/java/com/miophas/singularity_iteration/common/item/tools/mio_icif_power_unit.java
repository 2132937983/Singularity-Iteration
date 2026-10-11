package com.miophas.singularity_iteration.common.item.tools;

/**
 * Power unit.
 * It extends the electric tool base class.
 * It is a component in recipes for the mining drill, the chainsaw and other electric tools.
 */
@SuppressWarnings("null")
public class mio_icif_power_unit extends mio_icif_tool_elc {

    // Default maximum energy of the power unit.
    public static final int POWER_UNIT_MAX_ENERGY = 10000;

    // Default energy that the power unit uses for each use.
    public static final int POWER_UNIT_ENERGY_PER_USE = 50;

    /**
     * Default constructor. The item starts with no energy.
     * @param properties the item properties
     */
    public mio_icif_power_unit(Properties properties) {
        super(properties, POWER_UNIT_MAX_ENERGY, POWER_UNIT_MAX_ENERGY, "power_unit", POWER_UNIT_MAX_ENERGY, POWER_UNIT_ENERGY_PER_USE, 1);
    }

    /**
     * Constructor with an initial energy value.
     * @param properties the item properties
     * @param initialEnergy the initial energy
     */
    public mio_icif_power_unit(Properties properties, int initialEnergy) {
        super(properties, POWER_UNIT_MAX_ENERGY, POWER_UNIT_MAX_ENERGY - initialEnergy, "power_unit", POWER_UNIT_MAX_ENERGY, POWER_UNIT_ENERGY_PER_USE, 1);
    }

    /**
     * Constructor with a maximum energy value and an initial energy value.
     * @param properties the item properties
     * @param maxEnergy the maximum energy
     * @param initialEnergy the initial energy
     */
    public mio_icif_power_unit(Properties properties, int maxEnergy, int initialEnergy) {
        super(properties, maxEnergy, maxEnergy - initialEnergy, "power_unit", maxEnergy, POWER_UNIT_ENERGY_PER_USE, 1);
    }

    /**
     * Constructor with all parameters.
     * @param properties the item properties
     * @param maxEnergy the maximum energy
     * @param initialEnergy the initial energy
     * @param chargeRate the charge rate
     * @param energyPerUse the energy that each use consumes
     * @param toolTier the tool tier
     */
    public mio_icif_power_unit(Properties properties, int maxEnergy, int initialEnergy, int chargeRate, int energyPerUse, int toolTier) {
        super(properties, maxEnergy, maxEnergy - initialEnergy, "power_unit", chargeRate, energyPerUse, toolTier);
    }
}

