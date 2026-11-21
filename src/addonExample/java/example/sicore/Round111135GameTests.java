package example.sicore;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * 0.1.11.135 (fusion overhaul F3): the hybrid blanket feeds a steam plant: heat from the alpha share, the neutrons, lithium-6
 * (4.8 / 14.1) and fission; 35% of it is the output; the demonstration blanket's numbers (118,400,000 EU/t of heat,
 * 41,440,000 EU/t out, M about 13.9, about one fission per neutron); the synced plant readouts and their texts.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round111135GameTests {
    private Round111135GameTests() {}

    private static final String BLANKET = "fusion.FusionBlanket";

    private static Class<?> cls(String name) { return Round33GameTests.common(name); }

    private static double stat(String name, Object... args) {
        try {
            for (Method m : cls(BLANKET).getDeclaredMethods()) {
                if (!m.getName().equals(name) || m.getParameterCount() != args.length || m.getParameterTypes()[0] != long.class) continue;
                m.setAccessible(true);
                return ((Number) m.invoke(null, args)).doubleValue();
            }
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(name, e);
        }
        throw new AssertionError("no static " + name);
    }

    private static long num(String field) {
        try {
            var f = cls(BLANKET).getDeclaredField(field);
            f.setAccessible(true);
            return ((Number) f.get(null)).longValue();
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(field, e);
        }
    }

    @GameTest(template = "empty", batch = "round111135_plant", timeoutTicks = 20)
    public static void hybridBlanketFeedsASteamPlant(GameTestHelper h) {
        List<String> wrong = new ArrayList<>();
        long full = num("FULL_NEUTRONS");
        if (full != 7_840_000L) wrong.add("full neutrons " + full);
        if (num("EFFICIENCY_PERCENT") != 35) wrong.add("efficiency " + num("EFFICIENCY_PERCENT"));
        // the demonstration blanket: its rods give 2,400 EU/t in the fission reactor's terms
        double heat = stat("thermal", full, 0.0, 2_400.0), out = stat("output", full, 0.0, 2_400.0);
        if (Math.round(heat) != 118_400_000L) wrong.add("demo heat " + heat);
        if (Math.round(out) != 41_440_000L) wrong.add("demo output " + out);
        double m = stat("multiplication", full, 0.0, 2_400.0), fissions = stat("fissionsPerNeutron", full, 2_400.0);
        if (Math.abs(m - 14.85) > 0.05) wrong.add("demo multiplication " + m);
        if (Math.abs(fissions - 0.976) > 0.01) wrong.add("fissions per neutron " + fissions);
        // lithium-6 adds 4.8 MeV per neutron it takes
        double plain = stat("thermal", full, 0.0, 0.0), lithium = stat("thermal", full, 1.0, 0.0);
        if (Math.abs((lithium - plain) / full - 4.8 / 14.1) > 1e-6) wrong.add("lithium gain " + (lithium - plain) / full);
        if (Math.abs(stat("multiplication", full, 1.0, 0.0) - (1 + 4.8 / 14.1)) > 1e-6) wrong.add("lithium-only multiplication");
        // no plasma, no heat
        if (stat("thermal", 0L, 1.0, 2_400.0) != 0 || stat("output", 0L, 1.0, 2_400.0) != 0) wrong.add("heat without neutrons");
        h.assertTrue(wrong.isEmpty(), "plant: " + wrong);
        h.succeed();
    }

    @GameTest(template = "empty", batch = "round111135_plant", timeoutTicks = 20)
    public static void plantReadouts(GameTestHelper h) throws Exception {
        int values = cls("menu.fusion.FusionControllerMenu").getDeclaredField("VALUES").getInt(null);
        h.assertTrue(values == 50, "menu values " + values);
        for (String lang : new String[]{"en_us", "zh_cn", "ja_jp", "ru_ru"}) {
            String json = Round33GameTests.resource("/assets/mio_icif/lang/" + lang + ".json");
            for (String k : new String[]{"balance.plant", "balance.thermal", "balance.multiplication", "balance.tbr", "balance.q_eng"})
                h.assertTrue(json != null && json.contains("\"gui.mio_icif.fusion." + k + "\""), lang + " lacks " + k);
        }
        String[][] pages = {{"", "fusion-fission hybrid"}, {"_zh_cn/", "聚变-裂变混合堆"}, {"_ja_jp/", "ハイブリッド炉"}};
        for (String[] p : pages) {
            String md = Round33GameTests.resource("/assets/mio_icif/guides/mio_icif/manual/" + p[0] + "machines/fusion_block_fusion_controller.md");
            h.assertTrue(md != null && md.contains("118,400,000") && md.contains("45,250") && md.contains(p[1]), p[0] + " manual lacks the plant");
        }
        h.succeed();
    }
}
