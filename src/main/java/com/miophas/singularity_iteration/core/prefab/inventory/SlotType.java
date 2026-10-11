package com.miophas.singularity_iteration.core.prefab.inventory;

import com.miophas.singularity_iteration.core.api.machine.ISlotType;

public enum SlotType implements ISlotType {
    INPUT,
    OUTPUT,
    BATTERY,
    FLUID_INPUT,
    FLUID_OUTPUT,
    UPGRADE,
    EXTRA,
    ROTOR,
    TURBINE,
    REACTOR,
    COIL,
    HEATING,
    FUEL,
    MEMORY,
    TOOL,
    NUCLEAR,
    DRILL,
    SCANNER,
    MINING_PIPE,
    FILTER,
    HEAT_CONDUCTOR,
    HAMMER_TOOL,    // Forge hammer tool slot
    HAMMER_INPUT,   // Forge hammer input slot
    HAMMER_OUTPUT,  // Forge hammer output slot
    CUTTER_TOOL,    // Plate cutter tool slot
    CUTTER_INPUT,   // Plate cutter input slot
    CUTTER_OUTPUT,  // Plate cutter output slot
    RTG_PELLET;      // Radioisotope pellet slot (for the RTG; holds one item only)

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