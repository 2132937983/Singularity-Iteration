package example.sicore;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * 0.1.11.62 (hit feel and constructivist FX phase Q1): the timing of the constructivist flight effects. The thrust wedge
 * keeps a two-state beat (two lengths, no noise) and grows with speed and the high-speed pose within bounds; the speed bars
 * come in between 0.9 and 1.5 blocks per tick; a boost ring grows and is cut after six ticks.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round11562GameTests {
    private Round11562GameTests() {}

    private static float pose(String name, Object... args) {
        return (Float) Round33GameTests.call(Round33GameTests.common("armory.WingmanPose"), name, args);
    }

    @GameTest(template = "empty", batch = "round11562_timing", timeoutTicks = 20)
    public static void constructivistTiming(GameTestHelper h) {
        java.util.Set<Float> lengths = new java.util.TreeSet<>();
        for (int t = 0; t < 40; t++) lengths.add(pose("thrustLength", 1.0F, 0F, t));
        h.assertTrue(lengths.size() == 2, "the thrust beat is not two lengths: " + lengths);
        h.assertTrue(pose("thrustLength", 0F, 0F, 0) < pose("thrustLength", 2F, 0F, 0)
            && pose("thrustLength", 2F, 0F, 0) < pose("thrustLength", 2F, 1F, 0), "no growth with speed and the high-speed pose");
        h.assertTrue(pose("thrustLength", 99F, 5F, 0) <= 0.35F + 4 * 0.45F + 0.6F + 1e-4F
            && Float.isFinite(pose("thrustLength", Float.NaN, Float.NaN, 0)), "the thrust is unbounded or not finite");
        h.assertTrue(pose("speedBarAlpha", 0.9F) == 0 && pose("speedBarAlpha", 1.5F) == 1 && pose("speedBarAlpha", 1.2F) > 0.4F,
            "the speed bars come in wrong");
        h.assertTrue(pose("boostRing", 0F) > 0 && pose("boostRing", 5.9F) > pose("boostRing", 1F) && pose("boostRing", 6F) == 0
            && pose("boostRing", -1F) == 0, "the boost ring is not cut at six ticks");
        h.succeed();
    }
}
