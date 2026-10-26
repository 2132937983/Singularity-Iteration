package example.sicore;

import io.netty.buffer.Unpooled;
import java.lang.reflect.Constructor;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * GameTests of 0.1.10.21: the exchange of the futures machine (M2-M5). A player listing trades between two players
 * with escrow, fee and self-trade prevention; a market commission fills the treasury and mints its reward; futures hold
 * their collateral; the saved data and the panel view survive a round trip (a hostile view is refused); the craft depth
 * follows the recipes.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
@SuppressWarnings("unchecked")
public final class Round1121GameTests {
    private Round1121GameTests() {}

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

    private static void set(Object target, String field, Object value) {
        try {
            target.getClass().getField(field).set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(field, e);
        }
    }

    private static Object make(String name, Object... args) {
        try {
            Constructor<?> c = common(name).getDeclaredConstructors()[0];
            c.setAccessible(true);
            return c.newInstance(args);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(name, e);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Object constant(String name, String value) { return Enum.valueOf((Class) common(name), value); }

    @SuppressWarnings("unchecked")
    private static <T> T field(Object target, String name) { return (T) get(target, name); }

    private static Object data(MinecraftServer server) { return call(common("exchange.ExchangeData"), "get", server); }

    private static Item coin() { return BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("mio_icif", "normal/item_coin")); }

    private static int count(ServerPlayer player, Item item) {
        int n = 0;
        for (ItemStack s : player.getInventory().items) if (s.is(item)) n += s.getCount();
        return n;
    }

    private static void expect(GameTestHelper h, Object result, boolean ok, String key, String what) {
        String got = (String) call(result, "key");
        h.assertTrue((boolean) call(result, "ok") == ok && got.endsWith("." + key), what + ": " + got);
    }

    /** A vanilla item that is not a datapack commodity and has no UU value (so the listing band is wide). */
    private static String freeItem(MinecraftServer server) {
        Class<?> manager = common("future.FutureCommodityManager");
        for (String id : List.of("minecraft:barrier", "minecraft:structure_void", "minecraft:debug_stick", "minecraft:nautilus_shell")) {
            if (!(boolean) call(manager, "hasCommodity", id)) return id;
        }
        throw new AssertionError("no free test item");
    }

    private static void leave(MinecraftServer server, ServerPlayer... players) {
        for (ServerPlayer p : players) server.getPlayerList().remove(p);
    }

    // ------------------------------------------------------------------ M3 order book, M2 escrow

    /**
     * A resting sale of one player fills a later purchase of another at the resting price; the buyer pays exactly that,
     * the seller is paid minus the fee into the mailbox, a purchase against one's own order does not fill, the
     * cancelled rest goes back to the mailbox, and the operator audit shows the pair.
     */
    @GameTest(batch = "round1121_exchange", template = "empty", timeoutTicks = 20)
    public static void listingTradesBetweenPlayersWithEscrow(GameTestHelper h) {
        MinecraftServer server = h.getLevel().getServer();
        Object d = data(server);
        String id = freeItem(server);
        Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(id));
        Map<String, Object> listings = field(d, "listings");
        Object listing = make("exchange.ExchangeData$Listing");
        set(listing, "item", id);
        set(listing, "currency", constant("exchange.ExchangeData$Currency", "COIN"));
        set(listing, "reference", 10L);
        listings.put(id, listing);
        ServerPlayer seller = h.makeMockServerPlayerInLevel(), buyer = h.makeMockServerPlayerInLevel();
        try {
            Object market = call(service(), "market", server, id);
            h.assertTrue(market != null && !(boolean) call(market, "system"), "the listing is not a player market");
            long price = Math.max((long) call(market, "low"), Math.min((long) call(market, "high"), 10L));
            h.assertTrue(price <= 200, "the test item has a narrow band: " + price);
            seller.getInventory().add(new ItemStack(item, 4));
            expect(h, call(service(), "place", seller, id, false, true, price, 4), true, "order_rested", "resting sale");
            h.assertTrue(count(seller, item) == 0, "the sale did not hold the items");
            int paid = (int) (price * 3);
            buyer.getInventory().add(new ItemStack(coin(), paid + 7));
            expect(h, call(service(), "place", buyer, id, true, false, price, 3), true, "bought", "purchase");
            h.assertTrue(count(buyer, item) == 3, "the buyer got " + count(buyer, item) + " items");
            h.assertTrue(count(buyer, coin()) == 7, "the buyer paid " + (paid + 7 - count(buyer, coin())) + " instead of " + paid);
            Object box = call(d, "mailbox", seller.getUUID());
            long fee = (long) Math.ceil(paid * (double) call(get(common("Singularity_Iteration_Config"), "EXCHANGE_FEE"), "get"));
            h.assertTrue((long) get(box, "coins") == paid - fee, "the seller was paid " + get(box, "coins") + " instead of " + (paid - fee));
            // a purchase against one's own resting order does not fill
            seller.getInventory().add(new ItemStack(coin(), (int) price));
            expect(h, call(service(), "place", seller, id, true, false, price, 1), false, "no_match", "purchase against one's own order");
            h.assertTrue(count(seller, item) == 0 && count(seller, coin()) == price, "the seller traded with their own order");
            Object book = ((Map<String, Object>) field(d, "books")).get(id);
            Collection<?> orders = (Collection<?>) call(get(book, "book"), "orders");
            h.assertTrue(orders.size() == 1 && (int) call(orders.iterator().next(), "remaining") == 1, "the rest of the sale is not in the book");
            long orderId = (long) call(orders.iterator().next(), "id");
            expect(h, call(service(), "cancel", buyer, orderId), false, "not_yours", "cancel by another player");
            expect(h, call(service(), "cancel", seller, orderId), true, "cancelled", "cancel");
            Map<String, Long> items = field(box, "items");
            h.assertTrue(items.getOrDefault(id, 0L) == 1, "the cancelled rest is not in the mailbox");
            long[] total = new long[1];
            List<?> pairs = (List<?>) call(service(), "auditPairs", server, 5, total);
            String a = seller.getUUID().toString(), b = buyer.getUUID().toString();
            h.assertTrue(pairs.stream().anyMatch(p -> {
                String x = (String) call(p, "a"), y = (String) call(p, "b");
                return (x.equals(a) && y.equals(b) || x.equals(b) && y.equals(a)) && (long) call(p, "quantity") == 3;
            }), "the audit does not show the trading pair");
        } finally {
            listings.remove(id);
            ((Map<?, ?>) field(d, "books")).remove(id);
            ((Map<?, ?>) field(d, "trades")).remove(id);
            ((Map<?, ?>) field(d, "bars")).remove(id);
            ((Map<?, ?>) field(d, "mail")).remove(seller.getUUID());
            ((Map<?, ?>) field(d, "mail")).remove(buyer.getUUID());
            leave(server, seller, buyer);
        }
        h.succeed();
    }

