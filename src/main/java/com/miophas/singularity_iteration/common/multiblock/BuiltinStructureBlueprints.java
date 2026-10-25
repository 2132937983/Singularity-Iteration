// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.multiblock;

import com.miophas.singularity_iteration.core.api.multiblock.StructureBlueprint;
import com.miophas.singularity_iteration.core.api.multiblock.StructureBlueprint.BlockRef;
import com.miophas.singularity_iteration.core.api.multiblock.StructureBlueprintRegistry;
import net.minecraft.resources.ResourceLocation;

/**
 * 0.1.10.19: the build plans of the SI multiblocks and placement rules for the JEI category "Multiblock
 * Structures" and the guide ({@code <SiStructure id="mio_icif:..."/>}). Each plan follows its validator in
 * this package (the shape and the count rules in the validator comments), or the face rules of the blocks
 * (blast furnace / fermenter heat port). Blocks are given by id, so the plans can be built before the
 * block registry is complete. Rows go from north to south, the characters from west to east.
 */
public final class BuiltinStructureBlueprints {
    private BuiltinStructureBlueprints() {}

    private static final String KEY = "structure.mio_icif.";

    public static void install() {
        nuclearReactor();
        fluidReactor();
        gesu();
        largeFabricator();
        oilRig();
        dimensionOilRig();
        heated("blast_furnace", block("producer/block_blast_furnace"), block("producer/block_blast_furnace_advanced"));
        heated("fermenter", block("producer/block_fermenter_elc"));
    }

    /** Nuclear Reactor Generator with a Reactor Chamber on each face (chambers are optional: 3 + chambers columns). */
    private static void nuclearReactor() {
        register(StructureBlueprint.builder(id("nuclear_reactor"))
            .layer("...", ".K.", "...")
            .layer(".K.", "KRK", ".K.")
            .layer("...", ".K.", "...")
            .key('R', block("generator/block_nuclear_reactor_generator"))
            .key('K', KEY + "nuclear_reactor.chamber", block("reactor/block_reactor_chamber"))
            .controller('R')
            .note(KEY + "nuclear_reactor.note.columns")
            .note(KEY + "nuclear_reactor.note.shared")
            .note(KEY + "nuclear_reactor.note.chunk"));
    }

    /** 5x5x5 Fluid Reactor (mio_icif_fluid_reactor_validator): reactor, 6 chambers, 98 shell blocks with at least 1 port. */
    private static void fluidReactor() {
        String[] shell = {"SSSSS", "SSSSS", "SSSSS", "SSSSS", "SSSSS"};
        String[] ring = {"SSSSS", "S...S", "S.K.S", "S...S", "SSSSS"};
        register(StructureBlueprint.builder(id("fluid_reactor"))
            .layer(shell)
            .layer(ring)
            .layer("SSFSS", "S.K.S", "HKRKD", "S.K.S", "SSSSS")
            .layer(ring)
            .layer(shell)
            .key('R', block("generator/block_nuclear_reactor_generator"))
            .key('K', block("reactor/block_reactor_chamber"))
            .key('S', KEY + "fluid_reactor.shell", block("reactor/block_reactor_vessel"))
            .key('F', KEY + "fluid_reactor.port", block("reactor/block_reactor_fluid_port"))
            .key('H', KEY + "fluid_reactor.port", block("reactor/block_reactor_access_hatch"))
            .key('D', KEY + "fluid_reactor.port", block("reactor/block_reactor_redstone_port"))
            .controller('R')
            .note(KEY + "fluid_reactor.note.ports")
            .note(KEY + "fluid_reactor.note.inner"));
    }

    /** GESU (mio_icif_gesu_validator): a module on each face of the core, any type in any order. */
    private static void gesu() {
        register(StructureBlueprint.builder(id("gesu"))
            .layer("...", ".I.", "...")
            .layer(".I.", "ICO", ".O.")
            .layer("...", ".O.", "...")
            .key('C', block("wiring/block_gesu_core"))
            .key('I', KEY + "gesu.input", block("wiring/block_gesu_input_iv"))
            .key('O', KEY + "gesu.output", block("wiring/block_gesu_output_iv"), block("wiring/block_gesu_output_luv"))
            .controller('C')
            .note(KEY + "gesu.note"));
    }

