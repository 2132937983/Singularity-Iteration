package example.sicore;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * 0.1.11.230 (key consolidation): the core burst is an action of the V/B keys (it had the R key): a key set to it holds no
 * unit on the server (the client sends the core burst), the other key keeps its own, and the same choice on both keys moves
 * over; every new text of the wheel, the panel and the migration notice exists in the four languages. The wheel, the key
 * defaults and the migration are client code, checked in the capture scene {@code wheel_gear}.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round111230GameTests {
    private Round111230GameTests() {}

    private static Class<?> nanoSuit() { return Round33GameTests.common("suit.nano.NanoSuit"); }

    @GameTest(template = "empty", batch = "round111230_core", timeoutTicks = 20)
    public static void theCoreBurstIsAnActionOfTheKeys(GameTestHelper h) {
        ServerPlayer p = Round33GameTests.survivalPlayer(h);
        try {
            String core = (String) nanoSuit().getField("CORE_BURST").get(null);
            Round33GameTests.call(nanoSuit(), "assign", p, 1, core);
            Object state = Round33GameTests.call(nanoSuit(), "state", p);
            String secondary = (String) state.getClass().getField("secondary").get(state);
            h.assertTrue(core.equals(secondary), "the core burst is not stored on B: " + secondary);
            Object unit = Round33GameTests.call(nanoSuit(), "effectiveBurst", p, "", core, 1);
            h.assertTrue(unit == null, "a key set to the core burst holds a unit on the server: " + unit);
            Round33GameTests.call(nanoSuit(), "assign", p, 0, core);
            String primary = (String) state.getClass().getField("primary").get(state);
            secondary = (String) state.getClass().getField("secondary").get(state);
            h.assertTrue(core.equals(primary) && secondary.isEmpty(), "the core burst on both keys (V " + primary + ", B " + secondary + ")");
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        } finally {
            Round33GameTests.remove(p);
        }
        h.succeed();
    }

    private static final String[] KEYS = {"gui.mio_icif.command.entry.gear", "gui.mio_icif.command.entry.favorites",
        "gui.mio_icif.command.sub.piece_call", "gui.mio_icif.command.sub.quantum_recall", "gui.mio_icif.command.sub.map",
        "gui.mio_icif.command.sub.holo", "gui.mio_icif.command.sub.jetpack", "gui.mio_icif.command.sub.hud", "gui.mio_icif.command.sub.hud_layout",
        "gui.mio_icif.command.sub.panel", "gui.mio_icif.command.fav_hint", "gui.mio_icif.command.fav_added", "gui.mio_icif.command.fav_removed",
        "gui.mio_icif.command.fav_full", "gui.mio_icif.equipment_console.burst_core", "message.mio_icif.keys_consolidated",
        "gui.mio_icif.command.sub.stance_auto", "gui.mio_icif.command.sub.stance_air", "gui.mio_icif.command.sub.stance_ground"};

    @GameTest(template = "empty", batch = "round111230_lang", timeoutTicks = 20)
    public static void theNewTextsAreTranslated(GameTestHelper h) {
        for (String lang : new String[]{"zh_cn", "en_us", "ja_jp", "ru_ru"}) {
            try (InputStream in = nanoSuit().getResourceAsStream("/assets/mio_icif/lang/" + lang + ".json")) {
                h.assertTrue(in != null, "missing " + lang);
                JsonObject o = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
                for (String k : KEYS) h.assertTrue(o.has(k) && !o.get(k).getAsString().isBlank(), lang + " lacks " + k);
            } catch (java.io.IOException e) {
                throw new AssertionError(lang, e);
            }
        }
        h.succeed();
    }
}
