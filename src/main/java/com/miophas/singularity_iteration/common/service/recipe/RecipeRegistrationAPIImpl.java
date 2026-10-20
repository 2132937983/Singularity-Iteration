package com.miophas.singularity_iteration.common.service.recipe;

import com.miophas.singularity_iteration.core.api.recipe.IRecipeRegistrationAPI;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 配方注册 API 实现
 *
 * <p>通过 {@link RecipeFactoryBridge} 创建配方实例，
 * 此文件零内部配方类引用。
 */
@SuppressWarnings("null")
public class RecipeRegistrationAPIImpl implements IRecipeRegistrationAPI {

    private static final Logger LOGGER = LoggerFactory.getLogger(RecipeRegistrationAPIImpl.class);
    private final Map<ResourceLocation, RegisteredRecipe> registeredRecipes = new HashMap<>();
    /** Maintained with {@link #registeredRecipes}; avoids a full scan for UI/diagnostic counters. */
    private final Map<String, Integer> recipeTypeCounts = new HashMap<>();

    public Map<ResourceLocation, RegisteredRecipe> getRegisteredRecipes() {
        synchronized (syncLock) {
            return Collections.unmodifiableMap(new LinkedHashMap<>(registeredRecipes));
        }
    }

    public int getRecipeCount(String recipeType) {
        if (recipeType == null) return 0;
        synchronized (syncLock) { return recipeTypeCounts.getOrDefault(recipeType, 0); }
    }

    public void clearRecipes() {
        synchronized (syncLock) {
            registeredRecipes.clear();
            recipeTypeCounts.clear();
        }
        // A previous runtime sync may still be represented by RecipeAPIImpl's
        // type cache.  Clearing the registration table must invalidate it too.
        RecipeAPIImpl.invalidateStaticCache();
        LOGGER.info("All registered recipes cleared");
    }

    @Override
    public boolean registerCompressorRecipe(ResourceLocation id, Ingredient input, ItemStack output, int processTime) {
        return registerRecipe(id, "compressor", () -> RecipeFactoryBridge.compressor("", input, output, processTime, 10, 1));
    }

    @Override
    public boolean registerExtractorRecipe(ResourceLocation id, Ingredient input, ItemStack output, int processTime) {
        return registerRecipe(id, "extractor", () -> RecipeFactoryBridge.extractor("", input, output, processTime, 15));
    }

    @Override
    public boolean registerElectricFurnaceRecipe(ResourceLocation id, Ingredient input, ItemStack output, int processTime, float experience) {
        return registerRecipe(id, "electric_furnace", () -> RecipeFactoryBridge.electricFurnace("", input, output, processTime, 20, experience));
    }

    @Override
    public boolean registerMaceratorRecipe(ResourceLocation id, Ingredient input, ItemStack output, int processTime) {
        return registerRecipe(id, "macerator", () -> RecipeFactoryBridge.macerator("", input, output, processTime, 10, 1));
    }

    @Override
    public boolean registerCentrifugeRecipe(ResourceLocation id, Ingredient input, ItemStack[] outputs, int processTime) {
        if (outputs == null || outputs.length == 0) { LOGGER.error("Centrifuge recipe {} has no outputs", id); return false; }
        ItemStack primary = outputs.length > 0 ? outputs[0] : ItemStack.EMPTY;
        ItemStack secondary = outputs.length > 1 ? outputs[1] : ItemStack.EMPTY;
        ItemStack tertiary = outputs.length > 2 ? outputs[2] : ItemStack.EMPTY;
        return registerRecipe(id, "centrifuge", () -> RecipeFactoryBridge.centrifuge("", input,
            primary, primary.getCount(), secondary, secondary.getCount(), tertiary, tertiary.getCount(), processTime, 40, 5000));
    }

