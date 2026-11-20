package example.sicore;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * 0.1.11.103 (solar overhaul S3): the quantum generator is made from the top panel of each solar route; route A keeps one
 * recipe per panel, each from the panel below it; the photon resonance panel sends HV packets. (The mod's common classes by
 * reflection: addon examples do not compile against them.)
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round111103GameTests {
    private Round111103GameTests() {}

    private static Item item(String path) { return BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("mio_icif", path)); }

    /** Every crafting recipe that makes {@code result}, by id. */
    private static List<RecipeHolder<?>> recipesFor(GameTestHelper h, Item result) {
        List<RecipeHolder<?>> found = new ArrayList<>();
        var access = h.getLevel().registryAccess();
        for (RecipeHolder<?> holder : h.getLevel().getRecipeManager().getRecipes()) {
            ItemStack out = holder.value().getResultItem(access);
            if (!out.isEmpty() && out.is(result)) found.add(holder);
        }
        return found;
    }

    private static boolean uses(RecipeHolder<?> holder, Item item) {
        for (Ingredient ingredient : holder.value().getIngredients())
            for (ItemStack stack : ingredient.getItems()) if (stack.is(item)) return true;
        return false;
    }

    /** One recipe: the quantum solar panel, the ultimate photon resonance panel, a matter fabricator and a quantum core. */
    @GameTest(template = "empty", timeoutTicks = 20)
    public static void quantumGeneratorJoinsBothRoutes(GameTestHelper h) {
        List<RecipeHolder<?>> recipes = recipesFor(h, item("generator/block_quantum_generator"));
        h.assertTrue(recipes.size() == 1, "quantum generator recipes: " + recipes.stream().map(RecipeHolder::id).toList());
        RecipeHolder<?> r = recipes.get(0);
        h.assertTrue(r.id().equals(ResourceLocation.fromNamespaceAndPath("mio_icif", "block_quantum_generator")), "recipe id " + r.id());
        for (String part : new String[]{"generator/block_quantum_solar_panel", "generator/block_ultimate_photon_resonance_solar_generator",
                "producer/block_matter_elc", "resource/item_quantum_core"})
            h.assertTrue(uses(r, item(part)), "the quantum generator does not take " + part);
        h.succeed();
    }

    /** Route A: one recipe a panel, each from the panel below it (the shortcuts are gone). */
    @GameTest(template = "empty", timeoutTicks = 20)
    public static void routeAHasOneRecipePerPanel(GameTestHelper h) {
        String[][] ladder = {
            {"generator/block_advanced_solar_panel", "block_advanced_solar_panel", "generator/block_solar_generator"},
            {"generator/block_hybrid_solar_panel", "block_hybrid_solar_panel", "generator/block_advanced_solar_panel"},
            {"generator/block_ultimate_hybrid_solar_panel", "block_ultimate_hybrid_solar_panel", "generator/block_hybrid_solar_panel"},
            {"generator/block_quantum_solar_panel", "shaped_quantum_solar_panel", "generator/block_ultimate_hybrid_solar_panel"}};
        for (String[] step : ladder) {
            List<RecipeHolder<?>> recipes = recipesFor(h, item(step[0]));
            h.assertTrue(recipes.size() == 1, step[0] + " recipes: " + recipes.stream().map(RecipeHolder::id).toList());
            RecipeHolder<?> r = recipes.get(0);
            h.assertTrue(r.id().getPath().equals(step[1]), step[0] + " is made by " + r.id());
            h.assertTrue(uses(r, item(step[2])), step[0] + " is not made from " + step[2]);
        }
        h.succeed();
    }

    /** The photon resonance panel is HV like the ultimate hybrid of the same rank: tier 3, 512 EU packets. */
    @GameTest(template = "empty", timeoutTicks = 20)
    public static void photonResonanceIsHv(GameTestHelper h) {
        BlockPos at = new BlockPos(1, 2, 1);
        h.setBlock(at, BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("mio_icif", "generator/block_photon_resonance_solar_generator")));
        BlockEntity be = h.getBlockEntity(at);
        int tier = (int) Round33GameTests.call(be, "getTier");
        Object cable = Round33GameTests.call(be, "getEffectiveCableTier");
        long rating = ((Number) Round33GameTests.call(cable, "getPowerRating")).longValue();
        Object profile = ((java.util.Optional<?>) Round33GameTests.call(Round33GameTests.common("energy.SolarGeneratorProfile"), "find",
            "mio_icif:generator/block_photon_resonance_solar_generator")).orElseThrow();
        long packet = (long) Round33GameTests.call(profile, "outputPacket");
        h.assertTrue(tier == 3 && rating == 512 && packet == 512, "tier " + tier + ", cable rating " + rating + ", packet " + packet);
        h.succeed();
    }
}
