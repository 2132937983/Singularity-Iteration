// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.exchange;

import com.miophas.singularity_iteration.common.Singularity_Iteration_Config;
import com.miophas.singularity_iteration.common.future.FutureCommodity;
import com.miophas.singularity_iteration.common.future.FutureCommodityManager;
import com.miophas.singularity_iteration.common.future.FutureMarketData;
import com.miophas.singularity_iteration.common.future.FuturePriceModel;
import com.miophas.singularity_iteration.common.future.event.FuturePriceEvent;
import com.miophas.singularity_iteration.common.future.MarketQuote;
import com.miophas.singularity_iteration.common.item.normal.mio_icif_normal;
import com.miophas.singularity_iteration.common.exchange.ExchangeData.Commission;
import com.miophas.singularity_iteration.common.exchange.ExchangeData.Currency;
import com.miophas.singularity_iteration.common.exchange.ExchangeData.Mailbox;
import com.miophas.singularity_iteration.common.exchange.ExchangeData.RewardKind;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * 0.1.10.21 (futures M2-M5): the rules of the SI exchange. All methods run on the server thread.
 *
 * <h2>Principles (proposal 3.2)</h2>
 * <ul>
 *   <li><b>Conservation</b>: the market creates no items. It sells its treasury stock (the items it bought from
 *       players) and, when that is empty, a small daily import at a premium ({@code importPerDay}). It mints coins only
 *       when it buys (within the daily buy budget of 0.1.10.20), pays a system commission, or settles a winning futures
 *       contract; it destroys coins when it sells, through fees, and through losing contracts. The ledger counts both.</li>
 *   <li><b>Players trade with players</b> through the order book (zero-sum, fee destroyed) and commissions.</li>
 *   <li><b>The player is the unit</b> (UUID): limits, self-trade prevention and listing evidence do not depend on the
 *       number of machines.</li>
 * </ul>
 *
 * <h2>Escrow</h2>
 * Every action checks that the player has what it costs before it changes a book; the matching therefore cannot fail
 * half way. Results go to the inventory when they fit and to the mailbox otherwise (collect at any futures machine).
 */
public final class ExchangeService {
    private ExchangeService() {}

    /** 0.1.10.25 (X1): orders and commissions up to 100,000 items (before: 1,024); futures stay at 1,024. */
    public static final int ORDER_DAYS = 7, MAX_ORDERS_PER_PLAYER = 32, MAX_QUANTITY = 100_000, FUTURES_MAX_QUANTITY = 1024;
    public static final int MAX_COMMISSIONS_PER_PLAYER = 8, MAX_COMMISSIONS = 512, COMMISSION_MAX_DAYS = 7;
    public static final int TRADES_KEPT = 64, BARS_KEPT = 60, LEDGER_DAYS = 60, EVIDENCE_DAYS = 30, DELIST_DAYS = 30;
    public static final int MAX_POSITIONS = 8;
    public static final long BARTER_POST_FEE = 5;
    public static final double COMMISSION_FEE = 0.02;
    public static final String SYSTEM = "system";

    /** The result of an action: a message key with arguments for the action bar. */
    public record Result(boolean ok, String key, Object... args) {
        static Result fail(String key, Object... args) { return new Result(false, "message.mio_icif.exchange." + key, args); }
        static Result done(String key, Object... args) { return new Result(true, "message.mio_icif.exchange." + key, args); }
    }

    /**
     * A tradable market: a datapack commodity (the market quotes it), a player listing (only the book) or (0.1.10.28) the
     * coin &lt;-&gt; UU board ({@link UuBoard}). {@code depthFactor}: the stabiliser's factor of the sell depths (1 = none).
     * {@code bountyFloor} (0.1.10.33): the lowest ask per item while a bounty needs the item ({@link BountyBoard#rewardPerItem};
     * 0 = none). {@code bidCap} (0.1.10.35): the most the market bids for one item, the inputs of {@code made_from} at the
     * market's ask now ({@link BidLinks}; {@link BidLinks#NONE} = no cap).
     */
    public record Market(String id, Item item, Currency currency, boolean system, FutureCommodity commodity, long price,
                         double fundamental, int depth, long low, long high, double depthFactor, long bountyFloor, double bidCap) { }

    // ------------------------------------------------------------------ markets

    public static ExchangeData data(MinecraftServer server) { return ExchangeData.get(server); }

    public static Market market(MinecraftServer server, String id) {
        FutureCommodity c = FutureCommodityManager.getCommodityById(id);
        if (c != null && c.getItem() != Items.AIR) {
            int price = FutureMarketData.get(server.overworld()).getPrice(id, c.getBasePrice());
            long low = Math.max(1, Math.round(c.getBasePrice() * Singularity_Iteration_Config.FUTURE_MIN_PRICE_MULTIPLIER.get()));
            long high = Math.max(low, Math.round(c.getBasePrice() * Singularity_Iteration_Config.FUTURE_MAX_PRICE_MULTIPLIER.get()));
            return new Market(id, c.getItem(), Currency.COIN, true, c, price, Valuation.fundamental(uuBuckets(server, c.getItem()),
                coinsPerBucket(server)), CraftDepthIndex.depth(server, c.getItem()), low, high, depthFactor(server), bountyFloor(server, id),
                bidCap(server, c));
        }
        // 0.1.10.28 (X4): the coin <-> UU board (players only; the market never trades UU-matter)
        if (UuBoard.is(id)) {
            if (!EconomySettings.uuBoard()) return null;
            long reference = UuBoard.reference(coinsPerBucket(server));
            return new Market(id, com.miophas.singularity_iteration.common.block.environment.fluid.mio_icif_fluids.UUMATTER_BUCKET.get(),
                Currency.COIN, false, null, lastPrice(data(server), id, reference), reference, 0, 1, UuBoard.HIGH, 1.0, 0, BidLinks.NONE);
        }
        ExchangeData.Listing l = data(server).listings.get(id);
        if (l == null) return null;
        Item item = item(id);
        if (item == null) return null;
        double fundamental = fundamentalIn(server, item, l.currency);
        long low = fundamental > 0 ? Math.max(1, Math.round(fundamental * Valuation.BAND_LOW)) : 1;
        long high = fundamental > 0 ? Math.max(low, Math.round(fundamental * Valuation.BAND_HIGH)) : Long.MAX_VALUE / 4;
        return new Market(id, item, l.currency, false, null, lastPrice(data(server), id, l.reference), fundamental,
            CraftDepthIndex.depth(server, item), low, high, 1.0, 0, BidLinks.NONE);
    }

    /**
     * 0.1.10.35: the cap of the bid of a commodity with {@code made_from}: its inputs at the market's ask now (the price of each
     * input commodity x (1 + spread / 2)); {@link BidLinks#NONE} without inputs or when an input is no commodity.
     */
    static double bidCap(MinecraftServer server, FutureCommodity c) {
        if (c == null || c.madeFrom().isEmpty()) return BidLinks.NONE;
        FutureMarketData fm = FutureMarketData.get(server.overworld());
        double spread = EconomySettings.spread();
        return BidLinks.cap(c.madeFrom(), input -> {
            FutureCommodity ic = FutureCommodityManager.getCommodityById(input);
            return ic == null ? 0 : BidLinks.askUnit(fm.getPrice(input, ic.getBasePrice()), spread);
        });
    }

    /** 0.1.10.35: the market's bid of one item of {@code c} at {@code price} before sell pressure (bid_price, then the cap). */
    public static double bidUnit(MinecraftServer server, FutureCommodity c, int price) {
        return BidLinks.bid(MarketQuote.bidUnit(price, EconomySettings.spread(), c.bidRatio()), bidCap(server, c));
    }

    /**
     * 0.1.10.33: the lowest ask per item of a commodity while an open NORMAL or ADVANCED bounty needs it: what the bounty pays
     * per item ({@link BountyBoard#rewardPerItem}; 0 = no such bounty). Before, the market did not sell the item at all while
     * the bounty was open (an emerald bounty stopped the emerald market until it was done or expired).
     */
    static long bountyFloor(MinecraftServer server, String id) {
        ExchangeData d = data(server);
        if (d.board.bounties.isEmpty()) return 0;
        return d.board.rewardPerItem(id, Bounties.prices(server));
    }

    /** All markets: the datapack commodities, then the player listings. */
    public static List<Market> markets(MinecraftServer server) {
        List<Market> out = new ArrayList<>();
        for (FutureCommodity c : FutureCommodityManager.getCommodities()) { Market m = market(server, c.getItemId()); if (m != null) out.add(m); }
        for (String id : data(server).listings.keySet()) { Market m = market(server, id); if (m != null) out.add(m); }
        // 0.1.10.28 (X4)
        Market uu = market(server, UuBoard.MARKET);
        if (uu != null) out.add(uu);
        return out;
    }

    static Item item(String id) {
        ResourceLocation rl = ResourceLocation.tryParse(id);
        if (rl == null || !BuiltInRegistries.ITEM.containsKey(rl)) return null;
        Item item = BuiltInRegistries.ITEM.get(rl);
        return item == Items.AIR ? null : item;
    }

    static String id(Item item) { return BuiltInRegistries.ITEM.getKey(item).toString(); }

    public static double uuBuckets(MinecraftServer server, Item item) {
        var q = com.miophas.singularity_iteration.core.runtime.uu.UuQuoteBook.quote(server, new ItemStack(item));
        return q == null ? 0 : q.buckets();
    }

    private static long calibratedGeneration = Long.MIN_VALUE;
    private static double calibrated;

    /** Coins per bucket of UU-matter, calibrated on the datapack commodities ({@link Valuation#coinsPerBucket}). */
    public static double coinsPerBucket(MinecraftServer server) {
        long generation = com.miophas.singularity_iteration.core.runtime.uu.UuQuoteBook.generation(server);
        if (generation != calibratedGeneration) {
            List<double[]> pairs = new ArrayList<>();
            for (FutureCommodity c : FutureCommodityManager.getCommodities()) pairs.add(new double[] {c.getBasePrice(), uuBuckets(server, c.getItem())});
            calibrated = Valuation.coinsPerBucket(pairs);
            calibratedGeneration = generation;
        }
        return calibrated;
    }

    /** Fundamental value of one item in the currency (coins, or mB of UU-matter). */
    static double fundamentalIn(MinecraftServer server, Item item, Currency currency) {
        double buckets = uuBuckets(server, item);
        return currency == Currency.UU ? buckets * 1000.0 : Valuation.fundamental(buckets, coinsPerBucket(server));
    }

    static long lastPrice(ExchangeData d, String id, long fallback) {
        var q = d.trades.get(id);
        return q == null || q.isEmpty() ? fallback : q.peekLast().price();
    }

