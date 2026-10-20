package example.sicore;

import com.google.gson.JsonParser;
import com.miophas.singularity_iteration.core.api.MioIcifAPI;
import com.miophas.singularity_iteration.core.api.recipe.IRecipeAPI;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.lang.reflect.Method;

/**
 * Every recipe type the recipe API names is registered, so API recipes of every kind (welder, blender,
 * electrolyzer, ...) reach the recipe manager and are found again, and one such recipe no longer
 * aborts the runtime sync for all the others.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class RecipeApiTypesGameTests {
    private RecipeApiTypesGameTests() {}

    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath(CoreExampleMod.ID, path); }

    @GameTest(batch = "recipe_api", template = "empty", timeoutTicks = 20)
    public static void everyApiRecipeTypeIsRegistered(GameTestHelper h) {
        IRecipeAPI api = MioIcifAPI.instance().getRecipeAPI();
        for (ResourceLocation type : api.getAllRecipeTypeIds()) {
            h.assertTrue(BuiltInRegistries.RECIPE_TYPE.containsKey(type), "listed recipe type " + type + " is not registered");
        }
        for (Method getter : IRecipeAPI.class.getMethods()) {
            if (getter.getParameterCount() != 0 || !getter.getName().endsWith("RecipeType") || getter.getReturnType() != RecipeType.class) continue;
            Object type;
            try {
                type = getter.invoke(api);
            } catch (ReflectiveOperationException e) {
                throw new AssertionError(getter.getName() + " fails: " + (e.getCause() != null ? e.getCause() : e));
            }
            h.assertTrue(type != null && BuiltInRegistries.RECIPE_TYPE.getKey((RecipeType<?>) type) != null, getter.getName() + " -> " + type);
        }
        h.succeed();
    }

    @GameTest(batch = "recipe_api", template = "empty", timeoutTicks = 20)
    public static void apiRecipesSyncAndAreFound(GameTestHelper h) {
        var registration = MioIcifAPI.instance().getRecipeRegistrationAPI();
        IRecipeAPI api = MioIcifAPI.instance().getRecipeAPI();
        h.assertTrue(registration.registerBlenderRecipe(id("api_blender"), Ingredient.of(Items.BARRIER), new ItemStack(Items.CLAY), 100), "blender");
        h.assertTrue(registration.registerWelderRecipe(id("api_welder"), Ingredient.of(Items.JIGSAW), new ItemStack(Items.IRON_BLOCK), 80), "welder");
        h.assertTrue(registration.registerCompressorRecipe(id("api_compressor"), Ingredient.of(Items.STRUCTURE_VOID), new ItemStack(Items.FLINT), 100), "compressor");
        registration.syncToRecipeManager();

        var manager = h.getLevel().getRecipeManager();
        // the compressor recipe arrives too: before, the blender recipe's getType() threw and the whole sync was lost
        for (String path : new String[] {"api_blender", "api_welder", "api_compressor"}) {
            h.assertTrue(manager.byKey(id(path)).isPresent(), path + " not synced to the recipe manager");
        }
        var blender = api.findBlenderRecipe(new ItemStack(Items.BARRIER), h.getLevel());
        h.assertTrue(blender.isPresent() && blender.get().id().equals(id("api_blender")), "blender recipe not found: " + blender);
        h.assertTrue(blender.get().value().getType() == api.getBlenderRecipeType(), "blender recipe type " + blender.get().value().getType());
        var welder = api.findWelderRecipe(new ItemStack[] {new ItemStack(Items.JIGSAW)}, h.getLevel());
        h.assertTrue(welder.isPresent() && welder.get().id().equals(id("api_welder")), "welder recipe not found: " + welder);
        h.succeed();
    }

    /** A JSON recipe of a generic type is that type's recipe, not an electric furnace recipe. */
    @GameTest(batch = "recipe_api", template = "empty", timeoutTicks = 20)
    public static void jsonRecipesKeepTheirType(GameTestHelper h) {
        String json = """
            {"ingredient": {"item": "minecraft:iron_ingot"}, "result": {"id": "minecraft:iron_block"}, "processingtime": 40}""";
        for (String type : new String[] {"welder", "blender", "electrolyzer", "recycler", "scanner", "vacuum_freezer"}) {
            var serializer = BuiltInRegistries.RECIPE_SERIALIZER.get(ResourceLocation.fromNamespaceAndPath("mio_icif", type));
            h.assertTrue(serializer != null, "no mio_icif:" + type + " serializer");
            Recipe<?> recipe = serializer.codec().codec().parse(JsonOps.INSTANCE, JsonParser.parseString(json)).getOrThrow();
            ResourceLocation parsed = BuiltInRegistries.RECIPE_TYPE.getKey(recipe.getType());
            h.assertTrue(parsed != null && parsed.getPath().equals(type), type + " JSON parsed as " + parsed + " (" + recipe.getClass().getSimpleName() + ")");
        }
        h.succeed();
    }
}
