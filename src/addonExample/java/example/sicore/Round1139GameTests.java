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
                "the shell block at " + pos + " is " + (want.contains(side) ? "not " : "") + "joined to its " + side + " neighbour: " + h.getBlockState(pos));
        }
    }

    /**
     * Shell blocks (casings and ports) join the shell blocks next to them, not other blocks, and let go when one is removed. Placed by
     * a plain setBlock (as the blueprint builder places them) they join on the next tick. The block state drives the connected
     * textures.
     */
    @GameTest(batch = "round1139_tunnel", template = "empty", timeoutTicks = 20)
    public static void steamTunnelShellBlocksJoinTheirNeighbours(GameTestHelper h) {
        Block casing = BuiltInRegistries.BLOCK.get(id("generator/block_steam_tunnel_casing"));
        BlockPos left = new BlockPos(1, 1, 1), middle = new BlockPos(2, 1, 1), right = new BlockPos(3, 1, 1), top = new BlockPos(2, 2, 1);
        BlockPos port = middle.south();
        h.setBlock(left, casing);
        h.setBlock(middle, casing);
        h.setBlock(right, casing);
        h.setBlock(top, casing);
        h.setBlock(port, BuiltInRegistries.BLOCK.get(id("generator/block_steam_tunnel_power_port")));
        h.setBlock(middle.north(), Blocks.IRON_BLOCK);
        h.runAfterDelay(2, () -> {
            assertLinks(h, middle, Direction.EAST, Direction.WEST, Direction.UP, Direction.SOUTH);
            assertLinks(h, left, Direction.EAST);
            assertLinks(h, right, Direction.WEST);
            assertLinks(h, top, Direction.DOWN);
            assertLinks(h, port, Direction.NORTH);
            h.setBlock(right, Blocks.AIR);
            assertLinks(h, middle, Direction.WEST, Direction.UP, Direction.SOUTH);
            h.assertTrue("none".equals(prop(h, middle, "fan")), "a lone casing shows a piece of a fan");
            h.succeed();
        });
    }

    /** Facing east, seen from outside: left is south (z 3), right is north (z 1). */
    private static final java.util.Map<BlockPos, String> FAN = java.util.Map.of(
        new BlockPos(4, 3, 3), "east_top_left", new BlockPos(4, 3, 2), "east_top", new BlockPos(4, 3, 1), "east_top_right",
        new BlockPos(4, 2, 3), "east_left", new BlockPos(4, 2, 1), "east_right",
        new BlockPos(4, 1, 3), "east_bottom_left", new BlockPos(4, 1, 2), "east_bottom", new BlockPos(4, 1, 1), "east_bottom_right");

    private static void assertFan(GameTestHelper h, boolean shown, boolean spinning) {
        for (var piece : FAN.entrySet()) {
            String value = prop(h, piece.getKey(), "fan");
            h.assertTrue(value.equals(shown ? piece.getValue() : "none"), "the casing at " + piece.getKey() + " shows fan=" + value
                + (shown ? ", not " + piece.getValue() : " on an unformed tunnel"));
            h.assertTrue(prop(h, piece.getKey(), "spinning").equals(String.valueOf(spinning)),
                "the fan piece at " + piece.getKey() + " has spinning=" + prop(h, piece.getKey(), "spinning") + ", not " + spinning);
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
            assertFan(h, true, false);
            h.assertTrue("none".equals(prop(h, EDGE, "fan")), "a casing off the front shows a piece of the fan");
            assertLinks(h, new BlockPos(2, 3, 2), Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST);
            h.setBlock(EDGE, Blocks.AIR);
            h.assertTrue("false".equals(prop(h, CONTROLLER, "formed")), "the controller of a broken tunnel still shows the hub");
            assertFan(h, false, false);
            h.setBlock(EDGE, casing);
            h.runAfterDelay(25, () -> {
                h.assertTrue(formed(h), "the whole tunnel did not form again: " + call(tunnel(h), "status"));
                assertFan(h, true, false);
                h.setBlock(CONTROLLER, Blocks.AIR);
                assertFan(h, false, false);
                h.succeed();
            });
        });
    }

    /**
     * Running, the controller is active and the 8 fan pieces spin; 2 seconds after the steam ran out (16,000 mB make 15 steps in
     * about 4 ticks) they stop and the controller is idle again.
     */
    @GameTest(batch = "round1139_tunnel", template = "empty", timeoutTicks = 120)
    public static void runningSteamTunnelSpinsItsFan(GameTestHelper h) {
        build(h);
        h.runAfterDelay(25, () -> {
            IFluidHandler port = h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, h.absolutePos(FLUID_PORT), Direction.NORTH);
            h.assertTrue(port != null && port.fill(steam(16_000), IFluidHandler.FluidAction.EXECUTE) == 16_000, "the fluid port took no steam");
            h.runAfterDelay(2, () -> {
                h.assertTrue("true".equals(prop(h, CONTROLLER, "active")), "the running tunnel is not active");
                assertFan(h, true, true);
                h.runAfterDelay(60, () -> {
                    h.assertTrue("false".equals(prop(h, CONTROLLER, "active")), "the tunnel is still active 3 seconds after its steam ran out");
                    assertFan(h, true, false);
                    h.succeed();
                });
            });
        });
    }

    private static void setLong(Object target, String name, long value) {
        try { var f = target.getClass().getDeclaredField(name); f.setAccessible(true); f.setLong(target, value); }
        catch (ReflectiveOperationException e) { throw new AssertionError(name, e); }
    }

    /** One tick of the controller, run here so that the power port does not take EU in between. */
    private static void tickTunnel(GameTestHelper h) {
        BlockEntity t = tunnel(h);
        try {
            t.getClass().getMethod("tick", net.minecraft.world.level.Level.class, BlockPos.class, BlockState.class, t.getClass())
                .invoke(null, h.getLevel(), t.getBlockPos(), t.getBlockState(), t);
        } catch (ReflectiveOperationException e) { throw new AssertionError("tick", e); }
    }

    /**
     * IC2 Classic's buffer rule: the tunnel runs while its 10,000 EU buffer has room for one step (512 EU) and then makes every step
     * the steam allows: 9,000 EU and 4,096 mB of steam become 11,048 EU in one tick (and 40 mB of water). At 9,600 EU it waits.
     */
    @GameTest(batch = "round1139_tunnel", template = "empty", timeoutTicks = 60)
    public static void steamTunnelBufferFollowsIc2Classic(GameTestHelper h) {
        build(h);
        h.runAfterDelay(25, () -> {
            h.assertTrue(formed(h), "the tunnel did not form: " + call(tunnel(h), "status"));
            BlockEntity t = tunnel(h);
            setLong(t, "energy", 9_000);
            ((IFluidHandler) t).fill(steam(4_096), IFluidHandler.FluidAction.EXECUTE);
            tickTunnel(h);
            h.assertTrue((Long) call(t, "energy") == 11_048, "9,000 EU + 4,096 mB of steam made " + call(t, "energy") + " EU, not 11,048");
            h.assertTrue(((FluidStack) call(t, "steam")).isEmpty(), "steam left: " + call(t, "steam"));
            h.assertTrue(((FluidStack) call(t, "water")).getAmount() == 40, "water returned: " + call(t, "water"));
            setLong(t, "energy", 9_600);
            ((IFluidHandler) t).fill(steam(1_024), IFluidHandler.FluidAction.EXECUTE);
            tickTunnel(h);
            h.assertTrue((Long) call(t, "energy") == 9_600 && ((FluidStack) call(t, "steam")).getAmount() == 1_024,
                "with 9,600 EU (no room for 512) the tunnel ran: " + call(t, "energy") + " EU, " + call(t, "steam"));
            h.succeed();
        });
    }

    /**
     * The fluid port opens the fluid reactor port's menu, whose slot takes a Fluid Ejector or Fluid Pulling Upgrade and nothing
     * else; with a Fluid Pulling Upgrade it draws steam from a tank next to it into the tunnel.
     */
    @GameTest(batch = "round1139_tunnel", template = "empty", timeoutTicks = 100)
    public static void steamTunnelFluidPortTakesFluidUpgrades(GameTestHelper h) {
        build(h);
        BlockPos tank = FLUID_PORT.north();
        h.setBlock(tank, BuiltInRegistries.BLOCK.get(id("build/block_iron_tank")));
        h.runAfterDelay(25, () -> {
            BlockEntity port = h.getBlockEntity(FLUID_PORT);
            var player = h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
            var menu = (net.minecraft.world.inventory.AbstractContainerMenu) call(port, "createMenu", 0, player.getInventory(), player);
            h.assertTrue(BuiltInRegistries.MENU.getKey(menu.getType()).equals(id("reactor_fluid_port_menu")),
                "the fluid port's menu is " + BuiltInRegistries.MENU.getKey(menu.getType()));
            var pulling = Round33GameTests.item("upgrade/fluid_pulling_upgrade");
            h.assertTrue(menu.getSlot(0).mayPlace(Round33GameTests.item("upgrade/fluid_ejector_upgrade")) && menu.getSlot(0).mayPlace(pulling),
                "the upgrade slot refuses the fluid upgrades");
            h.assertTrue(!menu.getSlot(0).mayPlace(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIRT)), "the upgrade slot takes dirt");
            IFluidHandler tankFluids = h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, h.absolutePos(tank), Direction.SOUTH);
            h.assertTrue(tankFluids != null && tankFluids.fill(steam(8_000), IFluidHandler.FluidAction.EXECUTE) > 0, "the tank took no steam");
            int before = tankFluids.getFluidInTank(0).getAmount();
            menu.getSlot(0).set(pulling);
            h.runAfterDelay(40, () -> {
                int after = tankFluids.getFluidInTank(0).getAmount();
                h.assertTrue(after < before, "the Fluid Pulling Upgrade moved no steam out of the tank: " + before + " -> " + after + " mB");
                h.succeed();
            });
        });
    }

    // ------------------------------------------------------------------ follow-ups: planting in the dark, reactor status

    private static Object field(Object target, String name) {
        for (Class<?> k = target.getClass(); k != null; k = k.getSuperclass()) {
            try { var f = k.getDeclaredField(name); f.setAccessible(true); return f.get(target); }
            catch (NoSuchFieldException e) { /* the superclass then */ }
            catch (ReflectiveOperationException e) { throw new AssertionError(name, e); }
        }
        throw new AssertionError("no field " + name + " on " + target.getClass());
    }

    /**
     * A crop stick on farmland at {@code at}, walled in with stone: around the stick and above it, and around and under the
     * farmland too (light comes up through farmland, which is not a full block). Light 0.
     */
    private static void darkStick(GameTestHelper h, BlockPos at) {
        h.setBlock(at.below(2), Blocks.STONE);
        for (Direction side : Direction.Plane.HORIZONTAL) {
            h.setBlock(at.below().relative(side), Blocks.STONE);
            h.setBlock(at.relative(side), Blocks.STONE);
        }
        h.setBlock(at.below(), Blocks.FARMLAND);
        h.setBlock(at, BuiltInRegistries.BLOCK.get(id("crop/stick")));
        h.setBlock(at.above(), Blocks.STONE);
    }

    private static net.minecraft.world.phys.BlockHitResult hitTop(GameTestHelper h, BlockPos at) {
        BlockPos abs = h.absolutePos(at);
        return new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(abs), Direction.UP, abs, false);
    }

    /**
     * Wheat seeds plant on a crop stick in the dark (IC2 plants a base seed whatever the light; the crop waits to grow). It used to
     * be refused at night or in shade without a word, so some sticks of a field "randomly" stayed empty. A seed bag still needs
     * the plant to be able to grow there now (IC2), and stays in the hand.
     */
    @GameTest(batch = "round1139_followups", template = "empty", timeoutTicks = 20)
    public static void baseSeedsPlantInTheDarkButSeedBagsWaitForLight(GameTestHelper h) {
        BlockPos first = new BlockPos(1, 2, 1), second = new BlockPos(3, 2, 3);
        darkStick(h, first);
        darkStick(h, second);
        h.runAfterDelay(5, () -> plantInTheDark(h, first, second));   // the light engine catches up with the new walls first
    }

    private static void plantInTheDark(GameTestHelper h, BlockPos first, BlockPos second) {
        var player = h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var crop = (com.miophas.singularity_iteration.core.api.crop.IPlanter) h.getBlockEntity(first);
        h.assertTrue(crop.getLightLevel() < 9, "the walled-in stick is not dark: light " + crop.getLightLevel());
        net.minecraft.world.item.ItemStack seeds = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.WHEAT_SEEDS, 4);
        h.getLevel().getBlockState(h.absolutePos(first)).useItemOn(seeds, h.getLevel(), player, net.minecraft.world.InteractionHand.MAIN_HAND, hitTop(h, first));
        h.assertTrue(crop.getPlant() != null, "wheat seeds were refused on a stick at light " + crop.getLightLevel());
        h.assertTrue(seeds.getCount() == 3, "planting took " + (4 - seeds.getCount()) + " seeds, not 1");
        var bag = (net.minecraft.world.item.ItemStack) call(h.getBlockEntity(first), "makeSeeds", crop.getPlant(), 1, 1, 1, 1);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, bag);
        var result = bag.useOn(new net.minecraft.world.item.context.UseOnContext(player, net.minecraft.world.InteractionHand.MAIN_HAND, hitTop(h, second)));
        var other = (com.miophas.singularity_iteration.core.api.crop.IPlanter) h.getBlockEntity(second);
        h.assertTrue(other.getPlant() == null && bag.getCount() == 1, "a seed bag planted in the dark (" + result + ")");
        h.succeed();
    }

    private static final BlockPos REACTOR = new BlockPos(2, 2, 2);

    private static net.minecraft.world.item.ItemStack moxQuad(GameTestHelper h) {
        var tag = new net.minecraft.nbt.CompoundTag();
        tag.putString("id", "mio_icif:reactor/item_reactor_mox_quad");
        tag.putInt("count", 1);
        var durability = new net.minecraft.nbt.CompoundTag();
        durability.putInt("remaining_uses", 10_000); durability.putInt("max_uses", 10_000); durability.putInt("tick_counter", 0);
        var components = new net.minecraft.nbt.CompoundTag();
        components.put("mio_icif:fuel_rod_durability", durability);
        tag.put("components", components);
        return net.minecraft.world.item.ItemStack.parse(h.getLevel().registryAccess(), tag).orElseThrow();
    }

    private static int status(GameTestHelper h) { return (Integer) call(h.getBlockEntity(REACTOR), "statusCode"); }

    private static long generation(GameTestHelper h) {
        return ((com.miophas.singularity_iteration.core.api.reactor.IReactorController) h.getBlockEntity(REACTOR)).getCurrentEnergyGeneration();
    }

    /**
     * The reactor of a player's report: 36 quad MOX rods in columns 1, 3, 4, 6, 7, 9 and 18 Component Heat Exchangers in columns
     * 2, 5, 8 of a reactor with six chambers, at 5,567 hull heat. Without a redstone signal it makes nothing and now says so
     * (status off); with a redstone block next to a chamber it makes about 12,391 EU/t (MOX at 5,567 / 10,000 heat).
     */
    @GameTest(batch = "round1139_followups", template = "empty", timeoutTicks = 100)
    public static void moxReactorOfTheReportRunsOnlyWithRedstoneAndSaysWhy(GameTestHelper h) {
        h.setBlock(REACTOR, BuiltInRegistries.BLOCK.get(id("generator/block_nuclear_reactor_generator")));
        for (Direction side : Direction.values()) h.setBlock(REACTOR.relative(side), BuiltInRegistries.BLOCK.get(id("reactor/block_reactor_chamber")));
        h.runAfterDelay(5, () -> {
            var reactor = (com.miophas.singularity_iteration.core.api.reactor.IReactorController) h.getBlockEntity(REACTOR);
            h.assertTrue(reactor.getAvailableColumns() == 9, "six chambers give " + reactor.getAvailableColumns() + " columns");
            var exchanger = BuiltInRegistries.ITEM.get(id("reactor/item_reactor_heat_switch_spread"));
            for (int slot = 0; slot < 54; slot++) {
                int col = slot % 9;
                var stack = col == 1 || col == 4 || col == 7 ? new net.minecraft.world.item.ItemStack(exchanger) : moxQuad(h);
                h.assertTrue(reactor.getItemHandler().insertItem(slot, stack, false).isEmpty(), "slot " + slot + " refused " + stack);
            }
            call(field(h.getBlockEntity(REACTOR), "heat"), "setHeat", 5_567L);
            h.runAfterDelay(25, () -> {
                h.assertTrue(status(h) == 1, "without a redstone signal the status is " + status(h) + ", not 1 (off)");
                h.assertTrue(generation(h) == 0, "the reactor made " + generation(h) + " EU/t without a redstone signal");
                h.setBlock(REACTOR.east(2), Blocks.REDSTONE_BLOCK);   // powers the east chamber
                h.runAfterDelay(25, () -> {
                    long eu = generation(h);
                    int running = status(h);
                    h.setBlock(REACTOR.east(2), Blocks.AIR);   // off again before the exchangers fill up
                    h.assertTrue(running == 0, "the running reactor reports status " + running + ", not 0");
                    h.assertTrue(eu >= 12_380 && eu <= 12_400, "the MOX reactor of the report made " + eu + " EU/t, not about 12,391");
                    h.succeed();
                });
            });
        });
    }

    /**
     * Fluid mode makes no EU: MOX doubles its heat above half the maximum hull heat (here 6,000 against 4,000), and the vented heat
     * goes into hot coolant. In generator mode the same rod makes EU and does not double its heat.
     */
    @GameTest(batch = "round1139_followups", template = "empty", timeoutTicks = 20)
    public static void moxInFluidModeMakesHotCoolantAndDoublesItsHeat(GameTestHelper h) {
        var parts = new com.miophas.singularity_iteration.core.runtime.reactor.ReactorCycle.Part[54];
        parts[10] = new com.miophas.singularity_iteration.core.runtime.reactor.ReactorCycle.Part(
            com.miophas.singularity_iteration.core.runtime.reactor.ReactorCycle.Profile.fuel(4, true), 0, 10_000);
        parts[11] = new com.miophas.singularity_iteration.core.runtime.reactor.ReactorCycle.Part(
            com.miophas.singularity_iteration.core.runtime.reactor.ReactorCycle.Profile.vent(1_000, 6, 0), 0, 0);
        var cool = com.miophas.singularity_iteration.core.runtime.reactor.ReactorCycle.step(parts.clone(), 9, 4_000, true, true);
        var hot = com.miophas.singularity_iteration.core.runtime.reactor.ReactorCycle.step(parts.clone(), 9, 6_000, true, true);
        h.assertTrue(cool.euPerTick() == 0 && hot.euPerTick() == 0, "fluid mode made EU: " + cool.euPerTick() + " / " + hot.euPerTick());
        h.assertTrue(hot.generatedHeat() == 2 * cool.generatedHeat(),
            "MOX above half heat made " + hot.generatedHeat() + " heat, not twice " + cool.generatedHeat());
        var generator = com.miophas.singularity_iteration.core.runtime.reactor.ReactorCycle.step(parts.clone(), 9, 6_000, true, false);
        h.assertTrue(generator.euPerTick() > 0 && generator.generatedHeat() == cool.generatedHeat(),
            "generator mode: " + generator.euPerTick() + " EU/t, " + generator.generatedHeat() + " heat");
        var fluid = com.miophas.singularity_iteration.core.runtime.reactor.FluidReactorCycle.step(parts.clone(), 9, 6_000, true, 10_000, 0, 10_000,
            com.miophas.singularity_iteration.core.runtime.reactor.FluidReactorCycle.COOLANT_HU_PER_MB);
        h.assertTrue(fluid.converted() > 0 && fluid.cycle().euPerTick() == 0, "the vented heat made " + fluid.converted() + " mB of hot coolant");
        h.succeed();
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
