package example.sicore;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Every SI fluid can be made, and a recipe viewer can show how: either a recipe outputs it (as a fluid,
 * or inside a cell) or a machine publishes it as one of its own conversions
 * ({@code MachineConversions.all()}). Machines with hardcoded outputs (Steam Generator, Oil Rig) are
 * the second kind, and their conversions must follow the machine's code.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class MachineConversionGameTests {
    private MachineConversionGameTests() {}

    private static final String COMMON = "com.miophas.singularity_iteration" + ".common.";

    private static ResourceLocation rl(String path) { return ResourceLocation.fromNamespaceAndPath("mio_icif", path); }
    private static Object call(Object target, String name, Object... args) { return Round33GameTests.call(target, name, args); }
    private static Fluid fluid(String path) { return BuiltInRegistries.FLUID.get(rl(path)); }

    private static Class<?> type(String name) {
        try {
            return Class.forName(COMMON + name);
        } catch (ClassNotFoundException e) {
            throw new AssertionError(e);
        }
    }

    private static List<?> conversions() {
        return (List<?>) call(type("blockentity.MachineConversions"), "all");
    }

    /** The fluid an item holds: an SI cell's contents, else any fluid container's. */
    private static FluidStack contents(ItemStack stack) {
        if (stack.isEmpty()) return FluidStack.EMPTY;
        FluidStack inside = (FluidStack) call(type("item.cell.mio_icif_cells"), "getCellFluid", stack);
        return inside.isEmpty() ? FluidUtil.getFluidContained(stack).orElse(FluidStack.EMPTY) : inside;
    }

    /** What a recipe makes: its result item and every public {@code get...()} output getter (fluids, cells). */
    private static void outputs(Recipe<?> recipe, GameTestHelper h, Set<Fluid> made) {
        made.add(contents(recipe.getResultItem(h.getLevel().registryAccess())).getFluid());
        for (Method m : recipe.getClass().getMethods()) {
            if (m.getParameterCount() != 0 || Modifier.isStatic(m.getModifiers()) || !m.getName().startsWith("get")
                || m.getName().contains("Input") || m.getName().equals("getToastSymbol")) continue;
            try {
                Object value = m.invoke(recipe);
                if (value instanceof FluidStack fluid) made.add(fluid.getFluid());
                else if (value instanceof ItemStack item) made.add(contents(item).getFluid());
            } catch (ReflectiveOperationException e) {
                throw new AssertionError(m.toString(), e);
            }
        }
    }

    @GameTest(batch = "conversions", template = "empty", timeoutTicks = 20)
    public static void everyModFluidHasAProducer(GameTestHelper h) {
        Set<Fluid> made = new LinkedHashSet<>();
        for (RecipeHolder<?> holder : h.getLevel().getRecipeManager().getRecipes()) outputs(holder.value(), h, made);
        for (Object conversion : conversions()) {
            for (Object fluid : (List<?>) call(conversion, "fluidOutputs")) made.add(((FluidStack) fluid).getFluid());
            for (Object item : (List<?>) call(conversion, "itemOutputs")) made.add(contents((ItemStack) item).getFluid());
        }
        Set<String> missing = new TreeSet<>();
        for (Fluid fluid : BuiltInRegistries.FLUID) {
            ResourceLocation id = BuiltInRegistries.FLUID.getKey(fluid);
            if (!id.getNamespace().equals("mio_icif") || !fluid.isSource(fluid.defaultFluidState())) continue;
            if (!made.contains(fluid)) missing.add(id.getPath());
        }
        h.assertTrue(missing.isEmpty(), "no recipe or machine conversion makes " + missing);
        h.succeed();
    }

    /** Each conversion belongs to a registered machine item and makes something. */
    @GameTest(batch = "conversions", template = "empty", timeoutTicks = 20)
    public static void conversionsNameTheirMachines(GameTestHelper h) {
        List<?> all = conversions();
        h.assertTrue(!all.isEmpty(), "no conversions");
        for (Object conversion : all) {
            var machine = (net.minecraft.world.level.ItemLike) call(conversion, "machine");
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(machine.asItem());
            h.assertTrue(id.getNamespace().equals("mio_icif"), "machine " + id);
            int outputs = ((List<?>) call(conversion, "fluidOutputs")).size() + ((List<?>) call(conversion, "itemOutputs")).size();
            h.assertTrue(outputs > 0, id + " converts into nothing");
        }
        h.succeed();
    }

    /** IC2 boiler: 1 mB of water is 100 mB of steam; 100 HU/mB at 0 bar, 200 at 220 bar, superheated from 220 bar. */
    @GameTest(batch = "conversions", template = "empty", timeoutTicks = 20)
    public static void steamGeneratorConversionsFollowTheBoiler(GameTestHelper h) {
        Class<?> boiler = type("blockentity.producer.mio_icif_steam_generator");
        h.assertTrue((Float) call(boiler, "huPerMb", 0) == 100F && (Float) call(boiler, "huPerMb", 220) == 200F,
            "HU per mB " + call(boiler, "huPerMb", 0) + " / " + call(boiler, "huPerMb", 220));
        List<?> conversions = (List<?>) call(boiler, "conversions");
        Set<Fluid> outputs = new LinkedHashSet<>();
        for (Object conversion : conversions) {
            List<?> in = (List<?>) ((List<?>) call(conversion, "fluidInputs")).get(0);
            FluidStack water = (FluidStack) in.get(0);
            FluidStack out = (FluidStack) ((List<?>) call(conversion, "fluidOutputs")).get(0);
            h.assertTrue(water.getFluid() == Fluids.WATER && water.getAmount() == 1 && out.getAmount() == 100,
                "conversion " + water.getAmount() + " mB -> " + out.getAmount() + " mB");
            outputs.add(out.getFluid());
        }
        h.assertTrue(outputs.equals(Set.of(fluid("steam"), fluid("superheatedsteam"))), "outputs " + outputs);
        h.succeed();
    }

    /** IC2 compressor.ini: {@code itemCellEmpty = itemCellEmpty@5}, the Compressed Air Cell. */
    @GameTest(batch = "conversions", template = "empty", timeoutTicks = 20)
    public static void compressorMakesCompressedAirCells(GameTestHelper h) {
        ItemStack empty = new ItemStack(BuiltInRegistries.ITEM.get(rl("cell/item_cell_empty")));
        boolean found = false;
        for (RecipeHolder<?> holder : h.getLevel().getRecipeManager().getRecipes()) {
            Recipe<?> recipe = holder.value();
            if (!BuiltInRegistries.RECIPE_TYPE.getKey(recipe.getType()).equals(rl("compressor"))) continue;
            if (recipe.getIngredients().isEmpty() || !recipe.getIngredients().get(0).test(empty)) continue;
            ItemStack result = recipe.getResultItem(h.getLevel().registryAccess());
            h.assertTrue(BuiltInRegistries.ITEM.getKey(result.getItem()).equals(rl("cell/item_cell_air")), "empty cell compresses into " + result);
            h.assertTrue(contents(result).getFluid() == fluid("air"), "air cell holds " + contents(result).getFluid());
            found = true;
        }
        h.assertTrue(found, "no compressor recipe takes an empty cell");
        h.succeed();
    }
}
