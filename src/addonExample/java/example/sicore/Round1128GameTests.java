package example.sicore;

import com.google.gson.JsonPrimitive;
import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractEnergyBlockEntity;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * GameTests of 0.1.10.28 (X4): the coin &lt;-&gt; UU board (a trade between two players moves UU-matter between the accounts
 * and coins minus the fee; a cancelled order gives the UU-matter back; the market never trades it), the scrap sink of the sell
 * box (coins as without it, the rest at the recycler's rate), the stabiliser (a week above the target lowers the factor and the
 * depths), the ID cards (a delegate card gives its level at the issuer's terminal, an edited card gives nothing, revoking ends
 * it; a personal card works only for its holder). Internals are reached by reflection.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
@SuppressWarnings("unchecked")
public final class Round1128GameTests {
    private Round1128GameTests() {}

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

    private static <T> T field(Object target, String name) { return (T) get(target, name); }

    private static Object data(MinecraftServer server) { return call(common("exchange.ExchangeData"), "get", server); }

    private static ResourceLocation rl(String path) { return ResourceLocation.fromNamespaceAndPath("mio_icif", path); }

    private static Item coin() { return BuiltInRegistries.ITEM.get(rl("normal/item_coin")); }

    private static Item card() { return BuiltInRegistries.ITEM.get(rl("normal/item_id_card")); }

    private static String uuMarket() { return (String) get(common("exchange.UuBoard"), "MARKET"); }

    private static int count(ServerPlayer player, Item item) {
        int n = 0;
        for (ItemStack s : player.getInventory().items) if (s.is(item)) n += s.getCount();
        return n;
    }

    private static Object box(Object d, UUID id) { return call(d, "mailbox", id); }

    private static long coins(Object d, ServerPlayer p) { return count(p, coin()) + (long) get(box(d, p.getUUID()), "coins"); }

    private static void give(ServerPlayer player, Item item, int n) {
        while (n > 0) {
            int s = Math.min(n, item.getDefaultMaxStackSize());
            player.getInventory().add(new ItemStack(item, s));
            n -= s;
        }
    }

    private static void forget(MinecraftServer server, Object d, ServerPlayer... players) {
        for (ServerPlayer p : players) {
            ((Map<?, ?>) field(d, "mail")).remove(p.getUUID());
            ((Map<?, ?>) field(d, "sold")).remove(p.getUUID());
            ((Map<?, ?>) field(d, "allowance")).remove(p.getUUID());
            ((Map<?, ?>) field(d, "lastSale")).remove(p.getUUID());
            ((Map<?, ?>) field(d, "cards")).remove(p.getUUID());
            server.getPlayerList().remove(p);
        }
    }

    // ------------------------------------------------------------------ the coin <-> UU board

    /** A sell order of lots rests on the UU account; a purchase moves UU-matter to the buyer and coins minus the fee to the seller. */
    @GameTest(batch = "round1128_uu", template = "empty", timeoutTicks = 20)
    public static void uuBoardTradesBetweenPlayers(GameTestHelper h) {
        MinecraftServer server = h.getLevel().getServer();
        Object d = data(server);
        ServerPlayer seller = h.makeMockServerPlayerInLevel(), buyer = h.makeMockServerPlayerInLevel();
        String id = uuMarket();
        try {
            Object m = call(service(), "market", server, id);
            h.assertTrue(m != null && !(boolean) call(m, "system"), "the UU board is missing or traded by the market");
            set(box(d, seller.getUUID()), "uu", 1_000L);
            give(buyer, coin(), 2_000);
            long sellerCoins = coins(d, seller), buyerCoins = coins(d, buyer);
            Object rest = call(service(), "place", seller, id, false, true, 300L, 5);
            h.assertTrue((boolean) call(rest, "ok"), "sell order: " + call(rest, "key"));
            h.assertTrue((long) get(box(d, seller.getUUID()), "uu") == 500, "the sell order did not hold 500 mB");
            Object bought = call(service(), "place", buyer, id, true, false, 300L, 5);
            h.assertTrue((boolean) call(bought, "ok"), "purchase: " + call(bought, "key"));
            h.assertTrue((long) get(box(d, buyer.getUUID()), "uu") == 500, "the buyer's account holds " + get(box(d, buyer.getUUID()), "uu"));
            h.assertTrue(buyerCoins - coins(d, buyer) == 1_500, "the buyer paid " + (buyerCoins - coins(d, buyer)));
            h.assertTrue(coins(d, seller) - sellerCoins == 1_485, "the seller got " + (coins(d, seller) - sellerCoins) + " (1,500 minus the 1 % fee)");
            Object none = call(service(), "place", buyer, id, true, false, 300L, 1);
            h.assertTrue(!(boolean) call(none, "ok") && coins(d, buyer) == buyerCoins - 1_500, "the market sold UU-matter or kept the escrow");
        } finally {
            forget(server, d, seller, buyer);
        }
        h.succeed();
    }

