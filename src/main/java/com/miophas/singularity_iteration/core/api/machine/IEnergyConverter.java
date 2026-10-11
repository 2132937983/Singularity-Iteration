package com.miophas.singularity_iteration.core.api.machine;

import com.miophas.singularity_iteration.core.api.energy.ICableTier;

/**
 * Energy converter interface.
 *
 * <p>It describes blocks that convert between energy types,
 * for example EU ↔ FE (Forge Energy) or EU ↔ KU (Kinetic Units).
 *
 * <h3>Energy types</h3>
 * <ul>
 *   <li>{@link EnergyType#EU}  — IndustrialCraft energy unit</li>
 *   <li>{@link EnergyType#FE}  — Forge Energy (general energy)</li>
 *   <li>{@link EnergyType#KU}  — Kinetic Units</li>
 *   <li>{@link EnergyType#HE}  — Heat Energy</li>
 * </ul>
 */
public interface IEnergyConverter extends IEnergyBlock {

    /**
     * Energy type.
     */
    enum EnergyType {
        /** EU — IndustrialCraft energy */
        EU,
        /** FE — Forge Energy */
        FE,
        /** KU — Kinetic Units */
        KU,
        /** HE — Heat Energy */
        HE,
        /** Custom type */
        CUSTOM
    }

    /**
     * Gets the energy type of the input side.
     * @return the input energy type
     */
    EnergyType getInputType();

    /**
     * Gets the energy type of the output side.
     * @return the output energy type
     */
    EnergyType getOutputType();

    /**
     * Gets the conversion ratio.
     * <p>Example: an EU → FE ratio of 4.0 means 1 EU = 4 FE.
     * @return the conversion ratio (output/input)
     */
    double getConversionRatio();

    /**
     * Gets the cable tier of the input side.
     * @return the input side tier
     */
    ICableTier getInputCableTier();

    /**
     * Gets the cable tier of the output side.
     * @return the output side tier
     */
    ICableTier getOutputCableTier();

    /**
     * Gets the conversion efficiency (0.0 - 1.0).
     * @return the efficiency
     */
    default double getEfficiency() {
        return 1.0;
    }

    /**
     * Gets the energy in the internal buffer now (in the output energy type).
     * @return the energy in the buffer now
     */
    long getBufferAmount();

    /**
     * Gets the capacity of the internal buffer (in the output energy type).
     * @return the buffer capacity
     */
    long getBufferCapacity();
}