    // ------------------------------------------------------------------ system quotes (0.1.10.20 M1 + treasury)

    /**
     * 0.1.10.25 (X1): items {@code player} may still buy from the market today ({@link Integer#MAX_VALUE} without a limit).
     * Before 0.1.10.25 buying and selling shared one limit of 100 items a day.
     */
    public static int buyLeft(MinecraftServer server, UUID player) {
        int cap = EconomySettings.buyItemsPerDay();
        if (cap <= 0) return Integer.MAX_VALUE;
        int used = FutureMarketData.get(server.overworld()).getPlayerVolume(player);
        return Math.max(0, cap - used);
    }

    /** 0.1.10.25: the daily import of a commodity (datapack {@code import_per_day}, else the config). */
    static int importPerDay(String id) {
        FutureCommodity c = FutureCommodityManager.getCommodityById(id);
        int v = c == null ? -1 : c.getMarket().importPerDay();
        return v >= 0 ? v : EconomySettings.importPerDay();
    }

    static double importPremium(Market m) {
        double v = m.commodity() == null ? -1 : m.commodity().getMarket().importPremium();
        return v >= 0 ? v : EconomySettings.importPremium();
    }

    /**
     * Items the market can sell today: treasury stock plus the rest of the daily import. None while the market's own
     * commission for the item is open (the system commissions of 0.1.10.21-0.1.10.25 left in a save): its reward is above
     * the market's ask, so buying from the market to deliver would create coins (players' orders still trade).
     * 0.1.10.33: an open NORMAL or ADVANCED bounty no longer stops the sales; it raises the ask to its reward per item
     * instead ({@link Market#bountyFloor}, {@link #systemCost}).
     */
    public static long supply(ExchangeData d, String id, long day) {
        if (d.importedDay != day) { d.imported.clear(); d.importedDay = day; d.setDirty(); }
        for (Commission c : d.commissions) if (c.system && c.item.equals(id)) return 0;
        // 0.1.10.35: a sell_only commodity (raw ores) is bought by the market, never sold
        FutureCommodity fc = FutureCommodityManager.getCommodityById(id);
        if (fc != null && fc.sellOnly()) return 0;
        long stock = d.treasury.getOrDefault(id, 0L);
        int importLeft = Math.max(0, importPerDay(id) - d.imported.getOrDefault(id, 0));
        return stock + importLeft;
    }

    /**
     * Coins that buying {@code n} from the market costs: stock at the purchase price, imports with the premium; 0.1.10.33: at
     * least the bounty floor per item while a bounty needs the item ({@link Market#bountyFloor}).
     */
    static long systemCost(ExchangeData d, Market m, int n) {
        long stock = d.treasury.getOrDefault(m.id(), 0L);
        int fromStock = (int) Math.min(n, stock), imported = n - fromStock;
        double spread = EconomySettings.spread();
        long cost = MarketQuote.buyCost((int) m.price(), fromStock, spread);
        if (imported > 0) cost += (long) Math.ceil(MarketQuote.buyCost((int) m.price(), imported, spread) * (1 + importPremium(m)));
        if (m.bountyFloor() > 0 && n > 0) cost = Math.max(cost, Math.multiplyExact(m.bountyFloor(), (long) n));
        return cost;
    }

    /** 0.1.10.25: the ask of one item now (treasury price, or the import price when the treasury is empty). */
    public static long askOne(ExchangeData d, Market m) { return systemCost(d, m, 1); }

    // ------------------------------------------------------------------ 0.1.10.25 (X1): sell depth, allowance

    /**
     * The bid curve of a datapack commodity at its price now (datapack depths, else the config); 0.1.10.28: the depths scaled by
     * the stabiliser's factor.
     */
    public static SellDepth.Curve curve(Market m) {
        FutureCommodity.Market cm = m.commodity() == null ? FutureCommodity.Market.DEFAULT : m.commodity().getMarket();
        int lp = cm.depthPlayer() > 0 ? cm.depthPlayer() : EconomySettings.sellDepthPlayer();
        int lg = cm.depthServer() > 0 ? cm.depthServer() : EconomySettings.sellDepthServer();
        // 0.1.10.34: the bid follows the price at the commodity's bid_price / base_price; 0.1.10.35: at most the inputs' ask
        double ratio = m.commodity() == null ? 1.0 : m.commodity().bidRatio();
        double unit = BidLinks.bid(MarketQuote.bidUnit(m.price(), EconomySettings.spread(), ratio), m.bidCap());
        return Stabilizer.scale(new SellDepth.Curve(unit, lp, lg), m.depthFactor());
    }

    /** 0.1.10.28 (X4): the stabiliser's factor now (1 when it is off). */
    public static double depthFactor(MinecraftServer server) {
        return EconomySettings.stabilizerRules().enabled() ? data(server).stabFactor : 1.0;
    }

    static double exportShare(String id) {
        FutureCommodity c = FutureCommodityManager.getCommodityById(id);
        double v = c == null ? -1 : c.getMarket().exportPerDay();
        return v > 0 ? v : EconomySettings.sellExportPerDay();
    }

    /** Items sold of {@code id} that the market has not passed on yet. */
    public static double pressure(ExchangeData d, String id) { return d.pressure.getOrDefault(id, 0.0); }

    /** Items {@code player} sold of {@code id} to the market today (read only: no new day is started). */
    public static long soldToday(ExchangeData d, UUID player, String id, long day) {
        if (d.soldDay != day) return 0;
        Map<String, Long> m = d.sold.get(player);
        return m == null ? 0 : m.getOrDefault(id, 0L);
    }

    /** The bid of the next item for {@code player} now. */
    public static long nextBid(ExchangeData d, Market m, UUID player, long day) {
        return (long) Math.floor(SellDepth.marginal(curve(m), soldToday(d, player, m.id(), day), pressure(d, m.id())));
    }

    /** The coin allowance applies: config above 0, and in single player only when switched on there. */
    static boolean allowanceOn(MinecraftServer server) {
        if (EconomySettings.sellAllowancePerDay() <= 0) return false;
        boolean single = server.isSingleplayer() && !server.isPublished();
        return !single || EconomySettings.sellAllowanceInSingleplayer();
    }

    /** Coins {@code player} may still receive from the market today ({@link Long#MAX_VALUE} without a limit). */
    public static long allowanceLeft(MinecraftServer server, ExchangeData d, UUID player, long day) {
        if (!allowanceOn(server)) return Long.MAX_VALUE;
        // 0.1.10.28 (X4): scaled by the stabiliser's factor
        long perDay = Stabilizer.allowance(EconomySettings.sellAllowancePerDay(), depthFactor(server));
        long[] a = d.allowance.get(player);
        if (a == null) return perDay;
        return SellDepth.allowance(a[0], a[1], day, perDay, EconomySettings.sellAllowanceDays());
    }

    static void spendAllowance(MinecraftServer server, ExchangeData d, UUID player, long day, long coins) {
        if (coins <= 0 || !allowanceOn(server)) return;
        long left = allowanceLeft(server, d, player, day);
        d.allowance.put(player, new long[] {Math.max(0, left - coins), day});
    }

    /**
     * One instant sale to the market: at most {@code quantity}, each item paid at least {@code limit} (0.1.10.29: the item's own
     * price, {@link SellDepth#maxForItemPrice}; before, the bid at its start), within the seller's allowance; one payout for
     * the whole batch. Records the pressure, the seller's volume today, the allowance, the treasury, the ledger and
     * the trade. The caller moves the items and the coins. Returns {sold, coins}.
     */
    static long[] sellToMarket(MinecraftServer server, ExchangeData d, UUID uuid, Market m, long quantity, long limit, long day) {
        SellDepth.Curve c = curve(m);
        Map<String, Long> mine = d.sold(uuid, day);
        long before = mine.getOrDefault(m.id(), 0L);
        double pressure = pressure(d, m.id());
        long take = SellDepth.maxForItemPrice(c, before, pressure, Math.max(1, limit), quantity);
        long allowance = allowanceLeft(server, d, uuid, day);
        if (allowance != Long.MAX_VALUE) take = SellDepth.maxForCoins(c, before, pressure, allowance, take);
        if (take <= 0) return new long[] {0, 0};
        long coins = (long) Math.floor(SellDepth.payout(c, before, pressure, take));
        mine.put(m.id(), before + take);
        d.pressure.put(m.id(), pressure + take);
        d.lastSale.put(uuid, day);   // 0.1.10.28: an active seller of the stabiliser
        // 0.1.10.33: the records (MarketRecords)
        d.records.costIn(m.id(), d.treasury.getOrDefault(m.id(), 0L), take, coins);
        d.records.add(uuid, day, MarketRecords.TRADE_IN, coins);
        d.treasury.merge(m.id(), take, Long::sum);
        spendAllowance(server, d, uuid, day, coins);
        d.ledger(day).minted += coins;
        trade(d, m.id(), day, server.overworld().getGameTime(), Math.max(1, coins / take), (int) Math.min(Integer.MAX_VALUE, take), uuid.toString(), SYSTEM);
        ExchangeEvents.trade(m.id(), day, Math.max(1, coins / take), take, Currency.COIN, uuid.toString(), SYSTEM);
        d.setDirty();
        return new long[] {take, coins};
    }

    // ------------------------------------------------------------------ orders

    static String team(ServerPlayer p) { return p.getTeam() == null ? "" : p.getTeam().getName(); }

    public static long today(MinecraftServer server) { return FutureMarketData.get(server.overworld()).getCurrentDay(); }

    /**
     * Places an order: trades against the book (price-time priority, never with oneself or one's team), then against
     * the market for a datapack commodity, and rests the rest when {@code rest} (good for {@link #ORDER_DAYS} days).
     *
     * @param limit the worst price per item the player accepts (coins, or mB of UU-matter)
     */
    public static Result place(ServerPlayer player, String id, boolean buy, boolean rest, long limit, int quantity) {
        return place(player, id, buy, rest, limit, quantity, null);
    }