    // ------------------------------------------------------------------ M2 treasury, system commissions

    /**
     * While the market's own commission for an item is open, the market does not sell it; deliveries go into the
     * treasury, and the coin reward is minted into the ledger.
     */
    @GameTest(batch = "round1121_exchange", template = "empty", timeoutTicks = 20)
    public static void marketCommissionStocksTheTreasury(GameTestHelper h) {
        MinecraftServer server = h.getLevel().getServer();
        List<?> commodities = (List<?>) call(common("future.FutureCommodityManager"), "getCommodities");
        if (commodities.isEmpty()) { h.succeed(); return; }
        String id = (String) call(commodities.get(0), "getItemId");
        Item item = (Item) call(commodities.get(0), "getItem");
        Object d = data(server);
        long day = (long) call(service(), "today", server);
        Map<String, Long> treasury = field(d, "treasury");
        long stock = treasury.getOrDefault(id, 0L);
        long minted = (long) get(call(d, "ledger", day), "minted");
        Object c = make("exchange.ExchangeData$Commission");
        long cid = (long) call(d, "newId");
        set(c, "id", cid);
        set(c, "system", true);
        set(c, "item", id);
        set(c, "quantity", 4);
        set(c, "kind", constant("exchange.ExchangeData$RewardKind", "COIN"));
        set(c, "rewardTotal", 40L);
        set(c, "day", day);
        set(c, "expiry", day + 3);
        List<Object> commissions = field(d, "commissions");
        commissions.add(c);
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        try {
            h.assertTrue((long) call(service(), "supply", d, id, day) == 0, "the market sells an item while it has its own commission for it");
            player.getInventory().add(new ItemStack(item, 4));
            expect(h, call(service(), "deliver", player, cid, 4), true, "delivered", "delivery");
            h.assertTrue(count(player, item) == 0 && count(player, coin()) == 40, "the reward was not paid");
            h.assertTrue(treasury.getOrDefault(id, 0L) == stock + 4, "the treasury did not take the items");
            h.assertTrue((long) get(call(d, "ledger", day), "minted") == minted + 40, "the reward is not in the ledger");
            h.assertTrue(!commissions.contains(c), "the filled commission stays on the board");
            expect(h, call(service(), "deliver", player, cid, 1), false, "no_commission", "delivery to a filled commission");
        } finally {
            commissions.remove(c);
            treasury.put(id, stock);
            ((Map<?, ?>) field(d, "mail")).remove(player.getUUID());
            leave(server, player);
        }
        h.succeed();
    }

