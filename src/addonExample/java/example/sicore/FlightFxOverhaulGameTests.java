package example.sicore;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Verifies the upgraded electromagnetic flight FX rules: LightTrails 2.0 speed/stance palettes & 7-tick ribbons,
 * and PhaseWingRules high-speed wing unfurl / sweep / Air-Brake palm rings.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class FlightFxOverhaulGameTests {
    private FlightFxOverhaulGameTests() {}

    private static final Class<?> TRAIL_RULES = Round33GameTests.common("suit.LightTrailRules");
    private static final Class<?> TRAIL = Round33GameTests.common("suit.LightTrail");
    private static final Class<?> WING_RULES = Round33GameTests.common("suit.PhaseWingRules");
    private static final Class<?> MODE = Round33GameTests.common("armory.WingmanMode");

    private static Object call(Object target, String name, Object... args) { return Round33GameTests.call(target, name, args); }
    private static float f(Object o) { return ((Number) o).floatValue(); }

    @GameTest(template = "empty", timeoutTicks = 5)
    public static void lightTrails2DynamicPalettesAndRibbon(GameTestHelper h) {
        // Player palette shifts from quantum cyan (low R, high G/B) to transonic electric violet (higher R, lower G)
        float[] sub = (float[]) call(TRAIL_RULES, "playerLineColor", 0.5);
        float[] sup = (float[]) call(TRAIL_RULES, "playerLineColor", 1.2);
        h.assertTrue(sub[2] > 0.9F && sub[1] > 0.8F && sub[0] < 0.4F, "subsonic should be cyan");
        h.assertTrue(sup[0] > 0.6F && sup[2] > 0.9F && sup[1] < 0.5F, "supersonic should shift to electric violet");

        // Wingman stance colours are distinct and Living Nano Lining gives emerald core
        Object[] modes = MODE.getEnumConstants();
        float[] off = (float[]) call(TRAIL_RULES, "wingmanLineColor", modes[0], false);
        float[] esc = (float[]) call(TRAIL_RULES, "wingmanLineColor", modes[2], false);
        float[] livingCore = (float[]) call(TRAIL_RULES, "wingmanCoreColor", modes[0], true);
        h.assertTrue(off[0] > 0.9F && esc[2] > 0.9F && esc[0] < 0.4F, "OFFENSIVE red vs ESCORT cyan");
        h.assertTrue(livingCore[1] > 0.95F && livingCore[2] > 0.6F, "Living Nano Lining emerald core");

        // Wingman ramp starts earlier than player ramp so cruising formations draw light trails
        h.assertTrue(f(call(TRAIL_RULES, "wingmanLevel", 0.3)) > 0.25F, "wingman trail visible at Mach 0.3");
        h.assertTrue(f(call(TRAIL_RULES, "wingmanLevel", 0.6)) >= 0.99F, "wingman trail full by Mach 0.6");

        // Custom 7-tick LightTrail keeps longer arc than default 3-tick trail
        Object ribbon;
        try { ribbon = TRAIL.getConstructor(float.class).newInstance(7.0F); }
        catch (ReflectiveOperationException e) { throw new AssertionError(e); }
        for (int t = 0; t <= 12; t++) call(ribbon, "sample", 100.0 + t, new Vec3(0, 64, -t * 2.0), 1.0F);
        int kept = (int) call(ribbon, "size");
        h.assertTrue(kept >= 7 && kept <= 9, "7-tick ribbon should retain 7..9 ticks, got " + kept);
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 5)
    public static void phaseEnergyWingsAndAirBrakeRules(GameTestHelper h) {
        // Wings stay hidden during hover and low-speed cruise
        h.assertTrue(f(call(WING_RULES, "unfurl", 0.0F)) == 0F, "hidden at hover");
        h.assertTrue(f(call(WING_RULES, "unfurl", 0.55F)) == 0F, "hidden at unfurl threshold");
        float mid = f(call(WING_RULES, "unfurl", 0.75F));
        h.assertTrue(mid > 0.35F && mid < 0.65F, "smooth mid unfurl: " + mid);
        h.assertTrue(f(call(WING_RULES, "unfurl", 0.95F)) == 1F, "full unfurl at 0.95");

        // Sweepback & span & brightness increase past Mach 1.0
        float sweepSub = f(call(WING_RULES, "sweepDeg", 0.6, 1.0F));
        float sweepSup = f(call(WING_RULES, "sweepDeg", 1.4, 1.0F));
        h.assertTrue(sweepSup > sweepSub + 15.0F, "wings sweep back with Mach: " + sweepSub + " -> " + sweepSup);
        float alphaSub = f(call(WING_RULES, "alpha", 1.0F, 0.7, 0.5F));
        float alphaSup = f(call(WING_RULES, "alpha", 1.0F, 1.3, 0.5F));
        h.assertTrue(alphaSup > alphaSub + 0.12F, "wings surge bright past Mach 1.0");

        // Air-Brake palm reverse-thrust rings ignite only above threshold
        h.assertTrue(f(call(WING_RULES, "brakeRingAlpha", 0.1F)) == 0F, "no brake ring at low decel");
        h.assertTrue(f(call(WING_RULES, "brakeRingAlpha", 1.0F)) > 0.8F, "bright brake ring at full brake");
        h.succeed();
    }
}
