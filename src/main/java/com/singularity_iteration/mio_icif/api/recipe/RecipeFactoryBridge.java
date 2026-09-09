package com.singularity_iteration.mio_icif.api.recipe;

import com.singularity_iteration.mio_icif.recipe.canner.canning.CanningRecipe;
import com.singularity_iteration.mio_icif.recipe.canner.mix.MixRecipe;
import com.singularity_iteration.mio_icif.recipe.centrifuge.mio_icif_CentrifugeRecipe;
import com.singularity_iteration.mio_icif.recipe.compressor.mio_icif_CompressorRecipe;
import com.singularity_iteration.mio_icif.recipe.extractor.mio_icif_ExtractorRecipe;
import com.singularity_iteration.mio_icif.recipe.generic.*;
import com.singularity_iteration.mio_icif.recipe.metal_former.cutting.mio_icif_CuttingRecipe;
import com.singularity_iteration.mio_icif.recipe.metal_former.extruding.mio_icif_ExtrudingRecipe;
import com.singularity_iteration.mio_icif.recipe.metal_former.rolling.mio_icif_RollingRecipe;
import com.singularity_iteration.mio_icif.recipe.mio_icif_PowderRecipe;
import com.singularity_iteration.mio_icif.recipe.washer.mio_icif_WasherRecipe;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * 配方工厂内部类型的集中适配层。
 *
 * <p>{@code RecipeRegistrationAPIImpl} 通过此类创建配方实例，
 * 而非直接引用 {@code recipe.*} 内部类。
 *
 * <p>此类为包级私有，不对外暴露。
 */
final class RecipeFactoryBridge {

    private RecipeFactoryBridge() {}

    static Recipe<?> compressor(String group, Ingredient input, ItemStack output, int processTime, int energyPerTick, int minVoltage) {
        return new mio_icif_CompressorRecipe(group, input, output, processTime, energyPerTick, minVoltage);
    }

    static Recipe<?> extractor(String group, Ingredient input, ItemStack output, int processTime, int energyPerTick) {
        return new mio_icif_ExtractorRecipe(group, input, output, processTime, energyPerTick);
    }

    static Recipe<?> electricFurnace(String group, Ingredient input, ItemStack output, int processTime, int energyPerTick, float experience) {
        return new ElectricFurnaceRecipe(group, input, output, processTime, energyPerTick, experience);
    }

    static Recipe<?> macerator(String group, Ingredient input, ItemStack output, int processTime, int energyPerTick) {
        return new mio_icif_PowderRecipe(group, input, output, processTime, energyPerTick);
    }

    static Recipe<?> centrifuge(String group, Ingredient input,
                                 ItemStack primary, int primaryCount,
                                 ItemStack secondary, int secondaryCount,
                                 ItemStack tertiary, int tertiaryCount,
                                 int processTime, int energyPerTick, int minHeatRequired) {
        return new mio_icif_CentrifugeRecipe(group, input,
            primary, primaryCount, secondary, secondaryCount, tertiary, tertiaryCount,
            processTime, energyPerTick, minHeatRequired);
    }

    static Recipe<?> recycler(String group, Ingredient input, ItemStack output, int processTime, int energyPerTick, float chance) {
        return new RecyclerRecipe(group, input, output, processTime, energyPerTick, chance);
    }

    static Recipe<?> washer(String group, Ingredient input,
                             ItemStack primary, int primaryCount,
                             ItemStack secondary, int secondaryCount,
                             ItemStack tertiary, int tertiaryCount,
                             int processTime, int energyPerTick, int waterAmount) {
        return new mio_icif_WasherRecipe(group, input,
            primary, primaryCount, secondary, secondaryCount, tertiary, tertiaryCount,
            processTime, energyPerTick, waterAmount);
    }

    static Recipe<?> blender(String group, Ingredient input, ItemStack output, int processTime, int energyPerTick, float experience) {
        return new BlenderRecipe(group, input, output, processTime, energyPerTick, experience);
    }

    static Recipe<?> fermenter(String group, Ingredient input, ItemStack output, int processTime, int energyPerTick, float experience) {
        return new FermenterRecipe(group, input, output, processTime, energyPerTick, experience);
    }

    static Recipe<?> mix(String group, FluidStack fluidInput, Ingredient itemInput, int itemInputCount,
                          FluidStack fluidOutput, ItemStack itemOutput,
                          int processTime, int energyPerTick) {
        return new MixRecipe(group, fluidInput, itemInput, itemInputCount, fluidOutput, itemOutput, processTime, energyPerTick);
    }