    // ------------------------------------------------------------------ M5 futures

    /** Opening a contract takes its collateral (contract price times quantity) from the inventory. */
    @GameTest(batch = "round1121_exchange", template = "empty", timeoutTicks = 20)
    public static void futuresHoldTheirCollateral(GameTestHelper h) {
        MinecraftServer server = h.getLevel().getServer();
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        Object d = data(server);
        List<Object> positions = field(d, "positions");
        int before = positions.size();
        try {
            Object market = null;
            for (Object c : (List<?>) call(common("future.FutureCommodityManager"), "getCommodities")) {
                if (!(boolean) call(common("future.FutureCommodityManager"), "isUnlocked", player, c)) continue;
                Object m = call(service(), "market", server, (String) call(c, "getItemId"));
                if (m != null && (market == null || (long) call(m, "price") < (long) call(market, "price"))) market = m;
            }
            if (market == null || (long) call(market, "price") > 400) { h.succeed(); return; }   // no cheap unlocked commodity
            long price = (long) call(service(), "futuresPrice", server, market, true, 3);
            long shortPrice = (long) call(service(), "futuresPrice", server, market, false, 3);
            h.assertTrue(price > 0 && price <= 1000 && shortPrice < price, "contract prices " + price + " / " + shortPrice);
            h.assertTrue((long) call(service(), "futuresPrice", server, market, true, 3) == price, "the quote changed within the day");
            int have = (int) (price * 2 + 3);
            player.getInventory().add(new ItemStack(coin(), have));
            expect(h, call(service(), "openFuture", player, (String) call(market, "id"), true, 2, 3), true, "future_opened", "open");
            h.assertTrue(count(player, coin()) == 3, "collateral taken: " + (have - count(player, coin())) + ", contract " + price * 2);
            h.assertTrue(positions.size() == before + 1, "no position");
            expect(h, call(service(), "openFuture", player, (String) call(market, "id"), true, 2, 5), false, "bad_quantity", "5-day contract");
        } finally {
            positions.removeIf(p -> player.getUUID().equals(get(p, "owner")));
            leave(server, player);
        }
        h.succeed();
    }

    // ------------------------------------------------------------------ persistence and the panel view

