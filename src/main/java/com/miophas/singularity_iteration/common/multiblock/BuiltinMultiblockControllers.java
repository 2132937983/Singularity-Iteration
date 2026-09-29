package com.miophas.singularity_iteration.common.multiblock;

import com.miophas.singularity_iteration.common.block.energycontainer.mio_icif_gesu_core;
import com.miophas.singularity_iteration.common.block.generator.mio_icif_Block_Nuclear_Reactor_Generator;
import com.miophas.singularity_iteration.common.block.producer.mio_icif_block_large_fabricator_core;
import com.miophas.singularity_iteration.core.runtime.multiblock.MultiblockControllers;

/** 内置多方块控制方块的注册清单；结构发现本身在 core。 */
public final class BuiltinMultiblockControllers {

    private BuiltinMultiblockControllers() {}

    public static void install() {
        MultiblockControllers.register(mio_icif_gesu_core.class,
            () -> new ToCoreValidatorAdapter(new mio_icif_gesu_validator()), "GESU");
        MultiblockControllers.register(mio_icif_block_large_fabricator_core.class,
            () -> new ToCoreValidatorAdapter(new mio_icif_large_fabricator_validator()), "LargeFabricator");
        MultiblockControllers.register(mio_icif_Block_Nuclear_Reactor_Generator.class,
            () -> new ToCoreValidatorAdapter(new mio_icif_fluid_reactor_validator()), "Reactor");
    }
}