    @Override
    public boolean registerRecyclerRecipe(ResourceLocation id, Ingredient input, ItemStack[] outputs, float[] chances, int processTime) {
        if (outputs == null || outputs.length != 1) { LOGGER.error("Recycler recipe {} requires exactly one output; multi-output recipes are not supported by this API", id); return false; }
        if (chances != null && chances.length > 1) { LOGGER.error("Recycler recipe {} requires at most one chance", id); return false; }
        // No chance given: the recycler's own scrap chance (1/8) applies to the recipe's output.
        float chance = chances == null || chances.length == 0 ? -1.0f : chances[0];
        if (chances != null && chances.length == 1 && (chance < 0.0f || chance > 1.0f)) { LOGGER.error("Recycler recipe {} has invalid chance {}", id, chance); return false; }
        return registerRecipe(id, "recycler", () -> RecipeFactoryBridge.recycler("", input, outputs[0], processTime, 2, chance));
    }

    @Override
    public boolean registerOreWasherRecipe(ResourceLocation id, Ingredient input, ItemStack[] outputs, int processTime) {
        if (outputs == null || outputs.length == 0) { LOGGER.error("Ore washer recipe {} has no outputs", id); return false; }
        ItemStack primary = outputs.length > 0 ? outputs[0] : ItemStack.EMPTY;
        ItemStack secondary = outputs.length > 1 ? outputs[1] : ItemStack.EMPTY;
        ItemStack tertiary = outputs.length > 2 ? outputs[2] : ItemStack.EMPTY;
        return registerRecipe(id, "washer", () -> RecipeFactoryBridge.washer("", input,
            primary, primary.getCount(), secondary, secondary.getCount(), tertiary, tertiary.getCount(), processTime, 5, 1000));
    }

    @Override
    public boolean registerThermalCentrifugeRecipe(ResourceLocation id, Ingredient input, ItemStack[] outputs, int processTime, int heatRequired) {
        if (outputs == null || outputs.length == 0) { LOGGER.error("Thermal centrifuge recipe {} has no outputs", id); return false; }
        ItemStack primary = outputs.length > 0 ? outputs[0] : ItemStack.EMPTY;
        ItemStack secondary = outputs.length > 1 ? outputs[1] : ItemStack.EMPTY;
        ItemStack tertiary = outputs.length > 2 ? outputs[2] : ItemStack.EMPTY;
        return registerRecipe(id, "thermal_centrifuge", () -> RecipeFactoryBridge.centrifuge("", input,
            primary, primary.getCount(), secondary, secondary.getCount(), tertiary, tertiary.getCount(), processTime, 40, heatRequired));
    }

    @Override
    public boolean registerBlenderRecipe(ResourceLocation id, Ingredient input, ItemStack output, int processTime) {
        return registerRecipe(id, "blender", () -> RecipeFactoryBridge.blender("", input, output, processTime, 5, 0.0f));
    }

    @Override
    @Deprecated
    public boolean registerFermenterRecipe(ResourceLocation id, Ingredient input, ItemStack output, int processTime) {
        LOGGER.error("Fermenter recipe {} not registered: the fermenter takes fluids and heat; use "
            + "registerFermenterRecipe(id, FluidStack input, int heat, FluidStack output)", id);
        return false;
    }

    @Override
    public boolean registerFermenterRecipe(ResourceLocation id, net.neoforged.neoforge.fluids.FluidStack input, int heat,
                                           net.neoforged.neoforge.fluids.FluidStack output) {
        if (input == null || input.isEmpty() || output == null || output.isEmpty() || heat <= 0) {
            LOGGER.error("Fermenter recipe {} needs an input fluid, an output fluid and a positive heat", id);
            return false;
        }
        return registerRecipe(id, "fermenter", () -> RecipeFactoryBridge.fermenter("", input, heat, output));
    }

