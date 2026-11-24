package example.sicore;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Verifies SuitUpFxRules: tech-tier palettes, electromagnetic vs classic reentry, contracting lock-in rings,
 * 3-beat Core Ignition curves (dual-helix scan, Quantum dorsal phase-wing flex, visor sweep), and drawn-blade spin-to-grip.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class SuitUpAndBladeFxGameTests {
    private SuitUpAndBladeFxGameTests() {}

    private static final Class<?> RULES = Round33GameTests.common("armory.SuitUpFxRules");

    private static Object call(String name, Object... args) { return Round33GameTests.call(RULES, name, args); }
    private static float f(Object o) { return ((Number) o).floatValue(); }

    @GameTest(template = "empty", timeoutTicks = 5)
    public static void suitTierReentryAndLockRings(GameTestHelper h) {
        Object std = call("tierOf", false, false, false);
        Object nano = call("tierOf", false, true, false);
        Object quant = call("tierOf", true, false, false);
        Object living = call("tierOf", true, false, true);

        h.assertTrue(!(boolean) call("isElectromagneticReentry", std), "standard metal keeps classic reentry");
        h.assertTrue((boolean) call("isElectromagneticReentry", nano) && (boolean) call("isElectromagneticReentry", quant),
            "nano/quantum use electromagnetic reentry");

        float[] qSec = (float[]) call("secondaryColor", quant);
        float[] lCore = (float[]) call("coreColor", living);
        h.assertTrue(qSec[0] > 0.6F && qSec[2] > 0.9F, "quantum secondary violet");
        h.assertTrue(lCore[1] > 0.95F && lCore[2] > 0.6F, "living lining emerald core");

        // Lock-in ring contracts inward; purge ring expands outward
        float r0 = f(call("lockRingRadius", 0F)), r1 = f(call("lockRingRadius", 1F));
        h.assertTrue(r0 > 0.6F && r1 < 0.25F, "lock ring contracts: " + r0 + " -> " + r1);
        h.assertTrue(f(call("purgeRingRadius", 1F)) > f(call("purgeRingRadius", 0F)) + 1.0F, "purge ring expands");
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 5)
    public static void coreIgnitionBootAndBladeSpinRules(GameTestHelper h) {
        // Dual-helix body scan & Quantum dorsal phase-wing self-test flex
        h.assertTrue(Math.abs(f(call("bootScanProgress", 0.35F)) - 0.5F) < 1.0E-4F, "mid body scan");
        h.assertTrue(f(call("bootWingFlex", 0.10F)) == 0F, "wing flex waits for beat 3");
        h.assertTrue(f(call("bootWingFlex", 0.38F)) > 0.50F, "wing flex peaks around 0.38s");
        h.assertTrue(f(call("bootWingFlex", 0.80F)) == 0F, "wing flex folds back");

        // First-person optical visor telemetry sweep
        h.assertTrue(Math.abs(f(call("bootVisorScan", 0.225F)) - 0.5F) < 1.0E-4F && f(call("bootVisorScan", 0.50F)) < 0F, "visor sweep timing");

        // Drawn-blade 360-degree axial spin-to-grip & color unpacking with dark-color floor
        h.assertTrue(f(call("bladeSpinDeg", 2.0F)) == 0F, "no spin during early hover");
        float midSpin = f(call("bladeSpinDeg", 10.5F));
        h.assertTrue(midSpin > 90F && midSpin < 270F, "smooth mid spin: " + midSpin);
        h.assertTrue(f(call("bladeSpinDeg", 18.0F)) == 360F, "full 360 finish at snap end");

        float[] crimson = (float[]) call("unpackRgb", 0xFF2233);
        float[] blackFallback = (float[]) call("unpackRgb", 0x050505);
        h.assertTrue(crimson[0] > 0.95F && crimson[1] < 0.2F, "crimson blade color preserved");
        h.assertTrue(blackFallback[2] > 0.9F, "very dark blade lifts to luminous cyan fallback");
        h.succeed();
    }
}