    /**
     * 0.1.10.27 (X2): {@link #place} with the purchased items going into a settlement container first ({@code into}, may be
     * null); what does not fit goes to the inventory, then the mailbox. The rules are those of a manual order.
     */
    public static Result place(ServerPlayer player, String id, boolean buy, boolean rest, long limit, int quantity, SettlementStorage into) {
        MinecraftServer server = player.server;
        ExchangeData d = data(server);
        Market m = market(server, id);
        if (m == null) return Result.fail("unknown_market");
        if (m.system() && !FutureCommodityManager.isUnlocked(player, m.commodity())) return Result.fail("locked");
        if (quantity < 1 || quantity > MAX_QUANTITY) return Result.fail("bad_quantity");
        // 0.1.10.33: an instant sale of a datapack commodity may name a limit under the band (down to 1): the band is a rule
        // of the orders that rest on the book, and the market's own bid falls under it when much was sold (the sell box never
        // had the band); a resting order and a purchase keep it
        if (!limitAllowed(m, buy, rest, limit)) return Result.fail("outside_band", m.low(), m.high());
        if (rest && openOrders(d, player.getUUID()) >= MAX_ORDERS_PER_PLAYER) return Result.fail("too_many_orders", MAX_ORDERS_PER_PLAYER);
        UUID uuid = player.getUUID();
        String owner = uuid.toString(), team = team(player);
        long day = today(server);
        Item coin = mio_icif_normal.COIN.get();
        InventoryPlan plan = new InventoryPlan(player.getInventory());
        Mailbox box = d.mailbox(uuid);
        // escrow first: the whole order at the limit price (or the items)
        long escrow = buy ? Math.multiplyExact(limit, (long) quantity) : quantity;
        boolean uuLots = UuBoard.is(id);
        if (buy) {
            if (m.currency() == Currency.UU) { if (box.uu < escrow) return Result.fail("not_enough_uu"); }
            else if (!takeCoins(plan, d, uuid, escrow)) return Result.fail("not_enough_coins");
        } else if (uuLots) {
            // 0.1.10.28 (X4): a sale on the coin <-> UU board holds lots of the UU account
            if (box.uu < UuBoard.mb(quantity)) return Result.fail("not_enough_uu");
        } else if (!plan.removeMain(m.item(), quantity, true)) return Result.fail("not_enough_items");
        if (buy && m.currency() == Currency.UU) box.uu -= escrow;
        if (!buy && uuLots) box.uu -= UuBoard.mb(quantity);
        ExchangeData.Book book = d.book(id, m.currency());
        Matched x = execute(server, d, m, id, uuid, team, buy, limit, quantity, day);
        int left = x.left;
        long refund = x.refund, proceeds = x.proceeds, received = x.received;
        // the rest rests or goes back
        if (left > 0 && rest) {
            long orderId = d.newId();
            book.book.rest(new OrderBook.Order(orderId, owner, team, buy, limit, left, day));
            book.names.put(orderId, player.getGameProfile().getName());
            // 0.1.10.35 (item 3): the order lasts orderHours of game time (before: 7 market days)
            book.expires.put(orderId, CommissionHours.expiresAt(server.overworld().getGameTime(), EconomySettings.orderHours()));
        } else if (left > 0) {
            if (buy) refund += limit * left;
            else if (uuLots) box.uu += UuBoard.mb(left);
            else addBack(plan, d, uuid, m.item(), id, left);
        }
        // results to the inventory (or the mailbox)
        if (buy && uuLots) {
            // 0.1.10.28: UU-matter bought on the board goes to the account
            credit(d, uuid, Currency.UU, UuBoard.mb(received));
            pay(plan, d, uuid, m.currency(), refund);
        } else if (buy) {
            // 0.1.10.27 (X2): into the settlement container first
            if (into != null && received > 0) received -= into.insert(new ItemStack(m.item()), received, false);
            give(plan, d, uuid, m.item(), id, received);
            pay(plan, d, uuid, m.currency(), refund);
        } else {
            pay(plan, d, uuid, m.currency(), proceeds);
        }
        if (!plan.unchanged()) throw new IllegalStateException("inventory changed during an exchange action");
        plan.commit();
        if (into != null) into.commit();
        d.setDirty();
        int filled = quantity - left;
        if (filled == 0 && rest) return Result.done("order_rested", quantity);
        if (filled == 0) return Result.fail("no_match");
        return Result.done(buy ? "bought" : "sold", filled, quantity);
    }

    /**
     * 0.1.10.33: the limit of an order is allowed: within the band, or for an instant sale ({@code rest} false) of a datapack
     * commodity anywhere from 1 up to the band's top (it trades with resting bids at their price and with the market at its
     * bid, both at or above the limit).
     */
    public static boolean limitAllowed(Market m, boolean buy, boolean rest, long limit) {
        if (limit > m.high()) return false;
        if (limit >= m.low()) return true;
        return !buy && !rest && m.system() && limit >= 1;
    }

    /**
     * 0.1.10.27 (X2): the passes of one order, moved unchanged out of {@link #place} so that an account (the Trade Terminal)
     * trades by the same rules: the book (a sale meets resting bids of players first only where they pay at least the
     * market's own bid), the market of a datapack commodity (sell depth and allowance, or treasury and import), then for a
     * sale the rest of the resting bids down to the limit. The caller holds the escrow and moves the results.
     */
    static Matched execute(MinecraftServer server, ExchangeData d, Market m, String id, UUID uuid, String team, boolean buy, long limit,
                           int quantity, long day) {
        String owner = uuid.toString();
        ExchangeData.Book book = d.book(id, m.currency());
        Matched x = new Matched(quantity);
        long firstLimit = !buy && m.system() ? Math.max(limit, nextBid(d, m, uuid, day)) : limit;
        matchBook(server, d, book, m, id, owner, team, buy, limit, firstLimit, day, x);
        // the market for a datapack commodity
        if (x.left > 0 && m.system()) {
            if (buy) {
                marketBuy(server, d, m, id, uuid, limit, day, x);
            } else {
                // 0.1.10.25 (X1): the sell depth instead of the daily budget; any amount, each item at a bid of at least limit
                long[] sold = sellToMarket(server, d, uuid, m, x.left, limit, day);
                if (sold[0] > 0) {
                    x.proceeds += sold[1];
                    x.left -= (int) sold[0];
                }
            }
        }
        if (x.left > 0 && !buy && firstLimit > limit) matchBook(server, d, book, m, id, owner, team, false, limit, limit, day, x);
        return x;
    }

    /** The market's part of a purchase: treasury stock, then the daily import, within the buyer's daily limit. */
    static void marketBuy(MinecraftServer server, ExchangeData d, Market m, String id, UUID uuid, long limit, long day, Matched x) {
        String owner = uuid.toString();
        int take = (int) Math.min(Math.min(x.left, buyLeft(server, uuid)), supply(d, id, day));
        while (take > 0 && systemCost(d, m, take) > limit * take) take--;
        if (take <= 0) return;
        long cost = systemCost(d, m, take);
        long stock = d.treasury.getOrDefault(id, 0L);
        int fromStock = (int) Math.min(take, stock);
        if (fromStock > 0) d.treasury.put(id, stock - fromStock);
        if (take > fromStock) d.imported.merge(id, take - fromStock, Integer::sum);
        // 0.1.10.25: stock that leaves the treasury no longer weighs on the bids
        if (fromStock > 0) d.pressure.computeIfPresent(id, (k, v) -> v - fromStock > 0 ? v - fromStock : null);
        x.refund += limit * take - cost;
        x.received += take;
        x.left -= take;
        d.ledger(day).burned += cost;
        d.records.add(uuid, day, MarketRecords.TRADE_OUT, cost);   // 0.1.10.33
        FutureMarketData.get(server.overworld()).addPlayerVolume(uuid, take);
        trade(d, id, day, server.overworld().getGameTime(), Math.max(1, cost / take), take, SYSTEM, owner);
        ExchangeEvents.trade(id, day, Math.max(1, cost / take), take, Currency.COIN, SYSTEM, owner);
    }

    // ------------------------------------------------------------------ 0.1.10.27 (X2): trades of an account

    /** The vanilla team of a player by name (works while the player is offline); "" without one. */
    public static String team(MinecraftServer server, String name) {
        var t = server.getScoreboard().getPlayersTeam(name);
        return t == null ? "" : t.getName();
    }

    /**
     * 0.1.10.27 (X2): an instant sale for an account (the Trade Terminal): the passes of a manual sale now
     * ({@link #execute}: book, sell depth, allowance, ledger, pressure) under {@code owner}. The caller has taken the
     * {@code quantity} items and keeps the unsold ones; the coins go to the owner's mailbox. Returns {sold, coins}.
     * A datapack commodity of coins only; the caller checks the unlock.
     */
    public static long[] sellForAccount(MinecraftServer server, UUID owner, String team, String id, long limit, int quantity) {
        Market m = market(server, id);
        if (m == null || !m.system() || m.currency() != Currency.COIN || quantity < 1) return new long[] {0, 0};
        ExchangeData d = data(server);
        long day = today(server);
        Matched x = execute(server, d, m, id, owner, team, false, Math.max(1, limit), Math.min(quantity, MAX_QUANTITY), day);
        credit(d, owner, Currency.COIN, x.proceeds);
        d.setDirty();
        return new long[] {Math.min(quantity, MAX_QUANTITY) - x.left, x.proceeds};
    }

    /**
     * 0.1.10.27 (X2): a purchase for an account (the Trade Terminal): the passes of a manual purchase now under
     * {@code owner}, paid from the owner's mailbox coins (escrow at the limit, the rest back to the mailbox). The caller
     * places the bought items. Returns {bought, coins paid}; {0, 0} when the mailbox does not hold the escrow.
     */
    public static long[] buyForAccount(MinecraftServer server, UUID owner, String team, String id, long limit, int quantity) {
        Market m = market(server, id);
        if (m == null || !m.system() || m.currency() != Currency.COIN || quantity < 1 || limit < 1) return new long[] {0, 0};
        if (limit < m.low() || limit > m.high()) return new long[] {0, 0};
        ExchangeData d = data(server);
        Mailbox box = d.mailbox(owner);
        int n = Math.min(quantity, MAX_QUANTITY);
        long escrow = Math.multiplyExact(limit, (long) n);
        if (box.coins < escrow) return new long[] {0, 0};
        box.coins -= escrow;
        long day = today(server);
        Matched x = execute(server, d, m, id, owner, team, true, limit, n, day);
        long back = x.refund + limit * x.left;
        box.coins += back;
        d.setDirty();
        return new long[] {x.received, escrow - back};
    }

    /** What the matching of one order has done so far. */
    static final class Matched {
        int left;
        long refund;            // currency back to the taker (buy: limit minus price paid)
        long proceeds;          // currency to the taker (sell)
        long received;          // items to the taker (buy)

        Matched(int quantity) { this.left = quantity; }
    }

