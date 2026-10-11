package com.miophas.singularity_iteration.common.multiblock;

import com.miophas.singularity_iteration.common.block.energycontainer.mio_icif_gesu_core;
import com.miophas.singularity_iteration.common.block.generator.mio_icif_Block_Nuclear_Reactor_Generator;
import com.miophas.singularity_iteration.common.block.producer.mio_icif_block_large_fabricator_core;
import com.miophas.singularity_iteration.core.runtime.multiblock.MultiblockControllers;

/**
 * Registers the builtin controller blocks in the core registry {@code MultiblockControllers}.
 *
 * <p>The structure search in {@link mio_icif_multiblock_manager#notifyBlockChanged}
 * uses this registry to find the validator for a controller block. The registry
 * matches the exact block class. A subclass of a registered block is not found.
 *
 * <p>This class registers the GESU, the Large Fabricator and the Fluid Reactor.
 * It does not register the Oil Rig and the Dimension Oil Rig. Those two structures
 * form only from the tick of their controller block entity.
 *
 * <p>The structure search lives in core. This class only holds the list.
 */
public final class BuiltinMultiblockControllers {

    private BuiltinMultiblockControllers() {}

    /**
     * Adds the builtin controllers to the registry.
     *
     * <p>Call this method one time during mod setup, before a world loads.
     * Each factory makes a new validator for each formation attempt.
     */
    public static void install() {
        MultiblockControllers.register(mio_icif_gesu_core.class,
            () -> new ToCoreValidatorAdapter(new mio_icif_gesu_validator()), "GESU");
        MultiblockControllers.register(mio_icif_block_large_fabricator_core.class,
            () -> new ToCoreValidatorAdapter(new mio_icif_large_fabricator_validator()), "LargeFabricator");
        MultiblockControllers.register(mio_icif_Block_Nuclear_Reactor_Generator.class,
            () -> new ToCoreValidatorAdapter(new mio_icif_fluid_reactor_validator()), "Reactor");
    }
}