    /** A cancelled sell order of the board gives the UU-matter back to the account. */
    @GameTest(batch = "round1128_uu", template = "empty", timeoutTicks = 20)
    public static void uuBoardCancelGivesUuBack(GameTestHelper h) {
        MinecraftServer server = h.getLevel().getServer();
        Object d = data(server);
        ServerPlayer seller = h.makeMockServerPlayerInLevel();
        String id = uuMarket();
        try {
            set(box(d, seller.getUUID()), "uu", 700L);
            Object r = call(service(), "place", seller, id, false, true, 5_000L, 7);
            h.assertTrue((boolean) call(r, "ok") && (long) get(box(d, seller.getUUID()), "uu") == 0, "the order did not hold 700 mB");
            Object book = ((Map<String, Object>) field(d, "books")).get(id);
            long orderId = -1;
            for (Object o : (List<?>) call(get(book, "book"), "orders")) if (seller.getUUID().toString().equals(call(o, "owner"))) orderId = (long) call(o, "id");
            h.assertTrue(orderId > 0, "no resting order");
            Object c = call(service(), "cancel", seller, orderId);
            h.assertTrue((boolean) call(c, "ok") && (long) get(box(d, seller.getUUID()), "uu") == 700, "the UU-matter did not come back");
        } finally {
            forget(server, d, seller);
        }
        h.succeed();
    }

    // ------------------------------------------------------------------ the scrap sink