    /** Trades the rest of an order against the book down to {@code matchLimit} (the escrow is at {@code limit}). */
    static void matchBook(MinecraftServer server, ExchangeData d, ExchangeData.Book book, Market m, String id, String owner, String team,
                          boolean buy, long limit, long matchLimit, long day, Matched x) {
        if (x.left <= 0) return;
        double fee = EconomySettings.exchangeFee();
        for (OrderBook.Fill f : book.book.match(owner, team, buy, matchLimit, x.left)) {
            long value = Math.multiplyExact(f.price(), (long) f.quantity());
            long cut = (long) Math.ceil(value * fee);
            UUID maker = UUID.fromString(f.makerOwner());
            if (buy) {
                x.refund += (limit - f.price()) * f.quantity();
                x.received += f.quantity();
                credit(d, maker, m.currency(), value - cut);
            } else {
                x.proceeds += value - cut;
                deliverGoods(d, maker, id, f.quantity());
            }
            if (m.currency() == Currency.COIN) {
                // 0.1.10.33: the records (the buyer pays the value, the seller receives it less the fee)
                UUID taker = UUID.fromString(owner);
                d.records.add(buy ? taker : maker, day, MarketRecords.TRADE_OUT, value);
                d.records.add(buy ? maker : taker, day, MarketRecords.TRADE_IN, value - cut);
            }
            if (m.currency() == Currency.COIN) toReserve(server, d, day, cut);   // 0.1.10.35: into the reserve (was destroyed)
            x.left -= f.quantity();
            trade(d, id, day, server.overworld().getGameTime(), f.price(), f.quantity(), f.makerOwner(), owner);
            ExchangeEvents.trade(id, day, f.price(), f.quantity(), m.currency(), buy ? f.makerOwner() : owner, buy ? owner : f.makerOwner());
        }
    }

    public static int openOrders(ExchangeData d, UUID player) {
        String owner = player.toString();
        int n = 0;
        for (ExchangeData.Book b : d.books.values()) for (OrderBook.Order o : b.book.orders()) if (o.owner().equals(owner)) n++;
        return n;
    }

    /** Cancels an own resting order; its escrow comes back. */
    public static Result cancel(ServerPlayer player, long orderId) {
        ExchangeData d = data(player.server);
        String owner = player.getUUID().toString();
        for (ExchangeData.Book b : d.books.values()) {
            OrderBook.Order o = b.book.find(orderId);
            if (o == null) continue;
            if (!o.owner().equals(owner)) return Result.fail("not_yours");
            int left = b.book.cancel(orderId);
            b.names.remove(orderId);
            b.expires.remove(orderId);
            refundOrder(d, player.getUUID(), b, o.buy(), o.price(), left);
            d.setDirty();
            return Result.done("cancelled", left);
        }
        return Result.fail("no_order");
    }

    static void refundOrder(ExchangeData d, UUID owner, ExchangeData.Book b, boolean buy, long price, int left) {
        if (left <= 0) return;
        if (buy) credit(d, owner, b.currency, price * left);
        else deliverGoods(d, owner, b.commodity, left);
    }

    /** The goods of a market to an account: items to the mailbox, or (0.1.10.28) UU-matter lots of the board to the UU account. */
    static void deliverGoods(ExchangeData d, UUID owner, String id, long quantity) {
        if (quantity <= 0) return;
        if (UuBoard.is(id)) credit(d, owner, Currency.UU, UuBoard.mb(quantity));
        else addItems(d.mailbox(owner), id, quantity);
    }

    // ------------------------------------------------------------------ mailbox, UU

    /** Moves the mailbox items and coins into the inventory as far as they fit (UU-matter stays on the account). */
    public static Result collect(ServerPlayer player) {
        ExchangeData d = data(player.server);
        Mailbox box = d.mailbox(player.getUUID());
        InventoryPlan plan = new InventoryPlan(player.getInventory());
        long moved = 0;
        // 0.1.10.29: in bank mode the coins stay on the account (Withdraw takes them)
        if (box.coins > 0 && !d.bankMode.contains(player.getUUID())) {
            long rest = plan.insert(mio_icif_normal.COIN.get(), box.coins);
            moved += box.coins - rest;
            box.coins = rest;
        }
        for (Iterator<Map.Entry<String, Long>> it = box.items.entrySet().iterator(); it.hasNext(); ) {
            var e = it.next();
            Item item = item(e.getKey());
            if (item == null) continue;
            long rest = plan.insert(item, e.getValue());
            moved += e.getValue() - rest;
            if (rest == 0) it.remove(); else e.setValue(rest);
        }
        plan.commit();
        d.setDirty();
        return moved > 0 ? Result.done("collected", moved) : Result.fail("nothing_to_collect");
    }

    static void credit(ExchangeData d, UUID player, Currency currency, long amount) {
        if (amount <= 0) return;
        Mailbox box = d.mailbox(player);
        if (currency == Currency.UU) box.uu = Math.addExact(box.uu, amount); else box.coins = Math.addExact(box.coins, amount);
    }

    static void addItems(Mailbox box, String id, long amount) {
        if (amount <= 0) return;
        box.items.merge(id, amount, Long::sum);
    }

    /**
     * Currency to the player: coins to the inventory (rest to the coin account), UU to the account. 0.1.10.29: in bank mode the
     * coins go to the coin account.
     */
    static void pay(InventoryPlan plan, ExchangeData d, UUID player, Currency currency, long amount) {
        if (amount <= 0) return;
        if (currency == Currency.UU || d.bankMode.contains(player)) { credit(d, player, currency, amount); return; }
        long rest = plan.insert(mio_icif_normal.COIN.get(), amount);
        credit(d, player, Currency.COIN, rest);
    }

    /**
     * 0.1.10.29: takes the coins of a payment from the coin account and the inventory together ({@link CoinAccount#split}: the
     * account first in bank mode, else the inventory first). False, and nothing taken, when both together hold less. Call it as
     * the last check of an action: the account changes at once, the inventory with the plan.
     */
    static boolean takeCoins(InventoryPlan plan, ExchangeData d, UUID player, long amount) {
        if (amount <= 0) return amount == 0;
        Item coin = mio_icif_normal.COIN.get();
        Mailbox box = d.mailbox(player);
        long[] split = CoinAccount.split(amount, box.coins, plan.count(coin, false), d.bankMode.contains(player));
        if (split == null) return false;
        if (split[1] > 0 && !plan.removeMain(coin, split[1], false)) return false;
        box.coins -= split[0];
        return true;
    }

    /** 0.1.10.29: coins of the inventory onto the coin account ({@code amount} 0 = all). */
    public static Result depositCoins(ServerPlayer player, long amount) {
        ExchangeData d = data(player.server);
        Item coin = mio_icif_normal.COIN.get();
        InventoryPlan plan = new InventoryPlan(player.getInventory());
        long n = CoinAccount.deposit(amount, plan.count(coin, false));
        if (n <= 0 || !plan.removeMain(coin, n, false)) return Result.fail("no_coins");
        plan.commit();
        credit(d, player.getUUID(), Currency.COIN, n);
        d.setDirty();
        return Result.done("coins_deposited", n);
    }

    /** 0.1.10.29: coins of the coin account into the inventory as far as they fit ({@code amount} 0 = all). */
    public static Result withdrawCoins(ServerPlayer player, long amount) {
        ExchangeData d = data(player.server);
        Mailbox box = d.mailbox(player.getUUID());
        long want = CoinAccount.withdraw(amount, box.coins);
        if (want <= 0) return Result.fail("no_account_coins");
        InventoryPlan plan = new InventoryPlan(player.getInventory());
        long rest = plan.insert(mio_icif_normal.COIN.get(), want);
        long moved = want - rest;
        if (moved <= 0) return Result.fail("inventory_full");
        plan.commit();
        box.coins -= moved;
        d.setDirty();
        return Result.done("coins_withdrawn", moved);
    }

    /** 0.1.10.29: bank mode on or off. */
    public static Result setBankMode(ServerPlayer player, boolean on) {
        ExchangeData d = data(player.server);
        if (on) d.bankMode.add(player.getUUID()); else d.bankMode.remove(player.getUUID());
        d.setDirty();
        return Result.done(on ? "bank_mode_on" : "bank_mode_off");
    }

    static void give(InventoryPlan plan, ExchangeData d, UUID player, Item item, String id, long amount) {
        if (amount <= 0) return;
        long rest = plan.insert(item, amount);
        addItems(d.mailbox(player), id, rest);
    }

    static void addBack(InventoryPlan plan, ExchangeData d, UUID player, Item item, String id, long amount) {
        give(plan, d, player, item, id, amount);
    }

    /** UU-matter of the inventory containers onto the account (buckets, cells, universal cells). */
    public static Result depositUu(ServerPlayer player) {
        long total = UuAccount.deposit(player);
        if (total <= 0) return Result.fail("no_uu");
        ExchangeData d = data(player.server);
        d.mailbox(player.getUUID()).uu += total;
        d.setDirty();
        return Result.done("uu_deposited", total);
    }

    /** UU-matter from the account into empty containers of the inventory. */
    public static Result withdrawUu(ServerPlayer player) {
        ExchangeData d = data(player.server);
        Mailbox box = d.mailbox(player.getUUID());
        long filled = UuAccount.withdraw(player, box.uu);
        if (filled <= 0) return Result.fail("no_uu_container");
        box.uu -= filled;
        d.setDirty();
        return Result.done("uu_withdrawn", filled);
    }

    // ------------------------------------------------------------------ 0.1.10.25 (X1): sell box and daily auction

    public static final int MAX_AUCTION_ORDERS = 2048;

    /**
     * The commodity a stack of the sell box sells as, or null: a datapack commodity the player has unlocked, and a plain
     * stack (no name, enchantment, damage or other changed component), so nothing valuable is sold by mistake.
     * 0.1.10.26 (X3): or an item of the commodity's {@code tag} (sold as the commodity), and a stack with changed components
     * when the commodity sets {@code accept_components}.
     */
    public static String sellable(net.minecraft.world.entity.player.Player player, ItemStack stack) {
        String id = sellableAs(stack);
        if (id == null) return null;
        FutureCommodity c = FutureCommodityManager.getCommodityById(id);
        return c == null || FutureCommodityManager.isLocked(player, c) ? null : id;
    }

    /**
     * 0.1.10.27 (X2): the commodity a stack sells as without a player (the rules of {@link #sellable} except the unlock): the
     * Trade Terminal's sell buffer takes such stacks, and its orders check the owner's unlock when they run.
     */
    public static String sellableAs(ItemStack stack) {
        if (stack.isEmpty()) return null;
        FutureCommodity c = FutureCommodityManager.getCommodityById(id(stack.getItem()));
        if (c == null || c.getItem() != stack.getItem()) c = byTag(stack);
        if (c == null) return null;
        if (!stack.isComponentsPatchEmpty() && !c.getSelling().acceptComponents()) return null;
        return c.getItemId();
    }

    /** 0.1.10.27 (X2): a plain stack into an account's mailbox (true), or false for a stack with changed components. */
    public static boolean mailStack(MinecraftServer server, UUID owner, ItemStack stack) {
        if (stack.isEmpty()) return true;
        if (!stack.isComponentsPatchEmpty() || owner == null) return false;
        ExchangeData d = data(server);
        addItems(d.mailbox(owner), id(stack.getItem()), stack.getCount());
        d.setDirty();
        return true;
    }

