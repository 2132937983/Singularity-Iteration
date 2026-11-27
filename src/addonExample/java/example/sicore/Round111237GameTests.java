package example.sicore;

import java.io.InputStream;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * 0.1.11.237 (SlashBlade collab B5): the suit synergy and the Wingmen's slash arts on {@code BladeResources} alone: with
 * the nano suit the edge vents twice as fast; a Wingman fires an art only when the blade can pay it (EU, heat, mass; the
 * quantum collapse with EU); the manual page of the collab blades is shipped in three languages.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round111237GameTests {
    private Round111237GameTests() {}

    private static Class<?> res() { return Round33GameTests.common("armory.blade.BladeResources"); }

    private static Object kind(String name) { return Round1102GameTests.constant("armory.blade.CollabBlade", name); }

    private static Object fresh(String kind) { return Round33GameTests.call(Round33GameTests.common("armory.blade.BladeCore"), "fresh", kind(kind)); }

    private static boolean affordable(Object core, String kind) { return (Boolean) Round33GameTests.call(res(), "affordable", core, kind(kind)); }

    @GameTest(template = "empty", batch = "round111237_vent", timeoutTicks = 20)
    public static void theNanoSuitDoublesTheVenting(GameTestHelper h) {
        Object hot = Round33GameTests.call(Round33GameTests.call(fresh("NANO"), "withEu", 1000L), "withHeat", 50F, false);
        float alone = (Float) Round33GameTests.call(Round33GameTests.call(res(), "tick", hot, kind("NANO"), 1F), "heat");
        float suited = (Float) Round33GameTests.call(Round33GameTests.call(res(), "tick", hot, kind("NANO"), 2F), "heat");
        h.assertTrue(Math.abs((50 - suited) - 2 * (50 - alone)) < 1.0E-4, "with the nano suit the edge vents " + (50 - suited) + " instead of twice " + (50 - alone));
        h.succeed();
    }

    @GameTest(template = "empty", batch = "round111237_wingman", timeoutTicks = 20)
    public static void aWingmanFiresOnlyWhatTheBladeCanPay(GameTestHelper h) {
        h.assertTrue(!affordable(fresh("NANO"), "NANO"), "an empty nano blade can pay its art");
        h.assertTrue(affordable(Round33GameTests.call(fresh("NANO"), "withEu", 10_000L), "NANO"), "a charged nano blade cannot pay its art");
        Object over = Round33GameTests.call(Round33GameTests.call(fresh("NANO"), "withEu", 10_000L), "withHeat", 100F, true);
        h.assertTrue(!affordable(over, "NANO"), "an overheated nano blade can pay its art");
        h.assertTrue(affordable(Round33GameTests.call(fresh("QUANTUM"), "withEu", 20_000L), "QUANTUM"), "a Wingman cannot collapse with EU");
        Object noMass = Round33GameTests.call(fresh("SINGULARITY"), "withEu", 100_000L);
        h.assertTrue(!affordable(noMass, "SINGULARITY"), "a massless singularity can pay its art");
        h.assertTrue(affordable(Round33GameTests.call(noMass, "withMass", 12F, false), "SINGULARITY"), "a fed singularity cannot pay its art");
        h.succeed();
    }

    @GameTest(template = "empty", batch = "round111237_manual", timeoutTicks = 20)
    public static void theManualExplainsTheBlades(GameTestHelper h) {
        Class<?> anchor = Round33GameTests.common("armory.WingmanStrikes");
        for (String dir : new String[]{"", "_zh_cn/", "_ja_jp/"}) {
            String path = "/assets/mio_icif/guides/mio_icif/manual/" + dir + "suit/collab_blades.md";
            try (InputStream in = anchor.getResourceAsStream(path)) {
                h.assertTrue(in != null, "missing " + path);
                String text = new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                h.assertTrue(text.contains("parent: suit/index.md") && text.contains("mio_icif:item_tool_nanosaber"), path + " is not a suit page");
            } catch (java.io.IOException e) {
                throw new AssertionError(path, e);
            }
        }
        h.succeed();
    }
}
