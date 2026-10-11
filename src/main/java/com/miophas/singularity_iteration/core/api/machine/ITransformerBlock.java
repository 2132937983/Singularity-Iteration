package com.miophas.singularity_iteration.core.api.machine;

import com.miophas.singularity_iteration.core.api.energy.ICableTier;
import net.minecraft.core.Direction;

/**
 * Interface for transformer blocks.
 *
 * <p>A transformer is core power infrastructure. It converts energy between voltage tiers.
 * A block that implements this interface can step up energy from the low-voltage side to the high-voltage side.
 * It can also step down energy from the high-voltage side to the low-voltage side.
 *
 * <h3>Modes</h3>
 * <ul>
 *   <li>{@link TransformerMode#STEP_UP}   - step-up mode: input on the low-voltage side, output on the high-voltage side</li>
 *   <li>{@link TransformerMode#STEP_DOWN} - step-down mode: input on the high-voltage side, output on the low-voltage side</li>
 * </ul>
 */
public interface ITransformerBlock extends IEnergyBlock {

    /**
     * Transformer mode.
     */
    enum TransformerMode {
        /** Step-up mode: low voltage to high voltage. */
        STEP_UP,
        /** Step-down mode: high voltage to low voltage. */
        STEP_DOWN
    }

    /**
     * Gets the current transformer mode.
     * @return the transformer mode
     */
    TransformerMode getTransformerMode();

    /**
     * Sets the transformer mode.
     * @param mode the new mode
     */
    void setTransformerMode(TransformerMode mode);

    /**
     * Gets the cable tier of the low-voltage side.
     * @return the low-voltage tier
     */
    ICableTier getLowTier();

    /**
     * Gets the cable tier of the high-voltage side.
     * @return the high-voltage tier
     */
    ICableTier getHighTier();

    /**
     * Gets the cable tier of the current input side. It depends on the mode.
     * @return the input-side tier
     */
    ICableTier getInputTier();

    /**
     * Gets the cable tier of the current output side. It depends on the mode.
     * @return the output-side tier
     */
    ICableTier getOutputTier();

    /**
     * Gets the energy in the internal buffer.
     * @return the current buffer energy in EU
     */
    long getInternalBuffer();

    /**
     * Gets the capacity of the internal buffer.
     * @return the buffer capacity in EU
     */
    long getBufferCapacity();

    /**
     * Checks if the given side is connected to the transformer.
     * @param side the side
     * @return true if the side is connected
     */
    boolean isConnected(Direction side);
}
