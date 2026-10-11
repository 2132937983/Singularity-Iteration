package com.miophas.singularity_iteration.core.api.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Callback for work completion.
 *
 * <p>The machine calls this callback when it completes one processing cycle.
 * Use it to run custom logic after a recipe completes (for example statistics or notifications).
 *
 * <p>Example:
 * <pre>{@code
 * IWorkCompleteCallback callback = (level, pos, output) -> {
 *     logger.info("Machine at " + pos + " completed work, output: " + output);
 * };
 * }</pre>
 */
@FunctionalInterface
public interface IWorkCompleteCallback {

    /**
     * Called when the work completes.
     *
     * @param level the world
     * @param pos the machine position
     * @param output the output item (can be empty)
     */
    void onWorkComplete(Level level, BlockPos pos, ItemStack output);
}