    /** The saved data keeps every part: save, load and save again give the same tag. */
    @GameTest(batch = "round1121_exchange", template = "empty", timeoutTicks = 20)
    public static void exchangeDataRoundTrip(GameTestHelper h) {
        var provider = h.getLevel().registryAccess();
        Class<?> type = common("exchange.ExchangeData");
        Object d = call(type, "load", new CompoundTag(), provider);
        UUID someone = UUID.randomUUID();
        ((Map<String, Long>) field(d, "treasury")).put("minecraft:iron_ingot", 12L);
        Object box = call(d, "mailbox", someone);
        set(box, "coins", 30L);
        set(box, "uu", 250L);
        ((Map<String, Long>) field(box, "items")).put("minecraft:stone", 5L);
        Object book = call(d, "book", "minecraft:iron_ingot", constant("exchange.ExchangeData$Currency", "COIN"));
        call(get(book, "book"), "rest", make("exchange.OrderBook$Order", 7L, someone.toString(), "", true, 9L, 3, 1L));
        Object c = make("exchange.ExchangeData$Commission");
        set(c, "id", 8L); set(c, "requester", someone); set(c, "item", "minecraft:stone"); set(c, "quantity", 16);
        set(c, "kind", constant("exchange.ExchangeData$RewardKind", "UU")); set(c, "rewardTotal", 100L); set(c, "expiry", 4L);
        ((List<Object>) field(d, "commissions")).add(c);
        Object l = make("exchange.ExchangeData$Listing");
        set(l, "item", "minecraft:piston"); set(l, "currency", constant("exchange.ExchangeData$Currency", "UU")); set(l, "reference", 40L);
        ((Map<String, Object>) field(d, "listings")).put("minecraft:piston", l);
        set(call(d, "ledger", 3L), "minted", 55L);
        CompoundTag first = (CompoundTag) call(d, "save", new CompoundTag(), provider);
        Object again = call(type, "load", first.copy(), provider);
        CompoundTag second = (CompoundTag) call(again, "save", new CompoundTag(), provider);
        h.assertTrue(first.equals(second), "the exchange data changed in a save / load round trip");
        h.assertTrue(((Map<?, ?>) field(again, "listings")).containsKey("minecraft:piston"), "listing lost");
        h.succeed();
    }

    /** The panel view decodes to what was sent; a view that claims more rows than allowed is refused. */
    @GameTest(batch = "round1121_exchange", template = "empty", timeoutTicks = 20)
    public static void panelViewCodec(GameTestHelper h) {
        MinecraftServer server = h.getLevel().getServer();
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        try {
            List<?> markets = (List<?>) call(service(), "markets", server);
            String selected = markets.isEmpty() ? "" : (String) call(markets.get(0), "id");
            Object view = call(common("exchange.ExchangeViews"), "build", player, selected, true);
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
            call(view, "write", buf);
            Object back = call(common("exchange.ExchangeView"), "read", buf);
            h.assertTrue(buf.readableBytes() == 0, "bytes left after the view");
            h.assertTrue(((List<?>) call(back, "markets")).size() == ((List<?>) call(view, "markets")).size(), "market rows");
            Object detail = call(back, "detail");
            h.assertTrue(selected.isEmpty() ? detail == null : detail != null && selected.equals(call(detail, "id")), "detail");
            FriendlyByteBuf hostile = new FriendlyByteBuf(Unpooled.buffer());
            hostile.writeVarLong(1); hostile.writeVarLong(0); hostile.writeVarLong(0); hostile.writeVarLong(0);
            hostile.writeVarInt(0); hostile.writeBoolean(true); hostile.writeVarInt(1_000_000);
            boolean refused = false;
            try {
                call(common("exchange.ExchangeView"), "read", hostile);
            } catch (AssertionError | RuntimeException e) {
                refused = true;
            }
            h.assertTrue(refused, "a view with a million rows was read");
        } finally {
            leave(server, player);
        }
        h.succeed();
    }

    // ------------------------------------------------------------------ craft depth (currency choice)

    /** Planks are deeper than logs; a piston (planks, cobblestone, smelted iron, redstone) is at least two steps. */
    @GameTest(batch = "round1121_exchange", template = "empty", timeoutTicks = 20)
    public static void craftDepthFollowsRecipes(GameTestHelper h) {
        MinecraftServer server = h.getLevel().getServer();
        Class<?> index = common("exchange.CraftDepthIndex");
        int log = (int) call(index, "depth", server, Items.OAK_LOG);
        int planks = (int) call(index, "depth", server, Items.OAK_PLANKS);
        int piston = (int) call(index, "depth", server, Items.PISTON);
        h.assertTrue(planks > log && piston > log && piston >= 2, "depths log " + log + ", planks " + planks + ", piston " + piston);
        h.succeed();
    }
}