    /** The sell box with the sink pays the coins of a sale without it; the rest becomes the recycler's products. */
    @GameTest(batch = "round1128_scrap", template = "empty", timeoutTicks = 20)
    public static void scrapSinkKeepsTheCoins(GameTestHelper h) {
        MinecraftServer server = h.getLevel().getServer();
        Object d = data(server);
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        Object m = null;
        for (Object c : (List<?>) call(common("future.FutureCommodityManager"), "getCommodities")) {
            if (call(c, "getUnlockAdvancement") != null) continue;
            Object x = call(service(), "market", server, (String) call(c, "getItemId"));
            if (x != null && (long) call(x, "price") <= 12) { m = x; break; }
        }
        if (m == null) { forget(server, d, player); h.succeed(); return; }
        String id = (String) call(m, "id");
        Item item = (Item) call(m, "item");
        Map<String, Double> pressure = field(d, "pressure");
        Double p0 = pressure.get(id);
        long stock = ((Map<String, Long>) field(d, "treasury")).getOrDefault(id, 0L);
        try {
            SimpleContainer box = new SimpleContainer(18);
            for (int i = 0; i < 18; i++) box.setItem(i, new ItemStack(item, 64));
            Object curve = call(service(), "curve", m);
            double p = p0 == null ? 0 : p0;
            long take = (long) call(common("exchange.SellDepth"), "maxForItemPrice", curve, 0.0, p, 1.0, 1152L);
            long coinsExpected = (long) Math.floor((double) call(common("exchange.SellDepth"), "payout", curve, 0.0, p, (double) take));
            Object y = call(common("exchange.ScrapSink"), "yieldOf", server, new ItemStack(item));
            long before = coins(d, player);
            Object r = call(service(), "sellBox", player, box, false, 0, true);
            h.assertTrue((boolean) call(r, "ok"), "sale: " + call(r, "key"));
            h.assertTrue(coins(d, player) - before == coinsExpected, "coins " + (coins(d, player) - before) + ", without the sink " + coinsExpected);
            if (y != null) {
                ItemStack product = (ItemStack) call(y, "product");
                double chance = (double) call(y, "chance");
                long products = (long) Math.floor((1152 - take) * chance + 1e-9) * product.getCount();
                h.assertTrue(count(player, product.getItem()) + ((Map<String, Long>) get(box(d, player.getUUID()), "items"))
                    .getOrDefault(BuiltInRegistries.ITEM.getKey(product.getItem()).toString(), 0L) >= products, "fewer products than the recycler's rate");
                long left = 0;
                for (int i = 0; i < 18; i++) if (box.getItem(i).is(item)) left += box.getItem(i).getCount();
                h.assertTrue(left < Math.ceil(1 / chance), "the box keeps " + left + " items (fewer than one product's worth expected)");
            }
        } finally {
            if (p0 == null) pressure.remove(id); else pressure.put(id, p0);
            ((Map<String, Long>) field(d, "treasury")).put(id, stock);
            forget(server, d, player);
        }
        h.succeed();
    }

    // ------------------------------------------------------------------ the stabiliser

    /** With the stabiliser on, a week above the target lowers the factor and the depths of the curve. */
    @GameTest(batch = "round1128_stabilizer", template = "empty", timeoutTicks = 20)
    public static void stabilizerLowersTheDepths(GameTestHelper h) {
        MinecraftServer server = h.getLevel().getServer();
        Object d = data(server);
        Class<?> settings = common("exchange.EconomySettings");
        Map<String, JsonPrimitive> saved = new HashMap<>((Map<String, JsonPrimitive>) call(settings, "datapackValues"));
        double f0 = (double) get(d, "stabFactor");
        long supply0 = (long) get(d, "supply"), supplyDay0 = (long) get(d, "supplyDay");
        long day = (long) call(service(), "today", server);
        Object ledger = call(d, "ledger", day);
        long minted0 = (long) get(ledger, "minted");
        try {
            Map<String, JsonPrimitive> on = new HashMap<>(saved);
            on.put("stabilizerEnabled", new JsonPrimitive(true));
            call(settings, "apply", on);
            set(d, "stabFactor", 1.0);
            set(ledger, "minted", minted0 + 1_000_000_000L);
            call(service(), "stabilize", d, day);
            double f = (double) get(d, "stabFactor");
            h.assertTrue(f < 1.0 && f >= 0.5, "the factor did not fall: " + f);
            Object m = null;
            for (Object c : (List<?>) call(common("future.FutureCommodityManager"), "getCommodities")) {
                m = call(service(), "market", server, (String) call(c, "getItemId"));
                if (m != null) break;
            }
            if (m != null) {
                h.assertTrue(Math.abs((double) call(m, "depthFactor") - f) < 1e-9, "the market does not carry the factor");
                Object curve = call(service(), "curve", m);
                h.assertTrue((double) call(curve, "lp") < (int) call(settings, "sellDepthPlayer") + 1e-9, "the depth did not shrink");
            }
        } finally {
            set(ledger, "minted", minted0);
            set(d, "stabFactor", f0);
            set(d, "supply", supply0);
            set(d, "supplyDay", supplyDay0);
            call(settings, "apply", saved);
        }
        h.succeed();
    }

    // ------------------------------------------------------------------ ID cards

