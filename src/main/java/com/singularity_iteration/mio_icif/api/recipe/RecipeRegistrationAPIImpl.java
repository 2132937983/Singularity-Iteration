package com.singularity_iteration.mio_icif.api.recipe;

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

    public Map<ResourceLocation, RegisteredRecipe> getRegisteredRecipes() {
        return Collections.unmodifiableMap(registeredRecipes);
    }

    public int getRecipeCount(String recipeType) {
        return (int) registeredRecipes.values().stream()
            .filter(r -> r.recipeType().equals(recipeType))
            .count();
    }

    public void clearRecipes() {
        registeredRecipes.clear();
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
        return registerRecipe(id, "macerator", () -> RecipeFactoryBridge.macerator("", input, output, processTime, 10));
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
        if (outputs == null || outputs.length == 0) { LOGGER.error("Recycler recipe {} has no outputs", id); return false; }
        return registerRecipe(id, "recycler", () -> RecipeFactoryBridge.recycler("", input, outputs[0], processTime, 2, 0.0f));
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
    public boolean registerFermenterRecipe(ResourceLocation id, Ingredient input, ItemStack output, int processTime) {
        return registerRecipe(id, "fermenter", () -> RecipeFactoryBridge.fermenter("", input, output, processTime, 5, 0.0f));
    }

    @Override
    public boolean registerFluidSolidRecipe(ResourceLocation id, Ingredient itemInput, String fluidInput, ItemStack output, int processTime) {
        String[] parts = fluidInput.split(":");
        if (parts.length != 2) { LOGGER.error("Invalid fluid input format for recipe {}: expected 'fluid_name:amount', got '{}'", id, fluidInput); return false; }
        try {
            String fluidName = parts[0];
            int fluidAmount = Integer.parseInt(parts[1]);
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
        Ingredient canIngredient = Ingredient.of(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("mio_icif", "cell_empty")));
        return registerRecipe(id, "canner", () -> RecipeFactoryBridge.canning("", canIngredient, input, output, processTime, 10));
    }

    @Override
    public boolean registerElectrolyzerRecipe(ResourceLocation id, Ingredient input, ItemStack[] outputs, int processTime) {
        if (outputs == null || outputs.length == 0) { LOGGER.error("Electrolyzer recipe {} has no outputs", id); return false; }
        return registerRecipe(id, "electrolyzer", () -> RecipeFactoryBridge.electrolyzer("", input, outputs[0], processTime, 20, 0.0f));
    }

    @Override
    public boolean registerExtruderRecipe(ResourceLocation id, Ingredient input, ItemStack output, int processTime) {
        return registerRecipe(id, "extruder", () -> RecipeFactoryBridge.extruding("", input, output, processTime, 30, 1));
    }

    @Override
    public boolean registerLatheRecipe(ResourceLocation id, Ingredient input, ItemStack output, int processTime) {
        return registerRecipe(id, "lathe", () -> RecipeFactoryBridge.lathe("", input, output, processTime, 10, 0.0f));
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
        if (inputs == null || inputs.length == 0) { throw new IllegalArgumentException("Welder recipe " + id + " has no inputs"); }
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
        if (inputs == null || inputs.length == 0) { throw new IllegalArgumentException("Precision assembler recipe " + id + " has no inputs"); }
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
    public boolean registerMassFabricatorRecipe(ResourceLocation id, Ingredient input, ItemStack output, int processTime) {
        return registerRecipe(id, "mass_fabricator", () -> RecipeFactoryBridge.massFabricator("", input, output, processTime, 100, 0.0f));
    }

    @Override
    public boolean registerReplicatorRecipe(ResourceLocation id, Ingredient input, ItemStack output, int processTime) {
        return registerRecipe(id, "replicator", () -> RecipeFactoryBridge.replicator("", input, output, processTime, 50, 0.0f));
    }

    @Override
    public boolean registerScannerRecipe(ResourceLocation id, Ingredient input, ItemStack output, int processTime) {
        return registerRecipe(id, "scanner", () -> RecipeFactoryBridge.scanner("", input, output, processTime, 20, 0.0f));
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
        }
        try {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server == null) { LOGGER.debug("Server not available, skipping recipe sync"); return; }
            RecipeManager recipeManager = server.getRecipeManager();
            if (recipeManager == null) { LOGGER.debug("RecipeManager is not available, skipping recipe sync"); return; }
            if (registeredRecipes.isEmpty()) return;

            synchronized (syncLock) {
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
                LOGGER.info("Synced {} runtime recipes to RecipeManager (total: {})", addedCount, existingRecipes.size());
            }
        } catch (Exception e) {
            LOGGER.error("Failed to sync recipes to RecipeManager: {}", e.getMessage(), e);
        } finally {
            synchronized (syncLock) { isSyncing = false; }
        }
    }

    private boolean registerRecipe(ResourceLocation id, String type, RecipeFactory recipeFactory) {
        if (id == null) { LOGGER.error("Recipe ID cannot be null"); return false; }
        if (registeredRecipes.containsKey(id)) { LOGGER.warn("Recipe {} is already registered, overwriting", id); }
        try {
            var recipe = recipeFactory.create();
            if (recipe == null) { LOGGER.warn("Recipe {} factory returned null, registration skipped", id); return false; }
            registeredRecipes.put(id, new RegisteredRecipe(type, recipe));
            LOGGER.info("Successfully registered {} recipe: {}", type, id);
            return true;
        } catch (Exception e) {
            LOGGER.error("Failed to register recipe {}: {}", id, e.getMessage(), e);
            return false;
        }
    }

    @FunctionalInterface
    private interface RecipeFactory { Recipe<?> create(); }

    public record RegisteredRecipe(String recipeType, Recipe<?> recipe) {}
}