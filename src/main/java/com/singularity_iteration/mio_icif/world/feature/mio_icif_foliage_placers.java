package com.singularity_iteration.mio_icif.world.feature;

import com.singularity_iteration.mio_icif.Singularity_Iteration;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacerType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

@SuppressWarnings("null")
public class mio_icif_foliage_placers {
    public static final DeferredRegister<FoliagePlacerType<?>> FOLIAGE_PLACERS =
        DeferredRegister.create(Registries.FOLIAGE_PLACER_TYPE, Singularity_Iteration.MOD_ID);

    public static final DeferredHolder<FoliagePlacerType<?>, FoliagePlacerType<RubberTreeFoliagePlacer>> RUBBER_TREE =
        FOLIAGE_PLACERS.register("rubber_tree", () -> new FoliagePlacerType<>(RubberTreeFoliagePlacer.CODEC));

    public static void register(IEventBus eventBus) {
        FOLIAGE_PLACERS.register(eventBus);
        Singularity_Iteration.LOGGER.info("Foliage placer types registered");
    }
}