    static Recipe<?> canning(String group, Ingredient can, Ingredient input, ItemStack output, int processTime, int energyPerTick) {
        return new CanningRecipe(group, can, input, output, processTime, energyPerTick);
    }

    static Recipe<?> laserEngraver(String group, Ingredient input, ItemStack output, int processTime, int energyPerTick, float experience) {
        return new LaserEngraverRecipe(group, input, output, processTime, energyPerTick, experience);
    }

    static Recipe<?> lathe(String group, Ingredient input, ItemStack output, int processTime, int energyPerTick, float experience) {
        return new LatheRecipe(group, input, output, processTime, energyPerTick, experience);
    }

    static Recipe<?> welder(String group, Ingredient input, ItemStack output, int processTime, int energyPerTick, float experience) {
        return new WelderRecipe(group, input, output, processTime, energyPerTick, experience);
    }

    static Recipe<?> vacuumFreezer(String group, Ingredient input, ItemStack output, int processTime, int energyPerTick, float experience) {
        return new VacuumFreezerRecipe(group, input, output, processTime, energyPerTick, experience);
    }

    static Recipe<?> electrolyzer(String group, Ingredient input, ItemStack output, int processTime, int energyPerTick, float experience) {
        return new ElectrolyzerRecipe(group, input, output, processTime, energyPerTick, experience);
    }

    static Recipe<?> precisionAssembler(String group, Ingredient input, ItemStack output, int processTime, int energyPerTick, float experience) {
        return new PrecisionAssemblerRecipe(group, input, output, processTime, energyPerTick, experience);
    }

    static Recipe<?> massFabricator(String group, Ingredient input, ItemStack output, int processTime, int energyPerTick, float experience) {
        return new MassFabricatorRecipe(group, input, output, processTime, energyPerTick, experience);
    }

    static Recipe<?> replicator(String group, Ingredient input, ItemStack output, int processTime, int energyPerTick, float experience) {
        return new ReplicatorRecipe(group, input, output, processTime, energyPerTick, experience);
    }

    static Recipe<?> scanner(String group, Ingredient input, ItemStack output, int processTime, int energyPerTick, float experience) {
        return new ScannerRecipe(group, input, output, processTime, energyPerTick, experience);
    }

    static Recipe<?> plasmaFurnace(String group, Ingredient input, ItemStack output, int processTime, int energyPerTick, float experience) {
        return new PlasmaFurnaceRecipe(group, input, output, processTime, energyPerTick, experience);
    }

    static Recipe<?> fusionReactor(String group, Ingredient input1, Ingredient input2, ItemStack output, int processTime, long energyRequired) {
        return new FusionReactorRecipe(group, input1, input2, output, processTime, energyRequired);
    }

    static Recipe<?> cutting(String group, Ingredient input, ItemStack output, int processTime, int energyPerTick, int ingredientCount) {
        return new mio_icif_CuttingRecipe(group, input, output, processTime, energyPerTick, ingredientCount);
    }

    static Recipe<?> extruding(String group, Ingredient input, ItemStack output, int processTime, int energyPerTick, int ingredientCount) {
        return new mio_icif_ExtrudingRecipe(group, input, output, processTime, energyPerTick, ingredientCount);
    }

    static Recipe<?> rolling(String group, Ingredient input, ItemStack output, int processTime, int energyPerTick, int ingredientCount) {
        return new mio_icif_RollingRecipe(group, input, output, processTime, energyPerTick, ingredientCount);
    }

    static Recipe<?> fluidRefining(String group, Ingredient ingredient, net.minecraft.world.level.material.Fluid inputFluid, net.neoforged.neoforge.fluids.FluidStack outputFluid, int processTime, int energyPerTick) {
        return new com.singularity_iteration.mio_icif.recipe.fluid_refining.FluidRefiningRecipe(group, ingredient, inputFluid, null, outputFluid, processTime, energyPerTick);
    }

    static Recipe<?> fluidRefining(String group, Ingredient ingredient, net.minecraft.tags.TagKey<net.minecraft.world.level.material.Fluid> inputFluidTag, net.neoforged.neoforge.fluids.FluidStack outputFluid, int processTime, int energyPerTick) {
        return new com.singularity_iteration.mio_icif.recipe.fluid_refining.FluidRefiningRecipe(group, ingredient, net.minecraft.world.level.material.Fluids.EMPTY, inputFluidTag, outputFluid, processTime, energyPerTick);
    }
}