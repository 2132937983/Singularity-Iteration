package example.sicore;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * 0.1.11.167: the TESU relay is a pylon three blocks tall: the relay block is the base and the relay column (part middle and
 * top) stands on it. It needs the two blocks above free to be placed; a relay from before grows its column when it first ticks
 * (if there is room); breaking the head takes the whole pylon and drops one relay; a click on the shaft is a click on the relay;
 * the pylon lights while the relay is powered. It still needs no power of its own.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round111167GameTests {
    private Round111167GameTests() {}

    private static Block block(String path) { return BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("mio_icif", path)); }
    private static Item item(String path) { return BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("mio_icif", path)); }
    private static Object call(Object target, String name, Object... args) { return Round33GameTests.call(target, name, args); }

    private static Block relay() { return block("wiring/block_tesu_relay"); }
    private static Block column() { return block("wiring/block_tesu_relay_column"); }

    /** The column part ("middle" or "top") at the world position {@code at}, or what stands there instead. */
    private static String part(GameTestHelper h, BlockPos at) {
        BlockState state = h.getLevel().getBlockState(at);
        if (!state.is(column())) return String.valueOf(state);
        for (Property<?> p : state.getProperties())
            if (p.getName().equals("part")) return ((StringRepresentable) state.getValue(p)).getSerializedName();
        return "no part";
    }

    private static boolean pylonAt(GameTestHelper h, BlockPos base) {
        return h.getLevel().getBlockState(base).is(relay()) && part(h, base.above()).equals("middle") && part(h, base.above(2)).equals("top");
    }

    private static ServerPlayer player(GameTestHelper h) {
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        BlockPos away = h.absolutePos(new BlockPos(4, 1, 0));
        player.moveTo(away.getX() + 0.5, away.getY(), away.getZ() + 0.5);   // off the blocks the tests place
        return player;
    }

    private static InteractionResult use(ServerPlayer player, ItemStack stack, BlockPos at, Direction face, boolean sneak) {
        player.setShiftKeyDown(sneak);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        InteractionResult r = player.gameMode.useItemOn(player, player.serverLevel(), stack, InteractionHand.MAIN_HAND,
            new BlockHitResult(Vec3.atCenterOf(at), face, at, false));
        player.setShiftKeyDown(false);
        return r;
    }

    /** Placed by a player: the relay and its column; with a block two above the floor there is no room and nothing is placed. */
    @GameTest(template = "empty", batch = "round111167_pylon")
    public static void aPlacedRelayStandsThreeBlocksTall(GameTestHelper h) {
        ServerPlayer player = player(h);
        BlockPos floor = h.absolutePos(new BlockPos(2, 1, 2)), blockedFloor = h.absolutePos(new BlockPos(0, 1, 2));
        h.getLevel().setBlock(floor, Blocks.STONE.defaultBlockState(), 3);
        h.getLevel().setBlock(blockedFloor, Blocks.STONE.defaultBlockState(), 3);
        h.getLevel().setBlock(blockedFloor.above(3), Blocks.STONE.defaultBlockState(), 3);   // the head's place is taken
        ItemStack stack = new ItemStack(item("wiring/block_tesu_relay"), 2);
        use(player, stack, floor, Direction.UP, false);
        h.assertTrue(pylonAt(h, floor.above()), "a placed relay is " + h.getLevel().getBlockState(floor.above()) + " / " + part(h, floor.above(2))
            + " / " + part(h, floor.above(3)));
        h.assertTrue(stack.getCount() == 1, "placing took " + (2 - stack.getCount()) + " relays");
        use(player, stack, blockedFloor, Direction.UP, false);
        h.assertTrue(h.getLevel().getBlockState(blockedFloor.above()).isAir() && stack.getCount() == 1,
            "a relay was placed without room for its head: " + h.getLevel().getBlockState(blockedFloor.above()));
        h.succeed();
    }

    /** A relay set without its column (one from before) grows it when it ticks, if there is room; without room it stays a base. */
    @GameTest(template = "empty", batch = "round111167_pylon")
    public static void anOldRelayGrowsItsColumn(GameTestHelper h) {
        BlockPos free = h.absolutePos(new BlockPos(2, 1, 2)), roofed = h.absolutePos(new BlockPos(0, 1, 0));
        h.getLevel().setBlock(free, relay().defaultBlockState(), 3);
        h.getLevel().setBlock(roofed.above(2), Blocks.STONE.defaultBlockState(), 3);
        h.getLevel().setBlock(roofed, relay().defaultBlockState(), 3);
        h.succeedWhen(() -> {
            h.assertTrue(pylonAt(h, free), "the old relay did not grow its column: " + part(h, free.above()) + " / " + part(h, free.above(2)));
            h.assertTrue(h.getLevel().getBlockState(roofed).is(relay()) && h.getLevel().getBlockState(roofed.above()).isAir(),
                "the roofed relay: " + h.getLevel().getBlockState(roofed) + " / " + h.getLevel().getBlockState(roofed.above()));
        });
    }

    /**
     * Breaking the head takes the shaft and the base too: a survival player with a diamond pickaxe gets one relay, a creative one
     * none (the test's mock player counts as creative; the survival one is NeoForge's fake player).
     */
    @GameTest(template = "empty", batch = "round111167_pylon")
    public static void breakingTheHeadTakesThePylon(GameTestHelper h) {
        BlockPos survival = h.absolutePos(new BlockPos(0, 1, 0)), creative = h.absolutePos(new BlockPos(4, 1, 4));
        h.getLevel().setBlock(survival, relay().defaultBlockState(), 3);
        h.getLevel().setBlock(creative, relay().defaultBlockState(), 3);
        h.startSequence()
            .thenWaitUntil(() -> h.assertTrue(pylonAt(h, survival) && pylonAt(h, creative), "the columns did not grow"))
            .thenExecute(() -> {
                net.neoforged.neoforge.common.util.FakePlayer miner = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(h.getLevel());
                miner.setGameMode(GameType.SURVIVAL);
                miner.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_PICKAXE));
                miner.gameMode.destroyBlock(survival.above(2));
                player(h).gameMode.destroyBlock(creative.above(2));
                for (BlockPos base : new BlockPos[]{survival, creative})
                    for (int i = 0; i <= 2; i++)
                        h.assertTrue(h.getLevel().getBlockState(base.above(i)).isAir(), "after breaking the head, " + i + " above " + base + ": "
                            + h.getLevel().getBlockState(base.above(i)));
                h.assertItemEntityCountIs(item("wiring/block_tesu_relay"), new BlockPos(0, 1, 0), 1.5, 1);
                h.assertItemEntityCountIs(item("wiring/block_tesu_relay"), new BlockPos(4, 1, 4), 1.5, 0);
            })
            .thenSucceed();
    }

    /** The connector's click on the shaft links the relay at its base. */
    @GameTest(template = "empty", batch = "round111167_pylon")
    public static void aClickOnTheShaftLinksTheRelay(GameTestHelper h) {
        BlockPos port = h.absolutePos(new BlockPos(0, 1, 4)), base = h.absolutePos(new BlockPos(2, 1, 2));
        h.getLevel().setBlock(port, block("wiring/block_tesu_wireless_port").defaultBlockState(), 3);
        h.getLevel().setBlock(base, relay().defaultBlockState(), 3);
        ServerPlayer player = player(h);
        ItemStack connector = new ItemStack(item("normal/item_tesu_connector"));
        h.startSequence()
            .thenWaitUntil(() -> h.assertTrue(pylonAt(h, base), "the column did not grow"))
            .thenExecute(() -> {
                h.assertTrue(use(player, connector, port, Direction.UP, true) == InteractionResult.SUCCESS, "the port did not become the target");
                h.assertTrue(use(player, connector, base.above(), Direction.NORTH, false) == InteractionResult.SUCCESS, "the click on the shaft failed");
                BlockEntity relay = Round111157GameTests.be(h, base);
                h.assertTrue(port.equals(call(relay, "parent")), "the relay is linked to " + call(relay, "parent"));
            })
            .thenSucceed();
    }

    /** Linked to a formed TESU's wireless port, the pylon lights: the base and both column parts are powered. */
    @GameTest(template = "reactor_loop", batch = "round111167_lights", timeoutTicks = 300)
    public static void aPoweredPylonLights(GameTestHelper h) {
        BlockPos controllerAt = Round111157GameTests.controller(h);
        BlockPos port = Round111157GameTests.build(h, controllerAt, Direction.NORTH, 0, 0, 1).get(0);
        BlockPos base = port.above(14);
        h.getLevel().setBlock(base, relay().defaultBlockState(), 3);
        BlockEntity relay = Round111157GameTests.be(h, base);
        call(relay, "link", port);
        h.succeedWhen(() -> {
            h.assertTrue((Boolean) call(relay, "isPowered"), "the relay is not powered");
            for (int i = 0; i <= 2; i++) {
                BlockState state = h.getLevel().getBlockState(base.above(i));
                h.assertTrue(state.hasProperty(BlockStateProperties.POWERED) && state.getValue(BlockStateProperties.POWERED),
                    "the pylon is dark " + i + " above the base: " + state);
            }
        });
    }
}
