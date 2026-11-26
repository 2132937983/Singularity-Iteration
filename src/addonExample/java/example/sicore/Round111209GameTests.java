package example.sicore;

import static example.sicore.Round33GameTests.call;
import static example.sicore.Round33GameTests.common;

import java.util.Collection;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * 0.1.11.209: a player with EMI (and JEI) found no recipe for the Neutron Plate. EMI never calls the JEI plugin of a mod
 * that has an EMI plugin, so the native EMI plugin now has every page of the JEI plugin, built from the data the JEI
 * pages use ({@code client.integration.RecipeViews}). EMI is not loaded in the GameTests: these check that data.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round111209GameTests {
    private Round111209GameTests() {}

    private static Class<?> views() { return common("client.integration.RecipeViews"); }

    private static Class<?> core(String name) {
        try {
            return Class.forName("com.miophas.singularity_iteration.core." + name);
        } catch (ClassNotFoundException e) {
            throw new AssertionError(name, e);
        }
    }

    @GameTest(template = "empty", batch = "round111209_emi_pages", timeoutTicks = 20)
    public static void theNeutronPlateHasANeutronPolymerizerRecipe(GameTestHelper h) {
        ResourceLocation type = ResourceLocation.fromNamespaceAndPath("mio_icif", "neutron_polymerizer");
        ResourceLocation plate = ResourceLocation.fromNamespaceAndPath("mio_icif", "resource/item_neutron_plate");
        boolean found = false;
        for (RecipeHolder<?> holder : h.getLevel().getRecipeManager().getRecipes()) {
            if (!type.equals(BuiltInRegistries.RECIPE_TYPE.getKey(holder.value().getType()))) continue;
            ItemStack result = holder.value().getResultItem(h.getLevel().registryAccess());
            if (plate.equals(BuiltInRegistries.ITEM.getKey(result.getItem()))) {
                h.assertTrue(!holder.value().getIngredients().isEmpty() && !holder.value().getIngredients().get(0).isEmpty(),
                    holder.id() + " has no input to show");
                found = true;
            }
        }
        h.assertTrue(found, "no mio_icif:neutron_polymerizer recipe makes the Neutron Plate");
        h.succeed();
    }

    @GameTest(template = "empty", batch = "round111209_emi_pages", timeoutTicks = 20)
    public static void theCannerTankPagesHaveACellForWater(GameTestHelper h) {
        List<?> fills = (List<?>) call(views(), "cellFills");
        h.assertTrue(!fills.isEmpty(), "no Tank → Cell pages");
        boolean water = false;
        for (Object fill : fills) {
            ItemStack empty = (ItemStack) call(fill, "empty"), filled = (ItemStack) call(fill, "filled");
            FluidStack fluid = (FluidStack) call(fill, "fluid");
            h.assertTrue(!empty.isEmpty() && !filled.isEmpty() && !fluid.isEmpty(), "an incomplete canner page: " + fill);
            water |= fluid.getFluid() == Fluids.WATER;
        }
        h.assertTrue(water, "no canner page for water");
        h.succeed();
    }

    @GameTest(template = "empty", batch = "round111209_emi_pages", timeoutTicks = 100)
    public static void everyStructureAndReactorLayoutHasItsPage(GameTestHelper h) {
        Collection<?> blueprints = (Collection<?>) call(core("api.multiblock.StructureBlueprintRegistry"), "all");
        h.assertTrue(!blueprints.isEmpty(), "no multiblock structures");
        for (Object blueprint : blueprints) {
            List<?> materials = (List<?>) call(views(), "structureMaterials", blueprint);
            h.assertTrue(materials.stream().anyMatch(m -> !((List<?>) m).isEmpty()), "no materials for " + call(blueprint, "id"));
        }
        Collection<?> layouts = (Collection<?>) call(core("api.reactor.ReactorLayoutRegistry"), "all");
        h.assertTrue(!layouts.isEmpty(), "no reactor layouts");
        Class<?> analysis = common("reactor.ReactorLayoutAnalysis");
        for (Object layout : layouts) {
            ResourceLocation id = (ResourceLocation) call(layout, "id");
            @SuppressWarnings("unchecked")
            List<Component> lines = (List<Component>) call(views(), "reactorLayoutLines", call(analysis, "of", layout));
            h.assertTrue(lines.size() >= 2, "no report for the layout " + id);
            // SI's own layouts use only SI's components: their report is never the "unknown component" line
            if (id.getNamespace().equals("mio_icif") && lines.get(0).getContents() instanceof TranslatableContents t)
                h.assertTrue(!t.getKey().endsWith(".unknown"), "the layout " + id + " has unknown components");
        }
        h.succeed();
    }

    @GameTest(template = "empty", batch = "round111209_emi_pages", timeoutTicks = 20)
    public static void everyLathePartHasABlankAToolAndAResult(GameTestHelper h) {
        List<?> shapes = (List<?>) call(common("item.lathe.LatheShapes"), "all");
        h.assertTrue(!shapes.isEmpty(), "no lathe parts");
        Class<?> blanks = common("item.lathe.LatheTurningBlanksItem");
        for (Object shape : shapes) {
            ItemStack blank = (ItemStack) call(views(), "latheBlank", call(shape, "material"));
            h.assertTrue(!blank.isEmpty(), "no blank for " + call(shape, "profile"));
            h.assertTrue(!((List<?>) call(views(), "latheTools", blank)).isEmpty(), "no lathe tool cuts " + blank);
            ItemStack part = (ItemStack) call(blanks, "getStackForState", blank, call(shape, "state"));
            h.assertTrue(!part.isEmpty(), "no part for " + call(shape, "profile"));
        }
        h.succeed();
    }
}
