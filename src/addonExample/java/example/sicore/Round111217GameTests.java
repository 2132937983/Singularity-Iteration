package example.sicore;

import static example.sicore.Round33GameTests.call;
import static example.sicore.Round33GameTests.common;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * 0.1.11.217: a player with EMI saw one Tin Plate on the compressor page of the Dense Tin Plate (it takes 9), put 4 in
 * and the compressor answered "no matching recipe". The EMI pages of the processing machines had a generic layout of
 * their own; they now take their stacks, counts, times and lines from {@code client.integration.MachineRecipeViews},
 * as the JEI categories do. Neither viewer is loaded in the GameTests: these check that data.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round111217GameTests {
    private Round111217GameTests() {}

    private static Class<?> views() { return common("client.integration.MachineRecipeViews"); }

    /** The recipes of a type of {@code mio_icif_ModRecipes} (by its field name). */
    private static List<RecipeHolder<?>> recipes(GameTestHelper h, String typeField) {
        Object type;
        try {
            type = call(common("recipe.mio_icif_ModRecipes").getField(typeField).get(null), "get");
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(typeField, e);
        }
        List<RecipeHolder<?>> out = new ArrayList<>();
        for (RecipeHolder<?> recipe : h.getLevel().getRecipeManager().getRecipes()) if (recipe.value().getType() == type) out.add(recipe);
        return out;
    }

    @GameTest(template = "empty", batch = "round111217_machine_pages", timeoutTicks = 20)
    public static void theDenseTinPlatePageAsksForNinePlates(GameTestHelper h) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("mio_icif", "compressor/tin_plate_to_dense");
        RecipeHolder<?> recipe = h.getLevel().getRecipeManager().byKey(id).orElseThrow(() -> new AssertionError("no recipe " + id));
        Object view = call(views(), "compressor", recipe.value());
        List<?> inputs = (List<?>) call(view, "inputs");
        h.assertTrue(!inputs.isEmpty() && inputs.stream().allMatch(s -> ((ItemStack) s).getCount() == 9), "the page shows " + inputs);
        ItemStack result = (ItemStack) call(view, "result");
        h.assertTrue("resource/item_tin_denseplate".equals(BuiltInRegistries.ITEM.getKey(result.getItem()).getPath()), "the result is " + result);
        // the time and power the compressor itself uses for the recipe
        Object cost = call(common("processing.Pressing"), "cost", recipe.value());
        h.assertTrue(((Number) call(view, "ticks")).intValue() == ((Number) call(cost, "ticks")).intValue()
            && ((Number) call(view, "euPerTick")).longValue() == ((Number) call(cost, "power")).longValue(), "the time or power is not the compressor's");
        h.assertTrue(((List<?>) call(view, "lines")).size() == 1, "one time and energy line");
        h.succeed();
    }

    /** Type field, view, the recipe's count getter, the view's stacks for that count. */
    private static final String[][] COUNTED = {
        {"COMPRESSOR_TYPE", "compressor", "getIngredientCount", "inputs"},
        {"POWDER_TYPE", "powder", "getIngredientCount", "inputs"},
        {"BLOCK_CUTTER_TYPE", "blockCutter", "getIngredientCount", "inputs"},
        {"ROLLING_TYPE", "rolling", "getIngredientCount", "inputs"},
        {"CUTTING_TYPE", "cutting", "getIngredientCount", "inputs"},
        {"EXTRUDING_TYPE", "extruding", "getIngredientCount", "inputs"},
        {"BLAST_FURNACE_TYPE", "blasting", "getIngredientCount", "ore"},
        {"MIX_TYPE", "mixing", "getMaterialCount", "material"},
    };

    @GameTest(template = "empty", batch = "round111217_machine_pages", timeoutTicks = 40)
    public static void everyPageShowsTheCountTheMachineNeeds(GameTestHelper h) {
        List<String> wrong = new ArrayList<>();
        int many = 0;
        for (String[] row : COUNTED) {
            for (RecipeHolder<?> recipe : recipes(h, row[0])) {
                int count = Math.max(1, ((Number) call(recipe.value(), row[2])).intValue());
                List<?> stacks = (List<?>) call(call(views(), row[1], recipe.value()), row[3]);
                if (count > 1) many++;
                // an ingredient of an empty tag has no stacks to show; every stack shown carries the count
                if (!stacks.stream().allMatch(s -> ((ItemStack) s).getCount() == count))
                    wrong.add(recipe.id() + " takes " + count + ", the page shows " + stacks);
            }
        }
        h.assertTrue(wrong.isEmpty(), wrong.size() + " pages show the wrong count, e.g. " + wrong.subList(0, Math.min(5, wrong.size())));
        h.assertTrue(many >= 40, "only " + many + " recipes take more than one item (the compressor alone has about 50)");
        h.succeed();
    }

    private static final String[][] ALL = {
        {"POWDER_TYPE", "powder"}, {"WASHER_TYPE", "washer"}, {"CENTRIFUGE_TYPE", "centrifuge"}, {"EXTRACTOR_TYPE", "extractor"},
        {"COMPRESSOR_TYPE", "compressor"}, {"BLOCK_CUTTER_TYPE", "blockCutter"}, {"ROLLING_TYPE", "rolling"},
        {"CUTTING_TYPE", "cutting"}, {"EXTRUDING_TYPE", "extruding"}, {"CANNING_TYPE", "canning"}, {"MIX_TYPE", "mixing"},
        {"BLAST_FURNACE_TYPE", "blasting"}, {"MOLECULAR_TRANSFORMER_TYPE", "molecularTransformer"},
    };

    @GameTest(template = "empty", batch = "round111217_machine_pages", timeoutTicks = 40)
    public static void everyMachinePageHasItsTimeLinesWaterAndAir(GameTestHelper h) {
        List<String> wrong = new ArrayList<>();
        for (String[] row : ALL) {
            List<RecipeHolder<?>> list = recipes(h, row[0]);
            if (list.isEmpty()) wrong.add(row[0] + ": no recipes");
            for (RecipeHolder<?> recipe : list) {
                Object view = call(views(), row[1], recipe.value());
                List<?> lines = (List<?>) call(view, "lines");
                int ticks = ((Number) call(view, "ticks")).intValue();
                // the molecular transformer has no fixed time: it runs as fast as the power comes
                boolean timed = !row[1].equals("molecularTransformer");
                if (lines.isEmpty() || (timed && ticks <= 0)) wrong.add(recipe.id() + ": " + ticks + " t, lines " + lines);
                if (row[1].equals("washer") && ((Number) call(recipe.value(), "getWaterAmount")).intValue() > 0
                        && (((Number) call(view, "water")).intValue() <= 0 || lines.size() < 2))
                    wrong.add(recipe.id() + ": the water is missing");
                if (row[1].equals("blasting") && ((Number) call(recipe.value(), "getAirCostPerTick")).intValue() > 0
                        && (((ItemStack) call(view, "airCells")).isEmpty() || !(Boolean) call(view, "usesAir")))
                    wrong.add(recipe.id() + ": the air cells are missing");
            }
        }
        h.assertTrue(wrong.isEmpty(), wrong.toString());
        h.succeed();
    }
}
