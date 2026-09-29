package com.miophas.singularity_iteration.core.prefab.component;

/** Implement on a custom BlockEntity to participate without inheriting a core entity class. */
public interface EnergyComponentHost {
    EnergyComponent energyComponent();
}
