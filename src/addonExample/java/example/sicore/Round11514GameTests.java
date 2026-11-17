package example.sicore;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * 0.1.11.14: the solar panels: the irradiance model (sun, air mass, clouds, temperature, clipping), the eight panels as one
 * implementation with their own numbers, and shade under cover. Common classes are reached by reflection (core must not
 * depend on common).
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round11514GameTests {
    private Round11514GameTests() {}

    static Class<?> common(String name) { return Round33GameTests.common(name); }
    static Object call(Object target, String name, Object... args) { return Round33GameTests.call(target, name, args); }

    private static final String PHYSICS = "energy.SolarPhysics";

    private static double d(Object o) { return ((Number) o).doubleValue(); }

    private static BlockEntity place(GameTestHelper h, BlockPos at, String id) {
        h.setBlock(at, BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("mio_icif", id)));
        return h.getBlockEntity(at);
    }

    /** Sunrise 0°, noon 90°, midnight −90°; about 950 W/m² overhead; rain 72 %, thunder 37 %; 45 °C costs 8 %; full output at 700 W/m². */
    @GameTest(template = "empty", timeoutTicks = 20)
    public static void solarPhysicsValues(GameTestHelper h) {
        Class<?> p = common(PHYSICS);
        h.assertTrue(Math.abs(d(call(p, "elevation", 0L))) < 1e-9 && Math.abs(d(call(p, "elevation", 6000L)) - 90) < 1e-9
            && Math.abs(d(call(p, "elevation", 18000L)) + 90) < 1e-9 && Math.abs(d(call(p, "elevation", 3000L)) - 45) < 1e-9, "sun elevation");
        double noon = d(call(p, "clearSky", 90.0)), low = d(call(p, "clearSky", 10.0));
        h.assertTrue(Math.abs(noon - 952.7) < 1 && low < noon / 3, "clear sky: noon " + noon + ", 10° " + low);
        double rain = d(call(p, "cloudIndex", 1.0, 0.0)), thunder = d(call(p, "cloudIndex", 1.0, 1.0));
        h.assertTrue(Math.abs(rain - 0.718) < 0.01 && Math.abs(thunder - 0.37) < 0.02, "clouds: rain " + rain + ", thunder " + thunder);
        h.assertTrue(Math.abs(d(call(p, "derate", 45.0)) - 0.92) < 1e-9, "45 °C cell: x0.92");
        h.assertTrue(d(call(p, "output", 700.0, 1.0)) == 1.0 && d(call(p, "output", 950.0, 1.0)) == 1.0
            && Math.abs(d(call(p, "output", 350.0, 1.0)) - 0.5) < 1e-9, "clipping at 700 W/m²");
        h.succeed();
    }

    /** The eight panels are one implementation; each keeps its rated output, night core and charge slots. */
    @GameTest(template = "empty", timeoutTicks = 20)
    public static void solarPanelsShareOneImplementation(GameTestHelper h) {
        Class<?> base = common("blockentity.generator.mio_icif_solar_generator");
        Object[][] panels = {
            {"generator/block_solar_generator", 1, 0, 1}, {"generator/block_advanced_solar_panel", 8, 1, 4},
            {"generator/block_hybrid_solar_panel", 64, 8, 4}, {"generator/block_ultimate_hybrid_solar_panel", 512, 64, 4},
            {"generator/block_quantum_solar_panel", 4096, 1024, 4}, {"generator/block_mets_advanced_solar_generator", 64, 6, 1},
            {"generator/block_photon_resonance_solar_generator", 512, 51, 1}, {"generator/block_ultimate_photon_resonance_solar_generator", 4096, 512, 1}};
        int x = 0;
        for (Object[] row : panels) {
            BlockEntity be = place(h, new BlockPos(1 + x, 2, 1), (String) row[0]);
            x += 2;
            h.assertTrue(base.isInstance(be), row[0] + " is not a solar panel");
            int day = (int) call(be, "getDayPower"), core = (int) call(be, "getCorePower"), slots = (int) call(be, "getChargeSlots");
            h.assertTrue(day == (int) row[1] && core == (int) row[2] && slots == (int) row[3],
                row[0] + ": " + day + " / " + core + " / " + slots + " slots");
            h.assertTrue((long) call(be, "ratedOutput") == (int) row[1], row[0] + " rated " + call(be, "ratedOutput"));
        }
        h.succeed();
    }

    /** A panel with a roof over it gets the diffuse light only (at most 15 % of the clear sky). */
    @GameTest(template = "empty", timeoutTicks = 40)
    public static void coveredPanelGetsDiffuseLightOnly(GameTestHelper h) {
        BlockPos at = new BlockPos(2, 2, 2);
        BlockEntity be = place(h, at, "generator/block_hybrid_solar_panel");
        h.setBlock(at.above(3), Blocks.STONE);
        h.runAfterDelay(10, () -> {   // the sky light under the roof is updated a few ticks later
            call(be, "sense", h.getLevel());
            int exposure = (int) call(be, "getExposure");
            double g = (double) call(be, "getIrradiance");
            Class<?> p = common(PHYSICS);
            double clear = d(call(p, "clearSky", d(call(p, "elevation", h.getLevel().getDayTime()))));
            h.assertTrue(exposure != 0, "a covered panel is not under open sky");
            h.assertTrue(g <= clear * 0.15 + 1e-6, "covered: " + g + " W/m² of " + clear);
            h.succeed();
        });
    }
}
