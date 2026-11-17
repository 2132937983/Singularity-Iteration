package example.sicore;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * 0.1.11.18: the water machines (WaterPhysics: the tide averaging the river's power, spring and neap tides, rain, the
 * rotor's disc, falling water) and the water kinetic generator and water generator in still water. Common classes are
 * reached by reflection.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round11518GameTests {
    private Round11518GameTests() {}

    static Class<?> common(String name) { return Round33GameTests.common(name); }
    static Object call(Object target, String name, Object... args) { return Round33GameTests.call(target, name, args); }

    private static final String WP = "blockentity.kuentity.kugenerator.WaterPhysics";

    private static double d(Object o) { return ((Number) o).doubleValue(); }

    private static BlockEntity place(GameTestHelper h, BlockPos at, String id) {
        h.setBlock(at, BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("mio_icif", id)));
        return h.getBlockEntity(at);
    }

    /** Over a tide cycle the mean power (v^3) is the river's at a mean moon; spring 1.3, neap 0.7; rain +30 %, thunder +60 %. */
    @GameTest(template = "empty", timeoutTicks = 20)
    public static void tideAndRiver(GameTestHelper h) {
        Class<?> wp = common(WP);
        for (int[] c : new int[][]{{1, 100}, {0, 130}, {2, 70}}) {
            double sum = 0;
            for (long t = 0; t < 12000; t += 10) sum += Math.pow(d(call(wp, "tideSpeed", t, c[0])), 3);
            double mean = sum / 1200;
            h.assertTrue(Math.abs(mean - c[1] / 100.0) < 0.01, "moon " + c[0] + ": mean tidal power " + mean);
        }
        h.assertTrue(d(call(wp, "tideSpeed", 6000L, 0)) < 1e-9 && d(call(wp, "tideSpeed", 3000L, 1)) > 1.3, "slack water and flood");
        h.assertTrue(Math.abs(Math.pow(d(call(wp, "riverSpeed", true, false)), 3) - 1.3) < 1e-9
            && Math.abs(Math.pow(d(call(wp, "riverSpeed", true, true)), 3) - 1.6) < 1e-9, "rain and thunder");
        h.assertTrue((int) call(wp, "meanOutput", 7, Enum.valueOf((Class) common(WP + "$Source"), "RIVER")) == 200
            && (int) call(wp, "meanOutput", 7, Enum.valueOf((Class) common(WP + "$Source"), "OCEAN")) == 250, "old means");
        h.succeed();
    }

    /** A diameter-7 rotor in a plane of still water: the whole disc is water, but still water outside a river gives nothing. */
    @GameTest(template = "empty", timeoutTicks = 20)
    public static void stillWaterTurnsNothing(GameTestHelper h) {
        BlockPos at = new BlockPos(4, 6, 4);
        BlockEntity be = place(h, at, "kugenerator/block_water_kinetic_generator");
        call(be, "setRotorStack", new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("mio_icif", "rotors/item_iron_rotor"))));
        for (int x = 1; x <= 7; x++) for (int y = 3; y <= 9; y++) h.setBlock(new BlockPos(x, y, 3), Blocks.WATER);
        call(be, "checkNaturalWater");
        int ku = (int) call(be, "calculateKineticOutput");
        Object disc = call(be, "getDisc");
        h.assertTrue(disc != null && (int) call(disc, "diameter") == 7, "disc " + disc);
        int water = (int) call(disc, "waterCells"), cells = (int) call(disc, "discCells");
        h.assertTrue(water == cells && cells > 30, "water " + water + " of " + cells);
        h.assertTrue(ku == 0, "still water gave " + ku + " KU/t");
        h.succeed();
    }

    /** Falling water moves (a waterfall) whatever the biome; still water does not. */
    @GameTest(template = "empty", timeoutTicks = 20)
    public static void fallingWaterMoves(GameTestHelper h) {
        BlockPos fall = new BlockPos(1, 3, 1), still = new BlockPos(3, 3, 1);
        h.setBlock(fall, Blocks.WATER.defaultBlockState().setValue(LiquidBlock.LEVEL, 8));
        h.setBlock(still, Blocks.WATER);
        Class<?> wp = common(WP);
        double v = d(call(wp, "cellSpeed", h.getLevel(), h.absolutePos(fall), Direction.NORTH, 0.0));
        double s = d(call(wp, "cellSpeed", h.getLevel(), h.absolutePos(still), Direction.NORTH, 0.0));
        h.assertTrue(Math.abs(v - 1.0) < 1e-9 && s == 0, "falling " + v + ", still " + s);
        h.succeed();
    }

    /** The water generator gives nothing from still water in front (it gave 2 EU/t from any water). */
    @GameTest(template = "empty", timeoutTicks = 40)
    public static void waterGeneratorNeedsMovingWater(GameTestHelper h) {
        BlockPos at = new BlockPos(2, 2, 2);
        BlockEntity be = place(h, at, "generator/block_water_generator");
        var facing = be.getBlockState().getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING);
        h.setBlock(at.relative(facing), Blocks.WATER);
        h.succeedWhen(() -> {
            h.assertTrue((boolean) call(be, "hasWaterInFront"), "water in front");
            h.assertTrue((int) call(be, "getFaceOutput") == 0, "still water gives " + call(be, "getFaceOutput"));
        });
    }
}
