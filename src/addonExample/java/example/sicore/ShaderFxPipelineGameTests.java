package example.sicore;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Verifies SiFxRules shader-pack HDR alpha calibration and clamping behavior.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class ShaderFxPipelineGameTests {
    private ShaderFxPipelineGameTests() {}

    private static final Class<?> RULES = Round33GameTests.common("client.render.SiFxRules");

    private static Object call(String name, Object... args) { return Round33GameTests.call(RULES, name, args); }
    private static float f(Object o) { return ((Number) o).floatValue(); }

    @GameTest(template = "empty", timeoutTicks = 5)
    public static void shaderAlphaCalibrationAndClamping(GameTestHelper h) {
        // Additive HDR bloom alpha is gently tempered (< 1.0) and strictly clamped to [0, 255] / [0, 1]
        int fullAdd = (int) call("calibrateAlphaByte", 255, true);
        int fullTrans = (int) call("calibrateAlphaByte", 255, false);
        h.assertTrue(fullAdd > 180 && fullAdd < fullTrans && fullTrans < 255,
            "shader alpha calibration: add=" + fullAdd + " trans=" + fullTrans);
        h.assertTrue((int) call("calibrateAlphaByte", -20, true) == 0, "negative byte clamps to 0");
        h.assertTrue((int) call("calibrateAlphaByte", 500, false) <= 255, "overflow byte clamps <= 255");

        float af = f(call("calibrateAlpha", 1.0F, true));
        h.assertTrue(af > 0.75F && af < 0.90F, "float additive calibration: " + af);
        h.assertTrue(f(call("calibrateAlpha", 0.0F, true)) == 0F, "zero stays zero");
        h.succeed();
    }
}