    @Override
    public boolean registerFluidSolidRecipe(ResourceLocation id, Ingredient itemInput, String fluidInput, ItemStack output, int processTime) {
        // "namespace:path:amount" or "path:amount": the amount is after the LAST colon
        // (split(":") rejected every namespaced fluid id before 0.1.7.36)
        int colon = fluidInput == null ? -1 : fluidInput.lastIndexOf(':');
        if (colon <= 0 || colon == fluidInput.length() - 1) {
            LOGGER.error("Invalid fluid input format for recipe {}: expected 'fluid_id:amount', got '{}'", id, fluidInput);
            return false;
        }
        String[] parts = {fluidInput.substring(0, colon), fluidInput.substring(colon + 1)};
        try {
            String fluidName = parts[0];
            int fluidAmount = Integer.parseInt(parts[1]);
            if (fluidAmount <= 0) { LOGGER.error("Fluid amount must be positive in recipe {}: {}", id, fluidAmount); return false; }
            var fluidLoc = ResourceLocation.parse(fluidName);
            if (!BuiltInRegistries.FLUID.containsKey(fluidLoc)) { LOGGER.error("Fluid not found: {}", fluidName); return false; }
            var fluid = BuiltInRegistries.FLUID.get(fluidLoc);
            var fluidStack = new net.neoforged.neoforge.fluids.FluidStack(fluid, fluidAmount);
            return registerRecipe(id, "mix", () -> RecipeFactoryBridge.mix("",
                fluidStack, itemInput, 1, net.neoforged.neoforge.fluids.FluidStack.EMPTY, output, processTime, 10));
        } catch (NumberFormatException e) {
            LOGGER.error("Invalid fluid amount in recipe {}: {}", id, parts[1]);
            return false;
        }
    }

    @Override
    public boolean registerCannerRecipe(ResourceLocation id, Ingredient input, ItemStack output, int processTime) {
        Ingredient canIngredient = Ingredient.of(com.miophas.singularity_iteration.common.item.cell.mio_icif_cells.CELL_EMPTY.get());
        return registerRecipe(id, "canner", () -> RecipeFactoryBridge.canning("", canIngredient, input, output, processTime, 10));
    }

    @Override
    public boolean registerElectrolyzerRecipe(ResourceLocation id, Ingredient input, ItemStack[] outputs, int processTime) {
        if (outputs == null || outputs.length != 1) { LOGGER.error("Electrolyzer recipe {} requires exactly one output; multi-output recipes are not supported by this API", id); return false; }
        return registerRecipe(id, "electrolyzer", () -> RecipeFactoryBridge.electrolyzer("", input, outputs[0], processTime, 20, 0.0f));
    }

    @Override
    public boolean registerExtruderRecipe(ResourceLocation id, Ingredient input, ItemStack output, int processTime) {
        return registerRecipe(id, "extruder", () -> RecipeFactoryBridge.extruding("", input, output, processTime, 30, 1));
    }

    @Override
    @Deprecated
    public boolean registerLatheRecipe(ResourceLocation id, Ingredient input, ItemStack output, int processTime) {
        LOGGER.error("Lathe recipe {} not registered: the lathe cuts turning blanks to profiles; name one with "
            + "registerLatheShape(id, blank, profile, part) and ask for it with a mio_icif:lathe_state ingredient", id);
        return false;
    }

    @Override
    public boolean registerLatheShape(ResourceLocation id, net.minecraft.world.item.Item blank, int[] profile, ItemStack part) {
        if (id == null || !(blank instanceof com.miophas.singularity_iteration.common.item.lathe.LatheTurningBlanksItem turning)
                || part == null || part.isEmpty()) {
            LOGGER.error("Lathe shape {} not registered: it needs an id, a turning blank and a part", id);
            return false;
        }
        var shape = new com.miophas.singularity_iteration.common.item.lathe.LatheShapes.Shape(turning.getMaterial(),
            profile == null ? null : profile.clone(), BuiltInRegistries.ITEM.getKey(part.getItem()).toString(), id);
        var refused = com.miophas.singularity_iteration.common.item.lathe.LatheShapes.register(shape);
        if (refused.isPresent()) {
            LOGGER.error("Lathe shape {} not registered: {}", id, refused.get());
            return false;
        }
        LOGGER.info("Successfully registered lathe shape {} ({})", id, shape.profile());
        return true;
    }

    @Override
    public boolean registerRollingMachineRecipe(ResourceLocation id, Ingredient input, ItemStack output, int processTime) {
        return registerRecipe(id, "rolling", () -> RecipeFactoryBridge.rolling("", input, output, processTime, 20, 1));
    }

