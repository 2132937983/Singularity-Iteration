package example.sicore;

import com.google.gson.JsonParser;
import com.miophas.singularity_iteration.core.api.MioIcifAPI;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * The fermenter runs on data-driven {@code mio_icif:fermenter} recipes: the shipped biomass recipe
 * keeps the IC2 values (20 mB + 4000 HU -> 400 mB biogas, 1 fertilizer per 500 mB), the JSON format
 * takes fluid tags and optional fields, and the machine follows the recipe.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class FermenterRecipeGameTests {
    private FermenterRecipeGameTests() {}

    private static ResourceLocation rl(String path) { return ResourceLocation.fromNamespaceAndPath("mio_icif", path); }
    private static Object call(Object target, String name, Object... args) { return Round33GameTests.call(target, name, args); }
    private static Fluid fluid(String path) { return BuiltInRegistries.FLUID.get(rl(path)); }

    @GameTest(batch = "fermenter", template = "empty", timeoutTicks = 20)
    public static void biomassRecipeIsDataDriven(GameTestHelper h) {
        var found = MioIcifAPI.instance().getRecipeAPI().findFermenterRecipe(new FluidStack(fluid("biomass"), 1), h.getLevel());
        h.assertTrue(found.isPresent(), "no fermenter recipe for biomass");
        h.assertTrue(found.get().id().equals(rl("fermenter/biomass_to_biogas")), "recipe " + found.get().id());
        Recipe<?> recipe = found.get().value();
        SizedFluidIngredient input = (SizedFluidIngredient) call(recipe, "getInput");
        FluidStack output = (FluidStack) call(recipe, "getOutput");
        ItemStack byproduct = (ItemStack) call(recipe, "getByproduct");
        h.assertTrue(input.amount() == 20 && (Integer) call(recipe, "getHeat") == 4000, "input " + input.amount() + " heat " + call(recipe, "getHeat"));
        h.assertTrue(output.getFluid() == fluid("biogas") && output.getAmount() == 400, "output " + output);
        h.assertTrue(BuiltInRegistries.ITEM.getKey(byproduct.getItem()).equals(rl("resource/item_fertilizer"))
            && (Integer) call(recipe, "getByproductInterval") == 500, "by-product " + byproduct);
        h.assertTrue(MioIcifAPI.instance().getRecipeAPI().findFermenterRecipe(new FluidStack(Fluids.WATER, 1), h.getLevel()).isEmpty(),
            "water ferments");
        h.succeed();
    }

    /** A datapack recipe: a fluid tag as input, no by-product (interval defaults to 500). */
    @GameTest(batch = "fermenter", template = "empty", timeoutTicks = 20)
    public static void jsonFormatTakesTagsAndDefaults(GameTestHelper h) {
        String json = """
            {"input": {"tag": "minecraft:water", "amount": 100}, "heat": 250, "output": {"id": "mio_icif:biogas", "amount": 50}}""";
        var serializer = BuiltInRegistries.RECIPE_SERIALIZER.get(rl("fermenter"));
        h.assertTrue(serializer != null, "no mio_icif:fermenter serializer");
        Recipe<?> recipe = serializer.codec().codec().parse(JsonOps.INSTANCE, JsonParser.parseString(json)).getOrThrow();
        h.assertTrue((Boolean) call(recipe, "accepts", Fluids.WATER) && !(Boolean) call(recipe, "accepts", Fluids.LAVA), "tag input");
        h.assertTrue(((ItemStack) call(recipe, "getByproduct")).isEmpty() && (Integer) call(recipe, "getByproductInterval") == 500, "defaults");
        h.assertTrue(serializer.codec().codec().parse(JsonOps.INSTANCE, JsonParser.parseString(
            "{\"input\": {\"fluid\": \"minecraft:water\"}, \"heat\": 0, \"output\": {\"id\": \"mio_icif:biogas\", \"amount\": 50}}")).isError(),
            "heat 0 accepted");
        h.succeed();
    }

    /** One run: 20 mB biomass + 4000 HU -> 400 mB biogas; the 500th mB gives the fertilizer. */
    @GameTest(batch = "fermenter", template = "empty", timeoutTicks = 20)
    public static void fermenterFollowsTheRecipe(GameTestHelper h) {
        BlockPos pos = new BlockPos(1, 1, 1);
        h.setBlock(pos, BuiltInRegistries.BLOCK.get(rl("producer/block_fermenter_elc")));
        BlockEntity be = h.getBlockEntity(pos);
        BlockPos abs = h.absolutePos(pos);
        IFluidHandler fluids = h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, abs, null);
        h.assertTrue(fluids != null, "no fluid capability");
        h.assertTrue(fluids.fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE) == 0, "takes water");
        h.assertTrue(fluids.fill(new FluidStack(fluid("biomass"), 1000), IFluidHandler.FluidAction.EXECUTE) == 1000, "refuses biomass");

        // 480 mB fermented before: this run reaches the 500 mB interval
        try {
            var processed = be.getClass().getDeclaredField("biomassProcessed");
            processed.setAccessible(true);
            processed.setLong(be, 480);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
        Class<?> type = be.getClass();
        // 0.1.10.24: the fermenter takes at most 100 HU per game tick (IC2) and this test runs inside one game tick, so the
        // heat of the run is put into the store directly; each tick() moves 100 HU of it into the run (Round1124GameTests
        // tests the intake over real ticks)
        ((com.miophas.singularity_iteration.core.api.capability.IMioIcifCapabilities.IHeatStorage) be).setHeat(4000);
        for (int i = 0; i < 41; i++) {
            call(type, "tick", h.getLevel(), abs, h.getLevel().getBlockState(abs), be);
        }
        IFluidHandler biogas = (IFluidHandler) call(be, "getBiogasTank");
        IFluidHandler biomass = (IFluidHandler) call(be, "getBiomassTank");
        h.assertTrue(biomass.getFluidInTank(0).getAmount() == 980, "biomass left " + biomass.getFluidInTank(0).getAmount());
        h.assertTrue(biogas.getFluidInTank(0).getFluid() == fluid("biogas") && biogas.getFluidInTank(0).getAmount() == 400,
            "biogas " + biogas.getFluidInTank(0).getAmount());
        IItemHandler items = (IItemHandler) call(be, "getItemHandler");
        ItemStack fertilizer = items.getStackInSlot(3);
        h.assertTrue(BuiltInRegistries.ITEM.getKey(fertilizer.getItem()).equals(rl("resource/item_fertilizer")) && fertilizer.getCount() == 1,
            "fertilizer " + fertilizer);
        h.succeed();
    }
}
