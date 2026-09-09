package com.singularity_iteration.mio_icif.Blocks.entity.slot;

import com.singularity_iteration.mio_icif.api.machine.ISlotType;

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
    HAMMER_TOOL,    // 锻造锤工具槽
    HAMMER_INPUT,   // 锻造锤输入槽
    HAMMER_OUTPUT,  // 锻造锤输出槽
    CUTTER_TOOL,    // 板材切割剪工具槽
    CUTTER_INPUT,   // 板材切割剪输入槽
    CUTTER_OUTPUT,  // 板材切割剪输出槽
    RTG_PELLET;      // 放射性同位素靶丸槽（RTG用，限制只能放一个）

    @Override
    public String getName() {
        return name().toLowerCase();
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