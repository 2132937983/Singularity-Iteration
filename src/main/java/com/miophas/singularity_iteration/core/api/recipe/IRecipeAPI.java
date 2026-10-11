package com.miophas.singularity_iteration.core.api.recipe;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;

@SuppressWarnings("rawtypes")
public interface IRecipeAPI {

    RecipeType<?> getPowderRecipeType();

    RecipeType<?> getCompressorRecipeType();

    RecipeType<?> getExtractorRecipeType();

    RecipeType<?> getWasherRecipeType();

    RecipeType<?> getCentrifugeRecipeType();

    /**
     * @deprecated The metal former is now split into the rolling machine, the cutting machine and the extruder.
     * Use {@link #getRollingMachineRecipeType()}, {@link #getCuttingRecipeType()} or {@link #getExtrudingRecipeType()} instead.
     */
    @Deprecated
    RecipeType<?> getMetalFormerRecipeType();

    /**
     * @deprecated Rolling machine recipe type. Use {@link #getMetalFormerRecipeType()} instead.
     */
    @Deprecated
    default RecipeType<?> getRollingMachineRecipeType() { return getMetalFormerRecipeType(); }

    RecipeType<?> getBlastFurnaceRecipeType();

    RecipeType<?> getCannerRecipeType();

    RecipeType<?> getBlockCutterRecipeType();

    RecipeType<?> getCuttingRecipeType();

    RecipeType<?> getExtrudingRecipeType();

    RecipeType<?> getWelderRecipeType();

    RecipeType<?> getPrecisionAssemblerRecipeType();

    RecipeType<?> getFusionReactorRecipeType();

    RecipeType<?> getLatheRecipeType();

    RecipeType<?> getLaserEngraverRecipeType();

    RecipeType<?> getMassFabricatorRecipeType();

    RecipeType<?> getPlasmaFurnaceRecipeType();

    RecipeType<?> getReplicatorRecipeType();

    RecipeType<?> getScannerRecipeType();

    RecipeType<?> getVacuumFreezerRecipeType();

    RecipeType<?> getBlenderRecipeType();

    RecipeType<?> getElectrolyzerRecipeType();

    RecipeType<?> getFermenterRecipeType();

    RecipeType<?> getRecyclerRecipeType();

    RecipeType<?> getNeutronPolymerizerRecipeType();

    RecipeType<?> getFluidRefiningRecipeType();

    /**
     * Finds a recipe that matches the input item.
     *
     * <p><b>WARNING:</b> This method checks only if the first Ingredient of the recipe matches the input item.
     * For recipes with more than one ingredient (for example welder or precision assembler recipes that need more than one input),
     * this method can return a <b>wrong match</b>. It does not check the other input slots.
     *
     * <p><b>Correct use:</b>
     * <ul>
     *   <li>Single-input recipes (compressor, macerator, extractor and similar): use this method.</li>
     *   <li>Multi-input recipes (welder, precision assembler, fusion reactor and similar): use {@link #findRecipeMultiInput(RecipeType, ItemStack[], Level)}.</li>
     * </ul>
     *
     * @deprecated This method is correct only for single-input recipes. For multi-input recipes, use {@link #findRecipeMultiInput(RecipeType, ItemStack[], Level)}.
     * @param type the recipe type
     * @param input the input item
     * @param level the world
     * @return the matching recipe, or Optional.empty() if there is no match
     */
    @Deprecated
    <T extends Recipe<?>> Optional<RecipeHolder<T>> findRecipe(RecipeType<T> type, ItemStack input, Level level);

    Optional<? extends RecipeHolder<?>> findPowderRecipe(ItemStack input, Level level);

    Optional<? extends RecipeHolder<?>> findCompressorRecipe(ItemStack input, Level level);

    Optional<? extends RecipeHolder<?>> findExtractorRecipe(ItemStack input, Level level);

    Optional<? extends RecipeHolder<?>> findWasherRecipe(ItemStack input, Level level);

    Optional<? extends RecipeHolder<?>> findCentrifugeRecipe(ItemStack input, Level level);

    // ========== Convenience recipe lookups (default methods call findRecipe) ==========

