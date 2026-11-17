package example.sicore;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * 0.1.11.65 (hit feel and constructivist FX phase Q4): the Wingman engine loop is silent on the ground, a low drone in the
 * hover, louder and higher with speed, kicked up just after a boost, and never above full volume.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round11565GameTests {
    private Round11565GameTests() {}

    private static float[] mix(float speed, int sinceBoost, boolean landed) {
        return (float[]) Round33GameTests.call(Round33GameTests.common("armory.WingmanPose"), "engineMix", speed, sinceBoost, landed);
    }

    @GameTest(template = "empty", batch = "round11565_engine", timeoutTicks = 20)
    public static void engineFollowsTheFlight(GameTestHelper h) {
        h.assertTrue(mix(1F, 100, true)[0] == 0, "an engine on the ground");
        float[] hover = mix(0F, 100, false), cruise = mix(1F, 100, false), fast = mix(3F, 100, false), boost = mix(0F, 0, false);
        h.assertTrue(hover[0] > 0.1F && hover[0] < cruise[0] && cruise[0] < fast[0], "the volume does not rise with speed");
        h.assertTrue(hover[1] < cruise[1] && cruise[1] < fast[1], "the pitch does not rise with speed");
        h.assertTrue(boost[0] > hover[0] && boost[1] > hover[1] && mix(0F, 10, false)[0] == hover[0], "the boost kick");
        h.assertTrue(mix(99F, 0, false)[0] <= 1F && Float.isFinite(mix(Float.NaN, 0, false)[0]), "the volume is unbounded");
        h.succeed();
    }
}
