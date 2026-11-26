package example.sicore;

import static example.sicore.Round33GameTests.call;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * 0.1.11.206: the wireless induction ports. The GESU's, a module on a face of the core, charges a player's battery nearby; the
 * TESU's, in a port slot, is a wireless port (the controller counts it) that also charges a player's battery and fills an
 * Electric Furnace without a wireless upgrade.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round111206GameTests {
    private Round111206GameTests() {}

    private static BlockState block(String id) { return BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("mio_icif", id)).defaultBlockState(); }

    /** An empty battery: the first battery item of the mod, drained to 0. */
    private static ItemStack emptyBattery() {
        var items = com.miophas.singularity_iteration.core.api.MioIcifAPI.instance().getItemAPI();
        for (Item item : BuiltInRegistries.ITEM) {
            if (!"mio_icif".equals(BuiltInRegistries.ITEM.getKey(item).getNamespace())) continue;
            ItemStack stack = new ItemStack(item);
            if (!items.isBattery(stack) || items.getBatteryCapacity(stack) < 100_000) continue;
            items.setBatteryEnergy(stack, 0);
            if (items.getBatteryStored(stack) == 0) return stack;
        }
        throw new net.minecraft.gametest.framework.GameTestAssertException("no battery item");
    }

    private static long charge(ItemStack battery) {
        return com.miophas.singularity_iteration.core.api.MioIcifAPI.instance().getItemAPI().getBatteryStored(battery);
    }

    /** A player standing at the world position {@code at} with an empty battery in the first inventory slot. */
    private static ServerPlayer playerWithBattery(GameTestHelper h, BlockPos at) {
        ServerPlayer p = ArmoryServiceGameTests.player(h);
        p.setPos(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        p.getInventory().setItem(9, emptyBattery());
        return p;
    }

    @GameTest(template = "reactor_loop", batch = "round111206_gesu", timeoutTicks = 200)
    public static void gesuInductionPortChargesPlayers(GameTestHelper h) {
        BlockPos core = new BlockPos(3, 2, 3);
        for (Direction d : Direction.values())
            h.setBlock(core.relative(d), block(d == Direction.UP ? "wiring/block_gesu_induction_port" : "wiring/block_gesu_input_iv"));
        h.setBlock(core, block("wiring/block_gesu_core"));
        ServerPlayer player = playerWithBattery(h, h.absolutePos(core.above(4)));
        h.startSequence()
            .thenWaitUntil(() -> h.assertTrue((Boolean) call(h.getBlockEntity(core), "isStructureComplete"), "the GESU did not form with the induction port"))
            .thenExecute(() -> call(call(h.getBlockEntity(core), "getEnergyStorageInternal"), "setStored", 50_000_000L))
            .thenWaitUntil(() -> h.assertTrue(charge(player.getInventory().getItem(9)) > 0, "the player's battery got nothing from the GESU induction port"))
            .thenExecute(() -> {
                h.assertTrue((Long) call(h.getBlockEntity(core), "getStoredEnergy") < 50_000_000L, "the GESU did not pay for the charge");
                player.discard();
            })
            .thenSucceed();
    }

    @GameTest(template = "reactor_loop", batch = "round111206_tesu", timeoutTicks = 300)
    public static void tesuInductionPortChargesPlayersAndPlainMachines(GameTestHelper h) {
        BlockPos controllerAt = Round111157GameTests.controller(h);
        List<BlockPos> ports = Round111157GameTests.build(h, controllerAt, Direction.NORTH, 0, 0, 1);
        h.getLevel().setBlock(ports.get(0), block("wiring/block_tesu_induction_port"), 3);   // the induction port in the wireless slot
        BlockEntity controller = Round111157GameTests.be(h, controllerAt);
        BlockPos corner = Round111157GameTests.corner(h);
        BlockPos furnaceAt = corner.offset(11, 1, 1);
        h.getLevel().setBlock(furnaceAt, block("producer/block_furnace_elc"), 3);   // no wireless upgrade
        BlockEntity furnace = Round111157GameTests.be(h, furnaceAt);
        ServerPlayer player = playerWithBattery(h, furnaceAt.above());   // in the open, inside the port's field
        h.startSequence()
            .thenWaitUntil(() -> h.assertTrue((Boolean) call(controller, "isStructureComplete") && (Integer) call(controller, "wirelessCount") == 1,
                "the TESU did not form with the induction port as its wireless port"))
            .thenExecute(() -> call(call(controller, "getEnergyStorageInternal"), "setEnergy", 1_000_000_000L))
            .thenWaitUntil(() -> {
                h.assertTrue((Long) call(call(furnace, "getEnergyStorageInternal"), "getAmount") > 0, "the furnace without an upgrade got nothing");
                h.assertTrue(charge(player.getInventory().getItem(9)) > 0, "the player's battery got nothing from the TESU induction port");
            })
            .thenExecute(() -> player.discard())
            .thenSucceed();
    }
}
