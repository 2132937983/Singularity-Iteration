package example.sicore;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandlerModifiable;

/**
 * 0.1.11.92: the fusion reactor. The plasma's start-up, burn and hazards (the pure model), the blanket (the fission reactor's
 * cycle with the plasma's neutrons): its best layout gives exactly 39,200,000 EU/t (2 x the Mekanism fusion reactor's 19.6M
 * FE/t) and nothing gives more; tritium breeding; the power output port's voltage; the structure in the world (it forms, turns
 * with the controller, breaks with a missing block); a disruption with and without ice. (The fusion classes are reached by
 * reflection: addon examples do not compile against the mod's internals.)
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round11592GameTests {
    private Round11592GameTests() {}

    private static final String PLASMA = "fusion.FusionPlasma", BLANKET = "fusion.FusionBlanket", STRUCTURE = "fusion.FusionStructure",
        CONTROLLER = "blockentity.fusion.FusionControllerBlockEntity", PORT = "blockentity.fusion.FusionPowerPortBlockEntity";

    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath("mio_icif", path); }
    private static Item item(String path) { return BuiltInRegistries.ITEM.get(id(path)); }
    private static Block block(String name) { return BuiltInRegistries.BLOCK.get(id("fusion/block_fusion_" + name)); }
    private static Class<?> cls(String name) { return Round33GameTests.common(name); }
    private static Object call(Object target, String name, Object... args) { return Round33GameTests.call(target, name, args); }
    private static long lng(Object o) { return ((Number) o).longValue(); }

    private static long num(String cls, String field) {
        try {
            Field f = cls(cls).getDeclaredField(field);
            f.setAccessible(true);
            return ((Number) f.get(null)).longValue();
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(field, e);
        }
    }

    private static Object make(String cls, Object... args) {
        try {
            for (Constructor<?> c : cls(cls).getDeclaredConstructors()) {
                if (c.getParameterCount() != args.length) continue;
                c.setAccessible(true);
                return c.newInstance(args);
            }
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(cls, e);
        }
        throw new AssertionError("no constructor of " + cls + " with " + args.length + " arguments");
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Object part(String name) {
        return Enum.valueOf((Class) cls(STRUCTURE + "$Part"), name);
    }

    /** Inputs for the plasma: formed, running, feed, heaters, cyclotron heaters, energy, deuterium, tritium, exhaust, not overheated. */
    private static Object run(int feed, int heaters, int cyclotron, long energy, long d, long t, boolean exhaust) {
        return make(PLASMA + "$Inputs", true, true, feed, heaters, cyclotron, energy, d, t, exhaust, false);
    }

    private static String state(Object plasma) { return call(plasma, "state").toString(); }
    private static String event(Object result) { return call(result, "event").toString(); }

    @GameTest(template = "empty", batch = "round11592_fusion_model", timeoutTicks = 20)
    public static void plasmaStartsIgnitesAndBurns(GameTestHelper h) {
        Object p = make(PLASMA);
        long energy = 1_000_000_000L, d = 1_000_000, t = 1_000_000;
        List<String> wrong = new ArrayList<>();
        int ignitionAt = -1;
        long maxHeating = 0, cooldown = num(PLASMA, "COOLDOWN_TICKS");
        for (int tick = 0; tick < 4_000 && ignitionAt < 0; tick++) {
            Object r = call(p, "tick", run(16, 4, 0, energy, d, t, true));
            d -= lng(call(r, "fuelUsed"));
            t -= lng(call(r, "fuelUsed"));
            maxHeating = Math.max(maxHeating, lng(call(r, "heating")));
            if (tick == cooldown - 2 && !state(p).equals("COOLING")) wrong.add("not cooling at tick " + tick + ": " + state(p));
            if (event(r).equals("IGNITION")) ignitionAt = tick;
        }
        if (ignitionAt < 0) wrong.add("no ignition in 4,000 ticks (" + state(p) + ", " + call(p, "kev") + " keV)");
        if (maxHeating > 16 * num(PLASMA, "FUSION_PER_FEED") / 10) wrong.add("heating " + maxHeating + " EU/t above fusion power / 10");
        for (int tick = 0; tick < 2_000; tick++) call(p, "tick", run(16, 4, 0, energy, d, t, true));
        if (!state(p).equals("BURNING")) wrong.add("not burning after ignition: " + state(p));
        double kev = ((Number) call(p, "kev")).doubleValue();
        if (Math.abs(kev - 15.0) > 0.2) wrong.add("burns at " + kev + " keV, expected about 15");
        Object r = call(p, "tick", run(16, 4, 0, energy, d, t, true));
        // 0.1.11.134: ELMs shake the burn by a few percent
        if (Math.abs(lng(call(r, "fusionPower")) - 9_800_000L) > 400_000L) wrong.add("fusion power " + call(r, "fusionPower") + " at 16 steps");
        if (lng(call(r, "heating")) != 0 || !Double.isInfinite(((Number) call(p, "q")).doubleValue())) wrong.add("an ignited plasma still heated: " + call(r, "heating"));
        if (Math.abs(lng(call(r, "neutronPower")) - 7_840_000L) > 320_000L) wrong.add("neutron power " + call(r, "neutronPower"));
        if (Math.abs(lng(call(r, "fuelUsed")) - 16) > 1) wrong.add("fuel " + call(r, "fuelUsed") + " a tick at 16 steps");
        h.assertTrue(wrong.isEmpty(), "plasma: " + wrong);
        h.succeed();
    }

    private static Object burning(int cyclotron) {
        Object p = make(PLASMA);
        for (int tick = 0; tick < 6_000 && !state(p).equals("BURNING"); tick++) call(p, "tick", run(16, 4, cyclotron, 1_000_000_000L, 1_000_000, 1_000_000, true));
        if (!state(p).equals("BURNING")) throw new AssertionError("could not reach a burn: " + state(p));
        return p;
    }

    @GameTest(template = "empty", batch = "round11592_fusion_model", timeoutTicks = 20)
    public static void plasmaEndsGentlyOrDisrupts(GameTestHelper h) {
        List<String> wrong = new ArrayList<>();
        long pulse = num(PLASMA, "PULSE_TICKS"), ash = num(PLASMA, "ASH_LIMIT"), upkeep = num(PLASMA, "MAGNET_UPKEEP");
        // the solenoid's flux runs out: the plasma ends gently, and the solenoid charges again
        Object p = burning(0);
        String last = "NONE";
        for (int tick = 0; tick <= pulse && !last.equals("PULSE_END"); tick++) last = event(call(p, "tick", run(16, 4, 0, 1_000_000_000L, 1_000_000, 1_000_000, true)));
        if (!last.equals("PULSE_END") || !state(p).equals("CHARGING")) wrong.add("no pulse end: " + last + " " + state(p));
        // two electron cyclotron heaters drive the current: the burn goes on
        p = burning(2);
        for (int tick = 0; tick < pulse + 20; tick++) call(p, "tick", run(16, 4, 2, 1_000_000_000L, 1_000_000, 1_000_000, true));
        if (!state(p).equals("BURNING")) wrong.add("two cyclotron heaters did not keep the burn: " + state(p));
        // the fuel runs out: gently
        p = burning(0);
        last = event(call(p, "tick", run(16, 4, 0, 1_000_000_000L, 0, 1_000_000, true)));
        if (!last.equals("PULSE_END") || !state(p).equals("CHARGING")) wrong.add("no fuel: " + last + " " + state(p));
        // helium ash without an exhaust port: a disruption
        p = burning(0);
        last = "NONE";
        for (int tick = 0; tick <= ash && !last.equals("DISRUPTION"); tick++) last = event(call(p, "tick", run(16, 4, 0, 1_000_000_000L, 1_000_000, 1_000_000, false)));
        if (!last.equals("DISRUPTION") || !state(p).equals("DISRUPTED")) wrong.add("no disruption from ash: " + last + " " + state(p));
        // no magnet upkeep while burning: the magnets quench and the plasma disrupts
        p = burning(0);
        Object r = call(p, "tick", run(16, 4, 0, upkeep - 1, 1_000_000, 1_000_000, true));
        if (!event(r).equals("DISRUPTION") || !state(p).equals("QUENCHED")) wrong.add("no quench: " + event(r) + " " + state(p));
        // a broken structure while burning: a disruption
        p = burning(0);
        r = call(p, "tick", make(PLASMA + "$Inputs", false, true, 16, 4, 0, 1_000_000_000L, 1_000_000L, 1_000_000L, true, false));
        if (!event(r).equals("DISRUPTION")) wrong.add("a broken structure did not disrupt: " + event(r));
        h.assertTrue(wrong.isEmpty(), "plasma endings: " + wrong);
        h.succeed();
    }

    /** A blanket from rows of symbols: p proton quad rod, V iridium overclocked vent, L lithium cell, U uranium rod, . empty. */
    private static ItemStack[] blanket(String... rows) {
        int cols = (int) num(BLANKET, "COLUMNS"), rowCount = (int) num(BLANKET, "ROWS");
        ItemStack[] grid = new ItemStack[cols * rowCount];
        for (int r = 0; r < rowCount; r++)
            for (int c = 0; c < cols; c++) {
                char ch = c < rows[r].length() ? rows[r].charAt(c) : '.';
                grid[r * cols + c] = switch (ch) {
                    case 'p' -> new ItemStack(item("reactor/item_reactor_proton_quad"));
                    case 'V' -> new ItemStack(item("reactor/item_reactor_iridium_oc_vent"));
                    case 'L' -> new ItemStack(item("reactor/item_reactor_lithium_cell"));
                    case 'U' -> new ItemStack(item("reactor/item_reactor_uranium_simple"));
                    default -> ItemStack.EMPTY;
                };
            }
        return grid;
    }

    private static Object cycle(ItemStack[] grid, long hull, long neutrons) {
        return call(cls(BLANKET), "cycle", grid, hull, neutrons);
    }

    @GameTest(template = "empty", batch = "round11592_fusion_model", timeoutTicks = 20)
    public static void bestBlanketReachesTwiceMekanismAndStrongerOnesGoPast(GameTestHelper h) {
        List<String> wrong = new ArrayList<>();
        long full = num(BLANKET, "FULL_NEUTRONS"), cap = num(PLASMA, "FULL_POWER"), limit = num(PLASMA, "OUTPUT_LIMIT");
        // proton quad rods boxed in by iridium overclocked vents: stable, and at full power (0.1.11.98: no longer a cap)
        ItemStack[] grid = blanket("VVVVVVVVV", "VpVVpVVpV", "VVVVVVVVV", "VVVVVVVVV", "VpVVpVVpV", "VVVVVVVVV");
        long hull = 0, output = 0;
        for (int i = 0; i < 100; i++) {
            Object c = cycle(grid, hull, full);
            if ((Boolean) call(c, "invalid") || (Boolean) call(c, "overheated")) { wrong.add("the best layout failed at cycle " + i + " (hull " + call(c, "hull") + ")"); break; }
            grid = (ItemStack[]) call(c, "stacks");
            hull = lng(call(c, "hull"));
            output = lng(call(c, "output"));
        }
        System.out.println("[Round11592] reference blanket (proton quad rods in iridium vents): " + output + " EU/t");
        // uncapped it gives 41,440,000 EU/t (the manual, the JEI text, the demo sign and the energy table say so)
        if (output != 41_440_000L) wrong.add("the reference layout gives " + output + " EU/t, expected 41,440,000 (past full power " + cap + ")");
        if (cap != 2 * 19_600_000L) wrong.add("full power is not twice the Mekanism fusion reactor (19.6M FE/t)");
        // 0.1.11.98: no cap: a stronger blanket goes past it; only the grid's top voltage bounds the output (overflow guard)
        if (lng(call(cls(BLANKET), "output", full, 0.0, 20_000.0)) <= cap) wrong.add("a stronger blanket does not go past 39.2M EU/t");
        if (lng(call(cls(BLANKET), "output", full, 1.0, 1e12)) != limit) wrong.add("the output is not bounded by the grid's top voltage");
        if (limit != Integer.MAX_VALUE) wrong.add("the output limit is " + limit + ", not the grid's top voltage");
        // weaker blankets give less
        // 0.1.11.135: the blanket makes heat; the steam cycle turns 35% of it into electricity
        long empty = lng(call(cycle(blanket("", "", "", "", "", ""), 0, full), "output"));
        if (empty != full * 125 / 100 * 35 / 100) wrong.add("an empty blanket gives " + empty + ", expected 35% of the alpha and neutron heat");
        long lithium = lng(call(cycle(blanket("L", "L", "L", "L", "L", "L"), 0, full), "output"));
        if (lithium >= cap || lithium <= empty) wrong.add("a lithium blanket gives " + lithium);
        long uranium = lng(call(cycle(blanket("UV", "VU", "UV", "VU", "UV", "VU"), 0, full), "output"));
        if (uranium >= cap) wrong.add("a few uranium rods already reach the cap: " + uranium);
        // no plasma: no output, the rods rest (subcritical)
        Object idle = cycle(blanket("VVVVVVVVV", "VpVVpVVpV", "VVVVVVVVV", "VVVVVVVVV", "VpVVpVVpV", "VVVVVVVVV"), 0, 0);
        if (lng(call(idle, "output")) != 0 || ((Number) call(idle, "fissionEu")).doubleValue() != 0) wrong.add("the blanket fissions without plasma");
        h.assertTrue(wrong.isEmpty(), "blanket: " + wrong);
        h.succeed();
    }

    @GameTest(template = "empty", batch = "round11592_fusion_model", timeoutTicks = 20)
    public static void lithiumBreedsMoreTritiumThanThePlasmaBurns(GameTestHelper h) {
        // five lithium cells in the first column: each breeds 2 neutrons x 40 a cycle, a tritium cell (10,000) in 125 cycles
        ItemStack[] grid = blanket("L", "L", "L", "L", "L", "");
        long full = num(BLANKET, "FULL_NEUTRONS");
        int cycles = 0;
        while (cycles < 200 && !grid[0].is(item("normal/item_tritium_cell"))) {
            grid = (ItemStack[]) call(cycle(grid, 0, full), "stacks");
            cycles++;
        }
        h.assertTrue(grid[0].is(item("normal/item_tritium_cell")), "no tritium after " + cycles + " cycles: " + grid[0]);
        double bred = 5.0 * num(CONTROLLER, "FUEL_PER_CELL") / (cycles * num(CONTROLLER, "CYCLE_TICKS"));
        h.assertTrue(bred > num(PLASMA, "REFERENCE_FEED"), "five lithium cells breed " + bred + " units a tick, the plasma burns " + num(PLASMA, "REFERENCE_FEED"));
        h.succeed();
    }

    @GameTest(template = "empty", batch = "round11592_fusion_model", timeoutTicks = 20)
    public static void powerPortVoltageAndStructureTurns(GameTestHelper h) {
        List<String> wrong = new ArrayList<>();
        // 0.1.11.98: above UHV (no output cap): UEV up to 536,870,912 EU/t, UIV for the grid's top 2,147,483,647 EU/t
        long[][] cases = {{100_000, 5}, {524_288, 5}, {524_289, 6}, {2_097_152, 6}, {9_800_000, 8}, {19_600_000, 8}, {39_200_000, 9},
            {134_217_728, 9}, {134_217_729, 10}, {200_000_000, 10}, {536_870_912, 10}, {536_870_913, 11}, {Integer.MAX_VALUE, 11}};
        for (long[] c : cases) {
            long tier = lng(call(cls(PORT), "tierFor", c[0]));
            if (tier != c[1]) wrong.add(c[0] + " EU/t -> tier " + tier + ", expected " + c[1]);
        }
        Object south = call(cls(STRUCTURE), "turns", Direction.SOUTH), north = call(cls(STRUCTURE), "turns", Direction.NORTH);
        if (!call(cls(STRUCTURE), "turn", 1, 0, 0, south).equals(new BlockPos(0, 0, 1))) wrong.add("east does not turn to south");
        if (!call(cls(STRUCTURE), "turn", 1, 0, 0, north).equals(new BlockPos(0, 0, -1))) wrong.add("east does not turn to north");
        Object s = call(cls(STRUCTURE), "tokamak");
        if (lng(call(s, "blanketCells")) != 536 || lng(call(s, "divertorCells")) != 192 || lng(call(s, "plasmaCells")) != 360)
            wrong.add("layout: " + call(s, "blanketCells") + " / " + call(s, "divertorCells") + " / " + call(s, "plasmaCells"));
        h.assertTrue(wrong.isEmpty(), "ports and structure: " + wrong);
        h.succeed();
    }

    /** Builds the tokamak around a controller at {@code controller} facing {@code facing}; returns every position it set. */
    private static List<BlockPos> build(GameTestHelper h, BlockPos controller, Direction facing) {
        var level = h.getLevel();
        int t = ((Number) call(cls(STRUCTURE), "turns", facing)).intValue();
        List<BlockPos> placed = new ArrayList<>();
        String[] ports = {"power_port", "cryo_port", "nbi_port", "ecrh_port", "exhaust_port", "fuel_port", "mitigation_port", "nbi_port", "ecrh_port"};
        int port = 0;
        for (Object c : (List<?>) call(call(cls(STRUCTURE), "tokamak"), "cells")) {
            int x = ((Number) call(c, "x")).intValue(), y = ((Number) call(c, "y")).intValue(), z = ((Number) call(c, "z")).intValue();
            BlockPos at = controller.offset((BlockPos) call(cls(STRUCTURE), "turn", x, y, z, t));
            String partName = call(c, "part").toString();
            BlockState state = switch (partName) {
                case "PLASMA" -> Blocks.AIR.defaultBlockState();
                case "CONTROLLER" -> block("controller").defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, facing);
                case "PORT_SLOT" -> block(ports[port++ % ports.length]).defaultBlockState();
                default -> ((Block) call(cls(STRUCTURE), "required", call(c, "part"))).defaultBlockState();
            };
            level.setBlock(at, state, Block.UPDATE_ALL);
            placed.add(at);
        }
        return placed;
    }

    private static void clear(GameTestHelper h, List<BlockPos> placed) {
        for (BlockPos p : placed) h.getLevel().setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
    }

    @GameTest(template = "empty", batch = "round11592_fusion_world", timeoutTicks = 200)
    public static void tokamakFormsTurnsAndBreaks(GameTestHelper h) {
        BlockPos at = h.absolutePos(new BlockPos(2, 6, 2));
        List<BlockPos> placed = build(h, at, Direction.EAST);
        h.runAfterDelay(3, () -> {
            BlockEntity c = h.getLevel().getBlockEntity(at);
            List<String> wrong = new ArrayList<>();
            call(c, "validate");
            if (!(Boolean) call(c, "isFormed")) wrong.add("the tokamak did not form");
            if (lng(call(c, "powerPorts")) != 1 || lng(call(c, "heaters")) != 4) wrong.add("ports: power " + call(c, "powerPorts") + ", heaters " + call(c, "heaters"));
            if (((List<?>) call(c, "ports")).size() != 9) wrong.add(((List<?>) call(c, "ports")).size() + " ports found, expected 9");
            // a missing blanket module breaks it, and the screen names it
            Object blanketPart = part("BLANKET");
            BlockPos blanket = (BlockPos) ((List<?>) call(call(cls(STRUCTURE), "tokamak"), "positions", at, Direction.EAST, blanketPart)).get(0);
            h.getLevel().setBlock(blanket, Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
            call(c, "validate");
            if ((Boolean) call(c, "isFormed")) wrong.add("a missing blanket module did not break it");
            long[] values = (long[]) call(c, "screenValues");
            if (values[26] != blanket.getX() || values[27] != blanket.getY() || values[28] != blanket.getZ() || values[29] != ((Enum<?>) blanketPart).ordinal())
                wrong.add("the screen does not name the missing blanket module");
            clear(h, placed);
            // the same structure built facing south forms too
            List<BlockPos> turned = build(h, at, Direction.SOUTH);
            BlockEntity s = h.getLevel().getBlockEntity(at);
            call(s, "validate");
            if (!(Boolean) call(s, "isFormed")) wrong.add("the tokamak facing south did not form");
            clear(h, turned);
            h.assertTrue(wrong.isEmpty(), "tokamak: " + wrong);
            h.succeed();
        });
    }

    /** 0.1.11.94: the mitigation takes coolant cells (it took ice): one is used, the first column survives; without, it goes. */
    @GameTest(template = "empty", batch = "round11592_fusion_disruption", timeoutTicks = 40)
    public static void disruptionSparesTheFirstWallOnlyWithCoolant(GameTestHelper h) {
        BlockPos at = new BlockPos(2, 2, 2);
        h.setBlock(at, block("controller").defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.EAST));
        BlockEntity c = h.getBlockEntity(at);
        var items = (IItemHandlerModifiable) call(c, "getItems");
        int slot = (int) num(CONTROLLER, "SLOT_MITIGATION");
        items.setStackInSlot(0, new ItemStack(item("reactor/item_reactor_iridium_oc_vent")));
        items.setStackInSlot(9, new ItemStack(item("reactor/item_reactor_iridium_oc_vent")));
        items.setStackInSlot(slot, new ItemStack(item("reactor/item_reactor_coolant_six"), 2));
        call(c, "disrupt", true, 15.0);
        List<String> wrong = new ArrayList<>();
        if (items.getStackInSlot(0).isEmpty() || items.getStackInSlot(9).isEmpty()) wrong.add("the coolant did not spare the first column");
        if (items.getStackInSlot(slot).getCount() != 1) wrong.add("the mitigation did not use exactly one coolant cell");
        items.setStackInSlot(slot, ItemStack.EMPTY);
        items.setStackInSlot(1, new ItemStack(item("reactor/item_reactor_iridium_oc_vent")));
        call(c, "disrupt", true, 15.0);
        if (!items.getStackInSlot(0).isEmpty() || !items.getStackInSlot(9).isEmpty()) wrong.add("without coolant the first column survived");
        if (items.getStackInSlot(1).isEmpty()) wrong.add("the second column was destroyed too");
        h.assertTrue(wrong.isEmpty(), "disruption: " + wrong);
        h.succeed();
    }
}
