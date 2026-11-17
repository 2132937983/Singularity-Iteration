package example.sicore;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * 0.1.11.72 (phase warp style phase W2): the timeline of the phase warp. The gather starts only on the way up through
 * Mach 0.8, is called off when the flight falls back or takes too long, and eases in; after the crossing the flash, the
 * warp and the drop follow in order and end; the streams stretch in the warp and settle in the drop; the afterimages are
 * brightest nearest and fade in the drop; ripples come out one after another and live 20 ticks; the gate grows and fades.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round11572GameTests {
    private Round11572GameTests() {}

    private static Object call(String name, Object... args) { return Round33GameTests.call(Round33GameTests.common("suit.WarpRules"), name, args); }

    private static float f(String name, Object... args) { return (Float) call(name, args); }

    private static boolean b(String name, Object... args) { return (Boolean) call(name, args); }

    private static String phase(float since) { return String.valueOf(call("phase", since)); }

    @GameTest(template = "empty", batch = "round11572_gather", timeoutTicks = 20)
    public static void gather(GameTestHelper h) {
        h.assertTrue(b("gatherStarts", 0.79, 0.81) && !b("gatherStarts", 0.81, 0.79) && !b("gatherStarts", 0.85, 0.9)
            && !b("gatherStarts", Double.NaN, 0.9), "the gather starts wrong");
        h.assertTrue(b("gatherCancels", 0.7, 5) && b("gatherCancels", 0.9, 61) && !b("gatherCancels", 0.85, 20), "the gather is not called off");
        h.assertTrue(f("gather", 0F) == 0 && f("gather", 6F) > 0.5F && f("gather", 12F) == 1 && f("gather", 50F) == 1, "the gather does not ease in");
        h.succeed();
    }

    @GameTest(template = "empty", batch = "round11572_timeline", timeoutTicks = 20)
    public static void timeline(GameTestHelper h) {
        h.assertTrue(phase(-1F).equals("NONE") && phase(0F).equals("FLASH") && phase(1.9F).equals("FLASH") && phase(2F).equals("WARP")
            && phase(41.9F).equals("WARP") && phase(42F).equals("DROP") && phase(57.9F).equals("DROP") && phase(58F).equals("NONE")
            && phase(Float.NaN).equals("NONE"), "the phases are out of order");
        h.assertTrue(f("streamBoost", 10F) > 1.5F && f("streamBoost", 50F) < f("streamBoost", 10F) && f("streamBoost", 50F) > 1F
            && f("streamBoost", 100F) == 1F, "the streams do not stretch and settle");
        h.assertTrue(f("ghost", 1, 10F) > f("ghost", 3, 10F) && f("ghost", 3, 10F) > 0 && f("ghost", 1, 55F) < f("ghost", 1, 10F)
            && f("ghost", 1, 1F) == 0 && f("ghost", 1, 70F) == 0, "the afterimages");
        h.assertTrue(call("ripple", 0, 1F) == null && call("ripple", 0, 3F) != null && call("ripple", 1, 3F) == null
            && call("ripple", 1, 13F) != null && call("ripple", 0, 22F) == null, "the ripples are not thrown one after another");
        float[] r0 = (float[]) call("ripple", 0, 4F), r1 = (float[]) call("ripple", 0, 15F);
        h.assertTrue(r1[0] > r0[0] && r1[1] > r0[1] && r1[2] < r0[2], "a ripple does not spread, grow and fade");
        float[] g0 = (float[]) call("gate", 0F), g1 = (float[]) call("gate", 4F), g2 = (float[]) call("gate", 6F);
        h.assertTrue(g1[0] > g0[0] && g1[1] < g0[1] && g2[1] == 0, "the gate does not grow and fade");
        h.succeed();
    }
}