    /**
     * Finds the blast furnace recipe for the input item.
     */
    @SuppressWarnings("unchecked")
    default Optional<? extends RecipeHolder<?>> findBlastFurnaceRecipe(ItemStack input, Level level) {
        return findRecipe((RecipeType) getBlastFurnaceRecipeType(), input, level);
    }

    /**
     * Finds the canner recipe for the input item.
     */
    @SuppressWarnings("unchecked")
    default Optional<? extends RecipeHolder<?>> findCannerRecipe(ItemStack input, Level level) {
        return findRecipe((RecipeType) getCannerRecipeType(), input, level);
    }

    /**
     * Finds the block cutter recipe for the input item.
     */
    @SuppressWarnings("unchecked")
    default Optional<? extends RecipeHolder<?>> findBlockCutterRecipe(ItemStack input, Level level) {
        return findRecipe((RecipeType) getBlockCutterRecipeType(), input, level);
    }

    /**
     * Finds the cutting machine recipe for the input item.
     */
    @SuppressWarnings("unchecked")
    default Optional<? extends RecipeHolder<?>> findCuttingRecipe(ItemStack input, Level level) {
        return findRecipe((RecipeType) getCuttingRecipeType(), input, level);
    }

    /**
     * Finds the rolling machine recipe for the input item.
     */
    @SuppressWarnings("unchecked")
    default Optional<? extends RecipeHolder<?>> findRollingRecipe(ItemStack input, Level level) {
        return findRecipe((RecipeType) getMetalFormerRecipeType(), input, level);
    }

    /**
     * Finds the extruder recipe for the input item.
     */
    @SuppressWarnings("unchecked")
    default Optional<? extends RecipeHolder<?>> findExtrudingRecipe(ItemStack input, Level level) {
        return findRecipe((RecipeType) getExtrudingRecipeType(), input, level);
    }

    /**
     * Finds the lathe recipe for the input item.
     */
    @SuppressWarnings("unchecked")
    default Optional<? extends RecipeHolder<?>> findLatheRecipe(ItemStack input, Level level) {
        return findRecipe((RecipeType) getLatheRecipeType(), input, level);
    }

    /**
     * Finds the laser engraver recipe for the input item.
     */
    @SuppressWarnings("unchecked")
    default Optional<? extends RecipeHolder<?>> findLaserEngraverRecipe(ItemStack input, Level level) {
        return findRecipe((RecipeType) getLaserEngraverRecipeType(), input, level);
    }

    /**
     * Finds the vacuum freezer recipe for the input item.
     */
    @SuppressWarnings("unchecked")
    default Optional<? extends RecipeHolder<?>> findVacuumFreezerRecipe(ItemStack input, Level level) {
        return findRecipe((RecipeType) getVacuumFreezerRecipeType(), input, level);
    }

    /**
     * Finds the blender recipe for the input item.
     */
    @SuppressWarnings("unchecked")
    default Optional<? extends RecipeHolder<?>> findBlenderRecipe(ItemStack input, Level level) {
        return findRecipe((RecipeType) getBlenderRecipeType(), input, level);
    }

    /**
     * Finds the electrolyzer recipe for the input item.
     */
    @SuppressWarnings("unchecked")
    default Optional<? extends RecipeHolder<?>> findElectrolyzerRecipe(ItemStack input, Level level) {
        return findRecipe((RecipeType) getElectrolyzerRecipeType(), input, level);
    }

    /**
     * Finds the fermenter recipe for the input item.
     */
    @SuppressWarnings("unchecked")
    default Optional<? extends RecipeHolder<?>> findFermenterRecipe(ItemStack input, Level level) {
        return findRecipe((RecipeType) getFermenterRecipeType(), input, level);
    }

    /**
     * Finds the recycler recipe for the input item.
     */
    @SuppressWarnings("unchecked")
    default Optional<? extends RecipeHolder<?>> findRecyclerRecipe(ItemStack input, Level level) {
        return findRecipe((RecipeType) getRecyclerRecipeType(), input, level);
    }

    /**
     * Finds the plasma furnace recipe for the input item.
     */
    @SuppressWarnings("unchecked")
    default Optional<? extends RecipeHolder<?>> findPlasmaFurnaceRecipe(ItemStack input, Level level) {
        return findRecipe((RecipeType) getPlasmaFurnaceRecipeType(), input, level);
    }

