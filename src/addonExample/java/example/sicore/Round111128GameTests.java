package example.sicore;

import java.util.List;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * 0.1.11.128 (armour and flight overhaul E5): the ionised sheath at high speed. It comes in from Mach 0.95 and is full from
 * Mach 1.3; a new set of 4 to 6 arcs every 2 and 3 ticks in turn; every point lies on the box round the body (jitter
 * aside); the colours switch every tick. (The mod's class by reflection: addon examples do not compile against it.)
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round111128GameTests {
    private Round111128GameTests() {}

    private static final Class<?> RULES = Round33GameTests.common("suit.ArcNetRules");

    private static Object call(String name, Object... args) { return Round33GameTests.call(RULES, name, args); }

    private static float c(String name) {
        try { return RULES.getField(name).getFloat(null); } catch (ReflectiveOperationException e) { throw new AssertionError(name, e); }
    }

    @GameTest(template = "empty", timeoutTicks = 5)
    @SuppressWarnings("unchecked")
    public static void arcNetWrapsTheBodyPastTheBarrier(GameTestHelper h) {
        h.assertTrue((float) call("level", 0.9) == 0F && (float) call("level", 1.3) == 1F && (float) call("level", 1.1) > 0F, "the net's ramp");
        long[] gens = new long[10];
        for (int t = 0; t < 10; t++) gens[t] = (long) call("generation", (long) t);
        h.assertTrue(java.util.Arrays.equals(gens, new long[]{0, 0, 0, 1, 1, 2, 2, 2, 3, 3}), "generations " + java.util.Arrays.toString(gens));
        float m = c("MARGIN"), j = c("JITTER") + 1.0E-4F;
        float[] lo = {-c("BOX_X") - m, c("BOX_TOP") - m, c("BOX_FRONT") - m}, hi = {c("BOX_X") + m, c("BOX_BOTTOM") + m, c("BOX_BACK") + m};
        for (long seed = 1; seed <= 40; seed++) {
            List<float[][]> arcs = (List<float[][]>) call("arcs", seed);
            h.assertTrue(arcs.size() >= 4 && arcs.size() <= 6, "arcs in a set: " + arcs.size());
            for (float[][] arc : arcs) for (float[] p : arc) {
                boolean onFace = false;
                for (int k = 0; k < 3; k++) {
                    h.assertTrue(p[k] >= lo[k] - j && p[k] <= hi[k] + j, "a point off the box: " + java.util.Arrays.toString(p));
                    if (Math.abs(p[k] - lo[k]) <= j || Math.abs(p[k] - hi[k]) <= j) onFace = true;
                }
                h.assertTrue(onFace, "a point inside the box: " + java.util.Arrays.toString(p));
            }
        }
        h.assertTrue(java.util.Arrays.deepEquals(((List<float[][]>) call("arcs", 7L)).toArray(), ((List<float[][]>) call("arcs", 7L)).toArray()), "a set changes");
        h.assertTrue(call("colour", 0, 5L) != call("colour", 0, 6L), "the colours do not switch");
        h.succeed();
    }
}
