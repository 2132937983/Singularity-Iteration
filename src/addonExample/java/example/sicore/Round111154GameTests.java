package example.sicore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * 0.1.11.154: the solar panels with SCXSYGL's models. They let light and their neighbours' faces through; the cells their
 * panels reach into are kept (nothing in an AIR cell, only a cable in a CABLE cell), for the panel being placed and for
 * blocks placed next to it; and panels of one kind side by side on their joining faces share their storage (grid-tie):
 * the group's energy is spread over it, one panel sends the packets of the whole group, the screen shows the group.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round111154GameTests {
    private Round111154GameTests() {}

    private static final String[] PANELS = {"generator/block_solar_generator", "generator/block_advanced_solar_panel",
        "generator/block_hybrid_solar_panel", "generator/block_ultimate_hybrid_solar_panel", "generator/block_quantum_solar_panel",
        "generator/block_mets_advanced_solar_generator", "generator/block_photon_resonance_solar_generator",
        "generator/block_ultimate_photon_resonance_solar_generator"};
    private static final String METS = "generator/block_mets_advanced_solar_generator", ADVANCED = "generator/block_advanced_solar_panel";
    private static final String CABLE = "wiring/cable/block_glass_cable";

    private static Block block(String path) { return BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("mio_icif", path)); }
    private static Class<?> footprint() { return Round33GameTests.common("block.generator.SolarFootprint"); }
    private static BlockState panel(String path, Direction facing) {
        return block(path).defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, facing);
    }
    private static Object call(Object target, String name, Object... args) { return Round33GameTests.call(target, name, args); }
    private static long stored(BlockEntity be) { return (Long) call(call(be, "getEnergyStorageInternal"), "getAmount"); }
    private static void charge(BlockEntity be, long eu) { call(call(be, "getEnergyStorageInternal"), "setEnergy", eu); }
    private static int gridSize(BlockEntity be) { return (Integer) call(be, "getGridSize"); }
    private static int dataGrid() {
        try {
            return Round33GameTests.common("blockentity.generator.mio_icif_solar_generator").getField("DATA_GRID").getInt(null);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("DATA_GRID", e);
        }
    }

    /** The cells a panel takes, relative to it, as "dx,dy,dz=KIND". */
    @SuppressWarnings("unchecked")
    private static List<String> cells(BlockState state) {
        var map = (Map<BlockPos, Object>) call(footprint(), "cells", state, BlockPos.ZERO);
        List<String> out = new ArrayList<>();
        map.forEach((p, k) -> out.add(p.getX() + "," + p.getY() + "," + p.getZ() + "=" + k));
        out.sort(null);
        return out;
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void solarPanelsKeepTheCellsTheirModelsReachInto(GameTestHelper h) {
        List<String> wrong = new ArrayList<>();
        for (String id : PANELS) if (block(id).defaultBlockState().canOcclude()) wrong.add(id + " occludes (its model does not fill the cell)");
        // the METS panel's wings: the cells beside it (cable under the wing), the row above
        List<String> mets = cells(panel(METS, Direction.NORTH));
        if (!mets.equals(List.of("-1,0,0=CABLE", "-1,1,0=AIR", "0,1,0=AIR", "1,0,0=CABLE", "1,1,0=AIR")))
            wrong.add("METS facing north takes " + mets);
        // turned with the panel: facing east its wings are to the north and south
        List<String> east = cells(panel(METS, Direction.EAST));
        if (!east.equals(List.of("0,0,-1=CABLE", "0,0,1=CABLE", "0,1,-1=AIR", "0,1,0=AIR", "0,1,1=AIR"))) wrong.add("METS facing east takes " + east);
        // the advanced panel: a flat panel over the front cell, the tilted one up and to the back
        List<String> advanced = cells(panel(ADVANCED, Direction.NORTH));
        if (!advanced.equals(List.of("0,0,-1=CABLE", "0,1,0=AIR", "0,1,1=AIR"))) wrong.add("advanced facing north takes " + advanced);
        // the ultimate hybrid panel reaches two cells up at the back
        if (!cells(panel("generator/block_ultimate_hybrid_solar_panel", Direction.NORTH)).contains("0,2,1=AIR")) wrong.add("ultimate hybrid misses its high cell");
        h.assertTrue(wrong.isEmpty(), "solar cells: " + wrong);
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void solarPanelFitsOnlyWhereItsCellsAreFree(GameTestHelper h) {
        BlockPos at = new BlockPos(3, 2, 3);
        var level = h.getLevel();
        BlockState mets = panel(METS, Direction.NORTH);
        List<String> wrong = new ArrayList<>();
        java.util.function.Supplier<Boolean> fits = () -> (Boolean) call(footprint(), "fits", level, h.absolutePos(at), mets, null);
        if (!fits.get()) wrong.add("does not fit in the open");
        h.setBlock(at.west(), block(CABLE));
        h.setBlock(at.east(), block(CABLE));
        if (!fits.get()) wrong.add("a cable under the wing stops it");
        h.setBlock(at.east(), Blocks.STONE);
        if (fits.get()) wrong.add("fits with stone under the wing");
        h.setBlock(at.east(), Blocks.AIR);
        h.setBlock(at.above().west(), Blocks.STONE);
        if (fits.get()) wrong.add("fits with stone where the wing rises");
        h.setBlock(at.above().west(), Blocks.AIR);
        h.setBlock(at.north(), Blocks.STONE);
        h.setBlock(at.south(), Blocks.STONE);
        if (!fits.get()) wrong.add("blocks in front and behind (free cells) stop it");
        // a placed panel keeps its cells from the others: its cell above is AIR, beside it CABLE, in front free
        h.setBlock(at, mets);
        BlockPos abs = h.absolutePos(at);
        Object above = call(footprint(), "takenAt", level, abs.above()), west = call(footprint(), "takenAt", level, abs.west()),
            front = call(footprint(), "takenAt", level, abs.north().north());
        if (!"AIR".equals(String.valueOf(above)) || !"CABLE".equals(String.valueOf(west)) || front != null)
            wrong.add("taken cells read above " + above + ", west " + west + ", two ahead " + front);
        h.assertTrue(wrong.isEmpty(), "solar fit: " + wrong);
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void playersCannotBuildIntoAPanelsCells(GameTestHelper h) {
        BlockPos at = new BlockPos(3, 2, 3);
        for (int x = 1; x <= 5; x++) for (int z = 1; z <= 5; z++) h.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
        h.setBlock(at, panel(METS, Direction.NORTH));
        var player = h.makeMockServerPlayerInLevel();
        Vec3 aside = Vec3.atCenterOf(h.absolutePos(new BlockPos(7, 2, 7)));   // out of the way of the blocks it places
        player.moveTo(aside.x, aside.y, aside.z);
        java.util.function.BiConsumer<ItemStack, BlockPos> placeOnTopOf = (stack, below) -> {
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            BlockPos abs = h.absolutePos(below);
            stack.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(abs).add(0, 0.5, 0), Direction.UP, abs, false)));
        };
        List<String> wrong = new ArrayList<>();
        placeOnTopOf.accept(new ItemStack(Items.STONE, 4), at.east().below());     // under the east wing
        if (!h.getBlockState(at.east()).isAir()) wrong.add("stone went under the wing");
        placeOnTopOf.accept(new ItemStack(block(CABLE).asItem(), 4), at.east().below());
        if (h.getBlockState(at.east()).getBlock() != block(CABLE)) wrong.add("a cable did not go under the wing");
        placeOnTopOf.accept(new ItemStack(Items.STONE, 4), at.east());               // on the cable: where the wing rises
        if (!h.getBlockState(at.east().above()).isAir()) wrong.add("stone went where the wing rises");
        placeOnTopOf.accept(new ItemStack(Items.STONE, 4), at.north().below());     // in front: free
        if (h.getBlockState(at.north()).getBlock() != Blocks.STONE) wrong.add("stone did not go in front (a free cell)");
        h.assertTrue(wrong.isEmpty(), "building around a panel: " + wrong);
        h.succeed();
    }

    @GameTest(template = "empty", batch = "round111154_grid", timeoutTicks = 60)
    public static void panelsTieOnlyThroughTheirJoiningFaces(GameTestHelper h) {
        // METS panels join front to back: a row along z; advanced panels left to right: a row along x
        BlockPos m0 = new BlockPos(1, 2, 1), a0 = new BlockPos(4, 2, 1), x0 = new BlockPos(1, 2, 6);
        for (int i = 0; i < 3; i++) h.setBlock(m0.south(i), panel(METS, i == 1 ? Direction.SOUTH : Direction.NORTH));
        for (int i = 0; i < 3; i++) h.setBlock(a0.east(i), panel(ADVANCED, Direction.NORTH));
        // advanced panels front to back do not join; nor do two kinds side by side
        h.setBlock(x0, panel(ADVANCED, Direction.NORTH));
        h.setBlock(x0.south(), panel(ADVANCED, Direction.NORTH));
        h.setBlock(x0.east(3), panel(METS, Direction.NORTH));
        h.setBlock(x0.east(3).south(), panel("generator/block_photon_resonance_solar_generator", Direction.NORTH));
        h.succeedWhen(() -> {
            List<String> wrong = new ArrayList<>();
            for (int i = 0; i < 3; i++) if (gridSize(h.getBlockEntity(m0.south(i))) != 3) wrong.add("METS row panel " + i + ": " + gridSize(h.getBlockEntity(m0.south(i))));
            for (int i = 0; i < 3; i++) if (gridSize(h.getBlockEntity(a0.east(i))) != 3) wrong.add("advanced row panel " + i + ": " + gridSize(h.getBlockEntity(a0.east(i))));
            if (gridSize(h.getBlockEntity(x0)) != 1) wrong.add("advanced panels joined front to back");
            if (gridSize(h.getBlockEntity(x0.east(3))) != 1) wrong.add("two kinds joined");
            // one advanced panel sends one LV packet a tick; in a row of three, three
            if ((Integer) call(h.getBlockEntity(a0), "getPacketCount") != 3) wrong.add("packets " + call(h.getBlockEntity(a0), "getPacketCount"));
            h.assertTrue(wrong.isEmpty(), "grid: " + wrong);
        });
    }

    @GameTest(template = "empty", batch = "round111154_grid", timeoutTicks = 60)
    public static void gridSharesItsStorageAndSplitsWhenBroken(GameTestHelper h) {
        BlockPos p0 = new BlockPos(2, 2, 1);
        for (int i = 0; i < 3; i++) h.setBlock(p0.south(i), panel(METS, Direction.NORTH));
        BlockEntity first = h.getBlockEntity(p0), middle = h.getBlockEntity(p0.south()), last = h.getBlockEntity(p0.south(2));
        charge(first, 60_000);
        h.startSequence()
            .thenWaitUntil(() -> {
                long a = stored(first), b = stored(middle), c = stored(last);
                h.assertTrue(a + b + c >= 60_000, "the grid lost energy: " + (a + b + c));
                h.assertTrue(Math.max(a, Math.max(b, c)) - Math.min(a, Math.min(b, c)) <= 64, "not spread: " + a + " / " + b + " / " + c);
            })
            .thenExecute(() -> {
                // the screen shows the group: three panels, their stored EU and capacity (200,000 each)
                var data = (ContainerData) call(middle, "getContainerData");
                int grid = dataGrid();
                long cap = ((long) data.get(grid + 3) << 32) | Integer.toUnsignedLong(data.get(grid + 4));
                h.assertTrue(data.get(grid) == 3 && cap == 600_000L, "screen data: " + data.get(grid) + " panels, capacity " + cap);
                h.setBlock(p0.south(), Blocks.AIR);
            })
            .thenWaitUntil(() -> h.assertTrue(gridSize(first) == 1 && gridSize(last) == 1,
                "the ends still share a grid: " + gridSize(first) + " / " + gridSize(last)))
            .thenSucceed();
    }

    /**
     * A row of four advanced panels sends through one cable what four panels would (four LV packets a tick); a lone panel
     * sends one.
     */
    @GameTest(template = "empty", batch = "round111154_throughput", timeoutTicks = 200)
    public static void oneCableCarriesTheWholeRow(GameTestHelper h) {
        BlockPos row = new BlockPos(1, 2, 1), lone = new BlockPos(1, 2, 6);
        for (int i = 0; i < 4; i++) h.setBlock(row.east(i), panel(ADVANCED, Direction.NORTH));
        h.setBlock(row.east(4), block(CABLE));
        h.setBlock(row.east(5), block("wiring/block_mfsu").defaultBlockState().setValue(BlockStateProperties.FACING, Direction.EAST));
        h.setBlock(lone, panel(ADVANCED, Direction.NORTH));
        h.setBlock(lone.east(), block(CABLE));
        h.setBlock(lone.east(2), block("wiring/block_mfsu").defaultBlockState().setValue(BlockStateProperties.FACING, Direction.EAST));
        charge(h.getBlockEntity(row), 30_000);
        charge(h.getBlockEntity(lone), 30_000);
        BlockEntity rowMfsu = h.getBlockEntity(row.east(5)), loneMfsu = h.getBlockEntity(lone.east(2));
        long[] start = new long[2];
        h.startSequence()
            .thenIdle(10)
            .thenExecute(() -> { start[0] = (Long) call(rowMfsu, "apiGetStoredEnergy"); start[1] = (Long) call(loneMfsu, "apiGetStoredEnergy"); })
            .thenIdle(40)
            .thenExecute(() -> {
                double rowRate = ((Long) call(rowMfsu, "apiGetStoredEnergy") - start[0]) / 40.0, loneRate = ((Long) call(loneMfsu, "apiGetStoredEnergy") - start[1]) / 40.0;
                System.out.println("[Round111154] a row of four advanced panels sends " + rowRate + " EU/t through one cable, a lone panel " + loneRate + " EU/t");
                h.assertTrue(rowRate >= 100, "the row sent only " + rowRate + " EU/t through one cable (a lone panel: " + loneRate + ")");
                h.assertTrue(loneRate <= 40, "a lone panel sent " + loneRate + " EU/t (one LV packet a tick is 32)");
            })
            .thenSucceed();
    }
}
