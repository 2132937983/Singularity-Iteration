package example.sicore;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * 0.1.11.100 (solar overhaul S0): every solar panel reads the sky through a solar profile. The standard profile is the
 * model of 0.1.11.99 exactly; the seven route panels use it until S1 and S2. The profile's other switches (direct light
 * only, heat after clipping, a held cell, night weather, several packets) work as documented. (The mod's common classes by
 * reflection: addon examples do not compile against them.)
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round111100GameTests {
    private Round111100GameTests() {}

    private static Class<?> common(String name) { return Round33GameTests.common(name); }
    private static Object call(Object target, String name, Object... args) { return Round33GameTests.call(target, name, args); }
    private static double d(Object o) { return ((Number) o).doubleValue(); }

    private static Object standard() {
        try { return common("energy.SolarProfile").getField("STANDARD").get(null); }
        catch (ReflectiveOperationException e) { throw new IllegalStateException(e); }
    }

    private static Object profile(Object... args) {
        Class<?> p = common("energy.SolarProfile");
        try { return p.getConstructors()[0].newInstance(args); }
        catch (java.lang.reflect.InvocationTargetException e) {
            if (e.getCause() instanceof RuntimeException r) throw r;
            throw new IllegalStateException(e);
        }
        catch (ReflectiveOperationException e) { throw new IllegalStateException(e); }
    }

    /** The standard profile gives what SolarPhysics gave: temperature, factor and output over a grid of cases. */
    @GameTest(template = "empty", timeoutTicks = 20)
    public static void standardProfileIsTheOldModel(GameTestHelper h) {
        Class<?> physics = common("energy.SolarPhysics");
        Object s = standard();
        for (double ambient : new double[]{-10, 2, 18, 22, 42}) {
            for (double g = 0; g <= 1100; g += 50) {
                double cell = d(call(s, "cellTemperature", ambient, g)), oldCell = d(call(physics, "cellTemperature", ambient, g));
                double f = d(call(s, "derate", cell)), oldF = d(call(physics, "derate", oldCell));
                double out = d(call(s, "output", g, f)), oldOut = d(call(physics, "output", g, oldF));
                h.assertTrue(cell == oldCell && f == oldF && out == oldOut,
                    "ambient " + ambient + ", " + g + " W/m²: " + cell + "/" + f + "/" + out + " vs " + oldCell + "/" + oldF + "/" + oldOut);
            }
        }
        h.assertTrue(d(call(s, "shade", true, 15)) == 1 && Math.abs(d(call(s, "shade", false, 15)) - 0.15) < 1e-12
            && Math.abs(d(call(s, "shade", false, 5)) - 0.05) < 1e-12 && d(call(s, "shade", false, 0)) == 0, "shade: open 1, covered 15 % of the sky light");
        for (double k : new double[]{0, 0.37, 0.718, 1}) h.assertTrue(d(call(s, "weather", k)) == k, "weather is the clear-sky index " + k);
        h.assertTrue(d(call(s, "night", 1.0, 1.0)) == 1 && d(call(s, "night", 0.5, 0.0)) == 1, "the standard night ignores the weather");
        h.assertTrue((int) call(s, "packets") == 1, "one packet");
        h.succeed();
    }

    /** The seven route panels use the standard profile for now and send one packet a tick. */
    @GameTest(template = "empty", timeoutTicks = 20)
    public static void routePanelsUseTheStandardProfile(GameTestHelper h) {
        Object s = standard();
        String[] panels = {"generator/block_solar_generator", "generator/block_advanced_solar_panel", "generator/block_hybrid_solar_panel",
            "generator/block_ultimate_hybrid_solar_panel", "generator/block_quantum_solar_panel", "generator/block_mets_advanced_solar_generator",
            "generator/block_photon_resonance_solar_generator", "generator/block_ultimate_photon_resonance_solar_generator"};
        int x = 0;
        for (String id : panels) {
            BlockPos at = new BlockPos(1 + x, 2, 1);
            x += 2;
            h.setBlock(at, BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("mio_icif", id)));
            BlockEntity be = h.getBlockEntity(at);
            h.assertTrue(s.equals(call(be, "getProfile")), id + " profile " + call(be, "getProfile"));
            h.assertTrue((int) call(be, "getPacketCount") == 1, id + " packets " + call(be, "getPacketCount"));
        }
        h.succeed();
    }

    /** Direct light only, heat after clipping, a held cell, night weather and two packets. */
    @GameTest(template = "empty", timeoutTicks = 20)
    public static void profileSwitches(GameTestHelper h) {
        // clip, diffuse, directOnly, cloudExponent, cellRise, fixedCell, gamma, derateMin, derateMax, derateAfterClip, nightRain, nightThunder, packets
        Object a = profile(850.0, 0.15, true, 3.0, 45.0, false, 0.004, 0.6, 1.05, true, 1.0, 1.0, 2);
        h.assertTrue(d(call(a, "shade", false, 15)) == 0 && d(call(a, "shade", true, 15)) == 1, "direct light only: a covered panel gets nothing");
        h.assertTrue(Math.abs(d(call(a, "weather", 0.718)) - Math.pow(0.718, 3)) < 1e-12, "weather: the index cubed");
        h.assertTrue(Math.abs(d(call(a, "cellTemperature", 18.0, 800.0)) - 63) < 1e-9, "cell 18 + 45 K at 800 W/m²");
        h.assertTrue(Math.abs(d(call(a, "output", 950.0, 0.8)) - 0.8) < 1e-12, "heat costs output after clipping");
        h.assertTrue(Math.abs(d(call(a, "output", 425.0, 0.8)) - 0.4) < 1e-12, "half the clip at x0.8");
        h.assertTrue(d(call(a, "output", 950.0, 1.05)) == 1, "never above the rated output");
        h.assertTrue((int) call(a, "packets") == 2, "two packets");

        Object b = profile(350.0, 0.6, false, 0.5, 20.0, true, 0.002, 0.85, 1.05, false, 0.5, 0.25, 1);
        h.assertTrue(d(call(b, "cellTemperature", 42.0, 1000.0)) == 25, "a held cell stays at 25 °C");
        h.assertTrue(Math.abs(d(call(b, "shade", false, 15)) - 0.6) < 1e-12, "covered: 60 % diffuse");
        h.assertTrue(Math.abs(d(call(b, "night", 1.0, 0.0)) - 0.5) < 1e-12 && Math.abs(d(call(b, "night", 1.0, 1.0)) - 0.25) < 1e-12
            && d(call(b, "night", 0.0, 0.0)) == 1 && Math.abs(d(call(b, "night", 0.5, 0.0)) - 0.75) < 1e-12, "night: rain x0.5, thunder x0.25");
        h.assertTrue(d(call(b, "output", 350.0, 1.0)) == 1, "full output at 350 W/m²");

        boolean refused = false;
        try { profile(0.0, 0.15, false, 1.0, 20.0, false, 0.004, 0.75, 1.1, false, 1.0, 1.0, 1); }
        catch (IllegalArgumentException e) { refused = true; }
        h.assertTrue(refused, "a profile without a clip level is refused");
        h.succeed();
    }
}
