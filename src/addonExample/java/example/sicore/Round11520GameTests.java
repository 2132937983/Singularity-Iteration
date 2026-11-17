package example.sicore;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * 0.1.11.20: the RTG's cold side (RtgPhysics) and the geomagnetic field's disturbance (GeomagPhysics: the daily
 * variation, storms by the seed, the long-run mean). Common classes are reached by reflection.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round11520GameTests {
    private Round11520GameTests() {}

    static Class<?> common(String name) { return Round33GameTests.common(name); }
    static Object call(Object target, String name, Object... args) { return Round33GameTests.call(target, name, args); }

    private static double d(Object o) { return ((Number) o).doubleValue(); }

    private static BlockEntity place(GameTestHelper h, BlockPos at, String id) {
        h.setBlock(at, BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("mio_icif", id)));
        return h.getBlockEntity(at);
    }

    /** Sq 0.55 at noon and 0.08 at midnight; storms are the same for a seed; at most 1.5; the long-run mean about 0.27. */
    @GameTest(template = "empty", timeoutTicks = 100)
    public static void geomagneticDisturbance(GameTestHelper h) {
        Class<?> gp = common("energy.GeomagPhysics");
        h.assertTrue(Math.abs(d(call(gp, "sq", 6000L)) - 0.55) < 1e-9 && Math.abs(d(call(gp, "sq", 18000L)) - 0.08) < 1e-9, "daily variation");
        Object a = call(gp, "storm", 42L, 7L), b = call(gp, "storm", 42L, 7L);
        h.assertTrue(java.util.Objects.equals(a, b), "storms by the seed");
        double sum = 0, max = 0;
        int n = 0;
        for (long t = 0; t < 400L * 24000; t += 600) {
            double v = d(call(gp, "disturbance", 12345L, t));
            sum += v;
            max = Math.max(max, v);
            n++;
        }
        double mean = sum / n;
        h.assertTrue(mean > 0.24 && mean < 0.31, "long-run mean " + mean);
        h.assertTrue(max <= 1.5 + 1e-9 && max > 0.9, "largest disturbance " + max);
        h.succeed();
    }

    /** Air 1; water beside it 1.15; ice beside it 1.25 (better than the water); lava touching it a further 0.6. */
    @GameTest(template = "empty", timeoutTicks = 20)
    public static void rtgColdSide(GameTestHelper h) {
        Class<?> rp = common("energy.RtgPhysics");
        BlockPos at = new BlockPos(3, 3, 3);
        place(h, at, "generator/block_rt_generator");
        double air = d(call(call(rp, "coldSide", h.getLevel(), h.absolutePos(at)), "factor"));
        h.setBlock(at.east(), Blocks.WATER);
        double water = d(call(call(rp, "coldSide", h.getLevel(), h.absolutePos(at)), "factor"));
        h.setBlock(at.west(), Blocks.ICE);
        double ice = d(call(call(rp, "coldSide", h.getLevel(), h.absolutePos(at)), "factor"));
        h.setBlock(at.north(), Blocks.LAVA);
        double lava = d(call(call(rp, "coldSide", h.getLevel(), h.absolutePos(at)), "factor"));
        h.assertTrue(Math.abs(water - 1.15) < 1e-9 && Math.abs(ice - 1.25) < 1e-9 && Math.abs(lava - 0.75) < 1e-9,
            "air " + air + ", water " + water + ", ice " + ice + ", ice and lava " + lava);
        h.assertTrue(air == 1 || air == 0.9, "air " + air);
        h.succeed();
    }

    /** Six pellets with ice beside it: 32 x 1.25 = 40 EU a tick. */
    @GameTest(template = "empty", timeoutTicks = 60)
    public static void rtgIceCooled(GameTestHelper h) {
        BlockPos at = new BlockPos(2, 2, 2);
        BlockEntity be = place(h, at, "generator/block_rt_generator");
        h.setBlock(at.east(), Blocks.ICE);
        var items = (net.neoforged.neoforge.items.IItemHandler) call(be, "getItemHandler");
        var pellet = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("mio_icif", "resource/item_rtg_pellet"));
        for (int i = 0; i < 6; i++) items.insertItem(i, new ItemStack(pellet), false);
        h.succeedWhen(() -> {
            double eu = d(call(be, "getLastEu"));
            h.assertTrue(Math.abs(eu - 40) < 1e-9, "EU a tick " + eu);
        });
    }
}
