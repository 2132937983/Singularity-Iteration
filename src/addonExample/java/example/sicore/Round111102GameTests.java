package example.sicore;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * 0.1.11.102 (solar overhaul S2): route B is photon resonance. The three panels make 48 / 384 / 3072 EU/t by day and
 * 15 / 25 / 35 % of it at night under open sky; they reach full output in weak light (600 / 450 / 350 W/m²), keep 30 / 45 /
 * 60 % of the sky light under cover, lose little to clouds, hardly mind heat, and rain halves the night output (a
 * thunderstorm leaves a quarter). (The mod's common classes by reflection: addon examples do not compile against them.)
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round111102GameTests {
    private Round111102GameTests() {}

    private static Class<?> common(String name) { return Round33GameTests.common(name); }
    private static Object call(Object target, String name, Object... args) { return Round33GameTests.call(target, name, args); }
    private static double d(Object o) { return ((Number) o).doubleValue(); }
    private static Block block(String path) { return BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("mio_icif", path)); }

    private static Object route(String name) {
        try { return common("energy.SolarRoutes").getField(name).get(null); }
        catch (ReflectiveOperationException e) { throw new IllegalStateException(e); }
    }

    /** id, profile, day EU/t, night EU/t, full-output level W/m², diffuse share under cover */
    private static final Object[][] PANELS = {
        {"generator/block_mets_advanced_solar_generator", "METS", 48, 7, 600.0, 0.30},
        {"generator/block_photon_resonance_solar_generator", "PHOTON", 384, 96, 450.0, 0.45},
        {"generator/block_ultimate_photon_resonance_solar_generator", "ULTIMATE_PHOTON", 3072, 1075, 350.0, 0.60}};

    /** Rated output, night output, the route's profile, one packet a tick. */
    @GameTest(template = "empty", timeoutTicks = 20)
    public static void routeBPanels(GameTestHelper h) {
        int x = 0;
        for (Object[] row : PANELS) {
            BlockPos at = new BlockPos(1 + x, 2, 1);
            x += 2;
            h.setBlock(at, block((String) row[0]));
            BlockEntity be = h.getBlockEntity(at);
            Object profile = call(be, "getProfile");
            h.assertTrue(route((String) row[1]).equals(profile), row[0] + " profile " + profile);
            h.assertTrue((int) call(be, "getDayPower") == (int) row[2] && (int) call(be, "getCorePower") == (int) row[3],
                row[0] + ": " + call(be, "getDayPower") + " / " + call(be, "getCorePower"));
            h.assertTrue((int) call(be, "getPacketCount") == 1, row[0] + " packets " + call(be, "getPacketCount"));
            h.assertTrue(!(boolean) call(profile, "directOnly") && (boolean) call(common("energy.SolarRoutes"), "photonResonance", profile),
                row[0] + " is not a photon resonance panel");
        }
        h.succeed();
    }

    /** Full output in weak light, a wide diffuse share, little lost to clouds or heat. */
    @GameTest(template = "empty", timeoutTicks = 20)
    public static void weakLightCloudsAndHeat(GameTestHelper h) {
        Class<?> physics = common("energy.SolarPhysics");
        for (Object[] row : PANELS) {
            Object p = route((String) row[1]);
            double clip = (double) row[4];
            h.assertTrue(d(call(p, "output", clip, 1.0)) == 1 && Math.abs(d(call(p, "output", clip / 2, 1.0)) - 0.5) < 1e-12,
                row[0] + ": full output at " + clip + " W/m²");
            h.assertTrue(Math.abs(d(call(p, "shade", false, 15)) - (double) row[5]) < 1e-12, row[0] + ": covered share");
        }
        Object p = route("PHOTON");
        double rain = d(call(p, "weather", d(call(physics, "cloudIndex", 1.0, 0.0))));
        double storm = d(call(p, "weather", d(call(physics, "cloudIndex", 1.0, 1.0))));
        h.assertTrue(Math.abs(rain - 0.847) < 0.01 && Math.abs(storm - 0.61) < 0.02, "rain " + rain + ", thunderstorm " + storm);
        h.assertTrue(Math.abs(d(call(p, "derate", 65.0)) - 0.92) < 1e-9 && d(call(p, "derate", 200.0)) == 0.85, "0.2 % per kelvin, at least 85 %");
        h.succeed();
    }

    /** The night output: clear sky 1, rain x0.5, a thunderstorm x0.25. */
    @GameTest(template = "empty", timeoutTicks = 20)
    public static void nightSkyWeather(GameTestHelper h) {
        for (Object[] row : PANELS) {
            Object p = route((String) row[1]);
            h.assertTrue(d(call(p, "night", 0.0, 0.0)) == 1 && Math.abs(d(call(p, "night", 1.0, 0.0)) - 0.5) < 1e-12
                && Math.abs(d(call(p, "night", 1.0, 1.0)) - 0.25) < 1e-12, row[0] + ": night sky factor");
        }
        h.succeed();
    }

    /**
     * Under a roof the ultimate photon resonance panel keeps 60 % of the sky light it sees (the basic panel 15 %). 0.1.11.105:
     * checked against the panel's own sky light and weather (a neighbouring basic panel's roof was not always in place).
     */
    @GameTest(batch = "solar_cover", template = "empty", timeoutTicks = 40)
    public static void coveredPanelKeepsMoreSkyLight(GameTestHelper h) {
        BlockPos at = new BlockPos(2, 1, 2);   // 0.1.11.105: the roof inside the 5x4x5 template (y 3); above it, neighbouring tests could clear it
        h.setBlock(at, block("generator/block_ultimate_photon_resonance_solar_generator"));
        h.setBlock(at.above(2), Blocks.STONE);
        BlockEntity be = h.getBlockEntity(at);
        h.runAfterDelay(10, () -> {   // the sky light under the roof is updated a few ticks later
            call(be, "sense", h.getLevel());
            int exposure = (int) call(be, "getExposure");
            if (exposure != 1) { h.fail("the roofed panel is not in diffuse light: exposure " + exposure); return; }
            var level = h.getLevel();
            BlockPos up = h.absolutePos(at).above();
            int sky = level.getBrightness(net.minecraft.world.level.LightLayer.SKY, up);
            Class<?> physics = common("energy.SolarPhysics");
            Object p = route("ULTIMATE_PHOTON");
            double clear = d(call(physics, "clearSky", d(call(physics, "elevation", level.getDayTime()))));
            boolean rain = level.getBiome(up).value().hasPrecipitation();
            double weather = rain ? d(call(p, "weather", d(call(physics, "cloudIndex", (double) level.getRainLevel(1), (double) level.getThunderLevel(1))))) : 1;
            double expected = clear * weather * 0.60 * sky / 15.0;
            double g = d(call(be, "getIrradiance"));
            h.assertTrue(Math.abs(g - expected) < 1e-6 && g <= clear * 0.60 + 1e-6,
                "covered: " + g + " W/m², expected " + expected + " (clear " + clear + ", sky light " + sky + ")");
            h.succeed();
        });
    }
}
