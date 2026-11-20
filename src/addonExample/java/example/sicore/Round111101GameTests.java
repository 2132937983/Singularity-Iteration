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
 * 0.1.11.101 (solar overhaul S1): route A is a concentrator. The four panels make 12 / 96 / 768 / 6144 EU/t in direct sun
 * and nothing at night; a covered one makes nothing; clouds leave the clear-sky index cubed; heat costs output after
 * clipping and the hybrid panels' cooling takes it back; the ultimate hybrid sends two HV packets a tick. (The mod's
 * common classes by reflection: addon examples do not compile against them.)
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round111101GameTests {
    private Round111101GameTests() {}

    private static Class<?> common(String name) { return Round33GameTests.common(name); }
    private static Object call(Object target, String name, Object... args) { return Round33GameTests.call(target, name, args); }
    private static double d(Object o) { return ((Number) o).doubleValue(); }
    private static Block block(String path) { return BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("mio_icif", path)); }

    private static Object route(String name) {
        try { return common("energy.SolarRoutes").getField(name).get(null); }
        catch (ReflectiveOperationException e) { throw new IllegalStateException(e); }
    }

    private static final Object[][] PANELS = {
        {"generator/block_advanced_solar_panel", "ADVANCED", 12, 1},
        {"generator/block_hybrid_solar_panel", "HYBRID", 96, 1},
        {"generator/block_ultimate_hybrid_solar_panel", "ULTIMATE_HYBRID", 768, 2},
        {"generator/block_quantum_solar_panel", "QUANTUM", 6144, 1}};

    /** Rated output, no night core, the route's profile and the packets a tick. */
    @GameTest(template = "empty", timeoutTicks = 20)
    public static void routeAPanels(GameTestHelper h) {
        int x = 0;
        for (Object[] row : PANELS) {
            BlockPos at = new BlockPos(1 + x, 2, 1);
            x += 2;
            h.setBlock(at, block((String) row[0]));
            BlockEntity be = h.getBlockEntity(at);
            Object profile = call(be, "getProfile");
            h.assertTrue(route((String) row[1]).equals(profile), row[0] + " profile " + profile);
            h.assertTrue((int) call(be, "getDayPower") == (int) row[2] && (int) call(be, "getCorePower") == 0,
                row[0] + ": " + call(be, "getDayPower") + " / " + call(be, "getCorePower"));
            h.assertTrue((int) call(be, "getPacketCount") == (int) row[3], row[0] + " packets " + call(be, "getPacketCount"));
            h.assertTrue((boolean) call(profile, "directOnly") && d(call(profile, "clip")) == 850, row[0] + " is not a concentrator");
        }
        h.succeed();
    }

    /** Under a roof a concentrator gets no light at all (a standard panel keeps the diffuse part). */
    @GameTest(template = "empty", timeoutTicks = 40)
    public static void coveredConcentratorMakesNothing(GameTestHelper h) {
        BlockPos at = new BlockPos(2, 1, 2);   // 0.1.11.105: the roof inside the 5x4x5 template (y 3); above it, neighbouring tests could clear it
        h.setBlock(at, block("generator/block_ultimate_hybrid_solar_panel"));
        BlockEntity be = h.getBlockEntity(at);
        h.setBlock(at.above(2), Blocks.STONE);
        h.runAfterDelay(10, () -> {   // the sky light under the roof is updated a few ticks later
            call(be, "sense", h.getLevel());
            h.assertTrue((int) call(be, "getExposure") != 0, "a covered panel is not under open sky");
            h.assertTrue(d(call(be, "getIrradiance")) == 0 && d(call(be, "getOutput")) == 0,
                "covered concentrator: " + call(be, "getIrradiance") + " W/m², " + call(be, "getOutput") + " EU/t");
            h.succeed();
        });
    }

    /** Clouds leave the clear-sky index cubed: rain about 37 %, a thunderstorm about 5 %. */
    @GameTest(template = "empty", timeoutTicks = 20)
    public static void cloudsTakeTheDirectBeam(GameTestHelper h) {
        Class<?> physics = common("energy.SolarPhysics");
        Object a = route("HYBRID");
        double rain = d(call(a, "weather", d(call(physics, "cloudIndex", 1.0, 0.0))));
        double storm = d(call(a, "weather", d(call(physics, "cloudIndex", 1.0, 1.0))));
        h.assertTrue(Math.abs(rain - 0.37) < 0.01 && storm < 0.06 && storm > 0.04, "rain " + rain + ", thunderstorm " + storm);
        h.succeed();
    }

    /**
     * Noon (950 W/m²) in a desert (42 °C air): the uncooled advanced panel loses about 28 %, each cooled tier less, the
     * quantum panel nothing; heat costs output although the light is above the full-output level.
     */
    @GameTest(template = "empty", timeoutTicks = 20)
    public static void heatAndCooling(GameTestHelper h) {
        double[] expected = {0.718, 0.825, 0.878, 1.0};
        double[] coolingShare = {0, 0.5, 0.75, 1};
        double previous = 0;
        for (int i = 0; i < PANELS.length; i++) {
            Object p = route((String) PANELS[i][1]);
            double cell = d(call(p, "cellTemperature", 42.0, 950.0));
            double share = d(call(p, "output", 950.0, d(call(p, "derate", cell))));
            h.assertTrue(Math.abs(share - expected[i]) < 0.005, PANELS[i][0] + ": desert noon " + share + " (cell " + cell + " °C)");
            h.assertTrue(share > previous, PANELS[i][0] + " is not cooler than the tier below");
            previous = share;
            double cooling = d(call(common("energy.SolarRoutes"), "cooling", p));
            h.assertTrue(Math.abs(cooling - coolingShare[i]) < 1e-9, PANELS[i][0] + " cooling " + cooling);
        }
        // the plains (18 °C) cost the uncooled panel less than the desert
        Object adv = route("ADVANCED");
        double plains = d(call(adv, "output", 950.0, d(call(adv, "derate", d(call(adv, "cellTemperature", 18.0, 950.0))))));
        h.assertTrue(plains > expected[0] + 0.05, "plains noon " + plains);
        h.succeed();
    }

    /**
     * The ultimate hybrid's buffer goes out as two 512 EU packets a tick through HV cable: more than one packet a tick
     * reaches the MFSU, and the HV cable holds.
     */
    @GameTest(template = "empty", batch = "round111101_packets", timeoutTicks = 200)
    public static void ultimateHybridSendsTwoHvPackets(GameTestHelper h) {
        BlockPos panel = new BlockPos(1, 2, 1), store = new BlockPos(5, 2, 1);
        h.setBlock(panel, block("generator/block_ultimate_hybrid_solar_panel"));
        for (int x = 2; x <= 4; x++) h.setBlock(new BlockPos(x, 2, 1), block("wiring/cable/block_gold_cable_2"));
        h.setBlock(store, block("wiring/block_mfsu"));
        BlockEntity source = h.getBlockEntity(panel), sink = h.getBlockEntity(store);
        h.runAfterDelay(20, () -> {   // the network has formed
            call(source, "apiGenerateEnergy", 900_000L, false);
            long start = (long) call(sink, "apiGetStoredEnergy");
            h.runAfterDelay(40, () -> {
                long gained = (long) call(sink, "apiGetStoredEnergy") - start;
                for (int x = 2; x <= 4; x++)
                    h.assertTrue(h.getBlockState(new BlockPos(x, 2, 1)).is(block("wiring/cable/block_gold_cable_2")), "the HV cable burnt at x=" + x);
                h.assertTrue(gained > 40 * 512 + 512, "40 ticks moved " + gained + " EU: one packet a tick at most");
                h.assertTrue(gained <= 40 * 1024 + 2 * 1024, "40 ticks moved " + gained + " EU: more than two packets a tick");
                h.succeed();
            });
        });
    }
}
