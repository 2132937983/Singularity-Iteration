package com.miophas.singularity_iteration.common.blockentity;

import com.miophas.singularity_iteration.common.blockentity.generator.mio_icif_fluid_reactor_handler;
import com.miophas.singularity_iteration.common.blockentity.huentity.hugenerator.mio_icif_heat_source_fluid;
import com.miophas.singularity_iteration.common.blockentity.oilrig.mio_icif_dimension_oil_rig_core_entity;
import com.miophas.singularity_iteration.common.blockentity.oilrig.mio_icif_oil_rig_core_entity;
import com.miophas.singularity_iteration.common.blockentity.producer.mio_icif_condenser;
import com.miophas.singularity_iteration.common.blockentity.producer.mio_icif_large_fabricator_core_entity;
import com.miophas.singularity_iteration.common.blockentity.producer.mio_icif_matter_elc;
import com.miophas.singularity_iteration.common.blockentity.producer.mio_icif_solar_distiller;
import com.miophas.singularity_iteration.common.blockentity.producer.mio_icif_steam_generator;
import com.miophas.singularity_iteration.common.blockentity.producer.mio_icif_steam_repressurizer;

import java.util.ArrayList;
import java.util.List;

/**
 * Every {@link MachineConversion} the mod's machines publish: the outputs that come from machine code
 * instead of a JSON recipe (steam, hot coolant, UU-matter, crude oil). Recipe viewers show these; a
 * machine with a new hardcoded output adds its {@code conversions()} here.
 */
public final class MachineConversions {
    private MachineConversions() {}

    public static List<MachineConversion> all() {
        return all(null);
    }

    /** {@link #all()} and the conversions that recipes add (Mass Fabricator and Large Fabricator scrap amplifiers), when {@code recipes} is given. */
    public static List<MachineConversion> all(@org.jetbrains.annotations.Nullable net.minecraft.world.item.crafting.RecipeManager recipes) {
        List<MachineConversion> all = new ArrayList<>();
        all.addAll(mio_icif_steam_generator.conversions());
        all.addAll(mio_icif_steam_repressurizer.conversions());
        all.addAll(mio_icif_condenser.conversions());
        all.addAll(mio_icif_solar_distiller.conversions());
        all.addAll(mio_icif_heat_source_fluid.conversions());
        all.addAll(mio_icif_fluid_reactor_handler.conversions());
        all.addAll(mio_icif_matter_elc.conversions(recipes));
        all.addAll(mio_icif_large_fabricator_core_entity.conversions(recipes));
        all.addAll(mio_icif_oil_rig_core_entity.conversions());
        all.addAll(mio_icif_dimension_oil_rig_core_entity.conversions());
        return all;
    }
}
