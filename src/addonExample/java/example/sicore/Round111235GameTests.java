package example.sicore;

import com.google.gson.JsonObject;
import java.util.List;
import java.util.UUID;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * 0.1.11.235 (SlashBlade collab B3): the quantum blade's entanglement on {@code Entanglement} alone (SlashBlade is not in
 * the GameTests): a hit entangles for 200 ticks, at most 6 (the oldest goes), a new hit renews; the collapse cut takes the
 * suit's locks first, then the entangled targets, without repeats and at most its maximum. The blade names its own slash
 * art and special effect, named in every language.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round111235GameTests {
    private Round111235GameTests() {}

    private static Class<?> ent() { return Round33GameTests.common("armory.blade.Entanglement"); }

    @SuppressWarnings("unchecked")
    private static List<Integer> live(Object e, long now) { return (List<Integer>) Round33GameTests.call(e, "live", now); }

    @GameTest(template = "empty", batch = "round111235_entangle", timeoutTicks = 20)
    public static void entanglementLastsTenSecondsForSixTargets(GameTestHelper h) {
        UUID who = UUID.randomUUID();
        Object e = Round33GameTests.call(ent(), "of", who);
        for (int id = 1; id <= 7; id++) Round33GameTests.call(e, "add", id, 100L + id);
        h.assertTrue(live(e, 110).equals(List.of(2, 3, 4, 5, 6, 7)), "seven hits keep " + live(e, 110) + " instead of the last six");
        Round33GameTests.call(e, "add", 2, 150L);
        h.assertTrue(live(e, 150).equals(List.of(3, 4, 5, 6, 7, 2)), "a new hit does not renew and move the target: " + live(e, 150));
        h.assertTrue(live(e, 304).equals(List.of(5, 6, 7, 2)), "entanglement does not end after 200 ticks: " + live(e, 304));
        h.assertTrue(live(e, 350).isEmpty(), "entanglement outlasts 200 ticks: " + live(e, 350));
        Round33GameTests.call(ent(), "forget", who);
        @SuppressWarnings("unchecked")
        List<Integer> t = (List<Integer>) Round33GameTests.call(ent(), "targets", new int[]{9, 3, 9}, List.of(3, 4, 5, 6, 7), 4);
        h.assertTrue(t.equals(List.of(9, 3, 4, 5)), "the collapse targets are " + t + " instead of locks first, then entangled, four at most");
        h.succeed();
    }

    @GameTest(template = "empty", batch = "round111235_named", timeoutTicks = 20)
    public static void theQuantumBladeNamesItsArtAndEffect(GameTestHelper h) {
        JsonObject p = Round111231GameTests.json("data/mio_icif/slashblade/named_blades/quantum_blade.json").getAsJsonObject("properties");
        h.assertTrue("mio_icif:collapse_cut".equals(p.get("slash_art").getAsString()), "the quantum blade's slash art is " + p.get("slash_art"));
        h.assertTrue(p.getAsJsonArray("special_effects").toString().contains("mio_icif:entangle"), "the quantum blade has no entanglement");
        for (String lang : new String[]{"zh_cn", "en_us", "ja_jp", "ru_ru"}) {
            JsonObject l = Round111231GameTests.json("assets/mio_icif/lang/" + lang + ".json");
            for (String k : new String[]{"slash_art.mio_icif.collapse_cut", "slash_art.mio_icif.collapse_cut.desc", "se.mio_icif.entangle",
                "se.mio_icif.entangle.desc"})
                h.assertTrue(l.has(k), lang + " has no " + k);
        }
        h.succeed();
    }
}
