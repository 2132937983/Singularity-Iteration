package example.sicore;

import com.google.gson.JsonObject;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * 0.1.11.234 (SlashBlade collab B2): the nano blade's piezo edge on {@code BladeResources} alone (SlashBlade is not in
 * the GameTests): the vibration damage (none without EU or overheated, 2 cold, up to 6 hot), the red-hot edge from heat 70,
 * the nanobots' mending (every 40 ticks, 400 EU, only when damaged and paid); and the blade names its own slash art and
 * special effect, named in every language.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round111234GameTests {
    private Round111234GameTests() {}

    private static Class<?> res() { return Round33GameTests.common("armory.blade.BladeResources"); }

    private static Object core(long eu, float heat, boolean over) {
        return Round33GameTests.call(Round33GameTests.call(Round33GameTests.call(Round33GameTests.common("armory.blade.BladeCore"), "fresh",
            Round1102GameTests.constant("armory.blade.CollabBlade", "NANO")), "withEu", eu), "withHeat", heat, over);
    }

    private static float piezo(Object c) { return (Float) Round33GameTests.call(res(), "piezoDamage", c); }

    @GameTest(template = "empty", batch = "round111234_piezo", timeoutTicks = 20)
    public static void thePiezoEdgeGrowsWithHeat(GameTestHelper h) {
        h.assertTrue(piezo(core(0, 50, false)) == 0, "an empty blade still vibrates");
        h.assertTrue(piezo(core(1000, 0, false)) == 2, "a cold edge adds " + piezo(core(1000, 0, false)) + " instead of 2");
        h.assertTrue(piezo(core(1000, 50, false)) == 4, "an edge at heat 50 adds " + piezo(core(1000, 50, false)) + " instead of 4");
        h.assertTrue(piezo(core(1000, 100, false)) == 6, "a hot edge adds " + piezo(core(1000, 100, false)) + " instead of 6");
        h.assertTrue(piezo(core(1000, 100, true)) == 0, "an overheated edge still vibrates");
        h.assertTrue(!(Boolean) Round33GameTests.call(res(), "redHot", core(1000, 69, false)), "the edge is red-hot at heat 69");
        h.assertTrue((Boolean) Round33GameTests.call(res(), "redHot", core(1000, 70, false)), "the edge is not red-hot at heat 70");
        h.succeed();
    }

    @GameTest(template = "empty", batch = "round111234_mend", timeoutTicks = 20)
    public static void theNanobotsMendOnTheirInterval(GameTestHelper h) {
        Object c = core(1000, 0, false);
        Object m = Round33GameTests.call(res(), "mend", c, 80L, 5);
        h.assertTrue(m != null && (Long) Round33GameTests.call(m, "eu") == 600, "a mend at tick 80 does not take 400 EU");
        h.assertTrue(Round33GameTests.call(res(), "mend", c, 81L, 5) == null, "the nanobots mend off their 40-tick interval");
        h.assertTrue(Round33GameTests.call(res(), "mend", c, 80L, 0) == null, "the nanobots mend an undamaged blade");
        h.assertTrue(Round33GameTests.call(res(), "mend", core(399, 0, false), 80L, 5) == null, "the nanobots mend without 400 EU");
        h.succeed();
    }

    @GameTest(template = "empty", batch = "round111234_named", timeoutTicks = 20)
    public static void theNanoBladeNamesItsArtAndEdge(GameTestHelper h) {
        JsonObject p = Round111231GameTests.json("data/mio_icif/slashblade/named_blades/nano_blade.json").getAsJsonObject("properties");
        h.assertTrue("mio_icif:piezo_cleave".equals(p.get("slash_art").getAsString()), "the nano blade's slash art is " + p.get("slash_art"));
        h.assertTrue(p.getAsJsonArray("special_effects").toString().contains("mio_icif:piezo_edge"), "the nano blade has no piezo edge");
        for (String lang : new String[]{"zh_cn", "en_us", "ja_jp", "ru_ru"}) {
            JsonObject l = Round111231GameTests.json("assets/mio_icif/lang/" + lang + ".json");
            for (String k : new String[]{"slash_art.mio_icif.piezo_cleave", "slash_art.mio_icif.piezo_cleave.desc", "se.mio_icif.piezo_edge",
                "se.mio_icif.piezo_edge.desc"})
                h.assertTrue(l.has(k), lang + " has no " + k);
        }
        h.succeed();
    }
}