    /**
     * Finds the mass fabricator recipe for the input item.
     */
    @SuppressWarnings("unchecked")
    default Optional<? extends RecipeHolder<?>> findMassFabricatorRecipe(ItemStack input, Level level) {
        return findRecipe((RecipeType) getMassFabricatorRecipeType(), input, level);
    }

    /**
     * Finds the replicator recipe for the input item.
     */
    @SuppressWarnings("unchecked")
    default Optional<? extends RecipeHolder<?>> findReplicatorRecipe(ItemStack input, Level level) {
        return findRecipe((RecipeType) getReplicatorRecipeType(), input, level);
    }

    /**
     * Finds the scanner recipe for the input item.
     */
    @SuppressWarnings("unchecked")
    default Optional<? extends RecipeHolder<?>> findScannerRecipe(ItemStack input, Level level) {
        return findRecipe((RecipeType) getScannerRecipeType(), input, level);
    }

    List<ResourceLocation> getAllRecipeTypeIds();

    @SuppressWarnings("unchecked")
    default Optional<? extends RecipeHolder<?>> findNeutronPolymerizerRecipe(ItemStack input, Level level) {
        return findRecipe((RecipeType) getNeutronPolymerizerRecipeType(), input, level);
    }

    // ========== Fluid refining recipe API ==========

    Optional<? extends RecipeHolder<?>> findFluidRefiningRecipe(ItemStack inputItem, net.neoforged.neoforge.fluids.FluidStack inputFluid, Level level);

    Optional<? extends RecipeHolder<?>> findFluidRefiningRecipeForFluid(net.neoforged.neoforge.fluids.FluidStack fluid, Level level);

    net.neoforged.neoforge.fluids.FluidStack getFluidRefiningOutputFluid(RecipeHolder<?> recipe);

    // ========== Recipe information API ==========

    /**
     * Gets the output item of the recipe.
     *
     * @param recipe the recipe
     * @return the output item, or an empty ItemStack if the recipe is null
     */
    ItemStack getRecipeOutput(RecipeHolder<?> recipe);

    /**
     * Gets the process time of the recipe (ticks).
     *
     * @param recipe the recipe
     * @return the process time (ticks), or 200 if the recipe is null or the value is not available
     */
    int getRecipeProcessTime(RecipeHolder<?> recipe);

    /**
     * Gets the energy cost of the recipe (EU).
     *
     * @param recipe the recipe
     * @return the total energy cost (EU), or 0 if the recipe is null or the value is not available
     */
    long getRecipeEnergyCost(RecipeHolder<?> recipe);

    /**
     * Gets the energy cost for each tick of the recipe (EU/tick).
     *
     * @param recipe the recipe
     * @return the energy for each tick (EU), or 0 if the recipe is null or the value is not available
     */
    long getRecipeEnergyPerTick(RecipeHolder<?> recipe);

    /**
     * Gets the quantity of input items that the recipe uses.
     *
     * @param recipe the recipe
     * @return the quantity used, or 1 if the recipe is null or the value is not available
     */
    int getRecipeIngredientCount(RecipeHolder<?> recipe);

    /**
     * Finds a recipe with more than one input.
     *
     * <p>Use it for welder, precision assembler, fusion reactor and other recipes that need more than one input item.
     * This method checks if all input slots match the ingredients of the recipe.
     *
     * @param type the recipe type
     * @param inputs the input items
     * @param level the world
     * @return the matching recipe, or Optional.empty() if there is no match
     */
    <T extends Recipe<?>> Optional<RecipeHolder<T>> findRecipeMultiInput(RecipeType<T> type, ItemStack[] inputs, Level level);

    Optional<? extends RecipeHolder<?>> findWelderRecipe(ItemStack[] inputs, Level level);

    Optional<? extends RecipeHolder<?>> findPrecisionAssemblerRecipe(ItemStack[] inputs, Level level);

    Optional<? extends RecipeHolder<?>> findFusionReactorRecipe(ItemStack[] inputs, Level level);

    /**
     * Clears the recipe cache.
     * 
     * <p>When the recipe system changes (for example a data pack reload or dynamic recipe registration),
     * call this method to clear the cache. This makes sure that lookups give correct results.
     * 
     * <p>Add-on mods must call this method after they register new recipes at runtime.
     */
    void invalidateCache();
}