package example.sicore;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * 0.1.11.74 (phase warp style phase W4): the bursts of light come up in two ticks, hold and ease out without a hard cut;
 * a fissure of light starts at the centre, reaches about its length, bends a little and is the same for the same seed.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round11574GameTests {
    private Round11574GameTests() {}

    private static Object call(String name, Object... args) { return Round33GameTests.call(Round33GameTests.common("suit.PhaseRules"), name, args); }

    private static float fade(float age, float life) { return (Float) call("burstFade", age, life); }

    @GameTest(template = "empty", batch = "round11574_fade", timeoutTicks = 20)
    public static void burstFade(GameTestHelper h) {
        h.assertTrue(fade(-1, 20) == 0 && fade(0, 20) == 0 && fade(1, 20) == 0.5F && fade(2, 20) == 1 && fade(8, 20) == 1, "the burst does not come up and hold");
        h.assertTrue(fade(14, 20) < 1 && fade(18, 20) < fade(14, 20) && fade(19.9F, 20) < 0.01F && fade(20, 20) == 0, "the burst does not ease out");
        for (float a = 2; a < 20; a += 0.5F) h.assertTrue(fade(a + 0.5F, 20) <= fade(a, 20) + 1e-6F, "the burst comes back at " + a);
        h.assertTrue(fade(Float.NaN, 20) == 0 && fade(5, 0) == 0, "odd input");
        h.succeed();
    }

    @GameTest(template = "empty", batch = "round11574_fissure", timeoutTicks = 20)
    public static void fissures(GameTestHelper h) {
        float[][] a = (float[][]) call("fissure", 9L, 2, 1.0F, 2.5F, 4), b = (float[][]) call("fissure", 9L, 2, 1.0F, 2.5F, 4);
        h.assertTrue(java.util.Arrays.deepEquals(a, b) && a.length == 5, "the fissure is not fixed by its seed");
        h.assertTrue(a[0][0] == 0 && a[0][1] == 0, "the fissure does not start at the centre");
        float reach = (float) Math.hypot(a[4][0], a[4][1]);
        h.assertTrue(reach > 1.8F && reach <= 2.5F + 1e-4F, "the fissure does not reach its length: " + reach);
        float end = (float) Math.atan2(a[4][1], a[4][0]);
        h.assertTrue(Math.abs(end - 1.0F) < 0.9F, "the fissure runs off its angle: " + end);
        float[][] c = (float[][]) call("fissure", 10L, 2, 1.0F, 2.5F, 4);
        h.assertTrue(!java.util.Arrays.deepEquals(a, c), "every fissure is the same");
        h.succeed();
    }
}