    /** 0.1.10.26: the first commodity (in datapack order) whose tag holds the stack's item, or null. */
    static FutureCommodity byTag(ItemStack stack) {
        for (FutureCommodity c : FutureCommodityManager.getCommodities()) {
            String tag = c.getSelling().tag();
            if (tag.isEmpty()) continue;
            ResourceLocation rl = ResourceLocation.tryParse(tag);
            if (rl != null && stack.is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, rl))) return c;
        }
        return null;
    }

    /** Items of the sell box per commodity, in the order they first appear. */
    public static Map<String, Long> boxContents(net.minecraft.world.entity.player.Player player, net.minecraft.world.Container box) {
        Map<String, Long> out = new java.util.LinkedHashMap<>();
        for (int i = 0; i < box.getContainerSize(); i++) {
            ItemStack s = box.getItem(i);
            String id = sellable(player, s);
            if (id != null) out.merge(id, (long) s.getCount(), Long::sum);
        }
        return out;
    }

    /** A stack that does not fit into the inventory: to the mailbox when it is plain (the mailbox keeps id and count), else dropped. */
    public static void toMailbox(ServerPlayer player, ItemStack stack) {
        if (stack.isEmpty()) return;
        if (!stack.isComponentsPatchEmpty()) { player.drop(stack, false); return; }
        ExchangeData d = data(player.server);
        addItems(d.mailbox(player.getUUID()), id(stack.getItem()), stack.getCount());
        d.setDirty();
    }

    /** Takes {@code amount} items that sell as commodity {@code id} out of the box (0.1.10.26: tag items and components too). */
    static void takeFromBox(net.minecraft.world.entity.player.Player player, net.minecraft.world.Container box, String id, long amount) {
        for (int i = 0; i < box.getContainerSize() && amount > 0; i++) {
            ItemStack s = box.getItem(i);
            if (s.isEmpty() || !id.equals(sellable(player, s))) continue;
            int n = (int) Math.min(amount, s.getCount());
            s.shrink(n);
            if (s.isEmpty()) box.setItem(i, ItemStack.EMPTY);
            amount -= n;
        }
        box.setChanged();
    }

    /**
     * Sells the sell box: now (each commodity down the bid curve, within the allowance; what is not sold stays in the box)
     * or into the daily auction (the items are held and sold together with everyone's at the change of the market day;
     * {@code minPercent}: the lowest average price in percent of the bid without pressure on that day).
     */
    public static Result sellBox(ServerPlayer player, net.minecraft.world.Container box, boolean toAuction, int minPercent) {
        return sellBox(player, box, toAuction, minPercent, false);
    }

    /**
     * 0.1.10.28 (X4): {@link #sellBox} with the scrap sink ({@code scrap}): after an instant sale of a commodity, what stays in
     * the box goes to the recycler's rate when the next bid is below the floor ({@link ScrapSink}); the products go to the
     * inventory (else the mailbox).
     */
    public static Result sellBox(ServerPlayer player, net.minecraft.world.Container box, boolean toAuction, int minPercent, boolean scrap) {
        return sellBox(player, box, toAuction, minPercent, scrap, 0);
    }

    /**
     * 0.1.10.29: {@link #sellBox} with a minimum for both ways: {@code minCoins} coins per item (0 = use {@code minPercent}). An
     * instant sale sells of each commodity the items paid at least the minimum ({@link #minLimit}); the auction takes the
     * minimum as its average in percent ({@link MinPrice#percentOf} of a coin minimum).
     */
    public static Result sellBox(ServerPlayer player, net.minecraft.world.Container box, boolean toAuction, int minPercent, boolean scrap,
                                 long minCoins) {
        MinecraftServer server = player.server;
        ExchangeData d = data(server);
        UUID uuid = player.getUUID();
        long day = today(server);
        Map<String, Long> contents = boxContents(player, box);
        if (contents.isEmpty()) return Result.fail("box_empty");
        if (toAuction && d.auction.size() + contents.size() > MAX_AUCTION_ORDERS) return Result.fail("auction_full");
        long items = 0, coins = 0;
        long[] scrapped = new long[2];
        for (Map.Entry<String, Long> e : contents.entrySet()) {
            Market m = market(server, e.getKey());
            if (m == null || !m.system()) continue;
            long take;
            if (toAuction) {
                take = e.getValue();
                addAuction(d, player, m.id(), take, auctionPercent(m, minPercent, minCoins), day);
            } else {
                long[] sold = sellToMarket(server, d, uuid, m, e.getValue(), minLimit(m, minPercent, minCoins), day);
                take = sold[0];
                coins += sold[1];
            }
            takeFromBox(player, box, m.id(), take);
            items += take;
            if (scrap && !toAuction && take < e.getValue() && ScrapSink.eligible(d, m, uuid, day)) scrapBox(player, box, m.id(), day, scrapped);
        }
        if (coins > 0) {
            InventoryPlan plan = new InventoryPlan(player.getInventory());
            pay(plan, d, uuid, Currency.COIN, coins);
            plan.commit();
        }
        d.setDirty();
        if (scrapped[0] > 0) return Result.done("box_sold_scrap", items, coins, scrapped[0], scrapped[1]);
        if (items == 0) return Result.fail(toAuction ? "box_empty" : "box_nothing_sold");
        return toAuction ? Result.done("auction_submitted", items) : Result.done("box_sold", items, coins);
    }

    /** 0.1.10.29: the lowest price per item of an instant sale: {@code minCoins}, else {@code minPercent} of the bid without pressure. */
    public static long minLimit(Market m, int minPercent, long minCoins) {
        return minCoins > 0 ? minCoins : MinPrice.limit(curve(m).unit(), true, minPercent);
    }

    /** 0.1.10.29: the auction minimum (percent) of a sale's minimum. */
    static int auctionPercent(Market m, int minPercent, long minCoins) {
        return minCoins > 0 ? MinPrice.percentOf(curve(m).unit(), minCoins) : Math.max(0, Math.min(100, minPercent));
    }

    /** 0.1.10.28: the stacks of the box that sell as {@code id} to the scrap sink (grouped by item and components). */
    static void scrapBox(ServerPlayer player, net.minecraft.world.Container box, String id, long day, long[] totals) {
        List<ItemStack> protos = new ArrayList<>();
        List<Long> counts = new ArrayList<>();
        for (int i = 0; i < box.getContainerSize(); i++) {
            ItemStack s = box.getItem(i);
            if (s.isEmpty() || !id.equals(sellable(player, s))) continue;
            int at = -1;
            for (int k = 0; k < protos.size() && at < 0; k++) if (ItemStack.isSameItemSameComponents(protos.get(k), s)) at = k;
            if (at < 0) { protos.add(s.copyWithCount(1)); counts.add(0L); at = protos.size() - 1; }
            counts.set(at, counts.get(at) + s.getCount());
        }
        long[] n = new long[counts.size()];
        for (int k = 0; k < n.length; k++) n[k] = counts.get(k);
        long[] stay = ScrapSink.recycle(player.server, data(player.server), protos, n, day, p -> Settlement.give(player, p, p.getCount()), totals);
        for (int k = 0; k < protos.size(); k++) {
            long remove = n[k] - stay[k];
            for (int i = 0; i < box.getContainerSize() && remove > 0; i++) {
                ItemStack s = box.getItem(i);
                if (s.isEmpty() || !ItemStack.isSameItemSameComponents(s, protos.get(k))) continue;
                int t = (int) Math.min(remove, s.getCount());
                s.shrink(t);
                if (s.isEmpty()) box.setItem(i, ItemStack.EMPTY);
                remove -= t;
            }
        }
        box.setChanged();
    }

    /** One auction order per player and commodity a day: a new submission adds to it (the latest minimum counts). */
    static void addAuction(ExchangeData d, ServerPlayer player, String item, long quantity, int minPercent, long day) {
        addAuction(d, player.getUUID(), player.getGameProfile().getName(), item, quantity, minPercent, day);
    }

    /** 0.1.10.27 (X2): the same for an account (the Trade Terminal submits for its owner). */
    public static void addAuction(ExchangeData d, UUID owner, String ownerName, String item, long quantity, int minPercent, long day) {
        for (ExchangeData.AuctionOrder a : d.auction) {
            if (a.owner.equals(owner) && a.item.equals(item)) {
                a.quantity += quantity;
                a.minPercent = Math.max(0, Math.min(100, minPercent));
                a.day = day;
                return;
            }
        }
        ExchangeData.AuctionOrder a = new ExchangeData.AuctionOrder();
        a.id = d.newId();
        a.owner = owner;
        a.ownerName = ownerName;
        a.item = item;
        a.quantity = quantity;
        a.minPercent = Math.max(0, Math.min(100, minPercent));
        a.day = day;
        d.auction.add(a);
    }

    /** Cancels an auction order; its items come back (inventory, else mailbox). */
    public static Result cancelAuction(ServerPlayer player, long orderId) {
        return cancelAuction(player, orderId, false);
    }

    /**
     * 0.1.10.28 (X4): {@code toMailbox} (the remote account of an ID card): the items go to the mailbox, nothing moves into the
     * inventory from afar.
     */
    public static Result cancelAuction(ServerPlayer player, long orderId, boolean toMailbox) {
        ExchangeData d = data(player.server);
        ExchangeData.AuctionOrder found = null;
        for (ExchangeData.AuctionOrder a : d.auction) if (a.id == orderId) found = a;
        if (found == null) return Result.fail("no_order");
        if (!found.owner.equals(player.getUUID())) return Result.fail("not_yours");
        d.auction.remove(found);
        Item item = item(found.item);
        InventoryPlan plan = new InventoryPlan(player.getInventory());
        if (item != null && !toMailbox) give(plan, d, player.getUUID(), item, found.item, found.quantity);
        else addItems(d.mailbox(player.getUUID()), found.item, found.quantity);
        plan.commit();
        d.setDirty();
        return Result.done("auction_cancelled", found.quantity);
    }

    /** What an auction order would receive if the market day changed now (the orders of today as they stand). */
    public static SellDepth.Fill auctionEstimate(MinecraftServer server, ExchangeData d, ExchangeData.AuctionOrder mine, long day) {
        Market m = market(server, mine.item);
        if (m == null || !m.system()) return new SellDepth.Fill(0, 0);
        List<ExchangeData.AuctionOrder> orders = new ArrayList<>();
        for (ExchangeData.AuctionOrder a : d.auction) if (a.item.equals(mine.item)) orders.add(a);
        if (!orders.contains(mine)) orders.add(mine);
        List<SellDepth.Fill> fills = clearing(server, d, m, orders, day);
        return fills.get(orders.indexOf(mine));
    }

    /** The auction of one commodity: bids from the orders (sold today, minimum, allowance) and the clearing. */
    static List<SellDepth.Fill> clearing(MinecraftServer server, ExchangeData d, Market m, List<ExchangeData.AuctionOrder> orders, long day) {
        SellDepth.Curve c = curve(m);
        List<SellDepth.Bid> bids = new ArrayList<>();
        for (ExchangeData.AuctionOrder a : orders) {
            long cap = allowanceLeft(server, d, a.owner, day);
            bids.add(new SellDepth.Bid(soldToday(d, a.owner, m.id(), day), a.quantity, c.unit() * a.minPercent / 100.0,
                cap == Long.MAX_VALUE ? Double.POSITIVE_INFINITY : cap));
        }
        return SellDepth.clear(c, pressure(d, m.id()), bids);
    }

    /**
     * Clears the auction at the change of the market day ({@code closing} = the day that ends): commodity by commodity
     * (sorted, so an allowance shared by several commodities is used in a fixed order), coins and unsold items to the
     * mailboxes, the sold items into the treasury and the pressure.
     */
    static void clearAuction(MinecraftServer server, ExchangeData d, long closing) {
        if (d.auction.isEmpty()) return;
        java.util.TreeMap<String, List<ExchangeData.AuctionOrder>> byItem = new java.util.TreeMap<>();
        for (ExchangeData.AuctionOrder a : d.auction) byItem.computeIfAbsent(a.item, k -> new ArrayList<>()).add(a);
        d.auction.clear();
        Map<UUID, long[]> results = new HashMap<>();   // per player: items sold, coins, items back
        for (Map.Entry<String, List<ExchangeData.AuctionOrder>> e : byItem.entrySet()) {
            List<ExchangeData.AuctionOrder> orders = e.getValue();
            Market m = market(server, e.getKey());
            if (m == null || !m.system()) {
                for (ExchangeData.AuctionOrder a : orders) {
                    addItems(d.mailbox(a.owner), a.item, a.quantity);
                    results.computeIfAbsent(a.owner, k -> new long[3])[2] += a.quantity;
                }
                continue;
            }
            List<SellDepth.Fill> fills = clearing(server, d, m, orders, closing);
            long soldAll = 0;
            for (int i = 0; i < orders.size(); i++) {
                ExchangeData.AuctionOrder a = orders.get(i);
                SellDepth.Fill f = fills.get(i);
                if (f.quantity() > 0) {
                    d.lastSale.put(a.owner, closing);
                    d.records.add(a.owner, closing, MarketRecords.TRADE_IN, f.coins());   // 0.1.10.33
                    credit(d, a.owner, Currency.COIN, f.coins());
                    spendAllowance(server, d, a.owner, closing, f.coins());
                    d.ledger(closing).minted += f.coins();
                    Map<String, Long> mine = d.sold(a.owner, closing);
                    mine.merge(a.item, f.quantity(), Long::sum);
                    trade(d, a.item, closing, server.overworld().getGameTime(), Math.max(1, f.coins() / f.quantity()),
                        (int) Math.min(Integer.MAX_VALUE, f.quantity()), a.owner.toString(), SYSTEM);
                    ExchangeEvents.trade(a.item, closing, Math.max(1, f.coins() / f.quantity()), f.quantity(), Currency.COIN, a.owner.toString(), SYSTEM);
                    soldAll += f.quantity();
                }
                if (a.quantity > f.quantity()) addItems(d.mailbox(a.owner), a.item, a.quantity - f.quantity());
                long[] r = results.computeIfAbsent(a.owner, k -> new long[3]);
                r[0] += f.quantity(); r[1] += f.coins(); r[2] += a.quantity - f.quantity();
            }
            if (soldAll > 0) {
                long paid = 0;
                for (SellDepth.Fill f : fills) paid += f.coins();
                d.records.costIn(m.id(), d.treasury.getOrDefault(m.id(), 0L), soldAll, paid);   // 0.1.10.33
                d.treasury.merge(m.id(), soldAll, Long::sum);
                d.pressure.merge(m.id(), (double) soldAll, Double::sum);
            }
        }
        results.forEach((owner, r) -> {
            ServerPlayer online = server.getPlayerList().getPlayer(owner);
            if (online != null) online.sendSystemMessage(Component.translatable("message.mio_icif.exchange.auction_cleared", r[0], r[1], r[2]));
        });
        d.setDirty();
    }

    /** The pressure after the export of the days since the last change of day. */
    static void exportPressure(ExchangeData d, long day) {
        long days = d.pressureDay == Long.MIN_VALUE ? 1 : Math.max(0, day - d.pressureDay);
        d.pressureDay = day;
        if (days == 0) return;
        d.pressure.replaceAll((id, p) -> SellDepth.export(p, exportShare(id), days));
        d.pressure.values().removeIf(p -> p <= 0);
    }

    // ------------------------------------------------------------------ commissions

    /**
     * Posts a commission: the reward is held until deliveries pay it out; the posting fee goes to the treasury reserve
     * (0.1.10.35; before: destroyed). 0.1.10.35 (item 3): it lasts {@code hours} of game time (before: 1-7 market days); the
     * players online are told (unless they turned the notice off).
     */
    public static Result post(ServerPlayer player, String itemId, int quantity, RewardKind kind, String rewardItem, long amount, int hours) {
        MinecraftServer server = player.server;
        ExchangeData d = data(server);
        Item item = item(itemId);
        Item coin = mio_icif_normal.COIN.get();
        if (item == null || item == coin) return Result.fail("bad_item");
        if (quantity < 1 || quantity > MAX_QUANTITY || amount < 1 || hours < 1 || hours > EconomySettings.commissionMaxHours())
            return Result.fail("bad_quantity");
        int mine = 0;
        for (Commission c : d.commissions) if (player.getUUID().equals(c.requester)) mine++;
        if (mine >= MAX_COMMISSIONS_PER_PLAYER) return Result.fail("too_many_commissions", MAX_COMMISSIONS_PER_PLAYER);
        if (d.commissions.size() >= MAX_COMMISSIONS) return Result.fail("board_full");
        Item reward = kind == RewardKind.ITEM ? item(rewardItem) : null;
        if (kind == RewardKind.ITEM && (reward == null || reward == item)) return Result.fail("bad_item");
        long fee = kind == RewardKind.COIN ? Math.max(1, (long) Math.ceil(amount * COMMISSION_FEE)) : BARTER_POST_FEE;
        InventoryPlan plan = new InventoryPlan(player.getInventory());
        Mailbox box = d.mailbox(player.getUUID());
        // 0.1.10.29: coins from the coin account and the inventory, taken after every other check
        if (kind == RewardKind.COIN) { if (!takeCoins(plan, d, player.getUUID(), Math.addExact(amount, fee))) return Result.fail("not_enough_coins"); }
        else {
            if (kind == RewardKind.ITEM && !plan.removeMain(reward, amount, true)) return Result.fail("not_enough_items");
            if (kind == RewardKind.UU && box.uu < amount) return Result.fail("not_enough_uu");
            if (!takeCoins(plan, d, player.getUUID(), fee)) return Result.fail("not_enough_coins");
        }
        plan.commit();
        if (kind == RewardKind.UU) box.uu -= amount;
        Commission c = new Commission();
        c.id = d.newId();
        c.requester = player.getUUID();
        c.requesterName = player.getGameProfile().getName();
        c.item = itemId;
        c.quantity = quantity;
        c.kind = kind;
        c.rewardItem = kind == RewardKind.ITEM ? rewardItem : "";
        c.rewardTotal = amount;
        c.day = today(server);
        // 0.1.10.35: the end in game time; the market day stays for the order of the list (3 market days an hour)
        c.expiresAt = CommissionHours.expiresAt(server.overworld().getGameTime(), hours);
        c.expiry = c.day + 3L * hours;
        c.counted = playMinutes(player) >= EconomySettings.listingMinPlayMinutes();
        d.commissions.add(c);
        toReserve(server, d, c.day, fee);
        d.setDirty();
        announceCommission(server, d, c, item);
        return Result.done("posted", quantity);
    }

    /** 0.1.10.35 (item 3): tells the players online about a new commission (not its poster, not those who turned it off). */
    static void announceCommission(MinecraftServer server, ExchangeData d, Commission c, Item item) {
        Component reward = switch (c.kind) {
            case COIN -> Component.translatable("message.mio_icif.exchange.commission_reward_coins", c.rewardTotal);
            case UU -> Component.translatable("message.mio_icif.exchange.commission_reward_uu", c.rewardTotal);
            case ITEM -> {
                Item r = item(c.rewardItem);
                yield r == null ? Component.literal(c.rewardItem) : Component.translatable("message.mio_icif.exchange.commission_reward_items",
                    new ItemStack(r).getHoverName(), c.rewardTotal);
            }
        };
        long[] hm = CommissionHours.hoursMinutes(CommissionHours.secondsLeft(server.overworld().getGameTime(), c.expiresAt));
        Component msg = Component.translatable("message.mio_icif.exchange.commission_new", new ItemStack(item).getHoverName(), c.quantity,
            reward, c.requesterName, hm[0]);
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (p.getUUID().equals(c.requester) || d.quietCommissions.contains(p.getUUID())) continue;
            p.sendSystemMessage(msg);
        }
    }

    /** 0.1.10.35: turns the chat notice of new commissions on or off for {@code player}. */
    public static Result commissionNotice(ServerPlayer player, boolean on) {
        ExchangeData d = data(player.server);
        boolean changed = on ? d.quietCommissions.remove(player.getUUID()) : d.quietCommissions.add(player.getUUID());
        if (changed) d.setDirty();
        return Result.done(on ? "commission_notice_on" : "commission_notice_off");
    }

    // ------------------------------------------------------------------ 0.1.10.35: the treasury reserve, the clock

    /**
     * 0.1.10.35 (item 4): a fee of {@code coins} goes into the treasury reserve up to its cap; what is above the cap is
     * destroyed as before ({@link ReserveRules#deposit}).
     */
    public static void toReserve(MinecraftServer server, ExchangeData d, long day, long coins) {
        if (coins <= 0) return;
        long[] r = ReserveRules.deposit(d.reserve, coins, reserveCap(d));
        d.reserve += r[0];
        ExchangeData.LedgerDay l = d.ledger(day);
        l.reserveIn += r[0];
        l.burned += r[1];
        d.setDirty();
    }

    /** 0.1.10.35: the cap of the reserve for the active players of the bounty board now ({@link ReserveRules#cap}). */
    public static long reserveCap(ExchangeData d) {
        int active = Math.max(1, d.board.pace.active());
        return ReserveRules.cap(EconomySettings.reserveWeeks(), BountyPacing.dailyPosts(active), EconomySettings.bountyNormalValue(),
            EconomySettings.bountyNormalMultiplier());
    }

    /** 0.1.10.35: coins of a bounty payment the reserve pays; the rest is new coins (counted by the caller). */
    public static long fromReserve(ExchangeData d, long day, long coins) {
        long take = ReserveRules.draw(d.reserve, coins);
        if (take <= 0) return 0;
        d.reserve -= take;
        d.ledger(day).reserveOut += take;
        d.setDirty();
        return take;
    }

    /**
     * 0.1.10.35: the work of every 200 ticks: resting orders and commissions whose game time is over end (escrow and reward
     * back to the mailboxes), then the shops ({@link Shops#tick}).
     */
    public static void tick(MinecraftServer server) {
        if (!FutureMarketData.get(server.overworld()).isInitialized()) return;
        ExchangeData d = data(server);
        long now = server.overworld().getGameTime(), day = today(server);
        boolean dirty = false;
        for (ExchangeData.Book b : d.books.values()) {
            if (b.expires.isEmpty()) continue;
            for (OrderBook.Order o : b.book.orders()) {
                Long at = b.expires.get(o.id());
                if (at == null || !CommissionHours.expired(now, at)) continue;
                int left = b.book.cancel(o.id());
                b.names.remove(o.id());
                b.expires.remove(o.id());
                refundOrder(d, UUID.fromString(o.owner()), b, o.buy(), o.price(), left);
                dirty = true;
            }
        }
        for (Commission c : new ArrayList<>(d.commissions)) {
            if (!CommissionHours.expired(now, c.expiresAt)) continue;
            refundCommission(d, c);
            finish(server, d, c, day);
            dirty = true;
        }
        if (Shops.tick(server, d, day)) dirty = true;
        if (dirty) d.setDirty();
    }

    static long playMinutes(ServerPlayer player) {
        return player.getStats().getValue(Stats.CUSTOM.get(Stats.PLAY_TIME)) / 1200L;
    }

    /** Delivers items to a commission (not one's own); the reward is paid in proportion. */
    public static Result deliver(ServerPlayer player, long id, int quantity) {
        return deliver(player, id, quantity, null);
    }

    /** 0.1.10.27 (X2): {@link #deliver} with the items from {@code from} (a settlement container; null = the inventory). */
    public static Result deliver(ServerPlayer player, long id, int quantity, ItemSource from) {
        MinecraftServer server = player.server;
        ExchangeData d = data(server);
        Commission c = commission(d, id);
        if (c == null) return Result.fail("no_commission");
        if (player.getUUID().equals(c.requester)) return Result.fail("own_commission");
        Item item = item(c.item);
        if (item == null) return Result.fail("bad_item");
        int left = c.quantity - c.delivered;
        int take = Math.min(quantity, left);
        if (take < 1) return Result.fail("bad_quantity");
        InventoryPlan plan = new InventoryPlan(player.getInventory());
        ItemSource src = from != null ? from : ItemSource.of(plan);
        long have = src.count(item);
        // 0.1.10.33: an item or UU-matter reward is paid in whole lots (before, 63 of "64 redstone for 1 diamond" paid nothing)
        long lot = CommissionLots.lot(c.quantity, c.rewardTotal, c.kind == RewardKind.COIN);
        take = (int) CommissionLots.take(Math.min(take, have), left, lot);
        if (take < 1 && have > 0 && lot > 1)
            return Result.fail("commission_lot", lot, CommissionLots.perLot(c.quantity, c.rewardTotal, lot));
        if (take < 1 || !src.take(item, take)) return Result.fail("not_enough_items");
        c.delivered += take;
        long due = c.rewardTotal * c.delivered / c.quantity - c.rewardPaid;
        c.rewardPaid += due;
        c.deliverers.add(player.getUUID());
        long day = today(server);
        switch (c.kind) {
            case COIN -> { pay(plan, d, player.getUUID(), Currency.COIN, due); if (c.system) d.ledger(day).minted += due; }
            case UU -> credit(d, player.getUUID(), Currency.UU, due);
            case ITEM -> { Item reward = item(c.rewardItem); if (reward != null) give(plan, d, player.getUUID(), reward, c.rewardItem, due); }
        }
        if (c.system) d.treasury.merge(c.item, (long) take, Long::sum);
        else addItems(d.mailbox(c.requester), c.item, take);
        plan.commit();
        if (c.delivered >= c.quantity) finish(server, d, c, day);
        d.setDirty();
        return Result.done("delivered", take, due);
    }

    /** The requester withdraws a commission: the unpaid reward comes back (to the mailbox). */
    public static Result withdraw(ServerPlayer player, long id) {
        ExchangeData d = data(player.server);
        Commission c = commission(d, id);
        if (c == null) return Result.fail("no_commission");
        if (!player.getUUID().equals(c.requester)) return Result.fail("not_yours");
        refundCommission(d, c);
        finish(player.server, d, c, today(player.server));
        d.setDirty();
        return Result.done("withdrawn");
    }

    static Commission commission(ExchangeData d, long id) {
        for (Commission c : d.commissions) if (c.id == id) return c;
        return null;
    }

    static void refundCommission(ExchangeData d, Commission c) {
        long left = c.rewardTotal - c.rewardPaid;
        if (left <= 0 || c.system || c.requester == null) return;
        switch (c.kind) {
            case COIN -> credit(d, c.requester, Currency.COIN, left);
            case UU -> credit(d, c.requester, Currency.UU, left);
            case ITEM -> addItems(d.mailbox(c.requester), c.rewardItem, left);
        }
        c.rewardPaid = c.rewardTotal;
    }

    /** Ends a commission; a delivered one of a counted requester is demand evidence for a listing. */
    static void finish(MinecraftServer server, ExchangeData d, Commission c, long day) {
        d.commissions.remove(c);
        if (c.system || !c.counted || c.delivered <= 0 || c.requester == null) return;
        Set<String> by = new HashSet<>();
        for (UUID u : c.deliverers) by.add(u.toString());
        long unit = c.kind == RewardKind.COIN ? c.rewardPaid / Math.max(1, c.delivered) : -1;
        long value = c.kind == RewardKind.COIN ? c.rewardPaid : 0;
        List<ListingRules.Evidence> list = d.evidence.computeIfAbsent(c.item, k -> new ArrayList<>());
        list.add(new ListingRules.Evidence(day, c.requester.toString(), Set.copyOf(by), unit, value));
        while (list.size() > 32) list.remove(0);
        tryList(server, d, c.item, day);
    }

    /** Lists an item when its evidence is enough (not a datapack commodity, not listed yet). */
    static void tryList(MinecraftServer server, ExchangeData d, String itemId, long day) {
        if (FutureCommodityManager.hasCommodity(itemId) || d.listings.containsKey(itemId)) return;
        Item item = item(itemId);
        if (item == null) return;
        int depth = CraftDepthIndex.depth(server, item);
        Currency currency = depth >= EconomySettings.uuDepth() ? Currency.UU : Currency.COIN;
        double fundamental = fundamentalIn(server, item, currency);
        var decision = ListingRules.evaluate(d.evidence.getOrDefault(itemId, List.of()), day, server.isSingleplayer(),
            EconomySettings.listingRequesters(), EconomySettings.listingFulfilled(),
            0, fundamental);
        if (!decision.list()) return;
        ExchangeData.Listing l = new ExchangeData.Listing();
        l.item = itemId;
        l.currency = currency;
        long price = decision.firstPrice();
        if (currency == Currency.UU) {
            // coin evidence converts to mB of UU through the calibration
            double perBucket = coinsPerBucket(server);
            price = perBucket > 0 ? Math.max(1, Math.round(price / perBucket * 1000.0)) : Math.max(1, Math.round(fundamental));
        }
        l.reference = Math.max(1, price);
        l.listedDay = day;
        l.lastTradeDay = day;
        d.listings.put(itemId, l);
        d.evidence.remove(itemId);
        d.setDirty();
        server.getPlayerList().broadcastSystemMessage(Component.translatable("message.mio_icif.exchange.listed",
            new ItemStack(item).getHoverName(), Component.translatable(currency == Currency.UU ? "gui.mio_icif.exchange.uu" : "gui.mio_icif.exchange.coins")), false);
    }

    // ------------------------------------------------------------------ futures (M5)

    /** Opens a cash-settled, fully collateralized contract on a datapack commodity ({@link Forwards}, {@link FuturesPricer}). */
    public static Result openFuture(ServerPlayer player, String id, boolean isLong, int quantity, int days) {
        MinecraftServer server = player.server;
        ExchangeData d = data(server);
        Market m = market(server, id);
        if (m == null || !m.system()) return Result.fail("no_futures");
        if (!FutureCommodityManager.isUnlocked(player, m.commodity())) return Result.fail("locked");
        if (quantity < 1 || quantity > FUTURES_MAX_QUANTITY || (days != 3 && days != 7)) return Result.fail("bad_quantity");
        int mine = 0;
        for (ExchangeData.Position p : d.positions) if (p.owner.equals(player.getUUID())) mine++;
        if (mine >= MAX_POSITIONS) return Result.fail("too_many_positions", MAX_POSITIONS);
        long price = futuresPrice(server, m, isLong, days);
        long collateral = Forwards.collateral(price, quantity);
        InventoryPlan plan = new InventoryPlan(player.getInventory());
        if (!takeCoins(plan, d, player.getUUID(), collateral)) return Result.fail("not_enough_coins");
        plan.commit();
        ExchangeData.Position p = new ExchangeData.Position();
        p.id = d.newId();
        p.owner = player.getUUID();
        p.ownerName = player.getGameProfile().getName();
        p.commodity = id;
        p.isLong = isLong;
        p.price = price;
        p.quantity = quantity;
        p.openDay = today(server);
        p.expiry = p.openDay + days;
        d.positions.add(p);
        d.setDirty();
        return Result.done("future_opened", quantity, price, collateral);
    }

    /** Simulated price paths per futures quote. */
    static final int FUTURE_PATHS = 8000;

    private static final Map<String, FuturesPricer.Quote> QUOTES = new java.util.HashMap<>();

    /** The contract price per item for a new position ({@link #futuresQuote}). */
    public static long futuresPrice(MinecraftServer server, Market m, boolean isLong, int days) {
        FuturesPricer.Quote q = futuresQuote(server, m, days);
        return isLong ? q.longPrice() : q.shortPrice();
    }

    /**
     * Long and short contract prices of a datapack commodity for {@code days}, from {@link FuturesPricer} on the
     * market's own price model ({@link FuturePriceModel}): the event active today goes on, a day without an event
     * gets one with {@link FuturePriceModel#EVENT_CHANCE}, the price moves only on its repricing days. Cached while
     * the day, the price and the active event stay the same.
     */
    public static FuturesPricer.Quote futuresQuote(MinecraftServer server, Market m, int days) {
        FutureCommodity c = m.commodity();
        long day = today(server);
        FuturePriceEvent active = FutureMarketData.get(server.overworld()).getActiveEvent(m.id(), day);
        int start = (int) Math.min(Integer.MAX_VALUE, m.price());
        double spread = EconomySettings.spread();
        String key = m.id() + '|' + day + '|' + start + '|' + days + '|' + System.identityHashCode(active) + '|' + spread + '|'
            + c.getBasePrice() + '|' + c.getVolatility() + '|' + FuturePriceModel.minPrice(c.getBasePrice()) + '|'
            + FuturePriceModel.maxPrice(c.getBasePrice());
        synchronized (QUOTES) {
            FuturesPricer.Quote known = QUOTES.get(key);
            if (known != null) return known;
        }
        String id = m.id();
        FuturesPricer.Model model = random -> {
            net.minecraft.util.RandomSource events = net.minecraft.util.RandomSource.create(random.nextLong());
            FuturePriceEvent[] event = {active};
            return (price, step) -> {
                long oldDay = day + step;
                if (event[0] != null && !event[0].isActive(oldDay)) event[0] = null;
                if (event[0] == null && random.nextDouble() < FuturePriceModel.EVENT_CHANCE)
                    event[0] = FuturePriceModel.randomEvent(id, oldDay, events);
                if (!c.repricesOn(oldDay + 1)) return price;
                return FuturePriceModel.next(c, price, event[0], oldDay, random::nextDouble);
            };
        };
        long seed = id.hashCode() * 0x9E3779B97F4A7C15L + day * 1_000_003L + days;
        FuturesPricer.Quote q = FuturesPricer.quote(start, days, model, spread, FUTURE_PATHS, seed);
        synchronized (QUOTES) {
            if (QUOTES.size() > 512) QUOTES.clear();
            QUOTES.put(key, q);
        }
        return q;
    }

    // ------------------------------------------------------------------ trades, bars

    static void trade(ExchangeData d, String id, long day, long time, long price, int quantity, String maker, String taker) {
        ArrayDeque<ExchangeData.Trade> q = d.trades.computeIfAbsent(id, k -> new ArrayDeque<>());
        q.addLast(new ExchangeData.Trade(day, time, price, quantity, maker, taker));
        while (q.size() > TRADES_KEPT) q.removeFirst();
        ExchangeData.Bar bar = bar(d, id, day, price);
        bar.high = Math.max(bar.high, price);
        bar.low = Math.min(bar.low, price);
        bar.close = price;
        bar.volume += quantity;
        ExchangeData.Listing l = d.listings.get(id);
        if (l != null) l.lastTradeDay = day;
    }

    static ExchangeData.Bar bar(ExchangeData d, String id, long day, long openPrice) {
        ArrayDeque<ExchangeData.Bar> q = d.bars.computeIfAbsent(id, k -> new ArrayDeque<>());
        ExchangeData.Bar last = q.peekLast();
        if (last != null && last.day == day) return last;
        ExchangeData.Bar bar = new ExchangeData.Bar();
        bar.day = day;
        bar.open = bar.high = bar.low = bar.close = openPrice;
        q.addLast(bar);
        while (q.size() > BARS_KEPT) q.removeFirst();
        return bar;
    }

    // ------------------------------------------------------------------ the day

    /**
     * The daily work, called when the market day changes: expire old orders and commissions (escrow to the mailboxes),
     * settle due futures, open the day's bars, delist idle listings, prune evidence, then the bounties of the day
     * (0.1.10.26: {@link Bounties}; the system commissions of earlier versions are no longer posted) and the market day event.
     */
    public static void onNewDay(MinecraftServer server, long day) {
        ExchangeData d = data(server);
        if (d.lastDay == day) return;
        long closing = d.lastDay == Long.MIN_VALUE ? day - 1 : d.lastDay;
        d.lastDay = day;
        // 0.1.10.25 (X1): the daily auction of the day that ends, then the export of the sell pressure
        clearAuction(server, d, closing);
        exportPressure(d, day);
        // orders
        for (ExchangeData.Book b : d.books.values()) {
            for (OrderBook.Order o : b.book.orders()) {
                // 0.1.10.35: an order with an end in game time ends in tick(); the orders of older versions keep 7 market days
                if (b.expires.containsKey(o.id()) || day - o.day() < ORDER_DAYS) continue;
                int left = b.book.cancel(o.id());
                b.names.remove(o.id());
                refundOrder(d, UUID.fromString(o.owner()), b, o.buy(), o.price(), left);
            }
        }
        // commissions (0.1.10.35: those with an end in game time end in tick())
        for (Commission c : new ArrayList<>(d.commissions)) {
            if (c.expiresAt > 0 || c.expiry > day) continue;
            refundCommission(d, c);
            finish(server, d, c, day);
        }
        // futures
        for (Iterator<ExchangeData.Position> it = d.positions.iterator(); it.hasNext(); ) {
            ExchangeData.Position p = it.next();
            if (p.expiry > day) continue;
            Market m = market(server, p.commodity);
            long settle = m == null ? p.price : m.price();
            long back = Forwards.settlement(p.isLong, p.price, p.quantity, settle);
            long collateral = Forwards.collateral(p.price, p.quantity);
            credit(d, p.owner, Currency.COIN, back);
            if (back > collateral) d.ledger(day).minted += back - collateral; else d.ledger(day).burned += collateral - back;
            d.records.add(p.owner, day, MarketRecords.FUTURES, back - collateral);   // 0.1.10.33
            it.remove();
        }
        // bars of the datapack commodities follow the daily price
        for (FutureCommodity c : FutureCommodityManager.getCommodities()) {
            int price = FutureMarketData.get(server.overworld()).getPrice(c.getItemId(), c.getBasePrice());
            bar(d, c.getItemId(), day, price);
        }
        // listings without trades for a long time go (their resting orders stay until they expire)
        d.listings.values().removeIf(l -> !l.datapack && day - l.lastTradeDay > DELIST_DAYS
            && (d.books.get(l.item) == null || d.books.get(l.item).book.size() == 0));
        d.evidence.values().forEach(list -> list.removeIf(e -> day - e.day() > EVIDENCE_DAYS));
        d.evidence.values().removeIf(List::isEmpty);
        for (String item : new ArrayList<>(d.evidence.keySet())) tryList(server, d, item, day);
        // 0.1.10.26 (X3): the bounties of three tiers replace postSystemCommissions (3 market commissions a day, paid in new coins)
        Bounties.onNewDay(server, d, day);
        // 0.1.10.33: the cost basis follows the stock (items returned without a price at the bid without pressure)
        reconcileCost(server, d);
        d.records.rollWeek(day);
        d.ledger(day);
        // 0.1.10.28 (X4): the stabiliser looks at the closed days; expired delegate ID cards leave the registers
        stabilize(d, closing);
        IdCards.prune(d, day);
        d.setDirty();
        ExchangeEvents.marketDay(server, day);
    }

    /** 0.1.10.33: settles the cost basis of every treasury stock (MarketRecords). */
    static void reconcileCost(MinecraftServer server, ExchangeData d) {
        java.util.Set<String> ids = new java.util.HashSet<>(d.records.cost.keySet());
        ids.addAll(d.treasury.keySet());
        for (String id : ids) {
            FutureCommodity c = FutureCommodityManager.getCommodityById(id);
            double unit = c == null ? 0 : bidUnit(server, c, FutureMarketData.get(server.overworld()).getPrice(id, c.getBasePrice()));
            d.records.reconcile(id, d.treasury.getOrDefault(id, 0L), unit);
        }
    }

    /**
     * 0.1.10.28 (X4): the money supply estimate takes the closed days not counted yet; the factor follows the net issue of
     * the last {@link Stabilizer#WINDOW_DAYS} closed days against the target ({@link Stabilizer}).
     */
    static void stabilize(ExchangeData d, long closing) {
        long net7 = 0;
        for (ExchangeData.LedgerDay l : d.ledger) {
            if (l.day > closing) continue;
            // 0.1.10.35: circulation (the reserve's fees out, its payments in), so the reserve changes nothing here
            if (d.supplyDay == Long.MIN_VALUE || l.day > d.supplyDay)
                d.supply = Stabilizer.supply(d.supply, l.minted + l.reserveOut, l.burned + l.reserveIn);
            if (closing - l.day < Stabilizer.WINDOW_DAYS) net7 += l.circulation();
        }
        d.supplyDay = Math.max(d.supplyDay, closing);
        d.lastSale.values().removeIf(v -> closing - v >= Stabilizer.WINDOW_DAYS);
        int active = d.lastSale.size();
        d.stabFactor = Stabilizer.next(EconomySettings.stabilizerRules(), d.stabFactor, net7, d.supply, active);
    }

    // ------------------------------------------------------------------ audit (M5)

    /** Two players and what they traded with each other: items, trades, coin value (UU listings count their mB). */
    public record PairVolume(String a, String b, long quantity, int trades, long value) { }

    /**
     * The pairs of players that traded most with each other in the last 7 days (among the kept recent trades of each
     * market), for operators: wash trading and transfers between accounts show up here. {@code total} receives the
     * quantity of all trades between players in that window.
     */
    public static List<PairVolume> auditPairs(MinecraftServer server, int top, long[] total) {
        ExchangeData d = data(server);
        long day = today(server);
        Map<String, long[]> pairs = new java.util.HashMap<>();
        long all = 0;
        for (var q : d.trades.values()) for (ExchangeData.Trade t : q) {
            if (day - t.day() > 7 || SYSTEM.equals(t.maker()) || SYSTEM.equals(t.taker())) continue;
            String key = t.maker().compareTo(t.taker()) < 0 ? t.maker() + "|" + t.taker() : t.taker() + "|" + t.maker();
            long[] v = pairs.computeIfAbsent(key, k -> new long[3]);
            v[0] += t.quantity();
            v[1]++;
            v[2] += t.price() * t.quantity();
            all += t.quantity();
        }
        if (total != null && total.length > 0) total[0] = all;
        List<PairVolume> out = new ArrayList<>();
        for (var e : pairs.entrySet()) {
            int bar = e.getKey().indexOf('|');
            long[] v = e.getValue();
            out.add(new PairVolume(e.getKey().substring(0, bar), e.getKey().substring(bar + 1), v[0], (int) v[1], v[2]));
        }
        out.sort((x, y) -> Long.compare(y.quantity(), x.quantity()));
        return out.size() > top ? new ArrayList<>(out.subList(0, top)) : out;
    }
}
