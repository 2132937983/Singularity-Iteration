package com.miophas.singularity_iteration.core.api.util;

/**
 * Interface for a debuggable block.
 * <p>
 * Equivalent to {@code IDebuggable} in IC2 1.12.2.
 * A block that implements this interface shows debug information in debug mode (wrench, shift + right-click).
 */
public interface IDebuggable {

    /**
     * Checks if this block is debuggable.
     *
     * @return true if the block is debuggable
     */
    boolean isDebuggable();

    /**
     * Gets the debug information text.
     *
     * @return the debug information
     */
    String getDebugText();
}