    /** Large Fabricator (mio_icif_large_fabricator_validator): 6 modules, at least 1 input and 1 tank, at most 1 scrap module. */
    private static void largeFabricator() {
        register(StructureBlueprint.builder(id("large_fabricator"))
            .layer("...", ".T.", "...")
            .layer(".E.", "ECT", ".S.")
            .layer("...", ".E.", "...")
            .key('C', block("producer/block_large_fabricator_core"))
            .key('E', KEY + "large_fabricator.input", block("producer/block_large_fabricator_input_iv"))
            .key('T', KEY + "large_fabricator.tank", block("producer/block_large_fabricator_tank"))
            .key('S', KEY + "large_fabricator.scrap", block("producer/block_large_fabricator_scrap"))
            .controller('C')
            .note(KEY + "large_fabricator.note"));
    }

    /** Oil Rig (mio_icif_oil_rig_validator): core, 10 modules, 11 titanium frames in a 3x6x3 box. */
    private static void oilRig() {
        StructureBlueprint.Builder plan = StructureBlueprint.builder(id("oil_rig"))
            .layer("BIB", "OCB", "BPB")
            .layers(2, ".F.", "FBF", ".F.")
            .layers(3, "...", ".F.", "...");
        register(rigKeys(plan, "oilrig/block_oil_rig_core").note(KEY + "oil_rig.note"));
    }

    /** Dimension Oil Rig (mio_icif_dimension_oil_rig_validator): core, 26 modules, 24 frames in a 7x8x7 box. */
    private static void dimensionOilRig() {
        String empty = ".......";
        StructureBlueprint.Builder plan = StructureBlueprint.builder(id("dimension_oil_rig"))
            .layer(empty, ".BBBBB.", ".B...B.", ".B...B.", ".B...B.", ".BBBBB.", empty)
            .layer("...F...", empty, "..IPO..", "F.BCB.F", "..OPI..", empty, "...F...")
            .layer(empty, "...F...", "...F...", ".FFBFF.", "...F...", "...F...", empty)
            .layer(empty, empty, "...F...", "..FBF..", "...F...", empty, empty)
            .layers(2, empty, empty, empty, "...F...", empty, empty, empty)
            .layer(empty, empty, "...F...", "..FFF..", "...F...", empty, empty)
            .layer(empty, empty, empty, "...F...", empty, empty, empty);
        register(rigKeys(plan, "oilrig/block_dimension_oil_rig_core").note(KEY + "dimension_oil_rig.note"));
    }

    private static StructureBlueprint.Builder rigKeys(StructureBlueprint.Builder plan, String core) {
        return plan
            .key('C', block(core))
            .key('I', KEY + "oil_rig.input", block("oilrig/block_oil_rig_input"))
            .key('O', KEY + "oil_rig.output", block("oilrig/block_oil_rig_output"))
            .key('P', KEY + "oil_rig.any_module", block("oilrig/block_oil_rig_panel"))
            .key('B', KEY + "oil_rig.any_module", block("oilrig/block_oil_rig_base"))
            .key('F', block("build/block_scaffold_titanium"))
            .controller('C');
    }

    /**
     * A machine that takes heat (HU) only through its front face, with a heat source in front of it: the
     * machine faces east, the heat source faces west, front to front. The heat sources give heat only
     * through their front face.
     */
    private static void heated(String name, BlockRef... machines) {
        BlockRef[] east = new BlockRef[machines.length];
        for (int i = 0; i < machines.length; i++) east[i] = machines[i].with("facing", "east");
        register(StructureBlueprint.builder(id(name))
            .layer("MH")
            .key('M', east)
            .key('H', KEY + "heat_source", heater("hugenerator/block_solid_heat_generator"), heater("hugenerator/block_fluid_heat_generator"),
                heater("hugenerator/block_heat_generator_elc"), heater("hugenerator/block_rt_heat_generator"),
                heater("hugenerator/block_heat_source_fluid"))
            .controller('M')
            .note(KEY + "heated.note.front")
            .note(KEY + name + ".note"));
    }

    private static BlockRef heater(String path) {
        return block(path).with("facing", "west");
    }

    private static BlockRef block(String path) {
        return BlockRef.of(id(path));
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("mio_icif", path);
    }

    private static void register(StructureBlueprint.Builder plan) {
        StructureBlueprintRegistry.register(plan.build());
    }
}
