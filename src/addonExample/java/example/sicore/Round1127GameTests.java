package example.sicore;

import com.miophas.singularity_iteration.core.api.exchange.BountyNeed;
import com.miophas.singularity_iteration.core.api.exchange.BountyReward;
import com.miophas.singularity_iteration.core.api.exchange.BountySpec;
import com.miophas.singularity_iteration.core.api.exchange.BountySplit;
import com.miophas.singularity_iteration.core.api.exchange.BountyTier;
import com.miophas.singularity_iteration.core.api.exchange.ExchangeApi;
import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractEnergyBlockEntity;
import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * GameTests of 0.1.10.27 (X2): the settlement slot (a shulker box sells at the curve's payout, delivers to a bounty, takes
 * purchases) and the Trade Terminal (pipes fill the sell buffer and empty the output buffer only; the auction order of an
 * offline owner; instant orders wait for the owner; the access levels; breaking sends the buffers to the owner's mailbox).
 * Internals are reached by reflection.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
@SuppressWarnings("unchecked")
public final class Round1127GameTests {
    private Round1127GameTests() {}

    private static Object call(Object target, String name, Object... args) { return Round33GameTests.call(target, name, args); }

    private static Class<?> common(String name) { return Round33GameTests.common(name); }

    private static Class<?> service() { return common("exchange.ExchangeService"); }

    private static Object get(Object target, String field) {
        try {
            if (target instanceof Class<?> type) return type.getField(field).get(null);
            return target.getClass().getField(field).get(target);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(field, e);
        }
    }

    private static Object make(String name, Object... args) {
        try {
            // 0.1.10.29: by the number of arguments (TerminalRules.Order has a second constructor)
            Constructor<?> c = null;
            for (Constructor<?> k : common(name).getDeclaredConstructors()) if (k.getParameterCount() == args.length && c == null) c = k;
            if (c == null) throw new AssertionError(name + ": no constructor of " + args.length + " arguments");
            c.setAccessible(true);
            return c.newInstance(args);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(name, e);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Object constant(String name, String value) { return Enum.valueOf((Class) common(name), value); }

    private static <T> T field(Object target, String name) { return (T) get(target, name); }

    private static Object data(MinecraftServer server) { return call(common("exchange.ExchangeData"), "get", server); }

    private static ResourceLocation rl(String path) { return ResourceLocation.fromNamespaceAndPath("mio_icif", path); }

    private static Item coin() { return BuiltInRegistries.ITEM.get(rl("normal/item_coin")); }

    private static int count(ServerPlayer player, Item item) {
        int n = 0;
        for (ItemStack s : player.getInventory().items) if (s.is(item)) n += s.getCount();
        return n;
    }

    private static long coins(Object d, ServerPlayer player) {
        Object box = ((Map<UUID, Object>) field(d, "mail")).get(player.getUUID());
        return count(player, coin()) + (box == null ? 0 : (long) get(box, "coins"));
    }

    private static void expect(GameTestHelper h, Object result, boolean ok, String key, String what) {
        String got = (String) call(result, "key");
        h.assertTrue((boolean) call(result, "ok") == ok && got.endsWith("." + key), what + ": " + got);
    }

    /** A datapack commodity without an unlock advancement, priced at most {@code maxPrice} and at least half its base price. */
    private static Object pick(MinecraftServer server, long maxPrice) {
        Class<?> manager = common("future.FutureCommodityManager");
        for (Object c : (List<?>) call(manager, "getCommodities")) {
            if (call(c, "getUnlockAdvancement") != null) continue;
            Object m = call(service(), "market", server, (String) call(c, "getItemId"));
            if (m == null || !(boolean) call(m, "system")) continue;
            long price = (long) call(m, "price");
            if (price > maxPrice || price * 2 < (int) call(c, "getBasePrice")) continue;
            Object book = call(data(server), "book", (String) call(m, "id"), constant("exchange.ExchangeData$Currency", "COIN"));
            if ((long) call(get(book, "book"), "best", true) != 0 || (long) call(get(book, "book"), "best", false) != 0) continue;
            return m;
        }
        return null;
    }

    private static void restore(Object d, String id, Double pressure, long stock, UUID... players) {
        Map<String, Double> p = field(d, "pressure");
        if (pressure == null) p.remove(id); else p.put(id, pressure);
        ((Map<String, Long>) field(d, "treasury")).put(id, stock);
        for (UUID player : players) {
            ((Map<?, ?>) field(d, "sold")).remove(player);
            ((Map<?, ?>) field(d, "allowance")).remove(player);
            ((Map<?, ?>) field(d, "mail")).remove(player);
            ((List<Object>) field(d, "auction")).removeIf(a -> player.equals(get(a, "owner")));
        }
    }

    private static ItemStack shulker(ItemStack... contents) {
        ItemStack box = new ItemStack(Items.SHULKER_BOX);
        box.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(contents)));
        return box;
    }

    private static int inBox(ItemStack box, Item item) {
        int n = 0;
        ItemContainerContents c = box.get(DataComponents.CONTAINER);
        if (c != null) for (ItemStack s : c.nonEmptyItems()) if (s.is(item)) n += s.getCount();
        return n;
    }

    // ------------------------------------------------------------------ settlement slot

    /** A shulker box sells its commodity at the payout of the curve; other items stay in it; it opens as an item container. */
    @GameTest(batch = "round1127_settle", template = "empty", timeoutTicks = 20)
    public static void settlementSellsFromAShulkerBox(GameTestHelper h) {
        MinecraftServer server = h.getLevel().getServer();
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        Object d = data(server);
        Object m = pick(server, 160);
        if (m == null) { server.getPlayerList().remove(player); h.succeed(); return; }
        String id = (String) call(m, "id");
        Item item = (Item) call(m, "item");
        Double p0 = ((Map<String, Double>) field(d, "pressure")).get(id);
        long stock = ((Map<String, Long>) field(d, "treasury")).getOrDefault(id, 0L);
        try {
            SimpleContainer slot = new SimpleContainer(1);
            slot.setItem(0, shulker(new ItemStack(item, 64), new ItemStack(Items.BARRIER, 3)));
            h.assertTrue((boolean) call(common("exchange.SettlementStorages"), "accepts", slot.getItem(0)), "a shulker box is not accepted");
            h.assertTrue(!(boolean) call(common("exchange.SettlementStorages"), "accepts", new ItemStack(Items.DIRT)), "dirt is accepted");
            Object st = call(common("exchange.SettlementStorages"), "open", slot, 0);
            h.assertTrue(st != null && (int) call(st, "kind") == 1, "the shulker box does not open as an item container");
            Object curve = call(service(), "curve", m);
            double p = p0 == null ? 0 : p0;
            long now = (long) call(common("exchange.SellDepth"), "maxForItemPrice", curve, 0.0, p, 1.0, 64L);
            long paid = (long) Math.floor((double) call(common("exchange.SellDepth"), "payout", curve, 0.0, p, (double) now));
            long before = coins(d, player);
            expect(h, call(common("exchange.Settlement"), "sell", player, st, false, 0), true, "box_sold", "settlement sold now");
            h.assertTrue(coins(d, player) - before == paid, "paid " + (coins(d, player) - before) + ", the curve pays " + paid);
            h.assertTrue(inBox(slot.getItem(0), item) == 64 - now, "the box keeps " + inBox(slot.getItem(0), item) + ", expected " + (64 - now));
            h.assertTrue(inBox(slot.getItem(0), Items.BARRIER) == 3, "the item that sells as nothing left the box");
        } finally {
            restore(d, id, p0, stock, player.getUUID());
            server.getPlayerList().remove(player);
        }
        h.succeed();
    }

    /** A shulker box delivers its plain items to a system bounty; the reward goes to the player. */
    @GameTest(batch = "round1127_settle", template = "empty", timeoutTicks = 20)
    public static void settlementDeliversToABounty(GameTestHelper h) {
        MinecraftServer server = h.getLevel().getServer();
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        Object d = data(server);
        long id = ExchangeApi.postBounty(server, new BountySpec(BountyTier.ADVANCED, List.of(new BountyNeed("minecraft:cobblestone", 8, 0)),
            List.of(BountyReward.coins(300)), 0, BountySplit.FIRST, 0));
        try {
            h.assertTrue(id > 0, "the bounty was not posted");
            SimpleContainer slot = new SimpleContainer(1);
            slot.setItem(0, shulker(new ItemStack(Items.COBBLESTONE, 10)));
            Object st = call(common("exchange.SettlementStorages"), "open", slot, 0);
            long before = coins(d, player);
            Object r = call(common("exchange.Settlement"), "deliverBounty", player, st, id);
            h.assertTrue((boolean) call(r, "ok"), "delivery refused: " + call(r, "key"));
            h.assertTrue(inBox(slot.getItem(0), Items.COBBLESTONE) == 2, "the box keeps " + inBox(slot.getItem(0), Items.COBBLESTONE) + " of 10");
            h.assertTrue(coins(d, player) - before == 300, "reward " + (coins(d, player) - before));
        } finally {
            ExchangeApi.cancelBounty(server, id);
            ((Map<UUID, Object>) field(d, "mail")).remove(player.getUUID());
            server.getPlayerList().remove(player);
        }
        h.succeed();
    }

    /** A purchase goes into the container; it is paid with the coins the player carries, as a manual purchase. */
    @GameTest(batch = "round1127_settle", template = "empty", timeoutTicks = 20)
    public static void settlementBuysIntoTheContainer(GameTestHelper h) {
        MinecraftServer server = h.getLevel().getServer();
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        Object d = data(server);
        Object m = pick(server, 160);
        if (m == null) { server.getPlayerList().remove(player); h.succeed(); return; }
        String id = (String) call(m, "id");
        Item item = (Item) call(m, "item");
        Double p0 = ((Map<String, Double>) field(d, "pressure")).get(id);
        long stock = ((Map<String, Long>) field(d, "treasury")).getOrDefault(id, 0L);
        try {
            ((Map<String, Long>) field(d, "treasury")).put(id, 20L);
            player.getInventory().add(new ItemStack(coin(), 64));
            player.getInventory().add(new ItemStack(coin(), 64));
            long limit = (long) call(service(), "askOne", d, m) + 50;
            SimpleContainer slot = new SimpleContainer(1);
            slot.setItem(0, shulker());
            Object st = call(common("exchange.SettlementStorages"), "open", slot, 0);
            long before = coins(d, player);
            Object r = call(common("exchange.Settlement"), "buyInto", player, st, id, limit, 4);
            h.assertTrue((boolean) call(r, "ok"), "purchase refused: " + call(r, "key"));
            h.assertTrue(inBox(slot.getItem(0), item) == 4, "the box holds " + inBox(slot.getItem(0), item) + " of 4");
            h.assertTrue(count(player, item) == 0, "bought items went to the inventory");
            h.assertTrue(coins(d, player) < before, "nothing was paid");
        } finally {
            restore(d, id, p0, stock, player.getUUID());
            server.getPlayerList().remove(player);
        }
        h.succeed();
    }

    // ------------------------------------------------------------------ Trade Terminal

    private static BlockEntity terminal(GameTestHelper h, BlockPos pos) {
        h.setBlock(pos, BuiltInRegistries.BLOCK.get(rl("producer/block_trade_terminal")));
        BlockEntity be = h.getBlockEntity(pos);
        h.assertTrue(be != null && be.getClass().getSimpleName().equals("TradeTerminalBlockEntity"), "no terminal block entity: " + be);
        ((AbstractEnergyBlockEntity) be).getEnergyStorageInternal().setEnergy(10_000);
        return be;
    }

    private static Object order(String side, String item, long amount, String mode, long limit) {
        return make("exchange.TerminalRules$Order", constant("exchange.TerminalRules$Side", side), item, amount,
            constant("exchange.TerminalRules$Mode", mode), limit, true);
    }

    /** Pipes put commodities into the sell buffer (nothing else), take nothing out of it, and empty the output buffer. */
    @GameTest(batch = "round1127_terminal", template = "empty", timeoutTicks = 20)
    public static void terminalBuffersForPipes(GameTestHelper h) {
        MinecraftServer server = h.getLevel().getServer();
        Object m = pick(server, 1_000_000);
        if (m == null) { h.succeed(); return; }
        Item item = (Item) call(m, "item");
        BlockPos pos = new BlockPos(1, 1, 1);
        BlockEntity be = terminal(h, pos);
        IItemHandler handler = h.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, h.absolutePos(pos), null);
        h.assertTrue(handler != null && handler.getSlots() == 36, "no item handler of 36 slots");
        h.assertTrue(handler.insertItem(0, new ItemStack(item, 10), false).isEmpty(), "the sell buffer refused a commodity");
        h.assertTrue(handler.insertItem(1, new ItemStack(Items.BARRIER, 1), false).getCount() == 1, "the sell buffer took a non-commodity");
        h.assertTrue(handler.extractItem(0, 10, false).isEmpty(), "a pipe took items out of the sell buffer");
        h.assertTrue(handler.insertItem(27, new ItemStack(item, 1), false).getCount() == 1, "a pipe put items into the output buffer");
        ((Container) call(be, "outputBuffer")).setItem(0, new ItemStack(item, 5));
        h.assertTrue(handler.extractItem(27, 5, false).getCount() == 5, "the output buffer did not give its items");
        h.setBlock(pos, Blocks.AIR);
        h.succeed();
    }

    /**
     * While the owner is offline the auction sell order joins the owner's daily auction order with the buffer above the kept
     * amount; an instant order waits for the owner.
     */
    @GameTest(batch = "round1127_terminal", template = "empty", timeoutTicks = 20)
    public static void terminalRunsTheAuctionForAnOfflineOwner(GameTestHelper h) {
        MinecraftServer server = h.getLevel().getServer();
        Object d = data(server);
        Object m = pick(server, 1_000_000);
        if (m == null) { h.succeed(); return; }
        String id = (String) call(m, "id");
        Item item = (Item) call(m, "item");
        BlockPos pos = new BlockPos(1, 1, 1);
        BlockEntity be = terminal(h, pos);
        ServerPlayer owner = h.makeMockServerPlayerInLevel();
        UUID uuid = owner.getUUID();
        call(be, "setOwnerFromPlacer", owner);
        server.getPlayerList().remove(owner);   // the owner goes offline
        List<Object> auction = field(d, "auction");
        try {
            Container sell = (Container) call(be, "sellBuffer");
            sell.setItem(0, new ItemStack(item, 64));
            sell.setItem(1, new ItemStack(item, 16));
            h.assertTrue((boolean) call(be, "setOrder", -1, order("SELL", id, 10, "AUCTION", 0)), "order not set");
            call(be, "runOrders", h.getLevel());
            h.assertTrue("OK".equals(((Enum<?>) call(be, "status", 0)).name()), "status " + call(be, "status", 0));
            Object mine = null;
            for (Object a : auction) if (uuid.equals(get(a, "owner")) && id.equals(get(a, "item"))) mine = a;
            h.assertTrue(mine != null && (long) get(mine, "quantity") == 70, "no auction order of 70: " + (mine == null ? null : get(mine, "quantity")));
            int left = 0;
            for (int i = 0; i < sell.getContainerSize(); i++) if (sell.getItem(i).is(item)) left += sell.getItem(i).getCount();
            h.assertTrue(left == 10, "the buffer keeps " + left + " of the kept 10");
            // an instant order waits for the owner
            sell.setItem(2, new ItemStack(item, 64));
            call(be, "setOrder", 0, order("SELL", id, 10, "INSTANT", 1));
            call(be, "runOrders", h.getLevel());
            h.assertTrue("OFFLINE".equals(((Enum<?>) call(be, "status", 0)).name()), "instant status " + call(be, "status", 0));
        } finally {
            auction.removeIf(a -> uuid.equals(get(a, "owner")));
            ((Map<?, ?>) field(d, "mail")).remove(uuid);
            h.setBlock(pos, Blocks.AIR);
        }
        h.succeed();
    }

    /** Owner, listed players, strangers; breaking the terminal sends its buffers to the owner's mailbox. */
    @GameTest(batch = "round1127_terminal", template = "empty", timeoutTicks = 20)
    public static void terminalAccessAndBreaking(GameTestHelper h) {
        MinecraftServer server = h.getLevel().getServer();
        Object d = data(server);
        Object m = pick(server, 1_000_000);
        if (m == null) { h.succeed(); return; }
        String id = (String) call(m, "id");
        Item item = (Item) call(m, "item");
        BlockPos pos = new BlockPos(1, 1, 1);
        BlockEntity be = terminal(h, pos);
        ServerPlayer owner = h.makeMockServerPlayerInLevel(), friend = h.makeMockServerPlayerInLevel(), stranger = h.makeMockServerPlayerInLevel();
        UUID uuid = owner.getUUID();
        try {
            call(be, "setOwnerFromPlacer", owner);
            h.assertTrue((int) call(be, "levelOf", owner) == 4 && (int) call(be, "levelOf", stranger) == 0, "owner / stranger levels");
            h.assertTrue((boolean) call(be, "setAccess", friend.getUUID(), "friend", 2), "access not set");
            h.assertTrue((int) call(be, "levelOf", friend) == 2, "the listed player's level " + call(be, "levelOf", friend));
            h.assertTrue(!(boolean) call(be, "setAccess", uuid, "owner", 1), "the owner was put on the list");
            ((Container) call(be, "sellBuffer")).setItem(0, new ItemStack(item, 20));
            ((Container) call(be, "outputBuffer")).setItem(0, new ItemStack(item, 3));
            h.setBlock(pos, Blocks.AIR);
            Object box = ((Map<UUID, Object>) field(d, "mail")).get(uuid);
            long mailed = box == null ? 0 : ((Map<String, Long>) get(box, "items")).getOrDefault(id, 0L);
            h.assertTrue(mailed == 23, "the owner's mailbox got " + mailed + " of 23");
        } finally {
            ((Map<?, ?>) field(d, "mail")).remove(uuid);
            for (ServerPlayer p : List.of(owner, friend, stranger)) server.getPlayerList().remove(p);
        }
        h.succeed();
    }
}
