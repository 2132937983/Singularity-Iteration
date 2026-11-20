package example.sicore;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * 0.1.11.105 (solar overhaul S5): the balance of the two routes, measured through the panels' own reading of the sky
 * ({@code mio_icif_solar_generator.evaluate}, the code their tick runs) over a whole day, one reading every 10 ticks, for
 * the plains (0.8) clear, in rain and in a thunderstorm, the desert (2.0, no rain) and the snowy plains (0.0). The daily
 * averages go to {@code solar_balance.json} in the game directory (build/addon-gametest). The test checks them against the
 * overhaul's draft (within 10 %) and checks what the routes are for: route A wins clear days and the desert, route B wins
 * rain, thunderstorms and the night. (The mod's common classes by reflection: addon examples do not compile against them.)
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round111105GameTests {
    private Round111105GameTests() {}

    private static Object call(Object target, String name, Object... args) { return Round33GameTests.call(target, name, args); }

    /** name, biome temperature, has rain, raining, rain level, thunder level */
    private static final Object[][] SKIES = {
        {"plains_clear", 0.8F, true, false, 0F, 0F}, {"plains_rain", 0.8F, true, true, 1F, 0F}, {"plains_storm", 0.8F, true, true, 1F, 1F},
        {"desert", 2.0F, false, false, 0F, 0F}, {"snowy_clear", 0.0F, true, false, 0F, 0F}};

    /** id, route, rank (1 to 3 pairs the routes), then the draft daily averages (EU/t) for plains clear, rain, desert, snowy. */
    private static final Object[][] PANELS = {
        {"generator/block_advanced_solar_panel", "A", 0, 3.3, 1.4, 2.9, 3.5},
        {"generator/block_hybrid_solar_panel", "A", 1, 28.6, 11.7, 25.7, 30.5},
        {"generator/block_ultimate_hybrid_solar_panel", "A", 2, 238.9, 95.3, 215.7, 253.1},
        {"generator/block_quantum_solar_panel", "A", 3, 1937.2, 741.4, 1937.2, 1937.2},
        {"generator/block_mets_advanced_solar_generator", "B", 1, 20.7, 18.0, 19.8, 21.2},
        {"generator/block_photon_resonance_solar_generator", "B", 2, 194.9, 166.1, 187.7, 199.7},
        {"generator/block_ultimate_photon_resonance_solar_generator", "B", 3, 1767.5, 1470.2, 1707.4, 1807.6}};

    /** The panel's daily average output (EU/t, multiplier 1) under one sky, open above. */
    static double daily(Class<?> solar, Object profile, int day, int core, Object[] sky) {
        double sum = 0;
        int n = 0;
        for (long t = 0; t < 24000; t += 10, n++) {
            Object r = call(solar, "evaluate", profile, day, core, t, 0, 15, (boolean) sky[2], (boolean) sky[3], (float) sky[4], (float) sky[5], (float) sky[1]);
            sum += (double) call(r, "output");
        }
        return sum / n;
    }

    /** Night only (12000 to 24000): the average output (EU/t). */
    static double night(Class<?> solar, Object profile, int day, int core, Object[] sky) {
        double sum = 0;
        int n = 0;
        for (long t = 12000; t < 24000; t += 10, n++) {
            Object r = call(solar, "evaluate", profile, day, core, t, 0, 15, (boolean) sky[2], (boolean) sky[3], (float) sky[4], (float) sky[5], (float) sky[1]);
            sum += (double) call(r, "output");
        }
        return sum / n;
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void solarRouteBalance(GameTestHelper h) {
        Class<?> solar = Round33GameTests.common("blockentity.generator.mio_icif_solar_generator");
        double[][] avg = new double[PANELS.length][SKIES.length];
        double[] nights = new double[PANELS.length];
        JsonObject root = new JsonObject();
        int x = 0;
        for (int i = 0; i < PANELS.length; i++) {
            BlockPos at = new BlockPos(1 + x, 2, 1);
            x += 2;
            h.setBlock(at, BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("mio_icif", (String) PANELS[i][0])));
            BlockEntity be = h.getBlockEntity(at);
            Object profile = call(be, "getProfile");
            int day = (int) call(be, "getDayPower"), core = (int) call(be, "getCorePower");
            JsonObject row = new JsonObject();
            row.addProperty("route", (String) PANELS[i][1]);
            row.addProperty("rated", day);
            row.addProperty("night_core", core);
            for (int k = 0; k < SKIES.length; k++) {
                avg[i][k] = daily(solar, profile, day, core, SKIES[k]);
                row.addProperty((String) SKIES[k][0], Math.round(avg[i][k] * 10) / 10.0);
            }
            nights[i] = night(solar, profile, day, core, SKIES[0]);
            row.addProperty("plains_clear_night", Math.round(nights[i] * 10) / 10.0);
            row.addProperty("plains_long_run", Math.round((0.82 * avg[i][0] + 0.18 * avg[i][1]) * 10) / 10.0);
            root.add((String) PANELS[i][0], row);
        }
        Path out = FMLPaths.GAMEDIR.get().resolve("solar_balance.json");
        try {
            Files.writeString(out, new GsonBuilder().setPrettyPrinting().create().toJson(root), StandardCharsets.UTF_8);
        } catch (java.io.IOException e) {
            throw new AssertionError("cannot write " + out, e);
        }

        // within 10 % (or 0.5 EU/t) of the draft: plains clear, plains rain, desert, snowy
        int[] draftSky = {0, 1, 3, 4};
        List<String> off = new ArrayList<>();
        for (int i = 0; i < PANELS.length; i++)
            for (int j = 0; j < draftSky.length; j++) {
                double draft = (double) PANELS[i][3 + j], got = avg[i][draftSky[j]];
                if (Math.abs(got - draft) > Math.max(0.5, draft * 0.10))
                    off.add(PANELS[i][0] + " " + SKIES[draftSky[j]][0] + ": " + String.format("%.1f", got) + " (draft " + draft + ")");
            }
        h.assertTrue(off.isEmpty(), "off the draft by more than 10 %: " + off);

        // what each route is for, rank by rank (A: index 1..3, B: index 4..6)
        for (int rank = 1; rank <= 3; rank++) {
            int a = rank, b = rank + 3;
            String pair = PANELS[a][0] + " / " + PANELS[b][0];
            h.assertTrue(avg[a][0] > avg[b][0], pair + ": route A must win a clear day in the plains");
            h.assertTrue(avg[a][3] > avg[b][3], pair + ": route A must win the desert");
            h.assertTrue(avg[b][1] > avg[a][1] && avg[b][2] > avg[a][2], pair + ": route B must win rain and thunderstorms");
            h.assertTrue(nights[b] > 0 && nights[a] == 0, pair + ": only route B makes power at night");
            double longA = 0.82 * avg[a][0] + 0.18 * avg[a][1], longB = 0.82 * avg[b][0] + 0.18 * avg[b][1];
            h.assertTrue(longB > 0.75 * longA && longB < 1.10 * longA, pair + ": long run in the plains A " + longA + ", B " + longB);
        }
        // route A: the cooling pays off in the desert (share of the clear-plains output)
        for (int i = 1; i < 4; i++)
            h.assertTrue(avg[i][3] / avg[i][0] > avg[i - 1][3] / avg[i - 1][0], PANELS[i][0] + " keeps less of its output in the desert than the tier below");
        h.succeed();
    }
}
