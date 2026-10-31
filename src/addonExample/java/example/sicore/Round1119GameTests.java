package example.sicore;

import com.miophas.singularity_iteration.core.api.capability.IMioIcifCapabilities;
import com.miophas.singularity_iteration.core.api.multiblock.StructureBlueprint;
import com.miophas.singularity_iteration.core.api.multiblock.StructureBlueprintRegistry;
import com.miophas.singularity_iteration.core.api.reactor.ReactorLayout;
import com.miophas.singularity_iteration.core.api.reactor.ReactorLayoutRegistry;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * GameTests of 0.1.10.19 (five player reports): the fluid generators show the fuel in the tank, a wrench
 * harvests SI machines (FTB Ultimine breaks with the player's tool), the Armory keeps a remote chunk
 * loaded, the JEI reactor layouts give the numbers of the reactor model for a full fuel load, and every
 * built-in structure blueprint forms its structure when it is built as drawn.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round1119GameTests {
    private Round1119GameTests() {}

    private static final BlockPos AT = new BlockPos(1, 2, 1);

    private static Object call(Object target, String name, Object... args) { return Round33GameTests.call(target, name, args); }

    private static ResourceLocation si(String path) { return ResourceLocation.fromNamespaceAndPath("mio_icif", path); }

    private static Block block(String path) { return BuiltInRegistries.BLOCK.get(si(path)); }

    private static Object staticField(Class<?> type, String name) {
        try {
            Field f = type.getDeclaredField(name);
            f.setAccessible(true);
            return f.get(null);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(name, e);
        }
    }

    // ------------------------------------------------------------------ #5 fuel in the GUI

    /**
     * The fluid heat generator and both semi-fluid generators: with a fuel other than biogas in the tank,
     * the menu (the data the screen draws) reports that fuel. Up to 0.1.10.18 the screens drew biogas.
     */
    @GameTest(batch = "round1119_fuel", template = "empty", timeoutTicks = 20)
    public static void fluidGeneratorsShowTheirFuel(GameTestHelper h) {
        Player player = h.makeMockPlayer(GameType.SURVIVAL);
        for (String path : List.of("hugenerator/block_fluid_heat_generator", "generator/block_semifluid_generator",
                "generator/block_advanced_semifluid_generator")) {
            Block generator = block(path);
            h.assertTrue(generator != Blocks.AIR, path + " not registered");
            h.setBlock(AT, generator);
            BlockEntity be = h.getBlockEntity(AT);
            IFluidHandler tank = null;
            for (Direction side : Direction.values()) {
                tank = h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, h.absolutePos(AT), side);
                if (tank != null) break;
            }
            h.assertTrue(tank != null, path + ": no fluid handler");
            // the advanced generator burns biogas only (its guide page); the others get a fuel other than biogas, so a screen that
            // always showed biogas fails
            boolean biogasOnly = path.equals("generator/block_advanced_semifluid_generator");
            Fluid fuel = null;
            for (Fluid fluid : BuiltInRegistries.FLUID) {
                if (fluid == Fluids.EMPTY || !fluid.defaultFluidState().isSource()) continue;
                if (BuiltInRegistries.FLUID.getKey(fluid).getPath().contains("biogas") != biogasOnly) continue;
                if (tank.fill(new FluidStack(fluid, 1000), IFluidHandler.FluidAction.SIMULATE) > 0) { fuel = fluid; break; }
            }
            h.assertTrue(fuel != null, path + (biogasOnly ? ": takes no biogas" : ": no fuel other than biogas"));
            tank.fill(new FluidStack(fuel, 1000), IFluidHandler.FluidAction.EXECUTE);
            AbstractContainerMenu menu = ((MenuProvider) be).createMenu(1, player.getInventory(), player);
            h.assertTrue(menu != null, path + ": no menu");
            menu.broadcastChanges();
            FluidStack shown = (FluidStack) call(menu, "getFuelFluid");
            h.assertTrue(shown.getFluid() == fuel, path + ": the screen shows " + BuiltInRegistries.FLUID.getKey(shown.getFluid())
                + ", the tank holds " + BuiltInRegistries.FLUID.getKey(fuel));
            h.setBlock(AT, Blocks.AIR);
        }
        h.succeed();
    }

    // ------------------------------------------------------------------ #4 wrench harvest (FTB Ultimine)

    /**
     * An SI machine that needs the correct tool: an empty hand cannot harvest it, a wrench can, and the
     * wrench drop is the machine itself. FTB Ultimine breaks each block with the player's tool, so this is
     * the drop of every machine in an Ultimine selection made with a wrench. Other blocks keep their rules.
     */
    @GameTest(batch = "round1119_wrench", template = "empty", timeoutTicks = 20)
    public static void wrenchHarvestsMachines(GameTestHelper h) {
        TagKey<Block> machines = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("c", "machine"));
        Block target = null;
        for (Holder<Block> holder : BuiltInRegistries.BLOCK.getTagOrEmpty(machines)) {
            Block b = holder.value();
            if (BuiltInRegistries.BLOCK.getKey(b).getNamespace().equals("mio_icif") && b.defaultBlockState().requiresCorrectToolForDrops()) {
                target = b;
                break;
            }
        }
        h.assertTrue(target != null, "no SI machine that needs the correct tool");
        h.setBlock(AT, target);
        ServerLevel level = h.getLevel();
        BlockPos pos = h.absolutePos(AT);
        BlockState state = level.getBlockState(pos);
        Player player = h.makeMockPlayer(GameType.SURVIVAL);
        h.assertTrue(!state.canHarvestBlock(level, pos, player), "an empty hand harvests " + target);
        ItemStack wrench = new ItemStack(BuiltInRegistries.ITEM.get(si("item_tool_wrench")));
        h.assertTrue(!wrench.isEmpty(), "no SI wrench item");
        player.setItemInHand(InteractionHand.MAIN_HAND, wrench);
        h.assertTrue(state.canHarvestBlock(level, pos, player), "the wrench is not a correct tool for " + target);
        Block machine = target;
        List<ItemStack> drops = Block.getDrops(state, level, pos, level.getBlockEntity(pos), player, wrench);
        h.assertTrue(drops.stream().anyMatch(s -> s.is(machine.asItem())), "wrench drops of " + target + ": " + drops);
        if (wrench.isDamageableItem()) {
            int before = wrench.getDamageValue();
            wrench.getItem().mineBlock(wrench, level, state, pos, player);
            h.assertTrue(wrench.getDamageValue() == before + 1, "a wrench break wears the wrench by " + (wrench.getDamageValue() - before));
        }
        Class<?> harvest = Round33GameTests.common("event.WrenchHarvest");
        h.assertTrue(!(boolean) call(harvest, "wrenchable", Blocks.STONE.defaultBlockState()), "stone counts as an SI machine");
        h.assertTrue((boolean) call(harvest, "wrenchable", state), target + " does not count as an SI machine");
        h.succeed();
    }

    // ------------------------------------------------------------------ #1 Armory in another dimension

    /** The remote Armory chunk gets a renewable 30 s ticket (a synchronous load alone lasts one tick). */
    @GameTest(batch = "round1119_armory", template = "empty", timeoutTicks = 20)
    public static void armoryRemoteTicketOutlivesTheRefresh(GameTestHelper h) {
        Object type = staticField(Round33GameTests.common("armory.ArmoryRemoteService"), "REMOTE_TICKET");
        h.assertTrue(type instanceof TicketType<?>, "no remote ticket type");
        long timeout = ((TicketType<?>) type).timeout();
        // the console asks every 20 ticks; the ticket must live longer than that and expire on its own
        h.assertTrue(timeout >= 100 && timeout <= 1200, "remote ticket lifespan " + timeout);
        h.succeed();
    }

    // ------------------------------------------------------------------ #2 reactor layouts

    /**
     * The built-in layouts give the numbers checked with the reactor model (verify-si1119), and the full
     * run sees the late melt that the 100 s run of the guide cannot see.
     */
    @GameTest(batch = "round1119_layouts", template = "empty", timeoutTicks = 20)
    public static void reactorLayoutsGiveModelNumbers(GameTestHelper h) {
        Map<String, Object[]> expected = new LinkedHashMap<>();
        expected.put("starter", new Object[] {"STABLE", 5.0});
        expected.put("dual_rod", new Object[] {"STABLE", 20.0});
        expected.put("quad_compact", new Object[] {"STABLE", 60.0});
        expected.put("dual_rods_six", new Object[] {"STABLE", 120.0});
        expected.put("high_output", new Object[] {"STABLE", 300.0});
        expected.put("high_output_plated", new Object[] {"STABLE", 300.0});
        expected.put("max_output", new Object[] {"STABLE", 660.0});
        expected.put("failure_vents", new Object[] {"MELTDOWN", 60.0});
        expected.put("failure_reflector", new Object[] {"MELTDOWN", 80.0});
        Class<?> analysis = Round33GameTests.common("reactor.ReactorLayoutAnalysis");
        Class<?> simulation = Round33GameTests.common("reactor.ReactorSimulation");
        for (Map.Entry<String, Object[]> e : expected.entrySet()) {
            ReactorLayout layout = ReactorLayoutRegistry.get(si(e.getKey()));
            h.assertTrue(layout != null, "layout " + e.getKey() + " not registered");
            Object result = call(call(analysis, "of", layout), "result");
            h.assertTrue(result != null, e.getKey() + ": not simulated");
            String verdict = call(result, "verdict").toString();
            double eu = (double) call(call(result, "report"), "euPerTick");
            h.assertTrue(verdict.equals(e.getValue()[0]) && Math.abs(eu - (double) e.getValue()[1]) < 0.01,
                e.getKey() + ": " + verdict + " " + eu + " EU/t, expected " + e.getValue()[0] + " " + e.getValue()[1]);
        }
        // the trap: 100 s say stable, the full run sees the vents melt (199 s) and the meltdown (280 s)
        ReactorLayout trap = ReactorLayoutRegistry.get(si("failure_reflector"));
        int[] columns = {3};
        Object profiles = call(simulation, "parse", trap.preset(), columns);
        h.assertTrue((boolean) call(call(simulation, "run", profiles, columns[0]), "stable"), "the 100 s run no longer calls the trap stable");
        Object full = call(simulation, "fullRun", profiles, columns[0]);
        h.assertTrue((int) call(full, "firstMeltCycle") == 199 && (int) call(full, "meltdownCycle") == 280,
            "full run: first melt " + call(full, "firstMeltCycle") + ", meltdown " + call(full, "meltdownCycle"));
        // the explosion of the plated design is much smaller than the plain one
        Object plain = call(call(call(analysis, "of", ReactorLayoutRegistry.get(si("high_output"))), "result"), "blast");
        Object plated = call(call(call(analysis, "of", ReactorLayoutRegistry.get(si("high_output_plated"))), "result"), "blast");
        h.assertTrue((float) call(plated, "power") < (float) call(plain, "power") / 10F, "plating does not shrink the explosion");
        h.succeed();
    }

    // ------------------------------------------------------------------ #3 structure blueprints

    /** Every built-in blueprint resolves all its blocks and has a controller. */
    @GameTest(batch = "round1119_blueprints", template = "empty", timeoutTicks = 20)
    public static void blueprintsResolve(GameTestHelper h) {
        List<String> names = List.of("nuclear_reactor", "fluid_reactor", "gesu", "large_fabricator", "oil_rig", "dimension_oil_rig",
            "blast_furnace", "fermenter");
        for (String name : names) {
            StructureBlueprint plan = StructureBlueprintRegistry.get(si(name));
            h.assertTrue(plan != null, "blueprint " + name + " not registered");
            h.assertTrue(plan.controllerPos() != null, name + ": no controller");
            for (StructureBlueprint.Key key : plan.keys().values())
                for (StructureBlueprint.BlockRef ref : key.blocks())
                    h.assertTrue(ref.resolve() != null, name + ": unknown block " + ref.block());
        }
        h.succeed();
    }

    /**
     * Each multiblock blueprint, built as drawn (first permitted block of each position), passes the
     * validator of its controller: the plans in JEI and in the guide match the structure checks.
     */
    @GameTest(batch = "round1119_blueprints_built", template = "empty", timeoutTicks = 40)
    public static void blueprintsFormTheirStructures(GameTestHelper h) {
        Map<String, String> validators = new LinkedHashMap<>();
        validators.put("gesu", "multiblock.mio_icif_gesu_validator");
        validators.put("large_fabricator", "multiblock.mio_icif_large_fabricator_validator");
        validators.put("oil_rig", "multiblock.mio_icif_oil_rig_validator");
        validators.put("dimension_oil_rig", "multiblock.mio_icif_dimension_oil_rig_validator");
        validators.put("fluid_reactor", "multiblock.mio_icif_fluid_reactor_validator");
        ServerLevel level = h.getLevel();
        BlockPos origin = h.absolutePos(new BlockPos(1, 2, 1));
        for (Map.Entry<String, String> e : validators.entrySet()) {
            StructureBlueprint plan = StructureBlueprintRegistry.get(si(e.getKey()));
            List<BlockPos> placed = build(level, origin, plan);
            BlockPos controller = origin.offset(plan.controllerPos());
            Object validator;
            try {
                validator = Round33GameTests.common(e.getValue()).getDeclaredConstructor().newInstance();
            } catch (ReflectiveOperationException ex) {
                throw new AssertionError(e.getValue(), ex);
            }
            Object result = call(validator, "validate", level, controller);
            boolean valid = (boolean) call(result, "isValid");
            String reason = valid ? "" : String.valueOf(call(result, "getErrorMessage"));
            for (BlockPos p : placed) level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
            h.assertTrue(valid, e.getKey() + ": the blueprint does not form the structure: " + reason);
        }
        h.succeed();
    }

    /** The reactor blueprint gives 9 columns; the heated blueprints face the heat port of the machine. */
    @GameTest(batch = "round1119_blueprints_placement", template = "empty", timeoutTicks = 60)
    public static void reactorAndHeatPlacements(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        BlockPos origin = h.absolutePos(new BlockPos(1, 2, 1));
        StructureBlueprint reactor = StructureBlueprintRegistry.get(si("nuclear_reactor"));
        build(level, origin, reactor);
        BlockPos core = origin.offset(reactor.controllerPos());
        List<BlockPos> heated = new ArrayList<>();
        for (String name : List.of("blast_furnace", "fermenter")) {
            StructureBlueprint plan = StructureBlueprintRegistry.get(si(name));
            BlockPos at = origin.offset(5 + heated.size() * 3, 0, 0);
            build(level, at, plan);
            heated.add(at.offset(plan.controllerPos()));
        }
        for (BlockPos machine : heated) {
            // the machine faces east, the heat source is east of it and faces west
            h.assertTrue(level.getCapability(IMioIcifCapabilities.HEAT_STORAGE_BLOCK, machine, Direction.EAST) != null,
                level.getBlockState(machine) + ": no heat port towards the heat source");
            h.assertTrue(level.getCapability(IMioIcifCapabilities.HEAT_STORAGE_BLOCK, machine, Direction.WEST) == null,
                level.getBlockState(machine) + ": heat port on the back");
        }
        h.succeedWhen(() -> {
            BlockEntity be = level.getBlockEntity(core);
            h.assertTrue(be != null, "no reactor");
            int columns = (int) call(be, "getAvailableColumns");
            h.assertTrue(columns == 9, "reactor columns " + columns + ", expected 9");
        });
    }

    private static List<BlockPos> build(ServerLevel level, BlockPos origin, StructureBlueprint plan) {
        List<BlockPos> placed = new ArrayList<>();
        // the controller first, as a player builds: a reactor chamber placed with no reactor beside it drops at once
        List<StructureBlueprint.Placement> order = new ArrayList<>(plan.placements());
        BlockPos controller = plan.controllerPos();
        if (controller != null) order.sort((a, b) -> Boolean.compare(!a.pos().equals(controller), !b.pos().equals(controller)));
        for (StructureBlueprint.Placement p : order) {
            BlockState state = p.key().blocks().get(0).resolve();
            if (state == null) throw new AssertionError(plan.id() + ": unknown block " + p.key().blocks().get(0).block());
            BlockPos at = origin.offset(p.pos());
            level.setBlock(at, state, 3);
            placed.add(at);
        }
        return placed;
    }
}
