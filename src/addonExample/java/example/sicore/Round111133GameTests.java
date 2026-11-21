package example.sicore;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * 0.1.11.133 (fusion overhaul F1): the plasma as a zero-dimensional power balance: the Bosch-Hale D-T reactivity, fusion
 * power with the square of the density, the reference burn (16 steps, 15 keV, 9,800,000 EU/t, the Lawson criterion met),
 * low densities that cannot ignite and burn driven with a finite Q, one heater that cannot ignite a full-density plasma but
 * can ignite a thinner one that then rises to full density, and the balance readouts sent to the screen. (Classes by name.)
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round111133GameTests {
    private Round111133GameTests() {}

    private static final String PLASMA = "fusion.FusionPlasma";

    private static Class<?> cls(String name) { return Round33GameTests.common(name); }
    private static Object call(Object target, String name, Object... args) { return Round33GameTests.call(target, name, args); }
    private static double dbl(Object o) { return ((Number) o).doubleValue(); }

    private static Object stat(String name, Object... args) {
        try {
            for (Method m : cls(PLASMA).getDeclaredMethods()) {
                if (!m.getName().equals(name) || m.getParameterCount() != args.length) continue;
                Class<?>[] p = m.getParameterTypes();
                boolean ok = true;
                for (int i = 0; i < p.length; i++)
                    ok &= (p[i] == int.class && args[i] instanceof Integer) || (p[i] == double.class && args[i] instanceof Double);
                if (!ok) continue;
                m.setAccessible(true);
                return m.invoke(null, args);
            }
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(name, e);
        }
        throw new AssertionError("no static " + name);
    }

    private static double field(String name) {
        try {
            var f = cls(PLASMA).getDeclaredField(name);
            f.setAccessible(true);
            return ((Number) f.get(null)).doubleValue();
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(name, e);
        }
    }

    private static Object make(String cls, Object... args) {
        try {
            for (Constructor<?> c : cls(cls).getDeclaredConstructors()) {
                if (c.getParameterCount() != args.length) continue;
                c.setAccessible(true);
                return c.newInstance(args);
            }
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(cls, e);
        }
        throw new AssertionError("no constructor of " + cls);
    }

    private static Object in(int feed, int heaters) {
        return make(PLASMA + "$Inputs", true, true, feed, heaters, 0, 1_000_000_000L, 10_000_000L, 10_000_000L, true, false);
    }

    private static String state(Object p) { return call(p, "state").toString(); }

    private static void runFor(Object p, int feed, int heaters, int ticks) {
        for (int t = 0; t < ticks; t++) call(p, "tick", in(feed, heaters));
    }

    /** Runs a plasma until it ignites or {@code ticks} pass; returns the tick of ignition or -1. */
    private static int runTo(Object p, int feed, int heaters, int ticks) {
        for (int t = 0; t < ticks; t++)
            if (call(call(p, "tick", in(feed, heaters)), "event").toString().equals("IGNITION")) return t;
        return -1;
    }

    @GameTest(template = "empty", batch = "round111133_plasma", timeoutTicks = 20)
    public static void reactivityAndScaling(GameTestHelper h) {
        List<String> wrong = new ArrayList<>();
        // Bosch-Hale: 1.136e-22 m³/s at 10 keV, rising to a peak near 64 keV
        double sv10 = dbl(stat("sigmaV", 10.0)), sv15 = dbl(stat("sigmaV", 15.0)), sv30 = dbl(stat("sigmaV", 30.0)), sv64 = dbl(stat("sigmaV", 64.0));
        if (Math.abs(sv10 / 1.136e-22 - 1) > 0.02) wrong.add("sigmaV(10) " + sv10);
        if (!(sv10 < sv15 && sv15 < sv30 && sv30 < sv64)) wrong.add("reactivity not rising to 64 keV");
        if (dbl(stat("sigmaV", 0.1)) != 0) wrong.add("reactivity below 0.2 keV");
        // power with the square of the density
        long full = ((Number) stat("fusionPower", 16, 15.0)).longValue(), half = ((Number) stat("fusionPower", 8, 15.0)).longValue();
        if (full != 9_800_000L) wrong.add("reference fusion power " + full);
        if (Math.abs(half - 2_450_000L) > 1) wrong.add("half density gives " + half + ", expected a quarter");
        double tau = dbl(stat("referenceTauE"));
        if (tau < 1.5 || tau > 2.5) wrong.add("reference confinement time " + tau + " s");
        h.assertTrue(wrong.isEmpty(), "scaling: " + wrong);
        h.succeed();
    }

    @GameTest(template = "empty", batch = "round111133_plasma", timeoutTicks = 20)
    public static void referenceBurnMeetsLawson(GameTestHelper h) {
        List<String> wrong = new ArrayList<>();
        Object p = make(PLASMA);
        int ign = runTo(p, 16, 4, 4_000);
        if (ign < 0) wrong.add("no ignition at 16 steps with 4 heaters: " + state(p) + " " + call(p, "kev"));
        runFor(p, 16, 4, 3_000);
        if (!state(p).equals("BURNING")) wrong.add("not burning: " + state(p));
        double kev = dbl(call(p, "kev")), triple = dbl(call(p, "tripleProduct"));
        if (Math.abs(kev - 15.0) > 0.05) wrong.add("burns at " + kev + " keV");
        if (triple < 0.95 * field("LAWSON")) wrong.add("triple product " + triple);
        Object r = call(p, "tick", in(16, 4));
        if (((Number) call(r, "fusionPower")).longValue() != 9_800_000L) wrong.add("fusion " + call(r, "fusionPower"));
        if (((Number) call(r, "heating")).longValue() != 0) wrong.add("an ignited plasma is still heated");
        if (Math.abs(dbl(call(p, "density")) - 1.0e20) > 1.0e15) wrong.add("density " + call(p, "density"));
        h.assertTrue(wrong.isEmpty(), "reference burn: " + wrong);
        h.succeed();
    }

    @GameTest(template = "empty", batch = "round111133_plasma", timeoutTicks = 20)
    public static void densityDecidesIgnition(GameTestHelper h) {
        List<String> wrong = new ArrayList<>();
        // a thin plasma cannot ignite: it burns driven, with the heating on and a finite Q above 1
        Object p = make(PLASMA);
        if (runTo(p, 8, 4, 4_000) >= 0) wrong.add("8 steps ignited");
        Object r = call(p, "tick", in(8, 4));
        double q = dbl(call(p, "q"));
        if (!state(p).equals("HEATING") || ((Number) call(r, "fusionPower")).longValue() <= 0) wrong.add("no driven burn: " + state(p));
        if (Double.isInfinite(q) || q < 1) wrong.add("driven Q " + q);
        // one heater cannot ignite a full-density plasma
        p = make(PLASMA);
        if (runTo(p, 16, 1, 4_000) >= 0) wrong.add("one heater ignited 16 steps");
        // but it ignites 12 steps, which then rise to 16 and settle at the reference burn
        p = make(PLASMA);
        if (runTo(p, 12, 1, 4_000) < 0) wrong.add("one heater did not ignite 12 steps: " + call(p, "kev"));
        runFor(p, 16, 1, 3_000);   // it cools below 8 keV as the density rises, is heated again and re-ignites
        if (!state(p).equals("BURNING") || Math.abs(dbl(call(p, "kev")) - 15.0) > 0.1) wrong.add("no burn after the density ramp: " + state(p) + " " + call(p, "kev"));
        h.assertTrue(wrong.isEmpty(), "ignition: " + wrong);
        h.succeed();
    }

    @GameTest(template = "empty", batch = "round111133_plasma", timeoutTicks = 20)
    public static void balanceReadoutsSynced(GameTestHelper h) throws Exception {
        Class<?> menu = cls("menu.fusion.FusionControllerMenu");
        int values = menu.getDeclaredField("VALUES").getInt(null);
        h.assertTrue(values == 40, "menu values " + values);
        for (String lang : new String[]{"en_us", "zh_cn", "ja_jp", "ru_ru"}) {
            String json = Round33GameTests.resource("/assets/mio_icif/lang/" + lang + ".json");
            for (String k : new String[]{"state.driven", "balance.title", "balance.density", "balance.tau", "balance.triple", "balance.alpha", "balance.heating"})
                h.assertTrue(json != null && json.contains("\"gui.mio_icif.fusion." + k + "\""), lang + " lacks " + k);
        }
        h.succeed();
    }
}
