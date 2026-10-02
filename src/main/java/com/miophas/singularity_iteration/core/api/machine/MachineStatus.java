package com.miophas.singularity_iteration.core.api.machine;

/**
 * Coarse machine state shown by the front status lamp, readable from a distance:
 * <ul>
 *   <li>{@link #OFF} - dark: idle, nothing to do and no energy (e.g. freshly placed)</li>
 *   <li>{@link #RUNNING} - green: working / generating</li>
 *   <li>{@link #NO_POWER} - blinking amber: has work but too little energy; generators: low-output standby</li>
 *   <li>{@link #BLOCKED} - red: output full, input material missing, or stopped by redstone</li>
 * </ul>
 * Evaluated on the server ({@code AbstractEnergyBlockEntity#machineStatus()}), synced to clients
 * only when it changes; the client block model reads it from the block entity.
 */
public enum MachineStatus {
    OFF, RUNNING, NO_POWER, BLOCKED;

    private static final MachineStatus[] VALUES = values();

    public static MachineStatus byId(int id) {
        return id >= 0 && id < VALUES.length ? VALUES[id] : OFF;
    }
}
