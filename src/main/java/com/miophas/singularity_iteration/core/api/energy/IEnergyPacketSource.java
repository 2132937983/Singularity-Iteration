package com.miophas.singularity_iteration.core.api.energy;

/**
 * Optional policy for entities using an owned core energy storage/component.
 * This interface alone does not register a tile or authorize foreign balance writes.
 * The engine samples it on the server thread; the getter must not mutate the world.
 * A changed policy revokes pending routing work at the next quote/commit validation.
 * Native output and FE extraction still share the storage's maxExtract allowance.
 */
public interface IEnergyPacketSource {
    EnergyPacketPolicy getEnergyPacketPolicy();
}
