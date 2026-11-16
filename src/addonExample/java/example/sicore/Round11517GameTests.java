package example.sicore;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * 0.1.11.17: the Stirling engines (StirlingPhysics: the Carnot factor against the design point, the heater warming with
 * the heat flow, water cooling) and the Stirling kinetic generator's flywheel. Common classes are reached by reflection.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round11517GameTests {
    private Round11517GameTests() {}

    static Class<?> common(String name) { return Round33GameTests.common(name); }
    static Object call(Object target, String name, Object... args) { return Round33GameTests.call(target, name, args); }

    private static final String SP = "energy.StirlingPhysics";

    private static double d(Object o) { return ((Number) o).doubleValue(); }

    private static BlockEntity place(GameTestHelper h, BlockPos at, String id) {
        h.setBlock(at, BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("mio_icif", id)));
        return h.getBlockEntity(at);
    }

    /** The design point is 100 %; a hotter cooler is worse, a colder one better; the heater reaches 650 °C at the rated flow. */
    @GameTest(template = "empty", timeoutTicks = 20)
    public static void stirlingCarnotFactor(GameTestHelper h) {
        Class<?> sp = common(SP);
        double design = d(call(sp, "relative", 650.0, 40.0)), desert = d(call(sp, "relative", 674.0, 64.0)), snow = d(call(sp, "relative", 634.0, 24.0));
        h.assertTrue(Math.abs(design - 1) < 1e-9, "design " + design);
        h.assertTrue(desert < 0.99 && snow > 1.0 && snow <= 1.1, "desert " + desert + ", snow " + snow);
        h.assertTrue(Math.abs(d(call(sp, "heaterTarget", 40.0, 1.0)) - 650) < 1e-9, "heater at the rated flow");
        double quarter = d(call(sp, "heaterTarget", 23.5, 0.25));
        h.assertTrue(quarter > 500 && quarter < 560, "heater at a quarter " + quarter);
        h.assertTrue(d(call(sp, "coolerTemperature", 30.0, true, 1.0)) <= 23 + 1e-9, "water cooling");
        h.assertTrue(Math.abs(d(call(sp, "relative", 20.0, 20.0)) - 0.4) < 1e-9, "cold engine floor");
        h.succeed();
    }

    /** A new Stirling generator is cold (0.4 of 0.5 EU/HU); at its rated flow the heater warms and the ratio comes up to 0.5. */
    @GameTest(template = "empty", timeoutTicks = 20)
    public static void stirlingWarmsUp(GameTestHelper h) {
        BlockEntity be = place(h, new BlockPos(1, 2, 1), "generator/block_stirling_generator");
        double cold = d(call(be, "ratio"));
        h.assertTrue(Math.abs(cold - 0.2) < 1e-9, "cold ratio " + cold);
        Object head = call(be, "head");
        var level = h.getLevel();
        var pos = h.absolutePos(new BlockPos(1, 2, 1));
        for (int t = 0; t < 1200; t++) call(head, "tick", level, pos, 256.0, false);
        double hot = d(call(head, "hot")), warm = d(call(be, "ratio"));
        h.assertTrue(hot > 600 && hot < 700, "heater after a minute " + hot);
        h.assertTrue(warm > 0.47 && warm <= 0.55, "warm ratio " + warm);
        call(head, "warm", 256.0);
        h.assertTrue(Math.abs(d(call(be, "ratio")) - 0.5) < 1e-9, "design ratio");
        h.succeed();
    }

    /** Fed 200 HU every tick, the generator's heater sees a flow of about 200 HU/t (the heat of every tick is counted). */
    @GameTest(template = "empty", timeoutTicks = 400)
    public static void stirlingCountsEveryTick(GameTestHelper h) {
        BlockPos at = new BlockPos(1, 2, 1);
        BlockEntity be = place(h, at, "generator/block_stirling_generator");
        Object heat = call(be, "getHeatStorageCapability", (net.minecraft.core.Direction) null);
        h.startSequence()
            .thenExecuteFor(200, () -> {
                call(heat, "receiveHeat", 200L, false);
                call(call(be, "getEnergyStorage"), "setEnergy", 0L);
            })
            .thenExecute(() -> {
                double flow = d(call(call(be, "head"), "flow"));
                h.assertTrue(flow > 180 && flow < 220, "heater flow " + flow + " HU/t");
            })
            .thenSucceed();
    }

    /** The flywheel gives KU in proportion to what it holds, once per tick, and its speed follows the square root. */
    @GameTest(template = "empty", timeoutTicks = 20)
    public static void stirlingKineticFlywheel(GameTestHelper h) {
        BlockEntity be = place(h, new BlockPos(1, 2, 1), "kugenerator/block_stirling_kinetic_generator");
        call(be, "setKinetic", 10_000L);
        long first = (long) call(be, "extractKinetic", 5_000L, false), second = (long) call(be, "extractKinetic", 5_000L, false);
        h.assertTrue(first == 1_000 && second == 0, "half full gives " + first + " then " + second);
        call(be, "setKinetic", 20_000L);
        h.assertTrue((boolean) call(be, "isOverspeed"), "a full flywheel is overspeed");
        int rpm = (int) call(be, "getRPM");
        h.assertTrue(rpm > 1600 && rpm < 1700, "rpm when full " + rpm);
        h.succeed();
    }
}
