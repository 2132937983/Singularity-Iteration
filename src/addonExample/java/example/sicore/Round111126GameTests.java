package example.sicore;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * 0.1.11.126 (armour and flight overhaul E3): the electromagnetic hover. One ripple per stroke of the 3.2 s float; the sole
 * ring breathes and its ripple fades out; the ground ripple only within 6 blocks and fainter with height; the glow of the
 * head ring and wing breathes only in the hover. (The mod's class by reflection: addon examples do not compile against it.)
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round111126GameTests {
    private Round111126GameTests() {}

    private static final Class<?> RULES = Round33GameTests.common("suit.HoverFieldRules");

    private static float f(String name, Object... args) { return ((Number) Round33GameTests.call(RULES, name, args)).floatValue(); }

    @GameTest(template = "empty", timeoutTicks = 5)
    public static void hoverFieldIsFaintAndBeats(GameTestHelper h) {
        h.assertTrue(f("phase", 0.0) == 0F && Math.abs(f("phase", 0.8) - 0.5F) < 1.0E-4F && Math.abs(f("phase", 1.6 + 0.4) - 0.25F) < 1.0E-4F,
            "two ripples per 3.2 s float");
        h.assertTrue(f("soleBreath", 0.0) == 1F && f("soleBreath", 1.59) < 0.71F, "the sole ring breathes");
        h.assertTrue(f("soleRippleAlpha", 1F) == 0F && f("soleRippleAlpha", 0F) <= 0.55F, "the sole ripple stays faint and fades");
        h.assertTrue(f("soleRippleRadius", 0F) < f("soleRippleRadius", 0.5F) && f("soleRippleRadius", 1F) <= 0.5F + 1.0E-4F, "the ripple runs out");
        h.assertTrue(f("groundAlpha", 0F, 7.0) == 0F && f("groundAlpha", 0F, 1.0) > f("groundAlpha", 0F, 4.0) && f("groundAlpha", 0F, 0.0) <= 0.5F,
            "the ground ripple: in reach only, fainter with height");
        h.assertTrue(f("glowBreath", 1.6, 0F) == 1F && Math.abs(f("glowBreath", 1.6, 1F) - 0.7F) < 1.0E-4F && Math.abs(f("glowBreath", 0.0, 1F) - 1F) < 1.0E-4F,
            "the glow breathes only in the hover");
        h.succeed();
    }
}
