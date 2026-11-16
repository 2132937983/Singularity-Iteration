package example.sicore;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * 0.1.11.15: the engines (semifluid, advanced semifluid, diesel generators): the part-load and warm-up efficiency, the
 * load following the grid, stopping with a full storage, and a diesel generator running on diesel. Common classes are
 * reached by reflection (core must not depend on common).
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round11515GameTests {
    private Round11515GameTests() {}

    static Class<?> common(String name) { return Round33GameTests.common(name); }
    static Object call(Object target, String name, Object... args) { return Round33GameTests.call(target, name, args); }

    private static final String GOV = "energy.EngineGovernor";

    private static double d(Object o) { return ((Number) o).doubleValue(); }

    private static Object governor(int cylinders) {
        try {
            return common(GOV).getConstructor(int.class).newInstance(cylinders);
        } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
    }

    /** Full load 100 %, 80 % load 105 %, 20 % load clearly worse; cold 80 %, warm (80 °C) 100 %. */
    @GameTest(template = "empty", timeoutTicks = 20)
    public static void engineEfficiencyCurves(GameTestHelper h) {
        Class<?> g = common(GOV);
        double full = d(call(g, "loadEfficiency", 1.0)), best = d(call(g, "loadEfficiency", 0.8)), low = d(call(g, "loadEfficiency", 0.2));
        h.assertTrue(Math.abs(full - 1) < 1e-9 && Math.abs(best - 1.05) < 1e-9 && low < 0.8, "load: full " + full + ", 80 % " + best + ", 20 % " + low);
        h.assertTrue(Math.abs(d(call(g, "warmth", 20.0)) - 0.8) < 1e-9 && Math.abs(d(call(g, "warmth", 80.0)) - 1) < 1e-9, "warm-up");
        h.succeed();
    }

    /** With 30 EU/t taken from a nearly full storage, a 120 EU/t engine settles near 25 % load and keeps the storage level. */
    @GameTest(template = "empty", timeoutTicks = 20)
    public static void engineFollowsTheLoad(GameTestHelper h) {
        Object gov = governor(6);
        long cap = 1_000_000;
        long[] stored = {950_000};
        java.util.function.IntUnaryOperator drain = n -> n;
        for (int t = 0; t < 2000; t++) {
            stored[0] -= 30;
            final long now = stored[0];
            java.util.function.DoubleUnaryOperator produce = eu -> eu;
            double made = d(call(gov, "tick", now, cap, 120.0, 160.0, drain, produce));
            stored[0] += Math.round(made);
        }
        double load = d(call(gov, "load"));
        h.assertTrue(load > 0.2 && load < 0.32, "load " + load);
        h.assertTrue(Math.abs(stored[0] - 950_000) < 20_000, "the storage stays near its level: " + stored[0]);
        h.succeed();
    }

    /** A full storage stops the engine: no fuel is taken. */
    @GameTest(template = "empty", timeoutTicks = 20)
    public static void engineStopsWhenFull(GameTestHelper h) {
        Object gov = governor(4);
        int[] taken = {0};
        java.util.function.IntUnaryOperator drain = n -> { taken[0] += n; return n; };
        java.util.function.DoubleUnaryOperator produce = eu -> eu;
        for (int t = 0; t < 200; t++) call(gov, "tick", 990_000L, 1_000_000L, 120.0, 160.0, drain, produce);
        h.assertTrue(!(boolean) call(gov, "running") && taken[0] == 0, "running " + call(gov, "running") + ", fuel taken " + taken[0]);
        h.succeed();
    }

    /** A diesel generator with diesel and an empty storage runs, warms up and gets at most 105 % of 160 EU/mB. */
    @GameTest(template = "empty", timeoutTicks = 260)
    public static void dieselGeneratorRunsAsAnEngine(GameTestHelper h) {
        BlockPos at = new BlockPos(1, 2, 1);
        h.setBlock(at, BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("mio_icif", "generator/block_diesel_generator")));
        BlockEntity be = h.getBlockEntity(at);
        var tank = (IFluidHandler) call(be, "getFuelTank");
        tank.fill(new FluidStack(BuiltInRegistries.FLUID.get(ResourceLocation.fromNamespaceAndPath("mio_icif", "dieseloil")), 1000), IFluidHandler.FluidAction.EXECUTE);
        h.runAfterDelay(240, () -> {
            long stored = (long) call(call(be, "getEnergyStorageInternal"), "getAmount");
            int used = 1000 - tank.getFluidInTank(0).getAmount();
            Object gov = call(be, "getEngine");
            h.assertTrue(stored > 0 && used > 0, "made " + stored + " EU from " + used + " mB");
            h.assertTrue(stored <= used * 160 * 1.05 + 1, stored + " EU from " + used + " mB is above the fuel value");
            h.assertTrue(d(call(gov, "coolant")) > 20.5, "the coolant warmed up: " + call(gov, "coolant"));
            h.succeed();
        });
    }
}
