package example.sicore;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * 0.1.11.71 (phase warp style phase W1): the star streams. They come in between 0.9 and 1.5 blocks per tick, grow with
 * the speed (at most 6 blocks), run in a tube round the flight and never through the body, start spread along their run
 * and slide back as time goes, the same for the same flyer; their tails shift from cyan to magenta as the flight gets fast.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round11571GameTests {
    private Round11571GameTests() {}

    private static Object call(String name, Object... args) { return Round33GameTests.call(Round33GameTests.common("suit.PhaseRules"), name, args); }

    private static float f(String name, Object... args) { return (Float) call(name, args); }

    @GameTest(template = "empty", batch = "round11571_streams", timeoutTicks = 20)
    public static void starStreams(GameTestHelper h) {
        h.assertTrue(f("streamAlpha", 0.9F) == 0 && f("streamAlpha", 1.5F) == 1 && f("streamAlpha", 1.2F) > 0.4F
            && f("streamAlpha", Float.NaN) == 0, "the streams come in wrong");
        h.assertTrue(f("streamLength", 2F) > f("streamLength", 1F) && f("streamLength", 99F) == 6F, "the stream length");
        java.util.Set<Float> starts = new java.util.TreeSet<>();
        for (int i = 0; i < 22; i++) {
            float p = f("streamPlace", i, 7L, 0F, 1.5F);
            h.assertTrue(p >= 0 && p < 1, "a stream off its run: " + p);
            starts.add(Math.round(p * 20) / 20F);
            float[] slot = (float[]) call("streamSlot", i, 7L);
            h.assertTrue(slot[1] >= 1.1F && slot[1] <= 3.4F, "a stream through the body or too far: " + slot[1]);
        }
        h.assertTrue(starts.size() >= 10, "the streams start bunched: " + starts);
        h.assertTrue(f("streamPlace", 3, 7L, 0.2F, 1.5F) == f("streamPlace", 3, 7L, 0.2F, 1.5F)
            && f("streamPlace", 3, 7L, 0.2F, 1.5F) != f("streamPlace", 3, 7L, 0.25F, 1.5F), "the streams do not slide or are not fixed");
        float[][] slow = (float[][]) call("streamColours", 1.5F), fast = (float[][]) call("streamColours", 3.5F);
        h.assertTrue(f("streamNear", 1F) == 0 && f("streamNear", 4F) == 1 && f("streamNear", 2.5F) > 0, "the streams do not fade at the camera");
        h.assertTrue(fast[1][0] > slow[1][0] && fast[1][1] < slow[1][1], "the tail does not shift to magenta");
        h.succeed();
    }
}
