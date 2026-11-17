package example.sicore;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * 0.1.11.73 (phase warp style phase W3): the strength of the warp screen effect: none before the crossing and after the
 * drop, full in the flash, easing down over the warp and out over the drop, never above 1.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round11573GameTests {
    private Round11573GameTests() {}

    private static float s(float since) {
        return (Float) Round33GameTests.call(Round33GameTests.common("suit.WarpRules"), "postStrength", since);
    }

    @GameTest(template = "empty", batch = "round11573_post", timeoutTicks = 20)
    public static void postStrength(GameTestHelper h) {
        h.assertTrue(s(-1F) == 0 && s(1E6F) == 0 && s(Float.NaN) == 0, "the screen effect outside the warp");
        h.assertTrue(s(0F) == 1F && s(1F) == 1F, "the flash is not full");
        h.assertTrue(s(10F) < 1F && s(40F) < s(10F) && s(40F) >= 0.6F, "the warp does not ease down");
        h.assertTrue(s(45F) < s(40F) && s(57F) < s(45F) && s(57F) > 0, "the drop does not ease out");
        for (int t = -2; t < 70; t++) h.assertTrue(s(t) >= 0 && s(t) <= 1, "out of 0..1 at " + t);
        h.succeed();
    }
}