    @Override
    public boolean registerCutterRecipe(ResourceLocation id, Ingredient input, ItemStack output, int processTime) {
        return registerRecipe(id, "cutting", () -> RecipeFactoryBridge.cutting("", input, output, processTime, 25, 1));
    }

    @Override
    public boolean registerWelderRecipe(ResourceLocation id, Ingredient input, ItemStack output, int processTime) {
        return registerRecipe(id, "welder", () -> RecipeFactoryBridge.welder("", input, output, processTime, 10, 0.0f));
    }

    @Override
    public boolean registerWelderRecipe(ResourceLocation id, Ingredient[] inputs, ItemStack output, int processTime) {
        if (inputs == null || inputs.length != 1) { LOGGER.error("Welder recipe {} requires exactly one input; multi-input recipes are not supported by this API", id); return false; }
        return registerRecipe(id, "welder", () -> RecipeFactoryBridge.welder("", inputs[0], output, processTime, 10, 0.0f));
    }

    @Override
    public boolean registerLaserEngraverRecipe(ResourceLocation id, Ingredient input, ItemStack output, int processTime) {
        return registerRecipe(id, "laser_engraver", () -> RecipeFactoryBridge.laserEngraver("", input, output, processTime, 20, 0.0f));
    }

    @Override
    public boolean registerPrecisionAssemblerRecipe(ResourceLocation id, Ingredient input, ItemStack output, int processTime) {
        return registerRecipe(id, "precision_assembler", () -> RecipeFactoryBridge.precisionAssembler("", input, output, processTime, 10, 0.0f));
    }

    @Override
    public boolean registerPrecisionAssemblerRecipe(ResourceLocation id, Ingredient[] inputs, ItemStack output, int processTime) {
        if (inputs == null || inputs.length != 1) { LOGGER.error("Precision assembler recipe {} requires exactly one input; multi-input recipes are not supported by this API", id); return false; }
        return registerRecipe(id, "precision_assembler", () -> RecipeFactoryBridge.precisionAssembler("", inputs[0], output, processTime, 10, 0.0f));
    }

    @Override
    public boolean registerVacuumFreezerRecipe(ResourceLocation id, Ingredient input, ItemStack output, int processTime) {
        return registerRecipe(id, "vacuum_freezer", () -> RecipeFactoryBridge.vacuumFreezer("", input, output, processTime, 30, 0.0f));
    }

    @Override
    public boolean registerPlasmaFurnaceRecipe(ResourceLocation id, Ingredient input, ItemStack output, int processTime) {
        return registerRecipe(id, "plasma_furnace", () -> RecipeFactoryBridge.plasmaFurnace("", input, output, processTime, 100, 0.0f));
    }

    @Override
    public boolean registerFusionReactorRecipe(ResourceLocation id, Ingredient input1, Ingredient input2, ItemStack output, long energyRequired, int processTime) {
        return registerRecipe(id, "fusion_reactor", () -> RecipeFactoryBridge.fusionReactor("", input1, input2, output, processTime, energyRequired));
    }

    @Override
    @Deprecated
    public boolean registerMassFabricatorRecipe(ResourceLocation id, Ingredient input, ItemStack output, int processTime) {
        LOGGER.error("Mass fabricator recipe {} not registered: the Mass Fabricator makes UU-matter from EU and takes "
            + "items only as amplifiers; use registerMassFabricatorAmplifier(id, amplifier, euCredit)", id);
        return false;
    }

    @Override
    public boolean registerMassFabricatorAmplifier(ResourceLocation id, Ingredient amplifier, int euCredit) {
        if (amplifier == null || amplifier.isEmpty() || euCredit <= 0) {
            LOGGER.error("Mass fabricator amplifier {} needs amplifier items and a positive EU credit", id);
            return false;
        }
        return registerRecipe(id, "mass_fabricator", () -> RecipeFactoryBridge.massFabricatorAmplifier("", amplifier, euCredit));
    }

    @Override
    @Deprecated
    public boolean registerReplicatorRecipe(ResourceLocation id, Ingredient input, ItemStack output, int processTime) {
        LOGGER.error("Replicator recipe {} not registered: the replicator copies scanned patterns for their UU-matter "
            + "price; set prices with a replication list, data/<namespace>/uu_replication/*.json", id);
        return false;
    }

