package example.sicore;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * 0.1.11.70 (phase warp style phase W0): the shape of a phase cone (full at the nozzle, closed at the tip, a white core
 * fading out through cyan to violet, violet growing with the burn), its rings (spread along the cone, shrinking and
 * fading as they slide out), its size (longer with the output and the burn, wider with the nozzle), the blink of an
 * attitude jet and the day bed (none at night).
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round11570GameTests {
    private Round11570GameTests() {}

    private static Class<?> rules() { return Round33GameTests.common("suit.PhaseRules"); }

    private static Object call(String name, Object... args) { return Round33GameTests.call(rules(), name, args); }

    private static float f(String name, Object... args) { return (Float) call(name, args); }

    @GameTest(template = "empty", batch = "round11570_cone", timeoutTicks = 20)
    public static void coneShapeAndColour(GameTestHelper h) {
        h.assertTrue(f("radius", 0F) == 1F && f("radius", 1F) == 0F && f("radius", 0.9F) < f("radius", 0.5F), "the cone does not close");
        float[] nozzle = (float[]) call("colour", 0F, 0F), tip = (float[]) call("colour", 1F, 0F);
        h.assertTrue(nozzle[0] == 1F && nozzle[1] == 1F && nozzle[2] == 1F && nozzle[3] > 0.8F, "the core is not white");
        h.assertTrue(tip[3] == 0F, "the tip does not fade out");
        float[] cool = (float[]) call("colour", 0.5F, 0F), hot = (float[]) call("colour", 0.5F, 1F);
        h.assertTrue(hot[1] < cool[1] && hot[0] > cool[0], "the burn does not turn the middle to violet");
        float a = 1;
        for (int i = 0; i <= 10; i++) {
            float[] c = (float[]) call("colour", i / 10F, 0.5F);
            h.assertTrue(c[3] <= a + 1e-5F, "the alpha rises along the cone at " + i);
            a = c[3];
        }
        h.assertTrue(f("length", 1F, 0F, 1F) > f("length", 0.3F, 0F, 1F) && f("length", 1F, 1F, 1F) > f("length", 1F, 0F, 1F)
            && f("width", 1F, 1.3F) > f("width", 1F, 0.7F), "the cone does not grow");
        h.assertTrue(f("rate", 1F) > f("rate", 0F) && Float.isFinite(f("length", Float.NaN, Float.NaN, Float.NaN)), "rate or NaN");
        h.succeed();
    }

    @GameTest(template = "empty", batch = "round11570_rings", timeoutTicks = 20)
    public static void ringsSlideAndFade(GameTestHelper h) {
        java.util.Set<Float> places = new java.util.TreeSet<>();
        for (int k = 0; k < 4; k++) {
            float p = f("ring", 1.37F, 1.6F, k);
            h.assertTrue(p >= 0 && p < 1, "a ring off its cone: " + p);
            places.add(Math.round(p * 100) / 100F);
        }
        h.assertTrue(places.size() == 4, "the rings stack: " + places);
        h.assertTrue(f("ring", 0.5F, 1F, 0) != f("ring", 0.6F, 1F, 0), "the rings do not slide");
        float[] near = (float[]) call("ringShape", 0.1F), far = (float[]) call("ringShape", 0.9F);
        h.assertTrue(far[0] < near[0] && far[1] < near[1], "the rings do not shrink and fade outwards");
        float[] b0 = (float[]) call("blink", 0F), b1 = (float[]) call("blink", 1F);
        h.assertTrue(b1[0] > b0[0] && b0[1] == 1F && b1[1] == 0F, "the blink does not grow and fade");
        h.assertTrue(f("underlay", 0.2F) == 0F && f("underlay", 1F) > 0.25F && f("underlay", Float.NaN) == 0F, "the day bed");
        h.succeed();
    }
}
