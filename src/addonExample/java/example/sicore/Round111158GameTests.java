package example.sicore;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * 0.1.11.158: the TESU wireless field. A wireless output port in a formed TESU fills machines with the wireless upgrade within
 * ±16 blocks every tick, from the controller (a machine without the upgrade gets nothing); relays carry the field step by step:
 * the connector links a relay only inside the cube of its target, a loop and a link to itself are refused, and a machine only a
 * relay covers is filled through the chain. The TESU holds 227,000,000,000 EU with 189 cells.
 *
 * <p>0.1.11.159: the connector works like the wireless node's remote (sneak + right-click sets the target, right-click a relay
 * links it to the target, the target stays), and the clicks go through the player's real block interaction
 * ({@code ServerPlayerGameMode.useItemOn}): in 0.1.11.158 the relay's status message took the click before the connector saw it,
 * which calling {@code useOn} directly never showed. A powered relay reports it (the client draws the particle stream from it).
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round111158GameTests {
    private Round111158GameTests() {}

    private static Block block(String path) { return BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("mio_icif", path)); }
    private static ItemStack item(String path) { return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("mio_icif", path))); }
    private static Object call(Object target, String name, Object... args) { return Round33GameTests.call(target, name, args); }
    private static Object storage(BlockEntity be) { return call(be, "getEnergyStorageInternal"); }
    private static long stored(BlockEntity be) { return (Long) call(storage(be), "getAmount"); }
    private static long capacity(BlockEntity be) { return (Long) call(storage(be), "getCapacity"); }

    /** An electric furnace at the world position {@code at}, with a wireless upgrade in its first upgrade slot if asked. */
    private static BlockEntity machine(GameTestHelper h, BlockPos at, boolean upgrade) {
        h.getLevel().setBlock(at, block("producer/block_furnace_elc").defaultBlockState(), 3);
        BlockEntity be = Round111157GameTests.be(h, at);
        if (upgrade) {
            IItemHandler items = (IItemHandler) call(be, "getItemHandler");
            int slot = (Integer) call(be, "getUpgradeSlotStart");
            ItemStack rest = items.insertItem(slot, item("upgrade/wireless_upgrade"), false);
            if (!rest.isEmpty()) throw new net.minecraft.gametest.framework.GameTestAssertException("the wireless upgrade did not fit the furnace");
        }
        return be;
    }

    /** A right click on the block at {@code at} with {@code connector}, as the game does it: the block first, then the item. */
    private static InteractionResult click(ServerPlayer player, ItemStack connector, BlockPos at, boolean sneak) {
        player.setShiftKeyDown(sneak);
        player.setItemInHand(InteractionHand.MAIN_HAND, connector);
        InteractionResult r = player.gameMode.useItemOn(player, player.serverLevel(), connector, InteractionHand.MAIN_HAND,
            new BlockHitResult(Vec3.atCenterOf(at), Direction.UP, at, false));
        player.setShiftKeyDown(false);
        return r;
    }

    @GameTest(template = "reactor_loop", batch = "round111158_field", timeoutTicks = 200)
    public static void wirelessPortFillsUpgradedMachines(GameTestHelper h) {
        BlockPos controllerAt = Round111157GameTests.controller(h);
        List<BlockPos> ports = Round111157GameTests.build(h, controllerAt, Direction.NORTH, 0, 0, 1);
        BlockEntity controller = Round111157GameTests.be(h, controllerAt);
        BlockPos corner = Round111157GameTests.corner(h);
        BlockEntity upgraded = machine(h, corner.offset(11, 1, 1), true), plain = machine(h, corner.offset(1, 1, 1), false);
        long[] start = new long[1];
        h.startSequence()
            .thenWaitUntil(() -> h.assertTrue((Boolean) call(controller, "isStructureComplete") && (Integer) call(controller, "wirelessCount") == 1,
                "the TESU with a wireless port did not form"))
            .thenExecute(() -> {
                h.assertTrue((Long) call(controller, "getEnergyCapacity") == 227_000_000_000L, "capacity " + call(controller, "getEnergyCapacity"));
                call(storage(controller), "setEnergy", 1_000_000_000L);
                start[0] = 1_000_000_000L;
            })
            .thenWaitUntil(() -> h.assertTrue(stored(upgraded) == capacity(upgraded) && capacity(upgraded) > 0,
                "the upgraded furnace holds " + stored(upgraded) + " of " + capacity(upgraded)))
            .thenExecute(() -> {
                h.assertTrue(stored(plain) == 0, "a furnace without the upgrade got " + stored(plain));
                h.assertTrue(start[0] - stored(controller) == stored(upgraded), "the TESU lost " + (start[0] - stored(controller)) + ", the furnace got " + stored(upgraded));
                h.assertTrue(ports.size() == 1 && call(Round111157GameTests.be(h, ports.get(0)), "controller") != null, "the wireless port is not bound");
            })
            .thenSucceed();
    }

    @GameTest(template = "reactor_loop", batch = "round111158_relay", timeoutTicks = 300)
    public static void relaysCarryTheFieldStepByStep(GameTestHelper h) {
        BlockPos controllerAt = Round111157GameTests.controller(h);
        BlockPos port = Round111157GameTests.build(h, controllerAt, Direction.NORTH, 0, 0, 1).get(0);
        BlockEntity controller = Round111157GameTests.be(h, controllerAt);
        // a column above the port: relay 1 at +14 (inside the port's cube), relay 2 at +28 (inside relay 1's), the machine at
        // +38 (only relay 2 covers it); relay 3 at 20 beside relay 1 (outside every cube)
        BlockPos r1 = port.above(14), r2 = port.above(28), m = port.above(38), r3 = r1.east(20);
        for (BlockPos p : new BlockPos[]{r1, r2, r3}) h.getLevel().setBlock(p, block("wiring/block_tesu_relay").defaultBlockState(), 3);
        BlockEntity relay1 = Round111157GameTests.be(h, r1), relay2 = Round111157GameTests.be(h, r2), relay3 = Round111157GameTests.be(h, r3);
        BlockEntity furnace = machine(h, m, true);
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        ItemStack connector = item("normal/item_tesu_connector");
        h.startSequence()
            .thenWaitUntil(() -> h.assertTrue((Boolean) call(controller, "isStructureComplete"), "the TESU did not form"))
            .thenExecute(() -> {
                call(storage(controller), "setEnergy", 1_000_000_000L);
                // no target yet: a click on a relay links nothing
                h.assertTrue(click(player, connector, r1, false) == InteractionResult.FAIL && call(relay1, "parent") == null, "a relay linked without a target");
                h.assertTrue(click(player, connector, m, true) == InteractionResult.FAIL
                    && call(Round33GameTests.common("item.normal.TesuConnectorItem"), "target", connector) == null, "the furnace became the target");
                h.assertTrue(click(player, connector, port, true) == InteractionResult.SUCCESS, "sneak-right-click on the port did not set the target");
                h.assertTrue(click(player, connector, r1, false) == InteractionResult.SUCCESS, "relay 1 did not link to the port");
                h.assertTrue(port.equals(call(relay1, "parent")), "relay 1 is linked to " + call(relay1, "parent"));
                // the target stays the port: relay 3, 20 blocks beside its cube, is out of reach
                h.assertTrue(click(player, connector, r3, false) == InteractionResult.FAIL && call(relay3, "parent") == null, "relay 3 linked from outside the port's cube");
                // relay 2 is outside the port's cube too; relay 1 as the target reaches it
                h.assertTrue(click(player, connector, r2, false) == InteractionResult.FAIL && call(relay2, "parent") == null, "relay 2 linked from 28 blocks away");
                h.assertTrue(click(player, connector, r1, true) == InteractionResult.SUCCESS, "sneak-right-click on relay 1 did not set the target");
                h.assertTrue(click(player, connector, r1, false) == InteractionResult.FAIL && port.equals(call(relay1, "parent")), "relay 1 linked to itself");
                h.assertTrue(click(player, connector, r2, false) == InteractionResult.SUCCESS && r1.equals(call(relay2, "parent")), "relay 2 did not link to relay 1");
                // relay 2 as the target: linking relay 1 to it would close a loop
                click(player, connector, r2, true);
                h.assertTrue(click(player, connector, r1, false) == InteractionResult.FAIL && port.equals(call(relay1, "parent")), "a loop was linked");
            })
            .thenWaitUntil(() -> h.assertTrue(stored(furnace) == capacity(furnace) && capacity(furnace) > 0,
                "the furnace 38 above the port (only relay 2 covers it) holds " + stored(furnace) + " of " + capacity(furnace)))
            .thenWaitUntil(() -> h.assertTrue((Boolean) call(relay1, "isPowered") && (Boolean) call(relay2, "isPowered"),
                "the linked relays do not report power (no particle stream)"))
            .thenExecute(() -> h.assertTrue(call(relay3, "feed") == null && !(Boolean) call(relay3, "isPowered"), "an unlinked relay is powered"))
            .thenSucceed();
    }
}
