package example.sicore;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Verifies WingmanDeployRules: V-wedge staggered aerial drop offsets, salvo timing/pitch ramp,
 * large quantum gate & Armory hatch curves, and tactical mode arrival palettes.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class WingmanDeployFxGameTests {
    private WingmanDeployFxGameTests() {}

    private static final Class<?> RULES = Round33GameTests.common("armory.WingmanDeployRules");
    private static final Class<?> MODE = Round33GameTests.common("armory.WingmanMode");

    private static Object call(String name, Object... args) { return Round33GameTests.call(RULES, name, args); }
    private static float f(Object o) { return ((Number) o).floatValue(); }

    @GameTest(template = "empty", timeoutTicks = 5)
    public static void vWedgeAndStaggerRhythm(GameTestHelper h) {
        double[] lead = (double[]) call("vFormationOffset", 0, 0.0F, false);
        double[] rightWing = (double[]) call("vFormationOffset", 1, 0.0F, false);
        double[] leftWing = (double[]) call("vFormationOffset", 2, 0.0F, false);
        double[] rear = (double[]) call("vFormationOffset", 3, 0.0F, false);

        // Symmetric wings (opposite X, same Y & Z), ascending altitude lead -> wings -> rear
        h.assertTrue(Math.abs(rightWing[0] + leftWing[0]) < 1.0E-6 && Math.abs(rightWing[0]) > 2.5, "wide V wings symmetric");
        h.assertTrue(rightWing[1] > lead[1] && rear[1] > rightWing[1], "ascending V altitude");
        double[] tightWing = (double[]) call("vFormationOffset", 1, 0.0F, true);
        h.assertTrue(Math.abs(tightWing[0]) < Math.abs(rightWing[0]), "indoor tight wedge pulls inward");

        // Stagger delay & ascending harmonic pitch
        int d0 = (int) call("staggerDelayTicks", 0), d2 = (int) call("staggerDelayTicks", 2);
        float p0 = f(call("staggerPitch", 0)), p3 = f(call("staggerPitch", 3));
        h.assertTrue(d0 == 0 && d2 == 6, "stagger delays: " + d0 + ", " + d2);
        h.assertTrue(p3 > p0 + 0.2F, "ascending harmonic pitch: " + p0 + " -> " + p3);
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 5)
    public static void gateHatchCurvesAndModePalettes(GameTestHelper h) {
        // Gate opens smoothly, holds at 1.0, shuts cleanly to 0.0
        h.assertTrue(f(call("gateOpen", 0F)) == 0F, "gate closed at t=0");
        h.assertTrue(f(call("gateOpen", 10F)) == 1F, "gate fully open during hold");
        h.assertTrue(f(call("gateOpen", 35F)) == 0F, "gate closed after life");
        h.assertTrue(f(call("gateFlash", 5F)) > 0.5F, "gate starburst flash near launch");

        // Hatch curve & body scan progress
        h.assertTrue(f(call("hatchOpen", 8F)) == 1F && f(call("hatchOpen", 40F)) == 0F, "hatch open/shut curve");
        h.assertTrue(f(call("scanProgress", 7F)) == 0.5F && f(call("scanProgress", 20F)) < 0F, "body scan progress");

        // Distinct mode palettes: OFFENSIVE crimson, DISPERSAL amber, ESCORT cyan
        Object[] modes = MODE.getEnumConstants();
        float[] off = (float[]) call("modeAccentColor", modes[0]);
        float[] disp = (float[]) call("modeAccentColor", modes[1]);
        float[] esc = (float[]) call("modeAccentColor", modes[2]);
        h.assertTrue(off[0] > 0.9F && off[2] < 0.3F, "OFFENSIVE crimson");
        h.assertTrue(disp[0] > 0.9F && disp[1] > 0.6F && disp[2] < 0.3F, "DISPERSAL amber");
        h.assertTrue(esc[2] > 0.9F && esc[1] > 0.8F && esc[0] < 0.3F, "ESCORT cyan");
        h.succeed();
    }
}
