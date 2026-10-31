package example.sicore;

import com.miophas.singularity_iteration.core.api.multiblock.StructureBlueprint;
import com.miophas.singularity_iteration.core.api.multiblock.StructureBlueprintRegistry;
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
import net.minecraft.world.level.block.state.properties.DirectionProperty;
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
}
