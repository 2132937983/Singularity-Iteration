package example.sicore;

import com.google.gson.JsonParser;
import com.miophas.singularity_iteration.core.api.MioIcifAPI;
import com.miophas.singularity_iteration.core.api.recipe.IRecipeRegistrationAPI;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.Optional;

/**
 * The SI machines read the recipe API: the Electric and Induction Furnaces smelt electric furnace
 * recipes, the Recycler gives a recycler recipe's output with its chance, the Electrolyzer takes
 * electrolyzer recipe inputs, the Mass Fabricator takes amplifiers, and the lathe names registered
 * profiles. The recipe methods that do not fit a machine (lathe, mass fabricator, scanner, replicator
 * recipes) refuse.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class MachineApiRecipesGameTests {
    private MachineApiRecipesGameTests() {}

    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath(CoreExampleMod.ID, path); }
    private static ResourceLocation si(String path) { return ResourceLocation.fromNamespaceAndPath("mio_icif", path); }
    private static Object call(Object target, String name, Object... args) { return Round33GameTests.call(target, name, args); }
    private static IRecipeRegistrationAPI registration() { return MioIcifAPI.instance().getRecipeRegistrationAPI(); }

    private static Class<?> type(String name) {
        try {
            return Class.forName("com.miophas.singularity_iteration.common." + name);
        } catch (ClassNotFoundException e) {
            throw new AssertionError(e);
        }
    }

    /** A powered machine at (1, 1, 1): its stored EU is topped up every tick. */
    private static BlockEntity machine(GameTestHelper h, String block, long energy) {
        BlockPos pos = new BlockPos(1, 1, 1);
        h.setBlock(pos, BuiltInRegistries.BLOCK.get(si(block)));
        BlockEntity be = h.getBlockEntity(pos);
        Object storage = call(be, "getEnergyStorageInternal");
        h.onEachTick(() -> call(storage, "setStored", energy));
        return be;
    }

    private static IItemHandler items(BlockEntity be) { return (IItemHandler) call(be, "getItemHandler"); }

    @GameTest(batch = "machine_api", template = "empty", timeoutTicks = 200)
    public static void electricFurnacesSmeltApiRecipes(GameTestHelper h) {
        h.assertTrue(registration().registerElectricFurnaceRecipe(id("flesh_to_leather"), Ingredient.of(Items.ROTTEN_FLESH),
            new ItemStack(Items.LEATHER), 100, 0.5F), "register");
        registration().syncToRecipeManager();
        BlockEntity furnace = machine(h, "producer/block_furnace_elc", 300L);
        h.assertTrue(items(furnace).insertItem(0, new ItemStack(Items.ROTTEN_FLESH, 2), false).isEmpty(), "rotten flesh refused");
        // the Induction Furnace finds the same recipe
        h.setBlock(new BlockPos(3, 1, 1), BuiltInRegistries.BLOCK.get(si("producer/block_induction_elc")));
        BlockEntity induction = h.getBlockEntity(new BlockPos(3, 1, 1));
        h.assertTrue(items(induction).insertItem(0, new ItemStack(Items.ROTTEN_FLESH), false).isEmpty(), "induction refuses rotten flesh");
        var lane = (Optional<?>) call(induction, "findRecipe", 0);
        h.assertTrue(lane.isPresent(), "induction finds no recipe");
        h.succeedWhen(() -> {
            ItemStack out = items(furnace).getStackInSlot(2);
            h.assertTrue(out.is(Items.LEATHER), "furnace output " + out);
            h.assertTrue((Float) call(furnace, "getStoredExperience") > 0F, "no XP banked");
        });
    }

    @GameTest(batch = "machine_api", template = "empty", timeoutTicks = 200)
    public static void recyclerGivesTheRecipeOutputWithItsChance(GameTestHelper h) {
        h.assertTrue(registration().registerRecyclerRecipe(id("recycle_poisonous_potato"), Ingredient.of(Items.POISONOUS_POTATO),
            new ItemStack[] {new ItemStack(Items.POTATO)}, new float[] {1.0F}, 45), "register");
        h.assertTrue(!registration().registerRecyclerRecipe(id("bad_chance"), Ingredient.of(Items.APPLE),
            new ItemStack[] {new ItemStack(Items.POTATO)}, new float[] {1.5F}, 45), "chance 1.5 accepted");
        registration().syncToRecipeManager();
        BlockEntity recycler = machine(h, "producer/block_recycler_elc", 45L);
        items(recycler).insertItem(0, new ItemStack(Items.POISONOUS_POTATO, 2), false);
        h.succeedWhen(() -> {
            ItemStack out = items(recycler).getStackInSlot(2);
            h.assertTrue(out.is(Items.POTATO) && out.getCount() == 2, "recycler output " + out + " (input left " + items(recycler).getStackInSlot(0) + ")");
        });
    }

    @GameTest(batch = "machine_api", template = "empty", timeoutTicks = 200)
    public static void electrolyzerTakesApiInputs(GameTestHelper h) {
        h.assertTrue(registration().registerElectrolyzerRecipe(id("kelp"), Ingredient.of(Items.KELP),
            new ItemStack[] {new ItemStack(Items.DRIED_KELP)}, 20), "register");
        registration().syncToRecipeManager();
        BlockEntity electrolyzer = machine(h, "producer/block_electrolyzer_elc", 400L);
        h.assertTrue(items(electrolyzer).insertItem(0, new ItemStack(Items.KELP), false).isEmpty(), "kelp refused");
        h.succeedWhen(() -> {
            h.assertTrue(items(electrolyzer).getStackInSlot(1).is(Items.DRIED_KELP), "output " + items(electrolyzer).getStackInSlot(1));
            h.assertTrue((Long) call(electrolyzer, "getChemicalEnergy") == 200L, "chemical energy " + call(electrolyzer, "getChemicalEnergy"));
        });
    }

    @GameTest(batch = "machine_api", template = "empty", timeoutTicks = 20)
    public static void massFabricatorTakesApiAmplifiers(GameTestHelper h) {
        h.assertTrue(registration().registerMassFabricatorAmplifier(id("nether_star"), Ingredient.of(Items.NETHER_STAR), 700_000), "register");
        h.assertTrue(!registration().registerMassFabricatorAmplifier(id("no_credit"), Ingredient.of(Items.APPLE), 0), "credit 0 accepted");
        registration().syncToRecipeManager();
        h.setBlock(new BlockPos(1, 1, 1), BuiltInRegistries.BLOCK.get(si("producer/block_matter_elc")));
        BlockEntity fabricator = h.getBlockEntity(new BlockPos(1, 1, 1));
        h.assertTrue((Integer) call(fabricator, "amplifierCredit", new ItemStack(Items.NETHER_STAR)) == 700_000, "nether star credit");
        h.assertTrue((Integer) call(fabricator, "amplifierCredit", new ItemStack(BuiltInRegistries.ITEM.get(si("normal/item_scrap")))) == 5_000,
            "built-in scrap credit");
        h.assertTrue(items(fabricator).insertItem(0, new ItemStack(Items.NETHER_STAR), true).isEmpty(), "amplifier slot refuses the nether star");
        // datapack form
        var serializer = BuiltInRegistries.RECIPE_SERIALIZER.get(si("mass_fabricator"));
        Recipe<?> recipe = serializer.codec().codec().parse(JsonOps.INSTANCE,
            JsonParser.parseString("{\"ingredient\": {\"item\": \"minecraft:diamond\"}, \"credit\": 9000}")).getOrThrow();
        h.assertTrue((Integer) call(recipe, "getCredit") == 9000, "JSON credit " + call(recipe, "getCredit"));
        h.succeed();
    }

    @GameTest(batch = "machine_api", template = "empty", timeoutTicks = 20)
    public static void latheShapesCanBeRegistered(GameTestHelper h) {
        Item iron = BuiltInRegistries.ITEM.get(si("normal/item_iron_turning_blanks"));
        Item wood = BuiltInRegistries.ITEM.get(si("normal/item_wooden_turning_blanks"));
        int[] profile = {2, 3, 2, 3, 2};
        h.assertTrue(registration().registerLatheShape(id("test_spindle"), iron, profile, new ItemStack(Items.IRON_SWORD)), "register");
        h.assertTrue(!registration().registerLatheShape(id("same_profile"), iron, profile.clone(), new ItemStack(Items.IRON_AXE)), "profile named twice");
        h.assertTrue(!registration().registerLatheShape(id("drill_tip"), iron, new int[] {5, 4, 3, 2, 1}, new ItemStack(Items.IRON_AXE)),
            "built-in profile taken");
        h.assertTrue(!registration().registerLatheShape(id("too_thick"), wood, new int[] {3, 3, 3, 3, 4}, new ItemStack(Items.STICK)), "wood thicker than 3");
        h.assertTrue(!registration().registerLatheShape(id("not_a_blank"), Items.STICK, new int[] {1, 1, 1, 1, 1}, new ItemStack(Items.STICK)), "stick as blank");

        Class<?> blanks = type("item.lathe.LatheTurningBlanksItem");
        ItemStack cut = (ItemStack) call(blanks, "getStackForState", new ItemStack(iron), profile);
        var shape = (Optional<?>) call(type("item.lathe.LatheShapes"), "of", cut);
        h.assertTrue(shape.isPresent() && id("test_spindle").equals(call(shape.get(), "id")), "blank at 2-3-2-3-2 is " + shape);
        h.assertTrue(call(shape.get(), "usedInItem") == Items.IRON_SWORD, "part " + call(shape.get(), "usedInItem"));
        h.succeed();
    }

    @GameTest(batch = "machine_api", template = "empty", timeoutTicks = 20)
    public static void recipesWithoutAMachineMeaningRefuse(GameTestHelper h) {
        var api = registration();
        Ingredient in = Ingredient.of(Items.APPLE);
        ItemStack out = new ItemStack(Items.GOLDEN_APPLE);
        h.assertTrue(!api.registerLatheRecipe(id("old_lathe"), in, out, 100), "lathe recipe accepted");
        h.assertTrue(!api.registerMassFabricatorRecipe(id("old_fabricator"), in, out, 100), "mass fabricator recipe accepted");
        h.assertTrue(!api.registerScannerRecipe(id("old_scanner"), in, out, 100), "scanner recipe accepted");
        h.assertTrue(!api.registerReplicatorRecipe(id("old_replicator"), in, out, 100), "replicator recipe accepted");
        h.succeed();
    }
}
