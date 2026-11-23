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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * 0.1.11.157: the TESU (太级储电罐), the 7 x 7 x 13 storage cylinder of data/mio_icif/multiblock/tesu.json. It forms from its
 * controller (turned with it), binds the ports in its port slots, stores 200,000,000,000 EU; input ports hand what they take to
 * the controller, output ports fill their two-packet buffer from it at the voltage set on them (IV to MAX, stepping round);
 * a block where the inside must be empty breaks it, and the screen data names that position.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round111157GameTests {
    private Round111157GameTests() {}

    private static Block block(String path) { return BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("mio_icif", path)); }
    private static Class<?> structure() { return Round33GameTests.common("multiblock.TesuStructure"); }
    private static Object call(Object target, String name, Object... args) { return Round33GameTests.call(target, name, args); }
    private static Object storage(BlockEntity be) { return call(be, "getEnergyStorageInternal"); }
    private static long stored(BlockEntity be) { return (Long) call(storage(be), "getAmount"); }

    /** The test area's lowest corner, in the world (test structures may be turned: everything here is in world positions). */
    static BlockPos corner(GameTestHelper h) {
        var b = h.getBounds();
        return BlockPos.containing(b.minX + 0.5, b.minY + 0.5, b.minZ + 0.5);
    }

    /**
     * Builds a TESU with its controller at the world position {@code controller} facing {@code facing}; the first
     * {@code inputs} port slots get input ports, the next {@code outputs} output ports, the rest casing. Returns the port
     * positions (world), inputs first.
     */
    static List<BlockPos> build(GameTestHelper h, BlockPos controller, Direction facing, int inputs, int outputs) {
        return build(h, controller, facing, inputs, outputs, 0);
    }

    /** 0.1.11.158: as {@link #build(GameTestHelper, BlockPos, Direction, int, int)}, the next {@code wireless} slots wireless output ports. */
    static List<BlockPos> build(GameTestHelper h, BlockPos controller, Direction facing, int inputs, int outputs, int wireless) {
        Object s = call(structure(), "get");
        @SuppressWarnings("unchecked")
        List<Object> cells = (List<Object>) call(s, "cells");
        var level = h.getLevel();
        List<BlockPos> ports = new ArrayList<>();
        int slot = 0;
        for (Object cell : cells) {
            BlockPos at = (BlockPos) call(structure(), "at", controller, facing, cell);
            String part = String.valueOf(call(cell, "part"));
            BlockState state = switch (part) {
                case "CASING" -> block("wiring/block_tesu_casing").defaultBlockState();
                case "GLASS" -> block("build/block_alloy_glass").defaultBlockState();
                case "CELL" -> block("wiring/block_tesu_cell").defaultBlockState();
                case "CONTROLLER" -> block("wiring/block_tesu_controller").defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, facing);
                case "PORT_SLOT" -> {
                    int n = slot++;
                    if (n < inputs + outputs + wireless) ports.add(at.immutable());
                    yield block(n < inputs ? "wiring/block_tesu_input" : n < inputs + outputs ? "wiring/block_tesu_output"
                        : n < inputs + outputs + wireless ? "wiring/block_tesu_wireless_port" : "wiring/block_tesu_casing").defaultBlockState();
                }
                default -> Blocks.AIR.defaultBlockState();
            };
            level.setBlock(at, state, 3);
        }
        return ports;
    }

    /** Facing north the TESU spans x 3..9, y 0..12, z 3..9 from the corner (0.1.11.168: a box; it was a cylinder). */
    static BlockPos controller(GameTestHelper h) { return corner(h).offset(6, 1, 3); }
    static BlockEntity be(GameTestHelper h, BlockPos world) {
        BlockEntity be = h.getLevel().getBlockEntity(world);
        if (be == null) throw new net.minecraft.gametest.framework.GameTestAssertException("no block entity at " + world + ": " + h.getLevel().getBlockState(world));
        return be;
    }

    @GameTest(template = "reactor_loop", batch = "round111157_tesu_form", timeoutTicks = 200)
    public static void tesuFormsStoresAndRelays(GameTestHelper h) {
        List<BlockPos> ports = build(h, controller(h), Direction.NORTH, 2, 2);
        BlockEntity controller = be(h, controller(h)), input = be(h, ports.get(0)), output = be(h, ports.get(2)), output2 = be(h, ports.get(3));
        h.startSequence()
            .thenWaitUntil(() -> {
                h.assertTrue((Boolean) call(controller, "isStructureComplete"), "the TESU did not form");
                h.assertTrue((Integer) call(controller, "inputCount") == 2 && (Integer) call(controller, "outputCount") == 2,
                    "ports " + call(controller, "inputCount") + " in / " + call(controller, "outputCount") + " out");
            })
            .thenExecute(() -> {
                h.assertTrue((Long) call(controller, "getEnergyCapacity") == 227_000_000_000L, "capacity " + call(controller, "getEnergyCapacity"));
                // what an input port takes goes on to the controller
                call(storage(input), "setEnergy", 1_000_000L);
            })
            .thenWaitUntil(() -> h.assertTrue(stored(input) == 0 && stored(controller) + stored(output) + stored(output2) == 1_000_000L,
                "input " + stored(input) + ", controller " + stored(controller)))
            .thenExecute(() -> {
                // an output port holds two packets of its voltage from the controller (IV: 2 x 8,192)
                h.assertTrue(stored(output) == 2 * 8_192L, "the output port holds " + stored(output));
                long rating = (Long) call(call(output, "getEffectiveCableTier"), "getPowerRating");
                h.assertTrue(rating == 8_192L, "the output port's packet is " + rating);
            })
            .thenSucceed();
    }

    @GameTest(template = "reactor_loop", batch = "round111157_tesu_turn", timeoutTicks = 100)
    public static void tesuTurnsWithItsController(GameTestHelper h) {
        // facing east the cylinder lies west of its controller: x 3..9 from a controller at x 9 (the layout's depth runs west)
        BlockPos controller = corner(h).offset(9, 1, 6);
        build(h, controller, Direction.EAST, 1, 1);
        BlockEntity be = be(h, controller);
        h.succeedWhen(() -> h.assertTrue((Boolean) call(be, "isStructureComplete"), "a TESU facing east did not form"));
    }

    @GameTest(template = "reactor_loop", batch = "round111157_tesu_air", timeoutTicks = 200)
    public static void tesuBreaksOnABlockInsideAndNamesIt(GameTestHelper h) {
        List<BlockPos> ports = build(h, controller(h), Direction.NORTH, 1, 1);
        BlockEntity controller = be(h, controller(h)), port = be(h, ports.get(1));
        // 0.1.11.158: an energy cell of the full inside, 5 above the base (layout x 1, y 5, z 3: from the controller x -2, y +4, z +3)
        BlockPos inside = controller(h).offset(-2, 4, 3);
        h.startSequence()
            .thenWaitUntil(() -> h.assertTrue((Boolean) call(controller, "isStructureComplete"), "the TESU did not form"))
            .thenExecute(() -> h.getLevel().setBlock(inside, Blocks.STONE.defaultBlockState(), 3))
            .thenWaitUntil(() -> h.assertTrue(!(Boolean) call(controller, "isStructureComplete"), "stone for a cell did not break the TESU"))
            .thenExecute(() -> {
                h.assertTrue(inside.equals(call(controller, "wrongAt")) && "CELL".equals(String.valueOf(call(controller, "expected"))),
                    "the screen names " + call(controller, "wrongAt") + " (" + call(controller, "expected") + "), not " + inside);
                h.assertTrue(!(Boolean) call(port, "isStructureCompleted"), "a port stayed bound to a broken TESU");
            })
            .thenSucceed();
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void outputPortStepsThroughItsVoltages(GameTestHelper h) {
        BlockPos at = new BlockPos(2, 1, 2);
        h.setBlock(at, block("wiring/block_tesu_output"));
        BlockEntity port = h.getBlockEntity(at);
        long[] expected = {32_768L, 131_072L, 524_288L, 2_097_152L, 8_388_608L, 33_554_432L, 134_217_728L, 536_870_912L, 2_147_483_647L, 8_192L};
        List<String> wrong = new ArrayList<>();
        for (long packet : expected) {
            long rating = (Long) call(call(port, "step", false), "getPowerRating");
            long capacity = (Long) call(storage(port), "getCapacity");
            if (rating != packet || capacity != packet * 2) wrong.add(rating + " (buffer " + capacity + "), expected " + packet);
        }
        long back = (Long) call(call(port, "step", true), "getPowerRating");
        if (back != 2_147_483_647L) wrong.add("stepping down from IV gave " + back);
        h.assertTrue(wrong.isEmpty(), "output port voltages: " + wrong);
        h.succeed();
    }
}
