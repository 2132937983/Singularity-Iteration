package com.miophas.singularity_iteration.core.api.machine;

import com.miophas.singularity_iteration.core.api.machine.builder.IMachineBuilderAPI;
import org.jetbrains.annotations.Nullable;

/**
 * Interface for configurable machine blocks.
 *
 * <p>Block entities that implement this interface give their machine configuration through {@link #getConfiguration()}.
 * Custom machines of addon mods must implement this interface to support machine type queries at the API layer.
 */
public interface IMachineConfigurable {

    /**
     * Returns the machine configuration.
     *
     * @return the machine configuration, or null if no configuration is set
     */
    @Nullable
    IMachineBuilderAPI.MachineConfiguration getConfiguration();
}
