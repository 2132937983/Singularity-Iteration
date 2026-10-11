package com.miophas.singularity_iteration.core.api.block;

import net.minecraft.world.level.block.state.BlockState;

/**
 * Rubber wood block interface.
 *
 * <p>The mio_icif rubber wood blocks ({@code block_rub_wood}, {@code block_have_rub_wood}) implement this interface.
 * It gives one meaning for these states: "the block has resin", "the block has a tap hole", and "how to remove or restore the resin".
 *
 * <p>Add-on mods can:
 * <ul>
 *   <li>use {@link IRubberTreeAPI} to query the state of any rubber wood without a dependency on a specific block class;</li>
 *   <li>make a custom log implement this interface. Then the resin extractor and the collection API of this mod handle it the same way.</li>
 * </ul>
 *
 * <p>Implementation rules: {@link #isTappable(BlockState)} describes the structural condition (for example, a tap hole is present).
 * {@link #hasResin(BlockState)} describes if resin is available to collect now. A rubber wood can be tapped only when
 * both are true.
 */
public interface IRubberWood {

    /**
     * Checks if this block state has resin to collect now.
     *
     * @param state the block state
     * @return true if it has resin
     */
    boolean hasResin(BlockState state);

    /**
     * Checks if this block state can be tapped (for example, the rubber wood has a tap hole).
     *
     * <p>This method does not check if resin is present now. It describes only if the structure allows the resin extractor to tap it.
     *
     * @param state the block state
     * @return true if it can be tapped
     */
    boolean isTappable(BlockState state);

    /**
     * Returns the block state with a changed resin state.
     *
     * @param state    the original block state
     * @param hasResin true to set resin, false to remove resin
     * @return the changed block state
     */
    BlockState withResin(BlockState state, boolean hasResin);

    /**
     * Checks if this block state can grow resin again naturally (on a random tick).
     *
     * <p>The default implementation returns "can be tapped and has no resin now".
     *
     * @param state the block state
     * @return true if resin can grow again
     */
    default boolean canRegrowResin(BlockState state) {
        return isTappable(state) && !hasResin(state);
    }
}
