package example.sicore;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * 0.1.11.124 (armour and flight overhaul E1): the reaction-mass flight effects are archived. Their switch is a constant
 * {@code false}, the two options that turned them on are gone from every language, and the old quantum core attachments no
 * longer ship (the finished suit carries its cores as mounts). (The mod's classes by reflection, without initialising them:
 * addon examples do not compile against them.)
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round111124GameTests {
    private Round111124GameTests() {}

    @GameTest(template = "empty", timeoutTicks = 5)
    public static void legacyFlightEffectsAreOff(GameTestHelper h) {
        try {
            Class<?> legacy = Class.forName("com.miophas.singularity_iteration" + ".common.client.legacy.flight.LegacyFlightFx", false,
                Round111124GameTests.class.getClassLoader());
            h.assertTrue(!legacy.getField("ENABLED").getBoolean(null), "the archived flight effects are on");
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
        for (String lang : new String[]{"en_us", "zh_cn", "ja_jp", "ru_ru"}) {
            String json = Round33GameTests.resource("/assets/mio_icif/lang/" + lang + ".json");
            h.assertTrue(json != null, lang + " missing");
            for (String key : new String[]{"configuration.classic_flight_fx", "configuration.thruster_fx"})
                h.assertTrue(!json.contains("\"mio_icif." + key + "\""), lang + " still names the removed option " + key);
        }
        for (String page : new String[]{"suit", "_zh_cn/suit", "_ja_jp/suit"}) {
            String md = Round33GameTests.resource("/assets/mio_icif/guides/mio_icif/manual/" + page + "/maneuver_mode.md");
            h.assertTrue(md != null && !md.contains("thruster_fx。") && !md.contains("Client option: thruster_fx") && md.contains("0.1.11.124"),
                page + " manual still describes the thruster flames");
        }
        h.succeed();
    }
}
