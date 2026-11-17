package example.sicore;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * 0.1.11.12: the generator balance rulings: turbines above the basic kinetic generator for any KU, the diesel generator
 * at 160 EU/mB, the advanced semifluid generator on every fuel at 1.25 x, the quantum generator at 8192 EU/t, the quantum
 * solar panel at 1024 EU/t by night, one RTG table, superheated steam in the turbine, drop generators by burn time.
 * Common classes are reached by reflection (core must not depend on common).
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round11512GameTests {
    private Round11512GameTests() {}

    static Class<?> common(String name) { return Round33GameTests.common(name); }
    static Object call(Object target, String name, Object... args) { return Round33GameTests.call(target, name, args); }

    private static Object constant(String type, String name) {
        try {
            var f = common(type).getDeclaredField(name);
            f.setAccessible(true);
            return f.get(null);
        } catch (ReflectiveOperationException e) { throw new AssertionError(type + "." + name, e); }
    }

    private static double num(String type, String name) { return ((Number) constant(type, name)).doubleValue(); }

    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath("mio_icif", path); }

    private static BlockEntity place(GameTestHelper h, BlockPos at, String id) {
        h.setBlock(at, BuiltInRegistries.BLOCK.get(id(id)));
        return h.getBlockEntity(at);
    }

    /** Steam, Stirling or electric KU: the turbines beat the basic kinetic generator (they gave a tenth of their wind rate). */
    @GameTest(template = "empty", timeoutTicks = 20)
    public static void turbinesBeatTheBasicGeneratorOnAnyKu(GameTestHelper h) {
        double basic = 1.0 / num("blockentity.generator.mio_icif_kinetic_generator", "KU_TO_EU_RATIO");
        double turbo = num("blockentity.generator.mio_icif_turbo_kinetic_generator", "ARTIFICIAL_EU_PER_KU");
        double twin = num("blockentity.generator.mio_icif_twin_turbo_kinetic_generator", "ARTIFICIAL_EU_PER_KU");
        h.assertTrue(basic < turbo && turbo < twin && twin < 1, "basic " + basic + ", turbo " + turbo + ", twin " + twin);
        h.succeed();
    }

    /** The diesel generator's fuel value is 160 EU/mB at full load (it was 7.5); 0.1.11.15 tests the engine running on it. */
    @GameTest(template = "empty", timeoutTicks = 20)
    public static void dieselGeneratorGets160PerMb(GameTestHelper h) {
        h.assertTrue(num("blockentity.generator.mio_icif_diesel_generator", "EU_PER_MB") == 160, "160 EU/mB");
        h.succeed();
    }

    /** The advanced semifluid generator burns every semifluid fuel at 1.25 x the basic generator's value. */
    @GameTest(template = "empty", timeoutTicks = 20)
    public static void advancedSemifluidBurnsEveryFuel(GameTestHelper h) {
        Class<?> adv = common("blockentity.generator.mio_icif_advanced_semifluid_generator");
        Class<?> fuels = common("blockentity.generator.SemifluidFuels");
        for (String name : new String[]{"biogas", "biomass", "crudeoil", "dieseloil"}) {
            var fluid = BuiltInRegistries.FLUID.get(id(name));
            long basic = (long) call(call(fuels, "generatorValue", fluid), "energyPerMb");
            long advanced = (long) call(adv, "euPerMb", fluid);
            h.assertTrue(advanced * 4 == basic * 5, name + ": basic " + basic + ", advanced " + advanced);
        }
        BlockEntity be = place(h, new BlockPos(1, 2, 1), "generator/block_advanced_semifluid_generator");
        var tank = (IFluidHandler) call(be, "getFuelHandlerCapability", (Object) null);
        h.assertTrue(tank.fill(new FluidStack(BuiltInRegistries.FLUID.get(id("crudeoil")), 100), IFluidHandler.FluidAction.EXECUTE) == 100, "takes crude oil");
        h.succeed();
    }

    /** Quantum generator 8192 EU/t at IV; quantum solar panel 1024 EU/t by night; one RTG table in both energy modes. */
    @GameTest(template = "empty", timeoutTicks = 20)
    public static void quantumAndRtgValues(GameTestHelper h) {
        h.assertTrue(num("blockentity.generator.mio_icif_QuantumGenerator", "DEFAULT_PRODUCTION") == 8192, "quantum generator 8192 EU/t");
        h.assertTrue(num("blockentity.generator.mio_icif_QuantumSolarPanel", "NIGHT_POWER") == 1024, "quantum solar panel 1024 EU/t by night");
        long[] old = (long[]) constant("blockentity.generator.mio_icif_rt_generator", "GENERATION_RATES");
        long[] independent = (long[]) constant("blockentity.generator.mio_icif_rt_generator", "INDEPENDENT_GENERATION_RATES");
        h.assertTrue(java.util.Arrays.equals(old, independent), "RTG tables differ");
        h.succeed();
    }

    /** The steam turbine takes superheated steam at 1 mB per EU. */
    @GameTest(template = "empty", timeoutTicks = 20)
    public static void steamTurbineTakesSuperheatedSteam(GameTestHelper h) {
        BlockEntity be = place(h, new BlockPos(1, 2, 1), "generator/block_steam_turbine");
        var superheated = BuiltInRegistries.FLUID.get(id("superheatedsteam"));
        h.assertTrue((boolean) call(be, "acceptsFuel", superheated), "refused");
        h.assertTrue((long) call(call(be, "fuelValue", superheated), "energyPerMb") == 10, "10 EU per 10 mB (0.1.11.16: the enthalpy model, into the air)");
        h.succeed();
    }

    /** Drop generators: coal by its burn time; cobblestone (does not burn) no fuel at all since 0.1.11.19 (it cools the chamber). */
    @GameTest(template = "empty", timeoutTicks = 20)
    public static void dropGeneratorCountsBurnTime(GameTestHelper h) {
        BlockEntity be = place(h, new BlockPos(1, 2, 1), "generator/block_drop_generator");
        int coal = (int) call(be, "getItemValue", new ItemStack(Items.COAL)), stone = (int) call(be, "getItemValue", new ItemStack(Items.COBBLESTONE));
        h.assertTrue(coal == 200 && stone == 0, "coal " + coal + ", cobblestone " + stone);
        h.succeed();
    }
}
