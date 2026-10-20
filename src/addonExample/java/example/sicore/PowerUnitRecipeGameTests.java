package example.sicore;

import com.miophas.singularity_iteration.core.api.item.IBatteryItem;
import java.util.List;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The Mining Drill and Chainsaw and their component chain against IC2 2.2.828 shaped_recipes.ini:
 * Drill " I |III|IBI", Chainsaw " II|III|BI " (iron plates, Power Unit), Power Unit "BAC|BIM|BAC"
 * (RE batteries, copper cable, iron casings, basic circuit, electric motor), Small Power Unit
 * " AC|BIM| AC", Diamond Drill " D |DtD| d " (diamonds, turned iron blank, Mining Drill).
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class PowerUnitRecipeGameTests {
    private PowerUnitRecipeGameTests() {}

    private static ResourceLocation rl(String path) { return ResourceLocation.fromNamespaceAndPath("mio_icif", path); }
    private static ItemStack item(String path) { return new ItemStack(BuiltInRegistries.ITEM.get(rl(path))); }

    private static RecipeHolder<?> recipe(GameTestHelper h, String id) {
        var holder = h.getLevel().getRecipeManager().byKey(rl(id));
        h.assertTrue(holder.isPresent(), "no recipe " + id);
        return holder.get();
    }

    private static boolean takes(NonNullList<Ingredient> grid, int slot, ItemStack stack) {
        return grid.get(slot).test(stack);
    }

    /** Iron plates in a recipe grid. */
    private static int plates(NonNullList<Ingredient> grid, ItemStack plate) {
        int n = 0;
        for (Ingredient i : grid) if (i.test(plate)) n++;
        return n;
    }

    /**
     * IC2 Drill " I |III|IBI" and Chainsaw " II|III|BI " with the head as a part: Drill Bit " I |III"
     * then " H |IBI", Chainsaw Blade " II|III" then " H|BI"; still 6 iron plates and a Power Unit each.
     */
    @GameTest(batch = "tool_recipes", template = "empty", timeoutTicks = 20)
    public static void drillAndChainsawAreBuiltFromTheirHeads(GameTestHelper h) {
        ItemStack unit = item("item_tool_power_unit"), bit = item("resource/item_drill_bit"), blade = item("resource/item_chainsaw_blade");
        ItemStack plate = recipe(h, "item_drill_bit").value().getIngredients().get(1).getItems()[0];
        h.assertTrue(!bit.isEmpty() && !blade.isEmpty(), "drill bit or chainsaw blade not registered");

        NonNullList<Ingredient> bitGrid = recipe(h, "item_drill_bit").value().getIngredients();
        h.assertTrue(bitGrid.size() == 6 && bitGrid.get(0).isEmpty() && bitGrid.get(2).isEmpty() && plates(bitGrid, plate) == 4, "drill bit is not \" I |III\"");
        NonNullList<Ingredient> drill = recipe(h, "shaped_153_mining_drill").value().getIngredients();
        h.assertTrue(drill.size() == 6 && takes(drill, 1, bit) && takes(drill, 4, unit) && drill.get(0).isEmpty() && drill.get(2).isEmpty()
            && plates(drill, plate) == 2, "drill is not \" H |IBI\"");

        NonNullList<Ingredient> bladeGrid = recipe(h, "item_chainsaw_blade").value().getIngredients();
        h.assertTrue(bladeGrid.size() == 6 && bladeGrid.get(0).isEmpty() && plates(bladeGrid, plate) == 5, "chainsaw blade is not \" II|III\"");
        NonNullList<Ingredient> saw = recipe(h, "shaped_152_chainsaw").value().getIngredients();
        h.assertTrue(saw.size() == 4 && saw.get(0).isEmpty() && takes(saw, 1, blade) && takes(saw, 2, unit) && plates(saw, plate) == 1,
            "chainsaw is not \" H|BI\"");

        // the Power Unit's charge still goes into the tool
        ItemStack charged = unit.copy();
        ((IBatteryItem) charged.getItem()).setEnergy(charged, 4000);
        CraftingInput input = CraftingInput.of(3, 2, List.of(ItemStack.EMPTY, bit, ItemStack.EMPTY, plate.copy(), charged, plate.copy()));
        CraftingRecipe craft = (CraftingRecipe) recipe(h, "shaped_153_mining_drill").value();
        h.assertTrue(craft.matches(input, h.getLevel()), "bit, plates and power unit do not craft the drill");
        ItemStack out = craft.assemble(input, h.getLevel().registryAccess());
        h.assertTrue(out.is(item("item_tool_iron_driller").getItem()) && ((IBatteryItem) out.getItem()).getEnergy(out) == 4000,
            "drill " + out + " charge " + ((IBatteryItem) out.getItem()).getEnergy(out));
        h.succeed();
    }

    /**
     * IC2 recipes with lathe-shaped iron blanks: Water Kinetic Generator "S S| A |SMS" (4-3-4-3-4),
     * Stirling Kinetic Generator "GPG|GMG|GAG" (3-2-3-2-3), Mining Laser "RRc|AAC| HA" (4-3-2-2-2).
     */
    @GameTest(batch = "tool_recipes", template = "empty", timeoutTicks = 20)
    public static void kineticGeneratorsAndLaserTakeTurnedBlanks(GameTestHelper h) {
        Object[][] cases = {
            {"shaped_323_water_kinetic_generator", 4, new int[]{4, 3, 4, 3, 4}},
            {"shaped_322_stirling_kinetic_generator", 7, new int[]{3, 2, 3, 2, 3}},
            {"shaped_350_laser_miner", 7, new int[]{4, 3, 2, 2, 2}}};
        for (Object[] c : cases) {
            NonNullList<Ingredient> g = recipe(h, (String) c[0]).value().getIngredients();
            h.assertTrue(g.size() == 9, c[0] + " grid " + g.size());
            ItemStack turned = item("normal/item_iron_turning_blanks");
            var lathe = (com.miophas.singularity_iteration.core.api.item.ILatheItem) turned.getItem();
            int[] state = (int[]) c[2];
            for (int j = 0; j < 5; j++) if (state[j] < 5) lathe.setState(turned, j, state[j]);
            int slot = (Integer) c[1];
            h.assertTrue(takes(g, slot, turned), c[0] + " does not take the " + java.util.Arrays.toString(state) + " blank");
            h.assertTrue(!takes(g, slot, item("normal/item_iron_turning_blanks")), c[0] + " takes an uncut blank");
        }
        h.succeed();
    }

    /** Every lathe_state ingredient of the recipes is a named shape of LatheShapes, made for that recipe's result. */
    @GameTest(batch = "tool_recipes", template = "empty", timeoutTicks = 20)
    public static void everyLatheShapeInRecipesIsNamed(GameTestHelper h) {
        java.lang.reflect.Method of;
        try {
            of = Class.forName("com.miophas.singularity_iteration.common.item.lathe.LatheShapes").getMethod("of", ItemStack.class);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
        int found = 0;
        List<String> problems = new java.util.ArrayList<>();
        for (RecipeHolder<?> holder : h.getLevel().getRecipeManager().getRecipes()) {
            for (Ingredient ingredient : holder.value().getIngredients()) {
                var custom = ingredient.getCustomIngredient();
                if (custom == null || !custom.getClass().getSimpleName().equals("LatheStateIngredient")) continue;
                found++;
                ItemStack shown = ingredient.getItems()[0];
                try {
                    var shape = (java.util.Optional<?>) of.invoke(null, shown);
                    String result = BuiltInRegistries.ITEM.getKey(holder.value().getResultItem(h.getLevel().registryAccess()).getItem()).toString();
                    if (shape.isEmpty()) problems.add(holder.id() + ": unnamed shape");
                    else if (!shape.get().toString().contains(result)) problems.add(holder.id() + ": shape is not for " + result);
                } catch (ReflectiveOperationException e) {
                    throw new AssertionError(e);
                }
            }
        }
        h.assertTrue(found == 7, "lathe-shaped ingredients: " + found);
        h.assertTrue(problems.isEmpty(), problems.toString());
        h.succeed();
    }

    @GameTest(batch = "tool_recipes", template = "empty", timeoutTicks = 20)
    public static void powerUnitMatchesIc2AndKeepsTheBatteryCharge(GameTestHelper h) {
        RecipeHolder<?> holder = recipe(h, "shaped_309_power_unit");
        NonNullList<Ingredient> g = holder.value().getIngredients();
        ItemStack bat = item("normal/item_bat_lev0"), cable = item("wiring/cable/block_cable_o"), motor = item("resource/item_motor");
        h.assertTrue(g.size() == 9, "power unit grid " + g.size());
        h.assertTrue(takes(g, 0, bat) && takes(g, 3, bat) && takes(g, 6, bat), "B column is not RE batteries");
        h.assertTrue(takes(g, 1, cable) && takes(g, 7, cable), "A is not copper cable");
        h.assertTrue(takes(g, 5, motor), "M is not the electric motor");
        h.assertTrue(!g.get(2).isEmpty() && !g.get(8).isEmpty() && !g.get(4).isEmpty(), "casings or circuit missing");

        // the batteries' charge goes into the Power Unit
        ItemStack casing = g.get(2).getItems()[0].copy(), circuit = g.get(4).getItems()[0].copy();
        ItemStack b1 = bat.copy(), b2 = bat.copy(), b3 = bat.copy();
        ((IBatteryItem) b1.getItem()).setEnergy(b1, 3000);
        ((IBatteryItem) b2.getItem()).setEnergy(b2, 2000);
        ((IBatteryItem) b3.getItem()).setEnergy(b3, 0);
        CraftingInput input = CraftingInput.of(3, 3, List.of(
            b1, cable.copy(), casing.copy(),
            b2, circuit, motor.copy(),
            b3, cable.copy(), casing.copy()));
        CraftingRecipe craft = (CraftingRecipe) holder.value();
        h.assertTrue(craft.matches(input, h.getLevel()), "IC2 layout does not craft the power unit");
        ItemStack out = craft.assemble(input, h.getLevel().registryAccess());
        h.assertTrue(out.is(item("item_tool_power_unit").getItem()), "result " + out);
        long energy = ((IBatteryItem) out.getItem()).getEnergy(out);
        h.assertTrue(energy == 5000, "power unit charge " + energy);
        h.succeed();
    }

    /** IC2: Small Power Unit " AC|BIM| AC" (copper cable, iron casing, RE battery, basic circuit, motor). */
    @GameTest(batch = "tool_recipes", template = "empty", timeoutTicks = 20)
    public static void smallPowerUnitMatchesIc2(GameTestHelper h) {
        NonNullList<Ingredient> g = recipe(h, "shaped_310_power_unit_small").value().getIngredients();
        ItemStack bat = item("normal/item_bat_lev0"), cable = item("wiring/cable/block_cable_o"), motor = item("resource/item_motor");
        h.assertTrue(g.size() == 9 && g.get(0).isEmpty() && g.get(6).isEmpty(), "small power unit grid");
        h.assertTrue(takes(g, 1, cable) && takes(g, 7, cable) && takes(g, 3, bat) && takes(g, 5, motor), "A, B or M wrong");
        h.assertTrue(!g.get(2).isEmpty() && !g.get(8).isEmpty() && !g.get(4).isEmpty() && !takes(g, 2, bat), "casings or circuit missing");
        h.succeed();
    }

    /**
     * IC2: Diamond Drill " D |DtD| d " with t = itemTurningBlanks@344865, an iron blank turned to
     * 5, 4, 3, 2, 1; the drill's charge carries over.
     */
    @GameTest(batch = "tool_recipes", template = "empty", timeoutTicks = 20)
    public static void diamondDrillTakesTheTurnedBlank(GameTestHelper h) {
        RecipeHolder<?> holder = recipe(h, "shaped_311_diamond_driller");
        NonNullList<Ingredient> g = holder.value().getIngredients();
        ItemStack drill = item("item_tool_iron_driller");
        ItemStack diamond = new ItemStack(net.minecraft.world.item.Items.DIAMOND);
        h.assertTrue(g.size() == 9 && g.get(0).isEmpty() && g.get(2).isEmpty() && g.get(6).isEmpty() && g.get(8).isEmpty()
            && takes(g, 1, diamond) && takes(g, 3, diamond) && takes(g, 5, diamond) && takes(g, 7, drill), "diamond drill is not \" D |DtD| d \"");

        // turned on the lathe: position 0 stays at the full width 5, so it has no stored thickness
        ItemStack turned = item("normal/item_iron_turning_blanks");
        var lathe = (com.miophas.singularity_iteration.core.api.item.ILatheItem) turned.getItem();
        for (int j = 1; j < 5; j++) lathe.setState(turned, j, 5 - j);
        ItemStack almost = turned.copy();
        lathe.setState(almost, 4, 2);
        h.assertTrue(takes(g, 4, turned), "the 5-4-3-2-1 blank is not accepted");
        h.assertTrue(!takes(g, 4, item("normal/item_iron_turning_blanks")) && !takes(g, 4, almost), "a blank of another shape is accepted");
        ItemStack shown = g.get(4).getItems()[0];
        h.assertTrue(java.util.Arrays.equals(new int[]{5, 4, 3, 2, 1}, lathe.getCurrentState(shown)), "recipe viewers show another shape");

        ItemStack charged = drill.copy();
        ((IBatteryItem) charged.getItem()).setEnergy(charged, 3000);
        CraftingInput input = CraftingInput.of(3, 3, List.of(
            ItemStack.EMPTY, diamond.copy(), ItemStack.EMPTY,
            diamond.copy(), turned, diamond.copy(),
            ItemStack.EMPTY, charged, ItemStack.EMPTY));
        CraftingRecipe craft = (CraftingRecipe) holder.value();
        h.assertTrue(craft.matches(input, h.getLevel()), "IC2 layout does not craft the diamond drill");
        ItemStack out = craft.assemble(input, h.getLevel().registryAccess());
        h.assertTrue(out.is(item("item_tool_diamond_driller").getItem()), "result " + out);
        long energy = ((IBatteryItem) out.getItem()).getEnergy(out);
        h.assertTrue(energy == 3000, "diamond drill charge " + energy);
        h.succeed();
    }
}
