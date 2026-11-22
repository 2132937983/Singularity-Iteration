package example.sicore;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * 0.1.11.155: the solar grid as one bank. Basic solar generators tie on all four sides, so a 3 x 4 array is one grid; every
 * panel of a grid may hold the grid's whole capacity and the energy goes to the panels it is drawn from, so one cable on a
 * corner of the array takes what all twelve store and make (0.1.11.154 gave it one panel's 2 EU buffer: 1 EU/t); the grid
 * stops making energy when the grid, not the panel, is full.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round111155GameTests {
    private Round111155GameTests() {}

    private static final String BASIC = "generator/block_solar_generator", ADVANCED = "generator/block_advanced_solar_panel";
    private static final String CABLE = "wiring/cable/block_glass_cable";

    private static Block block(String path) { return BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("mio_icif", path)); }
    private static BlockState panel(String path, Direction facing) {
        return block(path).defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, facing);
    }
    private static Object call(Object target, String name, Object... args) { return Round33GameTests.call(target, name, args); }
    private static Object storage(BlockEntity be) { return call(be, "getEnergyStorageInternal"); }
    private static double stored(BlockEntity be) { return (Double) call(call(storage(be), "scexExactAmount"), "toDouble"); }
    private static long capacity(BlockEntity be) { return (Long) call(storage(be), "getCapacity"); }
    private static int gridSize(BlockEntity be) { return (Integer) call(be, "getGridSize"); }

    /** A 3 x 4 array of basic solar generators (facings mixed: they tie on every side) at x 2..4, z 1..4. */
    private static List<BlockPos> basicArray(GameTestHelper h) {
        List<BlockPos> at = new ArrayList<>();
        Direction[] facings = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
        for (int x = 0; x < 3; x++) for (int z = 0; z < 4; z++) {
            BlockPos p = new BlockPos(2 + x, 2, 1 + z);
            h.setBlock(p, panel(BASIC, facings[(x + z) % 4]));
            at.add(p);
        }
        return at;
    }

    @GameTest(template = "empty", batch = "round111155_array", timeoutTicks = 200)
    public static void oneCableTakesAWholeBasicArray(GameTestHelper h) {
        List<BlockPos> array = basicArray(h);
        BlockPos corner = array.get(0), lone = new BlockPos(2, 2, 7);
        h.setBlock(corner.west(), block(CABLE));
        h.setBlock(corner.west(2), block("wiring/block_mfsu").defaultBlockState().setValue(BlockStateProperties.FACING, Direction.WEST));
        h.setBlock(lone, panel(BASIC, Direction.NORTH));
        h.setBlock(lone.east(), block(CABLE));
        h.setBlock(lone.east(2), block("wiring/block_mfsu").defaultBlockState().setValue(BlockStateProperties.FACING, Direction.EAST));
        BlockEntity arrayMfsu = h.getBlockEntity(corner.west(2)), loneMfsu = h.getBlockEntity(lone.east(2));
        long[] start = new long[2];
        h.startSequence()
            .thenWaitUntil(() -> {
                for (BlockPos p : array) h.assertTrue(gridSize(h.getBlockEntity(p)) == 12, "the array is not one grid: " + gridSize(h.getBlockEntity(p)));
                // every panel may hold the grid's capacity: twelve 2 EU buffers
                h.assertTrue(capacity(h.getBlockEntity(corner)) == 24, "a panel of the array holds " + capacity(h.getBlockEntity(corner)));
            })
            .thenExecute(() -> {
                // 2 EU in every panel (24 in the grid), 2 in the lone one
                for (BlockPos p : array) call(storage(h.getBlockEntity(p)), "setEnergy", 2L);
                call(storage(h.getBlockEntity(lone)), "setEnergy", 2L);
                start[0] = (Long) call(arrayMfsu, "apiGetStoredEnergy");
                start[1] = (Long) call(loneMfsu, "apiGetStoredEnergy");
            })
            .thenIdle(20)
            .thenExecute(() -> {
                long fromArray = (Long) call(arrayMfsu, "apiGetStoredEnergy") - start[0], fromLone = (Long) call(loneMfsu, "apiGetStoredEnergy") - start[1];
                System.out.println("[Round111155] in 20 ticks one cable took " + fromArray + " EU from a 3 x 4 array, " + fromLone + " EU from a lone panel");
                // the eleven other panels' 22 EU came through the corner as well (and what they made)
                h.assertTrue(fromArray >= fromLone + 20, "one cable took " + fromArray + " EU from the array, " + fromLone + " from a lone panel");
                // what the panels hold went to the corner: the others keep at most what they made since the last tick
                for (int i = 1; i < array.size(); i++) {
                    double held = stored(h.getBlockEntity(array.get(i)));
                    h.assertTrue(held <= 1.5, "panel " + array.get(i).toShortString() + " still holds " + held + " EU");
                }
            })
            .thenSucceed();
    }

    @GameTest(template = "empty", batch = "round111155_array", timeoutTicks = 200)
    public static void aGridStopsWhenTheGridIsFull(GameTestHelper h) {
        BlockPos p0 = new BlockPos(1, 2, 2);
        for (int i = 0; i < 3; i++) h.setBlock(p0.east(i), panel(ADVANCED, Direction.NORTH));
        BlockEntity first = h.getBlockEntity(p0), second = h.getBlockEntity(p0.east()), third = h.getBlockEntity(p0.east(2));
        h.startSequence()
            .thenWaitUntil(() -> h.assertTrue(gridSize(first) == 3 && capacity(first) == 96_000 && capacity(third) == 96_000,
                "grid " + gridSize(first) + ", capacities " + capacity(first) + " / " + capacity(third)))
            .thenExecute(() -> {
                // the grid's whole capacity in one panel (the others emptied: setting them reads as a draw, so the spread
                // follows them for FOLLOW_TICKS; the check waits that out)
                call(storage(first), "setEnergy", 96_000L);
                call(storage(second), "setEnergy", 0L);
                call(storage(third), "setEnergy", 0L);
            })
            .thenIdle(60)
            .thenExecute(() -> {
                double total = stored(first) + stored(second) + stored(third);
                h.assertTrue(total <= 96_000 + 1e-6, "a full grid made more: " + total + " EU in a 96,000 EU grid");
                h.assertTrue(total >= 96_000 - 1e-6, "a full grid lost energy: " + total);
                // nothing draws: spread by capacity
                h.assertTrue(Math.abs(stored(first) - stored(third)) <= 1, "not spread: " + stored(first) + " / " + stored(third));
            })
            .thenSucceed();
    }
}