    private static BlockEntity terminal(GameTestHelper h, BlockPos pos) {
        h.setBlock(pos, BuiltInRegistries.BLOCK.get(rl("producer/block_trade_terminal")));
        BlockEntity be = h.getBlockEntity(pos);
        ((AbstractEnergyBlockEntity) be).getEnergyStorageInternal().setEnergy(10_000);
        return be;
    }

    /**
     * A delegate card written at the terminal gives its holder the level at the owner's terminal; an edited copy gives
     * nothing; revoking ends it. A personal card works only for its holder.
     */
    @GameTest(batch = "round1128_cards", template = "empty", timeoutTicks = 20)
    public static void cardsDelegateAndRevoke(GameTestHelper h) {
        MinecraftServer server = h.getLevel().getServer();
        Object d = data(server);
        ServerPlayer owner = h.makeMockServerPlayerInLevel(), friend = h.makeMockServerPlayerInLevel(), stranger = h.makeMockServerPlayerInLevel();
        BlockPos pos = new BlockPos(1, 1, 1);
        BlockEntity be = terminal(h, pos);
        Class<?> cards = common("exchange.IdCards");
        try {
            call(be, "setOwnerFromPlacer", owner);
            h.assertTrue((int) call(be, "levelOf", friend) == 0, "the friend has access before the card");
            owner.getInventory().add(new ItemStack(card()));
            Object r = call(cards, "issue", owner, friend.getGameProfile(), 2, 3);
            h.assertTrue((boolean) call(r, "ok"), "issue: " + call(r, "key"));
            ItemStack written = ItemStack.EMPTY;
            for (ItemStack s : owner.getInventory().items) if (s.is(card()) && call(cards, "data", s) != null) written = s;
            h.assertTrue(!written.isEmpty(), "no written card");
            ItemStack given = written.copy();
            written.setCount(0);
            friend.getInventory().add(given.copy());   // add() empties the stack it is given; keep `given` readable below
            h.assertTrue((int) call(be, "levelOf", friend) == 2, "the delegate card gives " + call(be, "levelOf", friend));
            // an edited copy (level 3, same signature) in the stranger's hand gives nothing
            Object data = call(cards, "data", given);
            DataComponentType<Object> type = (DataComponentType<Object>) BuiltInRegistries.DATA_COMPONENT_TYPE.get(rl("id_card"));
            ItemStack forged = given.copy();
            forged.set(type, withLevel(data, 3, stranger.getUUID()));
            stranger.getInventory().add(forged);
            h.assertTrue((int) call(be, "levelOf", stranger) == 0, "an edited card gave access");
            // revoke
            int n = (int) call(cards, "revoke", server, owner.getUUID(), -1L);
            h.assertTrue(n == 1 && (int) call(be, "levelOf", friend) == 0, "the revoked card still gives access");
            // a personal card
            ItemStack personal = (ItemStack) call(cards, "writePersonal", owner);
            owner.setItemInHand(InteractionHand.MAIN_HAND, personal);
            h.assertTrue(call(cards, "personalCard", owner) != null, "the owner's personal card is not accepted");
            stranger.setItemInHand(InteractionHand.MAIN_HAND, personal.copy());
            h.assertTrue(call(cards, "personalCard", stranger) == null, "a stranger used the owner's personal card");
        } finally {
            h.setBlock(pos, Blocks.AIR);
            forget(server, d, owner, friend, stranger);
        }
        h.succeed();
    }

    /** A copy of the card data with another level and holder, keeping the old signature. */
    private static Object withLevel(Object data, int level, UUID holder) {
        try {
            var c = common("exchange.IdCardData").getDeclaredConstructors()[0];
            return c.newInstance(call(data, "issuer"), call(data, "issuerName"), holder, "x", level, call(data, "expiry"), call(data, "serial"),
                call(data, "personal"), call(data, "signature"));
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("IdCardData", e);
        }
    }
}
