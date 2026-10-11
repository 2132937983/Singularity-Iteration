package com.miophas.singularity_iteration.core.api.recipe;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.resources.ResourceLocation;

/**
 * Recipe registration API.
 *
 * <p>Use this interface to register custom recipes. Add-on mods use it to add new processing recipes.
 *
 * <p>Example:
 * <pre>{@code
 * IRecipeRegistrationAPI recipeAPI = MioIcifAPI.instance().getRecipeRegistrationAPI();
 *
 * // Register a compressor recipe
 * recipeAPI.registerCompressorRecipe(
 *     new ResourceLocation("mymod", "compress_iron"),
 *     Ingredient.of(Items.IRON_INGOT),
 *     new ItemStack(ModItems.COMPRESSED_IRON.get()),
 *     200 // process time in ticks
 * );
 *
 * // Register an extractor recipe
 * recipeAPI.registerExtractorRecipe(
 *     new ResourceLocation("mymod", "extract_rubber"),
 *     Ingredient.of(ModItems.RUBBER_WOOD.get()),
 *     new ItemStack(ModItems.RUBBER.get(), 3),
 *     150
 * );
 * }</pre>
 */
public interface IRecipeRegistrationAPI {

    /**
     * Registers a compressor recipe.
     *
     * @param id the recipe ID
     * @param input the input ingredient
     * @param output the output item
     * @param processTime the process time, in ticks
     * @return true if the registration is successful
     */
    boolean registerCompressorRecipe(ResourceLocation id, Ingredient input, ItemStack output, int processTime);

    /**
     * Registers an extractor recipe.
     *
     * @param id the recipe ID
     * @param input the input ingredient
     * @param output the output item
     * @param processTime the process time, in ticks
     * @return true if the registration is successful
     */
    boolean registerExtractorRecipe(ResourceLocation id, Ingredient input, ItemStack output, int processTime);

    /**
 * Registers an electric furnace recipe.
     *
     * @param id the recipe ID
     * @param input the input ingredient
     * @param output the output item
     * @param processTime the process time, in ticks
     * @param experience the experience that the player gets for one output
     * @return true if the registration is successful
     */
    boolean registerElectricFurnaceRecipe(ResourceLocation id, Ingredient input, ItemStack output, int processTime, float experience);

    /**
     * Registers a macerator recipe.
     *
     * @param id the recipe ID
     * @param input the input ingredient
     * @param output the output item
     * @param processTime the process time, in ticks
     * @return true if the registration is successful
     */
    boolean registerMaceratorRecipe(ResourceLocation id, Ingredient input, ItemStack output, int processTime);

    /**
     * Registers a centrifuge recipe.
     *
     * @param id the recipe ID
     * @param input the input ingredient
     * @param outputs the output items (more than one output is permitted)
     * @param processTime the process time, in ticks
     * @return true if the registration is successful
     */
    boolean registerCentrifugeRecipe(ResourceLocation id, Ingredient input, ItemStack[] outputs, int processTime);

    /**
     * Registers a recycler recipe.
     *
     * @param id the recipe ID
     * @param input the input ingredient
     * @param outputs the possible output items (each output is random)
     * @param chances the chance for each output, from 0 to 1. Index i applies to {@code outputs[i]}
     * @param processTime the process time, in ticks
     * @return true if the registration is successful
     */
    boolean registerRecyclerRecipe(ResourceLocation id, Ingredient input, ItemStack[] outputs, float[] chances, int processTime);

    /**
     * Registers an ore washer recipe.
     *
     * @param id the recipe ID
     * @param input the input ingredient
     * @param outputs the output items
     * @param processTime the process time, in ticks
     * @return true if the registration is successful
     */
    boolean registerOreWasherRecipe(ResourceLocation id, Ingredient input, ItemStack[] outputs, int processTime);

    /**
     * Registers a thermal centrifuge recipe.
     *
     * @param id the recipe ID
     * @param input the input ingredient
     * @param outputs the output items
     * @param processTime the process time, in ticks
     * @param heatRequired the minimum heat that the machine must have
     * @return true if the registration is successful
     */
    boolean registerThermalCentrifugeRecipe(ResourceLocation id, Ingredient input, ItemStack[] outputs, int processTime, int heatRequired);

