package example.sicore;

import com.miophas.singularity_iteration.core.api.multiblock.StructureBlueprint;
import com.miophas.singularity_iteration.core.api.multiblock.StructureBlueprintRegistry;
import com.miophas.singularity_iteration.core.api.energy.storage.ILongEnergyStorage;
import com.miophas.singularity_iteration.core.runtime.energy.FeLedger;
import com.miophas.singularity_iteration.core.runtime.energy.BatteryFeCapability;
import com.miophas.singularity_iteration.core.runtime.energy.ForeignEnergyCables;
import net.neoforged.neoforge.energy.IEnergyStorage;
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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * GameTests of 0.1.10.39. C1: the Steam Tunnel (IC2 Classic's large steam turbine), built from its JEI structure plan, so the plan
 * and the controller's check agree. Internals are reached by reflection.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round1139GameTests {
    private Round1139GameTests() {}

    /** The plan is 5 x 3 x 3 (x, y, z); placed here it fills x 0..4, y 1..3, z 1..3 of the empty template. */
    private static final BlockPos ORIGIN = new BlockPos(0, 1, 1);
    private static final BlockPos CONTROLLER = new BlockPos(4, 2, 2), FLUID_PORT = new BlockPos(2, 2, 1), POWER_PORT = new BlockPos(2, 2, 3);
    private static final BlockPos EDGE = new BlockPos(1, 1, 1), FACE = new BlockPos(1, 2, 1);

    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath("mio_icif", path); }

    private static Object call(Object target, String name, Object... args) { return Round33GameTests.call(target, name, args); }

    /** Builds the Steam Tunnel from its structure plan (the first block of each key). */
    private static void build(GameTestHelper h) {
        StructureBlueprint plan = StructureBlueprintRegistry.get(id("steam_tunnel"));
        h.assertTrue(plan != null && plan.sizeX() == 5 && plan.sizeY() == 3 && plan.sizeZ() == 3, "the Steam Tunnel plan is missing or not 5 x 3 x 3");
        for (StructureBlueprint.Placement p : plan.placements())
            h.setBlock(ORIGIN.offset(p.pos()), p.key().blocks().get(0).resolve());
    }

    private static BlockEntity tunnel(GameTestHelper h) { return h.getBlockEntity(CONTROLLER); }

    private static boolean formed(GameTestHelper h) { return (Boolean) call(tunnel(h), "isFormed"); }

    private static long portEnergy(GameTestHelper h) {
        return (Long) call(call(h.getBlockEntity(POWER_PORT), "getEnergyStorageInternal"), "getAmount");
    }

    private static FluidStack steam(int amount) {
        return new FluidStack(BuiltInRegistries.FLUID.get(id("steam")), amount);
    }

    /**
     * Built from its plan, the tunnel forms; 4,096 mB of steam through a fluid port become 2,048 EU in one tick (4 steps of 1,024 mB
     * into 512 EU) and 40 mB of water (1 per 100 mB, the rest owed), and the power port takes the EU as a power source.
     */
    @GameTest(batch = "round1139_tunnel", template = "empty", timeoutTicks = 80)
    public static void steamTunnelFormsAndTurnsSteamIntoEuAndWater(GameTestHelper h) {
        build(h);
        h.runAfterDelay(25, () -> {
            h.assertTrue(formed(h), "the tunnel built from its plan did not form: " + call(tunnel(h), "status"));
            h.assertTrue((Boolean) call(h.getBlockEntity(POWER_PORT), "isPowerSource"), "the power port of a formed tunnel is no power source");
            IFluidHandler port = h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, h.absolutePos(FLUID_PORT), Direction.NORTH);
            h.assertTrue(port != null && port.fill(steam(4_096), IFluidHandler.FluidAction.EXECUTE) == 4_096, "the fluid port took no steam");
            h.runAfterDelay(3, () -> {
                var t = tunnel(h);
                long total = (Long) call(t, "energy") + portEnergy(h);
                h.assertTrue(((FluidStack) call(t, "steam")).isEmpty(), "steam left: " + call(t, "steam"));
                h.assertTrue(total == 2_048, "4,096 mB of steam made " + total + " EU, not 2,048");
                h.assertTrue(((FluidStack) call(t, "water")).getAmount() == 40, "water returned: " + ((FluidStack) call(t, "water")).getAmount() + " mB, not 40");
                h.succeed();
            });
        });
    }

    /**
     * Removing a casing stops the tunnel at once and names what is missing; a port on an edge is refused; the tunnel forms again
     * once the structure is whole.
     */
    @GameTest(batch = "round1139_tunnel", template = "empty", timeoutTicks = 100)
    public static void steamTunnelBreaksAtOnceAndRefusesPortsOnEdges(GameTestHelper h) {
        build(h);
        Block casing = BuiltInRegistries.BLOCK.get(id("generator/block_steam_tunnel_casing"));
        h.runAfterDelay(25, () -> {
            h.assertTrue(formed(h), "the tunnel did not form");
            h.setBlock(EDGE, Blocks.AIR);
            h.assertTrue(!formed(h), "removing a casing did not stop the tunnel at once");
            h.assertTrue(call(tunnel(h), "status").toString().contains("problem.casing"), "the state does not name the missing casing: " + call(tunnel(h), "status"));
            h.setBlock(EDGE, BuiltInRegistries.BLOCK.get(id("generator/block_steam_tunnel_fluid_port")));
            call(tunnel(h), "validate");
            h.assertTrue(!formed(h) && call(tunnel(h), "status").toString().contains("port_on_edge"), "a port on an edge was accepted");
            h.setBlock(EDGE, casing);
            h.setBlock(FACE, BuiltInRegistries.BLOCK.get(id("generator/block_steam_tunnel_power_port")));   // a second power port, on a face
            h.runAfterDelay(25, () -> {
                h.assertTrue(formed(h), "the whole tunnel did not form again: " + call(tunnel(h), "status"));
                h.succeed();
            });
        });
    }

    /** An MFSU next to the power port (outside the tunnel) is charged by the running tunnel (HV packets from the port). */
    @GameTest(batch = "round1139_tunnel", template = "empty", timeoutTicks = 120)
    public static void steamTunnelPowerPortFeedsTheGrid(GameTestHelper h) {
        build(h);
        BlockPos storage = POWER_PORT.south();
        BlockState mfsu = BuiltInRegistries.BLOCK.get(id("wiring/block_mfsu")).defaultBlockState();
        if (mfsu.getBlock().getStateDefinition().getProperty("facing") instanceof DirectionProperty facing && facing.getPossibleValues().contains(Direction.SOUTH))
            mfsu = mfsu.setValue(facing, Direction.SOUTH);   // its output face away from the port
        h.setBlock(storage, mfsu);
        h.runAfterDelay(25, () -> {
            IFluidHandler port = h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, h.absolutePos(FLUID_PORT), Direction.NORTH);
            h.assertTrue(port != null && port.fill(steam(16_000), IFluidHandler.FluidAction.EXECUTE) == 16_000, "the fluid port took no steam");
            h.runAfterDelay(40, () -> {
                long stored = (Long) call(call(h.getBlockEntity(storage), "getEnergyStorageInternal"), "getAmount");
                h.assertTrue(stored > 2_048, "the MFSU beside the power port got " + stored + " EU");
                h.succeed();
            });
        });
    }

    // ------------------------------------------------------------------ connected textures

    /** The value of the block state property {@code name} at {@code pos}, as written in blockstate files. */
    private static String prop(GameTestHelper h, BlockPos pos, String name) {
        BlockState state = h.getBlockState(pos);
        Property<?> property = state.getBlock().getStateDefinition().getProperty(name);
        h.assertTrue(property != null, state + " has no property " + name);
        return valueName(property, state);
    }

    private static <T extends Comparable<T>> String valueName(Property<T> property, BlockState state) {
        return property.getName(state.getValue(property));
    }

    private static void assertLinks(GameTestHelper h, BlockPos pos, Direction... linked) {
        java.util.Set<Direction> want = java.util.Set.of(linked);
        for (Direction side : Direction.values()) {
            BooleanProperty link = switch (side) {
                case NORTH -> BlockStateProperties.NORTH; case SOUTH -> BlockStateProperties.SOUTH; case EAST -> BlockStateProperties.EAST;
                case WEST -> BlockStateProperties.WEST; case UP -> BlockStateProperties.UP; case DOWN -> BlockStateProperties.DOWN;
            };
            h.assertTrue(h.getBlockState(pos).getValue(link) == want.contains(side),
                "the casing at " + pos + " is " + (want.contains(side) ? "not " : "") + "joined to its " + side + " neighbour: " + h.getBlockState(pos));
        }
    }

    /**
     * Casings join the casings next to them (placed by a plain setBlock too, as the blueprint builder places them), not ports or other
     * blocks, and let go when one is removed. The block state drives the connected textures.
     */
    @GameTest(batch = "round1139_tunnel", template = "empty", timeoutTicks = 20)
    public static void steamTunnelCasingsJoinTheirNeighbours(GameTestHelper h) {
        Block casing = BuiltInRegistries.BLOCK.get(id("generator/block_steam_tunnel_casing"));
        BlockPos left = new BlockPos(1, 1, 1), middle = new BlockPos(2, 1, 1), right = new BlockPos(3, 1, 1), top = new BlockPos(2, 2, 1);
        h.setBlock(left, casing);
        h.setBlock(middle, casing);
        h.setBlock(right, casing);
        h.setBlock(top, casing);
        h.setBlock(middle.south(), BuiltInRegistries.BLOCK.get(id("generator/block_steam_tunnel_power_port")));
        h.setBlock(middle.north(), Blocks.IRON_BLOCK);
        assertLinks(h, middle, Direction.EAST, Direction.WEST, Direction.UP);
        assertLinks(h, left, Direction.EAST);
        assertLinks(h, right, Direction.WEST);
        assertLinks(h, top, Direction.DOWN);
        h.setBlock(right, Blocks.AIR);
        assertLinks(h, middle, Direction.WEST, Direction.UP);
        h.assertTrue("none".equals(prop(h, middle, "fan")), "a lone casing shows a piece of a fan");
        h.succeed();
    }

    /** Facing east, seen from outside: left is south (z 3), right is north (z 1). */
    private static final java.util.Map<BlockPos, String> FAN = java.util.Map.of(
        new BlockPos(4, 3, 3), "east_top_left", new BlockPos(4, 3, 2), "east_top", new BlockPos(4, 3, 1), "east_top_right",
        new BlockPos(4, 2, 3), "east_left", new BlockPos(4, 2, 1), "east_right",
        new BlockPos(4, 1, 3), "east_bottom_left", new BlockPos(4, 1, 2), "east_bottom", new BlockPos(4, 1, 1), "east_bottom_right");

    private static void assertFan(GameTestHelper h, boolean shown) {
        for (var piece : FAN.entrySet()) {
            String value = prop(h, piece.getKey(), "fan");
            h.assertTrue(value.equals(shown ? piece.getValue() : "none"), "the casing at " + piece.getKey() + " shows fan=" + value
                + (shown ? ", not " + piece.getValue() : " on an unformed tunnel"));
        }
    }

    /**
     * Formed, the front shows one big fan: the controller is the hub (formed=true) and the 8 casings around it the pieces of their
     * places; a casing elsewhere shows none. A broken tunnel takes the fan off at once, a whole one puts it back, and removing the
     * controller takes it off too. The tunnel's walls join into panels: a casing in the middle of the top joins its four neighbours.
     */
    @GameTest(batch = "round1139_tunnel", template = "empty", timeoutTicks = 100)
    public static void formedSteamTunnelShowsOneFanOnItsFront(GameTestHelper h) {
        build(h);
        Block casing = BuiltInRegistries.BLOCK.get(id("generator/block_steam_tunnel_casing"));
        h.runAfterDelay(25, () -> {
            h.assertTrue(formed(h), "the tunnel did not form: " + call(tunnel(h), "status"));
            h.assertTrue("true".equals(prop(h, CONTROLLER, "formed")), "the controller of a formed tunnel shows no hub");
            assertFan(h, true);
            h.assertTrue("none".equals(prop(h, EDGE, "fan")), "a casing off the front shows a piece of the fan");
            assertLinks(h, new BlockPos(2, 3, 2), Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST);
            h.setBlock(EDGE, Blocks.AIR);
            h.assertTrue("false".equals(prop(h, CONTROLLER, "formed")), "the controller of a broken tunnel still shows the hub");
            assertFan(h, false);
            h.setBlock(EDGE, casing);
            h.runAfterDelay(25, () -> {
                h.assertTrue(formed(h), "the whole tunnel did not form again: " + call(tunnel(h), "status"));
                assertFan(h, true);
                h.setBlock(CONTROLLER, Blocks.AIR);
                assertFan(h, false);
                h.succeed();
            });
        });
    }

    // ------------------------------------------------------------------ C2: universal cables

    private static Object staticField(Class<?> owner, String name) {
        try { var f = owner.getDeclaredField(name); f.setAccessible(true); return f.get(null); }
        catch (ReflectiveOperationException e) { throw new AssertionError(name, e); }
    }

    @SuppressWarnings("deprecation")
    private static long stored(GameTestHelper h, BlockPos at) {
        return (Long) call(call(h.getBlockEntity(at), "getEnergyStorageInternal"), "getAmount");
    }

    private static BlockState facing(Block block, Direction direction) {
        BlockState state = block.defaultBlockState();
        return state.getBlock().getStateDefinition().getProperty("facing") instanceof DirectionProperty facing
            && facing.getPossibleValues().contains(direction) ? state.setValue(facing, direction) : state;
    }

    /** One FE ratio (4 FE per EU, both ways) everywhere, and FE converts to EU and back without loss. */
    @GameTest(batch = "round1139_cables", template = "empty", timeoutTicks = 20)
    public static void feRatioIsOneLosslessConstant(GameTestHelper h) {
        int ratio = ILongEnergyStorage.FE_PER_EU;
        h.assertTrue(ratio == 4 && FeLedger.FE_PER_EU == ratio && BatteryFeCapability.FE_PER_EU == ratio, "the FE ratio differs in core");
        h.assertTrue((Integer) staticField(Round33GameTests.common("armory.ArmoryItemEnergy"), "FE_PER_EU") == ratio
            && (Integer) staticField(Round33GameTests.common("blockentity.energy_converter.mio_icif_energy_converter_entity"), "EU_TO_FE_RATE") == ratio,
            "the FE ratio differs in the armory or the converter");
        for (int fe = 0; fe <= 10_000; fe++)
            if (FeLedger.fe(FeLedger.eu(fe)) != fe) throw new AssertionError(fe + " FE did not survive the trip to EU and back");
        h.succeed();
    }

    /**
     * FE pushed into an SI cable by the block beyond it (here a block without any energy capability, like a generator that only
     * pushes) reaches SI storage at voltage 0: 32,768 FE through a low-voltage cable (32 EU) fill an MFSU with 8,192 EU less the
     * cable loss, and the cable does not melt.
     */
    @GameTest(batch = "round1139_cables", template = "empty", timeoutTicks = 80)
    public static void fePushedIntoACableEntersAtVoltageZero(GameTestHelper h) {
        BlockPos pusher = new BlockPos(1, 1, 2), cable = new BlockPos(2, 1, 2), storage = new BlockPos(3, 1, 2);
        Block lvCable = BuiltInRegistries.BLOCK.get(id("wiring/cable/block_tin_cable_1"));
        h.setBlock(pusher, Blocks.STONE);
        h.setBlock(cable, lvCable);
        h.setBlock(storage, facing(BuiltInRegistries.BLOCK.get(id("wiring/block_mfsu")), Direction.EAST));   // output away from the cable
        h.runAfterDelay(20, () -> {
            IEnergyStorage face = h.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK, h.absolutePos(cable), Direction.WEST);
            h.assertTrue(face != null && face.canReceive() && !face.canExtract(), "the cable has no receive-only FE face");
            int accepted = face.receiveEnergy(40_000, false);
            h.assertTrue(accepted == 32_768, "the cable took " + accepted + " FE, not its 32,768 FE inbox");
            h.runAfterDelay(40, () -> {
                long eu = stored(h, storage);
                h.assertTrue(eu >= 8_192 * 9 / 10 && eu <= 8_192, "the MFSU got " + eu + " EU of 8,192");
                h.assertTrue(h.getBlockState(cable).is(lvCable), "the low-voltage cable melted under FE input");
                h.succeed();
            });
        });
    }

    /**
     * SI cables give FE to another mod's machine (a battery) but neither connect to nor feed another mod's cable (its id names a
     * cable), so two networks cannot pass FE back and forth.
     */
    @GameTest(batch = "round1139_cables", template = "empty", timeoutTicks = 80)
    @SuppressWarnings("deprecation")
    public static void siCablesFeedForeignMachinesButNotForeignCables(GameTestHelper h) {
        BlockPos source = new BlockPos(1, 1, 2), cable = new BlockPos(2, 1, 2), battery = new BlockPos(3, 1, 2), foreignCable = new BlockPos(2, 1, 3);
        h.setBlock(source, facing(BuiltInRegistries.BLOCK.get(id("wiring/block_mfsu")), Direction.EAST));   // output into the cable
        h.setBlock(cable, BuiltInRegistries.BLOCK.get(id("wiring/cable/block_superconducting_cable")));
        h.setBlock(battery, CoreExampleMod.FOREIGN_BATTERY.get());
        h.setBlock(foreignCable, CoreExampleMod.FOREIGN_CABLE.get());
        call(call(h.getBlockEntity(source), "getEnergyStorageInternal"), "setStored", 1_000_000L);
        h.assertTrue(ForeignEnergyCables.is(h.getLevel(), h.absolutePos(foreignCable)) && !ForeignEnergyCables.is(h.getLevel(), h.absolutePos(battery)),
            "the foreign cable test block is not seen as a cable, or the battery is");
        h.assertTrue(h.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK, h.absolutePos(cable), Direction.SOUTH) == null,
            "the SI cable offers FE input toward another mod's cable");
        h.runAfterDelay(40, () -> {
            int toBattery = ((ForeignBattery) h.getBlockEntity(battery)).storage.getEnergyStored();
            int toCable = ((ForeignBattery) h.getBlockEntity(foreignCable)).storage.getEnergyStored();
            h.assertTrue(toBattery > 0, "the foreign battery got no FE from the SI network");
            h.assertTrue(toCable == 0, "the foreign cable got " + toCable + " FE from the SI network");
            h.succeed();
        });
    }

    /** The Energy Converter is a legacy block: still registered (placed ones keep working), hidden from viewers, no recipe. */
    @GameTest(batch = "round1139_cables", template = "empty", timeoutTicks = 20)
    public static void energyConverterIsALegacyBlock(GameTestHelper h) {
        ResourceLocation converter = id("energy_converter/energy_converter");
        h.assertTrue(BuiltInRegistries.BLOCK.containsKey(converter), "the converter is no longer registered");
        var hidden = net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, ResourceLocation.parse("c:hidden_from_recipe_viewers"));
        h.assertTrue(new net.minecraft.world.item.ItemStack(BuiltInRegistries.ITEM.get(converter)).is(hidden), "the converter is still shown to players");
        h.assertTrue(h.getLevel().getRecipeManager().byKey(id("shaped_energy_converter")).isEmpty(), "the converter can still be crafted");
        h.succeed();
    }
}
