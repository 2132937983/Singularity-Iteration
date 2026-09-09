package com.singularity_iteration.mio_icif.api.recipe;

import com.singularity_iteration.mio_icif.recipe.mio_icif_ModRecipes;
import com.singularity_iteration.mio_icif.recipe.generic.*;
import net.minecraft.world.item.crafting.RecipeType;

/**
 * 配方类型内部持有者的集中适配层。
 *
 * <p>{@code RecipeAPIImpl} 通过此类获取 {@link RecipeType}，
 * 而非直接引用 {@code recipe.mio_icif_ModRecipes} 或 {@code recipe.generic.*Recipes}。
 *
 * <p>此类为包级私有，不对外暴露。
 */
final class RecipeTypeBridge {

    private RecipeTypeBridge() {}

    static RecipeType<?> powder() { return mio_icif_ModRecipes.POWDER_TYPE.get(); }
    static RecipeType<?> compressor() { return mio_icif_ModRecipes.COMPRESSOR_TYPE.get(); }
    static RecipeType<?> extractor() { return mio_icif_ModRecipes.EXTRACTOR_TYPE.get(); }
    static RecipeType<?> washer() { return mio_icif_ModRecipes.WASHER_TYPE.get(); }
    static RecipeType<?> centrifuge() { return mio_icif_ModRecipes.CENTRIFUGE_TYPE.get(); }
    static RecipeType<?> rolling() { return mio_icif_ModRecipes.ROLLING_TYPE.get(); }
    static RecipeType<?> blastFurnace() { return mio_icif_ModRecipes.BLAST_FURNACE_TYPE.get(); }
    static RecipeType<?> canning() { return mio_icif_ModRecipes.CANNING_TYPE.get(); }
    static RecipeType<?> blockCutter() { return mio_icif_ModRecipes.BLOCK_CUTTER_TYPE.get(); }
    static RecipeType<?> cutting() { return mio_icif_ModRecipes.CUTTING_TYPE.get(); }
    static RecipeType<?> extruding() { return mio_icif_ModRecipes.EXTRUDING_TYPE.get(); }
    static RecipeType<?> welder() { return WelderRecipes.WELDER_TYPE.get(); }
    static RecipeType<?> precisionAssembler() { return PrecisionAssemblerRecipes.PRECISION_ASSEMBLER_TYPE.get(); }
    static RecipeType<?> fusionReactor() { return FusionReactorRecipes.FUSION_REACTOR_TYPE.get(); }
    static RecipeType<?> lathe() { return LatheRecipes.LATHE_TYPE.get(); }
    static RecipeType<?> laserEngraver() { return LaserEngraverRecipes.LASER_ENGRAVER_TYPE.get(); }
    static RecipeType<?> massFabricator() { return MassFabricatorRecipes.MASS_FABRICATOR_TYPE.get(); }
    static RecipeType<?> plasmaFurnace() { return PlasmaFurnaceRecipes.PLASMA_FURNACE_TYPE.get(); }
    static RecipeType<?> replicator() { return ReplicatorRecipes.REPLICATOR_TYPE.get(); }
    static RecipeType<?> scanner() { return ScannerRecipes.SCANNER_TYPE.get(); }
    static RecipeType<?> vacuumFreezer() { return VacuumFreezerRecipes.VACUUM_FREEZER_TYPE.get(); }
    static RecipeType<?> blender() { return BlenderRecipes.BLENDER_TYPE.get(); }
    static RecipeType<?> electrolyzer() { return ElectrolyzerRecipes.ELECTROLYZER_TYPE.get(); }
    static RecipeType<?> fermenter() { return FermenterRecipes.FERMENTER_TYPE.get(); }
    static RecipeType<?> recycler() { return RecyclerRecipes.RECYCLER_TYPE.get(); }
    static RecipeType<?> neutronPolymerizer() { return NeutronPolymerizerRecipes.NEUTRON_POLYMERIZER_TYPE.get(); }
    static RecipeType<?> fluidRefining() { return com.singularity_iteration.mio_icif.recipe.fluid_refining.FluidRefiningRecipes.FLUID_REFINING_TYPE.get(); }
}