    /**
     * Registers a blender recipe.
     *
     * @param id the recipe ID
     * @param input the input ingredient
     * @param output the output item
     * @param processTime the process time, in ticks
     * @return true if the registration is successful
     */
    boolean registerBlenderRecipe(ResourceLocation id, Ingredient input, ItemStack output, int processTime);

    /**
     * Registers a fermenter recipe.
     *
     * @param id the recipe ID
     * @param input the input ingredient
     * @param output the output item
     * @param processTime the process time, in ticks
     * @return true if the registration is successful
     */
    boolean registerFermenterRecipe(ResourceLocation id, Ingredient input, ItemStack output, int processTime);

    /**
     * Registers a fluid/solid canning machine recipe.
     *
     * @param id the recipe ID
     * @param itemInput the input item
     * @param fluidInput the input fluid, in the format {@code "fluid_id:amount"} (amount in mB)
     * @param output the output item
     * @param processTime the process time, in ticks
     * @return true if the registration is successful
     */
    boolean registerFluidSolidRecipe(ResourceLocation id, Ingredient itemInput, String fluidInput, ItemStack output, int processTime);

    /**
     * Registers a canner recipe.
     *
     * @param id the recipe ID
     * @param input the input ingredient
     * @param output the output item
     * @param processTime the process time, in ticks
     * @return true if the registration is successful
     */
    boolean registerCannerRecipe(ResourceLocation id, Ingredient input, ItemStack output, int processTime);

    /**
     * Registers an electrolyzer recipe.
     *
     * @param id the recipe ID
     * @param input the input ingredient
     * @param outputs the output items
     * @param processTime the process time, in ticks
     * @return true if the registration is successful
     */
    boolean registerElectrolyzerRecipe(ResourceLocation id, Ingredient input, ItemStack[] outputs, int processTime);

    /**
     * Registers an extruder recipe.
     *
     * @param id the recipe ID
     * @param input the input ingredient
     * @param output the output item
     * @param processTime the process time, in ticks
     * @return true if the registration is successful
     */
    boolean registerExtruderRecipe(ResourceLocation id, Ingredient input, ItemStack output, int processTime);

    /**
     * Registers a lathe recipe.
     *
     * @param id the recipe ID
     * @param input the input ingredient
     * @param output the output item
     * @param processTime the process time, in ticks
     * @return true if the registration is successful
     */
    boolean registerLatheRecipe(ResourceLocation id, Ingredient input, ItemStack output, int processTime);

    /**
     * Registers a rolling machine recipe.
     *
     * @param id the recipe ID
     * @param input the input ingredient
     * @param output the output item
     * @param processTime the process time, in ticks
     * @return true if the registration is successful
     */
    boolean registerRollingMachineRecipe(ResourceLocation id, Ingredient input, ItemStack output, int processTime);

    /**
     * Registers a cutter recipe.
     *
     * @param id the recipe ID
     * @param input the input ingredient
     * @param output the output item
     * @param processTime the process time, in ticks
     * @return true if the registration is successful
     */
    boolean registerCutterRecipe(ResourceLocation id, Ingredient input, ItemStack output, int processTime);

    /**
     * Registers a welder recipe.
     *
     * @param id the recipe ID
     * @param input the input ingredient
     * @param output the output item
     * @param processTime the process time, in ticks
     * @return true if the registration is successful
     */
    boolean registerWelderRecipe(ResourceLocation id, Ingredient input, ItemStack output, int processTime);

    /**
     * Registers a welder recipe with more than one input.
     *
     * @param id the recipe ID
     * @param inputs the input ingredients
     * @param output the output item
     * @param processTime the process time, in ticks
     * @return true if the registration is successful
     */
    boolean registerWelderRecipe(ResourceLocation id, Ingredient[] inputs, ItemStack output, int processTime);

    /**
     * Registers a laser engraver recipe.
     *
     * @param id the recipe ID
     * @param input the input ingredient
     * @param output the output item
     * @param processTime the process time, in ticks
     * @return true if the registration is successful
     */
    boolean registerLaserEngraverRecipe(ResourceLocation id, Ingredient input, ItemStack output, int processTime);

