package example.sicore;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * 0.1.11.19: the geothermal generator's lava around it (GeoPhysics) and the incinerators' chamber (IncineratorPhysics):
 * efficiency by temperature, a cold chamber making nothing, an item that does not burn cooling it and leaving ash.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round11519GameTests {
    private Round11519GameTests() {}

    static Class<?> common(String name) { return Round33GameTests.common(name); }
    static Object call(Object target, String name, Object... args) { return Round33GameTests.call(target, name, args); }

    private static double d(Object o) { return ((Number) o).doubleValue(); }

    private static BlockEntity place(GameTestHelper h, BlockPos at, String id) {
        h.setBlock(at, BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("mio_icif", id)));
        return h.getBlockEntity(at);
    }

    /** Depth 1 at Y 64, 2 at Y -64; a lava source on a face 1 EU/t and one on a corner 0.5, times the depth. */
    @GameTest(template = "empty", timeoutTicks = 20)
    public static void geothermalLavaAround(GameTestHelper h) {
        Class<?> gp = common("energy.GeoPhysics");
        h.assertTrue(d(call(gp, "depth", 64)) == 1 && d(call(gp, "depth", -64)) == 2 && d(call(gp, "depth", 0)) == 1.5, "depth factor");
        BlockPos at = new BlockPos(3, 3, 3);
        place(h, at, "generator/block_geo_generator");
        h.setBlock(at.east(), Blocks.LAVA);
        h.setBlock(at.offset(-2, 1, 1), Blocks.LAVA);
        Object scan = call(gp, "scan", h.getLevel(), h.absolutePos(at));
        double raw = d(call(scan, "raw")), eu = d(call(scan, "euPerTick")), depth = d(call(gp, "depth", h.absolutePos(at).getY()));
        h.assertTrue(Math.abs(raw - 1.5) < 1e-9, "weighted lava " + raw);
        h.assertTrue(Math.abs(eu - Math.min(12, 1.5 * depth)) < 1e-9, "EU/t " + eu + " at depth " + depth);
        h.succeed();
    }

    /** Nothing below 300 °C, half at 575 °C, all at 850 °C; burning heats the chamber toward 1,000 °C. */
    @GameTest(template = "empty", timeoutTicks = 20)
    public static void incineratorEfficiency(GameTestHelper h) {
        Class<?> ip = common("energy.IncineratorPhysics");
        h.assertTrue(d(call(ip, "efficiency", 300.0)) == 0 && Math.abs(d(call(ip, "efficiency", 575.0)) - 0.5) < 1e-9
            && d(call(ip, "efficiency", 850.0)) == 1, "efficiency curve");
        double t = 20;
        for (int i = 0; i < 600; i++) t = d(call(ip, "step", t, true));
        h.assertTrue(t > 900 && t < 1000, "chamber after 30 s of burning " + t);
        h.succeed();
    }

    /** A cold incinerator makes nothing from a fuel tick; a hot one its full rate; a cobblestone cools it 25 °C and leaves 2 ash. */
    @GameTest(template = "empty", timeoutTicks = 60)
    public static void incineratorQuench(GameTestHelper h) {
        BlockPos at = new BlockPos(1, 2, 1);
        BlockEntity be = place(h, at, "generator/block_drop_generator");
        h.assertTrue((long) call(be, "euPerFuelTick") == 0, "cold chamber");
        call(be, "setChamber", 850.0);
        h.assertTrue((long) call(be, "euPerFuelTick") == 20, "hot chamber");
        h.spawnItem(Items.COBBLESTONE, 1.5F, 2.5F, 1.5F);
        h.succeedWhen(() -> {
            int ash = (int) call(be, "getAsh");
            double chamber = d(call(be, "getChamber"));
            h.assertTrue(ash == 2, "ash " + ash);
            h.assertTrue(chamber < 830, "chamber " + chamber);
        });
    }
}
