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
 * 0.1.11.134 (fusion overhaul F2): the operating limits: the Martin L-H threshold, the Greenwald density and the Troyon beta
 * limit, the disruption hazard near them, H-mode with ELMs at the reference burn (no disruption at 16 steps, the average power
 * unchanged), one heater stuck in L-mode, 20 steps past the limits disrupting with its cause, the synced values and texts.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round111134GameTests {
    private Round111134GameTests() {}

    private static final String PLASMA = "fusion.FusionPlasma";

    private static Class<?> cls(String name) { return Round33GameTests.common(name); }
    private static Object call(Object target, String name, Object... args) { return Round33GameTests.call(target, name, args); }
    private static double dbl(Object o) { return ((Number) o).doubleValue(); }

    private static double stat(String name, double... args) {
        try {
            for (Method m : cls(PLASMA).getDeclaredMethods()) {
                if (!m.getName().equals(name) || m.getParameterCount() != args.length) continue;
                boolean ok = true;
                for (Class<?> p : m.getParameterTypes()) ok &= p == double.class;
                if (!ok) continue;
                Object[] a = new Object[args.length];
                for (int i = 0; i < args.length; i++) a[i] = args[i];
                return ((Number) m.invoke(null, a)).doubleValue();
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

    private static Object plasma(long seed) {
        Object p = make(PLASMA);
        call(p, "random", new java.util.SplittableRandom(seed));
        return p;
    }

    private static Object in(int feed, int heaters) {
        return make(PLASMA + "$Inputs", true, true, feed, heaters, 2, 1_000_000_000_000L, 100_000_000L, 100_000_000L, true, false);
    }

    private static String state(Object p) { return call(p, "state").toString(); }

    @GameTest(template = "empty", batch = "round111134_limits", timeoutTicks = 20)
    public static void limitFormulas(GameTestHelper h) {
        List<String> wrong = new ArrayList<>();
        double plh = stat("lhThreshold", 1.0e20) / 1.0e6, beta = stat("betaN", 1.0e20, 15.0), ng = field("GREENWALD");
        if (Math.abs(plh - 86.5) > 2) wrong.add("L-H threshold " + plh + " MW");
        if (Math.abs(beta - 3.04) > 0.05) wrong.add("beta_N " + beta);
        if (Math.abs(ng / 1.194e20 - 1) > 0.01) wrong.add("Greenwald " + ng);
        if (stat("hazard", 0.85) != 0) wrong.add("hazard below 0.9");
        if (Math.abs(stat("hazard", 1.0) - 0.02) > 1e-9) wrong.add("hazard at the limit " + stat("hazard", 1.0));
        if (stat("hazard", 1.05) < 0.1) wrong.add("hazard past the limit " + stat("hazard", 1.05));
        h.assertTrue(wrong.isEmpty(), "limits: " + wrong);
        h.succeed();
    }

    @GameTest(template = "empty", batch = "round111134_limits", timeoutTicks = 20)
    public static void referenceBurnHasElmsAndHolds(GameTestHelper h) {
        List<String> wrong = new ArrayList<>();
        Object p = plasma(11);
        for (int t = 0; t < 2_000; t++) call(p, "tick", in(16, 4));
        if (!state(p).equals("BURNING") || !(Boolean) call(p, "hMode")) wrong.add("no H-mode burn: " + state(p));
        double sum = 0;
        int start = ((Number) call(p, "elms")).intValue();
        for (int t = 0; t < 20_000 && state(p).equals("BURNING"); t++) sum += ((Number) call(call(p, "tick", in(16, 4)), "fusionPower")).longValue();
        if (!state(p).equals("BURNING")) wrong.add("16 steps disrupted: " + call(p, "cause"));
        double mean = sum / 20_000;
        if (Math.abs(mean / 9_800_000 - 1) > 0.01) wrong.add("mean fusion " + mean);
        int elms = ((Number) call(p, "elms")).intValue() - start;
        if (elms < 100 || elms > 1_000) wrong.add(elms + " ELMs in 1,000 s");
        if (dbl(call(p, "greenwaldFraction")) > 0.9 || dbl(call(p, "betaFraction")) > 0.9) wrong.add("the reference burn is near a limit");
        h.assertTrue(wrong.isEmpty(), "reference: " + wrong);
        h.succeed();
    }

    @GameTest(template = "empty", batch = "round111134_limits", timeoutTicks = 20)
    public static void heatersAndLimits(GameTestHelper h) {
        List<String> wrong = new ArrayList<>();
        // one heater (40 MW) cannot bring a dense plasma into H-mode: it never ignites
        Object p = plasma(3);
        for (int t = 0; t < 4_000; t++) call(p, "tick", in(12, 1));
        if (state(p).equals("BURNING") || (Boolean) call(p, "hMode")) wrong.add("one heater reached " + state(p) + " H " + call(p, "hMode"));
        // 20 steps run past the Greenwald density: a disruption within seconds, with its cause
        int disrupted = 0;
        for (long seed = 0; seed < 5; seed++) {
            p = plasma(seed);
            for (int t = 0; t < 3_000 && !state(p).equals("DISRUPTED"); t++) call(p, "tick", in(20, 4));
            String cause = call(p, "cause").toString();
            if (state(p).equals("DISRUPTED") && (cause.equals("DENSITY_LIMIT") || cause.equals("BETA_LIMIT"))) disrupted++;
        }
        if (disrupted < 5) wrong.add("20 steps disrupted " + disrupted + " of 5 times");
        h.assertTrue(wrong.isEmpty(), "heaters and limits: " + wrong);
        h.succeed();
    }

    @GameTest(template = "empty", batch = "round111134_limits", timeoutTicks = 20)
    public static void limitReadouts(GameTestHelper h) throws Exception {
        int values = cls("menu.fusion.FusionControllerMenu").getDeclaredField("VALUES").getInt(null);
        h.assertTrue(values == 45, "menu values " + values);
        h.assertTrue(field("MAX_FEED") == 20 && field("REFERENCE_FEED") == 16, "feed steps");
        for (String lang : new String[]{"en_us", "zh_cn", "ja_jp", "ru_ru"}) {
            String json = Round33GameTests.resource("/assets/mio_icif/lang/" + lang + ".json");
            for (String k : new String[]{"balance.h_mode", "balance.l_mode", "balance.limits", "cause.density_limit", "cause.beta_limit", "cause.ash"})
                h.assertTrue(json != null && json.contains("\"gui.mio_icif.fusion." + k + "\""), lang + " lacks " + k);
        }
        h.succeed();
    }
}
