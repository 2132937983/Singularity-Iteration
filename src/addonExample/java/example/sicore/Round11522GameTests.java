package example.sicore;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * 0.1.11.22 (performance, P1): the caches give what the work they replace gave. Common classes are reached by
 * reflection.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round11522GameTests {
    private Round11522GameTests() {}

    static Class<?> common(String name) { return Round33GameTests.common(name); }
    static Object call(Object target, String name, Object... args) { return Round33GameTests.call(target, name, args); }

    private static double d(Object o) { return ((Number) o).doubleValue(); }

    private static Object field(Object target, String name) {
        try {
            for (Class<?> k = target.getClass(); k != null; k = k.getSuperclass()) {
                try { var f = k.getDeclaredField(name); f.setAccessible(true); return f.get(target); }
                catch (NoSuchFieldException next) { /* superclass */ }
            }
        } catch (IllegalAccessException e) { throw new AssertionError(e); }
        throw new AssertionError("no field " + name);
    }

    /** The kept expansions: the same work whatever was asked in between, and steam to the air still 0.5 EU/mB. */
    @GameTest(template = "empty", timeoutTicks = 20)
    public static void steamMemoIsExact(GameTestHelper h) {
        Class<?> sp = common("energy.SteamPhysics");
        try {
            Object steam = sp.getField("STEAM").get(null), hot = sp.getField("SUPERHEATED").get(null);
            double eta = 0.75;
            double a = d(call(sp, "work", steam, 0.1, eta));
            for (double bar = 0.1; bar < 2; bar += 0.05) { call(sp, "work", steam, bar, eta); call(sp, "work", hot, bar, 0.7); }
            double b = d(call(sp, "work", steam, 0.1, eta));
            h.assertTrue(a == b && a > 0, "vacuum work " + a + " then " + b);
            h.assertTrue(Math.abs(d(call(sp, "euPerMb", steam, 1.0, eta)) - 0.5) < 1e-12, "steam to the air");
            h.assertTrue(d(call(sp, "work", hot, 1.0, eta)) > d(call(sp, "work", steam, 1.0, eta)), "superheated does more work");
        } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
        h.succeed();
    }

    /** A kept storm is the storm of its own seed and day, also after other seeds and days used its slot. */
    @GameTest(template = "empty", timeoutTicks = 20)
    public static void stormMemoKeepsSeeds(GameTestHelper h) {
        Class<?> gp = common("energy.GeomagPhysics");
        int storms = 0;
        for (long day = 0; day < 200; day++) {
            Object a = call(gp, "storm", 12345L, day);
            for (long other = 1; other < 9; other++) call(gp, "storm", 12345L + other, day + other);
            Object b = call(gp, "storm", 12345L, day);
            h.assertTrue(java.util.Objects.equals(a, b), "day " + day + ": " + a + " then " + b);
            if (a != null) storms++;
        }
        h.assertTrue(storms > 10 && storms < 45, storms + " storms in 200 days (12 % chance)");
        h.succeed();
    }

    /** The wind rose is packed again after each sample. */
    @GameTest(template = "empty", timeoutTicks = 20)
    public static void windRoseRepacksOnSample(GameTestHelper h) {
        try {
            Object rose = common("blockentity.kuentity.kugenerator.WindRoseBuffer").getConstructor().newInstance();
            call(rose, "add", 0, 100);
            int north = (int) call(rose, "data", 0);
            call(rose, "add", 90, 100);
            int after = (int) call(rose, "data", 0);
            h.assertTrue(north == 255 && after != north, "north share " + north + " then " + after);
        } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
        h.succeed();
    }

    /** Wind turbines spread their updates by position, each within the 32-tick interval. */
    @GameTest(template = "empty", timeoutTicks = 40)
    public static void windUpdatesArePhased(GameTestHelper h) {
        java.util.Set<Integer> phases = new java.util.HashSet<>();
        Class<?> tp = common("energy.TickPhase");
        for (int x = 0; x < 64; x++) {
            int p = (int) call(tp, "of", new BlockPos(x * 3, 70, x * 7), 32);
            h.assertTrue(p >= 0 && p < 32, "phase " + p);
            phases.add(p);
        }
        h.assertTrue(phases.size() > 16, phases.size() + " phases for 64 positions");
        BlockPos at = new BlockPos(2, 2, 2);
        h.setBlock(at, BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("mio_icif", "kugenerator/block_wind_kinetic_generator")));
        BlockEntity be = h.getBlockEntity(at);
        call(be, "setRotorStack", new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("mio_icif", "rotors/item_wood_rotor"))));
        h.runAfterDelay(2, () -> {
            h.assertTrue((boolean) field(be, "phased"), "phased on its first tick");
            int left = (int) field(be, "ticksUntilUpdate");
            h.assertTrue(left >= 0 && left <= 32, "next update in " + left);
            h.succeed();
        });
    }
}
