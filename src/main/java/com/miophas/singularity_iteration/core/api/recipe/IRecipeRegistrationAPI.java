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
     * Registers an electric furnace recipe. The Electric and Induction Furnaces check these before vanilla
     * smelting, at their own speed ({@code processTime} is for add-on machines).
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
     * Registers a recycler recipe: recycling an item that matches {@code input} gives the output instead of
     * scrap, with the given chance (else nothing). The recycler keeps its own speed.
     *
     * @param id the recipe ID
     * @param input the input ingredient
     * @param outputs exactly one output item
     * @param chances at most one chance, from 0 to 1; none (null or empty) for the recycler's own 1/8
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
     * <p>No Singularity Iteration machine reads these recipes: they are for add-on machines, which find
     * them through {@code IRecipeAPI}.
     *
     * @param id the recipe ID
     * @param input the input ingredient
     * @param output the output item
     * @param processTime the process time, in ticks
     * @return true if the registration is successful
     */
    boolean registerBlenderRecipe(ResourceLocation id, Ingredient input, ItemStack output, int processTime);

    /**
     * Item fermenter recipes do not exist: the fermenter takes fluids and heat. Always returns false.
     *
     * @deprecated use {@link #registerFermenterRecipe(ResourceLocation, net.neoforged.neoforge.fluids.FluidStack, int, net.neoforged.neoforge.fluids.FluidStack)}
     *             or a {@code mio_icif:fermenter} JSON recipe
     */
    @Deprecated
    boolean registerFermenterRecipe(ResourceLocation id, Ingredient input, ItemStack output, int processTime);

    /**
     * Registers a fermenter recipe (IC2 FermentationProperty): one run takes {@code input} (fluid and
     * amount) and {@code heat} HU and gives {@code output}. A fluid has one fermenter recipe; JSON
     * recipes ({@code "type": "mio_icif:fermenter"}) can also give a by-product.
     *
     * @param id the recipe ID
     * @param input the input fluid and the amount one run takes
     * @param heat the HU one run takes
     * @param output the output fluid and the amount one run gives
     * @return true if the registration is successful
     */
    default boolean registerFermenterRecipe(ResourceLocation id, net.neoforged.neoforge.fluids.FluidStack input, int heat,
                                            net.neoforged.neoforge.fluids.FluidStack output) {
        return false;
    }

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
     * Registers an electrolyzer recipe: the Electrolyzer takes items that match {@code input} as well as
     * water cells, gives the output and stores 200 EU of chemical energy per item, like a water cell (it
     * keeps its own rate). A recipe for water cells takes the place of the built-in one.
     *
     * @param id the recipe ID
     * @param input the input ingredient
     * @param outputs exactly one output item
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
     * The lathe does not turn items into items: it cuts turning blanks to profiles that crafting recipes
     * ask for. Always returns false.
     *
     * @deprecated name the profile with {@link #registerLatheShape} and ask for it in a crafting recipe
     *             with a {@code mio_icif:lathe_state} ingredient
     */
    @Deprecated
    boolean registerLatheRecipe(ResourceLocation id, Ingredient input, ItemStack output, int processTime);

    /**
     * Names a lathe profile (like IC2's 5-4-3-2-1 Drill Tip) as the part for {@code part}. Turning blanks
     * cut to it show the name and the part, the Shift list of a blank shows how many cuts it is away, and
     * JEI shows it on the lathe page. A crafting recipe asks for the part with an ingredient like
     * {@code {"type": "mio_icif:lathe_state", "item": "mio_icif:normal/item_iron_turning_blanks", "state": [5, 4, 3, 2, 1]}}.
     * The name is the translation key {@code lathe_shape.<namespace>.<path>} of {@code id} (the profile
     * when there is none). Call it on both sides, for example in {@code FMLCommonSetupEvent}.
     *
     * @param id the shape ID, also its name key
     * @param blank the turning blank: the iron or the wooden one
     * @param profile the thickness of the 5 positions, 1 to 5 for iron and 1 to 3 for wood
     * @param part the item the part is for
     * @return true if it was added; false for a bad profile or one that is already named
     */
    default boolean registerLatheShape(ResourceLocation id, net.minecraft.world.item.Item blank, int[] profile, ItemStack part) {
        return false;
    }

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
     * <p>No Singularity Iteration machine reads these recipes: they are for add-on machines, which find
     * them through {@code IRecipeAPI}.
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
     * <p>No Singularity Iteration machine reads these recipes: they are for add-on machines, which find
     * them through {@code IRecipeAPI}.
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
     * <p>No Singularity Iteration machine reads these recipes: they are for add-on machines, which find
     * them through {@code IRecipeAPI}.
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
     * <p>No Singularity Iteration machine reads these recipes: they are for add-on machines, which find
     * them through {@code IRecipeAPI}.
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
     * <p>No Singularity Iteration machine reads these recipes: they are for add-on machines, which find
     * them through {@code IRecipeAPI}.
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
     * <p>No Singularity Iteration machine reads these recipes: they are for add-on machines, which find
     * them through {@code IRecipeAPI}.
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
     * <p>No Singularity Iteration machine reads these recipes: they are for add-on machines, which find
     * them through {@code IRecipeAPI}.
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
     * <p>No Singularity Iteration machine reads these recipes: they are for add-on machines, which find
     * them through {@code IRecipeAPI}.
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
     * The Mass Fabricator makes only UU-matter, from EU; items are amplifiers. Always returns false.
     *
     * @deprecated use {@link #registerMassFabricatorAmplifier}
     */
    @Deprecated
    boolean registerMassFabricatorRecipe(ResourceLocation id, Ingredient input, ItemStack output, int processTime);

    /**
     * Registers a Mass Fabricator amplifier (IC2 matter amplifier, like scrap): one item that matches
     * {@code amplifier} adds {@code euCredit} EU of amplifier credit, and received EU covered by the credit
     * counts six times. It comes before the built-in amplifiers (scrap 5,000, scrap box 45,000). Datapacks:
     * {@code {"type": "mio_icif:mass_fabricator", "ingredient": {...}, "credit": 5000}}.
     *
     * @param id the recipe ID
     * @param amplifier the amplifier items
     * @param euCredit the EU of credit one item adds, more than 0
     * @return true if the registration is successful
     */
    default boolean registerMassFabricatorAmplifier(ResourceLocation id, Ingredient amplifier, int euCredit) {
        return false;
    }

    /**
     * The replicator copies scanned patterns for their UU-matter price; it has no recipes. Always returns false.
     *
     * @deprecated set prices with a replication list, {@code data/<namespace>/uu_replication/*.json} (API
     *             guide 12.1); an item made by recipes from priced items gets a derived price of its own
     */
    @Deprecated
    boolean registerReplicatorRecipe(ResourceLocation id, Ingredient input, ItemStack output, int processTime);

    /**
     * The scanner records any item that has a UU-matter price; it has no recipes. Always returns false.
     *
     * @deprecated set prices with a replication list, {@code data/<namespace>/uu_replication/*.json} (API
     *             guide 12.1); an item made by recipes from priced items gets a derived price of its own
     */
    @Deprecated
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