    @Override
    @Deprecated
    public boolean registerScannerRecipe(ResourceLocation id, Ingredient input, ItemStack output, int processTime) {
        LOGGER.error("Scanner recipe {} not registered: the scanner records any item with a UU-matter price; prices "
            + "come from replication lists, data/<namespace>/uu_replication/*.json", id);
        return false;
    }

    @Override
    public boolean registerFluidRefiningRecipe(ResourceLocation id, Ingredient ingredient, net.neoforged.neoforge.fluids.FluidStack inputFluid, net.neoforged.neoforge.fluids.FluidStack outputFluid, int processTime, int energyPerTick) {
        return registerRecipe(id, "fluid_refining", () -> RecipeFactoryBridge.fluidRefining("", ingredient, inputFluid.getFluid(), outputFluid, processTime, energyPerTick));
    }

    private final Object syncLock = new Object();
    private volatile boolean isSyncing = false;

    @Override
    public void syncToRecipeManager() {
        synchronized (syncLock) {
            if (isSyncing) { LOGGER.debug("Recipe sync already in progress, skipping"); return; }
            isSyncing = true;
            try {
                MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
                if (server == null) { LOGGER.debug("Server not available, skipping recipe sync"); return; }
                RecipeManager recipeManager = server.getRecipeManager();
                if (recipeManager == null) { LOGGER.debug("RecipeManager is not available, skipping recipe sync"); return; }
                if (registeredRecipes.isEmpty()) return;
                Set<ResourceLocation> runtimeIds = registeredRecipes.keySet();
                List<RecipeHolder<?>> existingRecipes = recipeManager.getRecipes().stream()
                    .filter(h -> !runtimeIds.contains(h.id()))
                    .collect(Collectors.toList());
                int addedCount = 0;
                for (var entry : registeredRecipes.entrySet()) {
                    Recipe<?> recipe = entry.getValue().recipe();
                    if (recipe != null) { existingRecipes.add(new RecipeHolder<>(entry.getKey(), recipe)); addedCount++; }
                }
                recipeManager.replaceRecipes(existingRecipes);
                RecipeAPIImpl.invalidateStaticCache();
                LOGGER.info("Synced {} runtime recipes to RecipeManager (total: {})", addedCount, existingRecipes.size());
            } catch (Exception e) {
                LOGGER.error("Failed to sync recipes to RecipeManager: {}", e.getMessage(), e);
            } finally {
                isSyncing = false;
            }
        }
    }

    private boolean registerRecipe(ResourceLocation id, String type, RecipeFactory recipeFactory) {
        if (id == null) { LOGGER.error("Recipe ID cannot be null"); return false; }
        synchronized (syncLock) {
            if (registeredRecipes.containsKey(id)) { LOGGER.warn("Recipe {} is already registered, overwriting", id); }
            try {
                var recipe = recipeFactory.create();
                if (recipe == null) { LOGGER.warn("Recipe {} factory returned null, registration skipped", id); return false; }
                var previous = registeredRecipes.put(id, new RegisteredRecipe(type, recipe));
                if (previous == null) {
                    recipeTypeCounts.merge(type, 1, Integer::sum);
                } else if (!Objects.equals(previous.recipeType(), type)) {
                    decrementRecipeType(previous.recipeType());
                    recipeTypeCounts.merge(type, 1, Integer::sum);
                }
                LOGGER.info("Successfully registered {} recipe: {}", type, id);
                return true;
            } catch (Exception e) {
                LOGGER.error("Failed to register recipe {}: {}", id, e.getMessage(), e);
                return false;
            }
        }
    }

    private void decrementRecipeType(String type) {
        if (type == null) return;
        recipeTypeCounts.computeIfPresent(type, (ignored, count) -> count <= 1 ? null : count - 1);
    }

    @FunctionalInterface
    private interface RecipeFactory { Recipe<?> create(); }

    public record RegisteredRecipe(String recipeType, Recipe<?> recipe) {}
}