    /**
     * Registers a precision assembler recipe.
     *
     * @param id the recipe ID
     * @param input the input ingredient
     * @param output the output item
     * @param processTime the process time, in ticks
     * @return true if the registration is successful
     */
    boolean registerPrecisionAssemblerRecipe(ResourceLocation id, Ingredient input, ItemStack output, int processTime);

    /**
     * Registers a precision assembler recipe with more than one input.
     *
     * @param id the recipe ID
     * @param inputs the input ingredients
     * @param output the output item
     * @param processTime the process time, in ticks
     * @return true if the registration is successful
     */
    boolean registerPrecisionAssemblerRecipe(ResourceLocation id, Ingredient[] inputs, ItemStack output, int processTime);

    /**
     * Registers a vacuum freezer recipe.
     *
     * @param id the recipe ID
     * @param input the input ingredient
     * @param output the output item
     * @param processTime the process time, in ticks
     * @return true if the registration is successful
     */
    boolean registerVacuumFreezerRecipe(ResourceLocation id, Ingredient input, ItemStack output, int processTime);

    /**
     * Registers a plasma furnace recipe.
     *
     * @param id the recipe ID
     * @param input the input ingredient
     * @param output the output item
     * @param processTime the process time, in ticks
     * @return true if the registration is successful
     */
    boolean registerPlasmaFurnaceRecipe(ResourceLocation id, Ingredient input, ItemStack output, int processTime);

    /**
     * Registers a fusion reactor recipe.
     *
     * @param id the recipe ID
     * @param input1 the first input ingredient
     * @param input2 the second input ingredient
     * @param output the output item
     * @param energyRequired the total energy that the recipe needs, in EU
     * @param processTime the process time, in ticks
     * @return true if the registration is successful
     */
    boolean registerFusionReactorRecipe(ResourceLocation id, Ingredient input1, Ingredient input2, ItemStack output, long energyRequired, int processTime);

    /**
     * Registers a mass fabricator recipe.
     *
     * @param id the recipe ID
     * @param input the input ingredient
     * @param output the output item
     * @param processTime the process time, in ticks
     * @return true if the registration is successful
     */
    boolean registerMassFabricatorRecipe(ResourceLocation id, Ingredient input, ItemStack output, int processTime);

    /**
     * Registers a replicator recipe.
     *
     * @param id the recipe ID
     * @param input the input ingredient
     * @param output the output item
     * @param processTime the process time, in ticks
     * @return true if the registration is successful
     */
    boolean registerReplicatorRecipe(ResourceLocation id, Ingredient input, ItemStack output, int processTime);

    /**
     * Registers a scanner recipe.
     *
     * @param id the recipe ID
     * @param input the input ingredient
     * @param output the output item
     * @param processTime the process time, in ticks
     * @return true if the registration is successful
     */
    boolean registerScannerRecipe(ResourceLocation id, Ingredient input, ItemStack output, int processTime);

    /**
     * Registers a fluid refining recipe.
     *
     * @param id the recipe ID
     * @param ingredient the input ingredient
     * @param inputFluid the input fluid and amount, in mB
     * @param outputFluid the output fluid and amount, in mB
     * @param processTime the process time, in ticks
     * @param energyPerTick the energy that the machine uses each tick, in EU/t
     * @return true if the registration is successful
     */
    boolean registerFluidRefiningRecipe(ResourceLocation id, Ingredient ingredient, net.neoforged.neoforge.fluids.FluidStack inputFluid, net.neoforged.neoforge.fluids.FluidStack outputFluid, int processTime, int energyPerTick);

    /**
     * Copies all recipes that were registered at runtime into the RecipeManager.
     *
     * <p>Minecraft loads recipes from data packs when the server starts.
     * Thus, recipes that you register at runtime are not in the RecipeManager.
     * Call this method to add them manually. Call it on the server side.
     *
     * <p>Call this method in {@link net.neoforged.neoforge.event.server.ServerStartedEvent}.
     *
     * <p>Example:
     * <pre>{@code
     * @SubscribeEvent
     * public void onServerStarted(ServerStartedEvent event) {
     *     MioIcifAPI.instance().getRecipeRegistrationAPI().syncToRecipeManager();
     * }
     * }</pre>
     */
    void syncToRecipeManager();
}