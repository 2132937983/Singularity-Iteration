package example.sicore;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.CommandNode;
import java.lang.reflect.Constructor;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * GameTests of 0.1.10.43 (exchange idea 5, Q9): treasury bonds (purchase, the player's and the server's cap, early exit,
 * maturity with interest from the reserve first) and the treasury fund (subscription, cancel, settlement, redemption less the
 * fee, a loss week and a profit week through the market day), the treasury's realised profit and loss hooks, the bonds part
 * of the wealth ranking, the commands (and the day windows of "bonds all"), the settings and the packet ids. The pages run in the offline harness. Internals are
 * reached by reflection; every test puts back what it changed in the exchange's data (today's ledger columns included).
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round1143ExchangeGameTests {
    private Round1143ExchangeGameTests() {}

    private static Object call(Object target, String name, Object... args) { return Round33GameTests.call(target, name, args); }

    private static Class<?> common(String name) { return Round33GameTests.common(name); }

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
            for (Constructor<?> c : common(name).getDeclaredConstructors()) {
                if (c.getParameterCount() != args.length) continue;
                c.setAccessible(true);
                return c.newInstance(args);
            }
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(name, e);
        }
        throw new AssertionError(name + ": no constructor of " + args.length);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Object constant(String type, String name) { return Enum.valueOf((Class) common(type), name); }

    private static int i(Object o) { return ((Number) o).intValue(); }

    private static long l(Object o) { return ((Number) o).longValue(); }

    private static double dbl(Object o) { return ((Number) o).doubleValue(); }

    private static Object data(MinecraftServer server) { return call(common("exchange.ExchangeService"), "data", server); }

    private static long today(MinecraftServer server) { return l(call(common("exchange.ExchangeService"), "today", server)); }

    private static long coins(Object d, UUID player) { return l(get(call(d, "mailbox", player), "coins")); }

    private static void setCoins(Object d, UUID player, long coins) { set(call(d, "mailbox", player), "coins", coins); }

    private static boolean ok(Object result) { return (boolean) call(result, "ok"); }

    private static String key(Object result) { return String.valueOf(call(result, "key")); }

    static final String[] COLUMNS = {"minted", "burned", "reserveIn", "reserveOut", "frozenIn", "frozenOut", "bondInterest", "bondReserve",
        "fundDividend", "fundLoss"};

    /** Today's ledger columns (to put them back after a test). */
    private static long[] columns(Object d, long day) {
        Object l = call(d, "ledger", day);
        long[] v = new long[COLUMNS.length];
        for (int k = 0; k < v.length; k++) v[k] = l(get(l, COLUMNS[k]));
        return v;
    }

    private static void restore(Object d, long day, long[] v) {
        Object l = call(d, "ledger", day);
        for (int k = 0; k < v.length; k++) set(l, COLUMNS[k], v[k]);
    }

    /** The change of one column since {@code before}. */
    private static long delta(Object d, long day, long[] before, String column) {
        for (int k = 0; k < COLUMNS.length; k++) if (COLUMNS[k].equals(column)) return l(get(call(d, "ledger", day), column)) - before[k];
        throw new AssertionError(column);
    }

    private static List<?> holdings(Object d, UUID player) { return (List<?>) call(common("exchange.BondRules"), "of", get(d, "bonds"), player); }

    /** Removes what a test player left: bonds, the fund investor, the mailbox. */
    private static void forget(Object d, UUID player) {
        ((List<?>) get(d, "bonds")).removeIf(b -> player.equals(get(b, "owner")));
        ((Map<?, ?>) get(get(d, "fund"), "investors")).remove(player);
        ((Map<?, ?>) get(d, "mail")).remove(player);
    }

    /** The first plain commodity anyone may buy, sold as its item. */
    private static String commodity() {
        Class<?> svc = common("exchange.ExchangeService");
        for (Object c : (List<?>) call(common("future.FutureCommodityManager"), "getCommodities")) {
            if (call(c, "getUnlockAdvancement") != null || (boolean) call(c, "sellOnly")) continue;
            String id = (String) call(c, "getItemId");
            ResourceLocation rl = ResourceLocation.tryParse(id);
            if (rl == null || !BuiltInRegistries.ITEM.containsKey(rl)) continue;
            if (id.equals(call(svc, "sellableAs", new ItemStack(BuiltInRegistries.ITEM.get(rl))))) return id;
        }
        throw new AssertionError("no plain commodity");
    }

    // ------------------------------------------------------------------ bonds

    /** A purchase freezes its principal; an early exit pays it less 2 %; at maturity the interest comes from the reserve first. */
    @GameTest(batch = "round1143_exchange", template = "empty", timeoutTicks = 20)
    public static void bondPurchaseExitAndMaturity(GameTestHelper h) {
        MinecraftServer server = h.getLevel().getServer();
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        UUID me = player.getUUID();
        Class<?> bf = common("exchange.BondsFund"), br = common("exchange.BondRules");
        Object d = data(server);
        long day = today(server), supply = l(get(d, "supply")), reserve = l(get(d, "reserve"));
        long[] ledger = columns(d, day);
        try {
            set(d, "supply", 40_000_000L);
            set(d, "reserve", 1_500L);
            setCoins(d, me, 1_000_000L);
            h.assertTrue(l(call(bf, "serverCap", d)) == 10_000_000L, "the server's cap: 25 % of 40,000,000: " + call(bf, "serverCap", d));
            Object r = call(bf, "buy", player, 14, 1L);
            h.assertTrue(!ok(r) && key(r).endsWith("bad_quantity"), "a term of 14 days is refused: " + key(r));
            r = call(bf, "buy", player, 7, 0L);
            h.assertTrue(!ok(r) && key(r).endsWith("bad_quantity"), "0 units are refused: " + key(r));
            int rate = i(call(bf, "rateNow", d, day));
            long[] before = columns(d, day);
            r = call(bf, "buy", player, 7, 100L);
            h.assertTrue(ok(r), "100 units for 7 days: " + key(r));
            h.assertTrue(coins(d, me) == 900_000L && delta(d, day, before, "frozenIn") == 100_000L, "100,000 paid and frozen: " + coins(d, me));
            List<?> mine = holdings(d, me);
            h.assertTrue(mine.size() == 1 && l(get(mine.get(0), "units")) == 100 && i(get(mine.get(0), "ratePpm")) == rate
                && l(get(mine.get(0), "maturity")) == day + 7, "one holding at today's rate (" + rate + " ppm) maturing in 7 market days");
            Object view = call(bf, "view", player, d, day, call(common("exchange.ExchangeViews"), "stabilizerLine", d, day));
            Object vb = call(view, "bonds");
            h.assertTrue(l(call(vb, "mine")) == 100_000L && ((List<?>) call(vb, "holdings")).size() == 1 && i(call(vb, "ratePpm")) == rate
                && l(call(vb, "serverCap")) == 10_000_000L, "the view: the holding, the rate, the caps");
            // early exit
            before = columns(d, day);
            r = call(bf, "exit", player, l(get(mine.get(0), "id")));
            h.assertTrue(ok(r) && coins(d, me) == 998_000L, "ended early: 98,000 back: " + coins(d, me));
            h.assertTrue(delta(d, day, before, "frozenOut") == 100_000L && delta(d, day, before, "reserveIn") + delta(d, day, before, "burned") == 2_000L,
                "100,000 unfrozen; the 2,000 penalty into the reserve (above its cap destroyed)");
            h.assertTrue(holdings(d, me).isEmpty() && !ok(call(bf, "exit", player, 123_456_789L)), "the holding is gone; an unknown one is refused");
            // maturity
            r = call(bf, "buy", player, 28, 10L);
            h.assertTrue(ok(r), "10 units for 28 days: " + key(r));
            Object b = holdings(d, me).get(0);
            long interest = l(call(b, "interest"));
            h.assertTrue(interest == l(call(br, "interest", 10_000L, i(get(b, "ratePpm")), 28)) && interest >= 80 && interest <= 800,
                "the interest of 10,000 for 4 weeks at 0.2 .. 2.0 % a week: " + interest);
            set(b, "maturity", day);
            long c0 = coins(d, me), reserveNow = l(get(d, "reserve"));
            before = columns(d, day);
            call(bf, "maturities", server, d, day);
            long fromReserve = Math.min(reserveNow, interest);
            h.assertTrue(coins(d, me) - c0 == 10_000L + interest, "principal and interest paid: " + (coins(d, me) - c0));
            h.assertTrue(delta(d, day, before, "bondInterest") == interest && delta(d, day, before, "bondReserve") == fromReserve
                && delta(d, day, before, "reserveOut") == fromReserve && delta(d, day, before, "minted") == interest - fromReserve
                && delta(d, day, before, "frozenOut") == 10_000L, "the ledger: " + interest + " interest, " + fromReserve + " of it from the reserve");
            h.assertTrue(holdings(d, me).isEmpty(), "the matured holding is gone");
        } finally {
            set(d, "supply", supply);
            set(d, "reserve", reserve);
            restore(d, day, ledger);
            forget(d, me);
        }
        h.succeed();
    }

    /** The player's cap, the server's cap (none sold before the money supply has an estimate), coins missing. */
    @GameTest(batch = "round1143_exchange", template = "empty", timeoutTicks = 20)
    public static void bondCaps(GameTestHelper h) {
        MinecraftServer server = h.getLevel().getServer();
        ServerPlayer a = h.makeMockServerPlayerInLevel(), b = h.makeMockServerPlayerInLevel();
        Class<?> bf = common("exchange.BondsFund"), es = common("exchange.EconomySettings");
        Object d = data(server);
        long day = today(server), supply = l(get(d, "supply"));
        long[] ledger = columns(d, day);
        try {
            h.assertTrue(l(call(es, "bondMaxPerPlayer")) == 1_000_000L && dbl(call(es, "bondMaxSupplyPercent")) == 25.0, "the default caps");
            set(d, "supply", 40_000_000L);
            setCoins(d, a.getUUID(), 5_000_000L);
            Object r = call(bf, "buy", a, 7, 1_001L);
            h.assertTrue(!ok(r) && key(r).endsWith("bond_player_cap"), "1,001 units: above the player's cap of 1,000,000: " + key(r));
            r = call(bf, "buy", a, 7, 1_000L);
            h.assertTrue(ok(r) && coins(d, a.getUUID()) == 4_000_000L, "1,000 units: the cap exactly: " + key(r));
            r = call(bf, "buy", a, 28, 1L);
            h.assertTrue(!ok(r) && key(r).endsWith("bond_player_cap"), "one more unit: refused: " + key(r));
            set(d, "supply", 400_000L);
            setCoins(d, b.getUUID(), 5_000_000L);
            r = call(bf, "buy", b, 7, 1L);
            h.assertTrue(!ok(r) && key(r).endsWith("bond_server_cap"), "all bonds above 25 % of the supply: no new ones: " + key(r));
            set(d, "supply", 0L);
            ((List<?>) get(d, "bonds")).removeIf(x -> a.getUUID().equals(get(x, "owner")));
            r = call(bf, "buy", b, 7, 1L);
            h.assertTrue(!ok(r) && key(r).endsWith("bond_server_cap"), "no supply estimate yet: none sold: " + key(r));
            set(d, "supply", 40_000_000L);
            setCoins(d, b.getUUID(), 999L);
            r = call(bf, "buy", b, 7, 1L);
            h.assertTrue(!ok(r) && key(r).endsWith("not_enough_coins") && coins(d, b.getUUID()) == 999L, "999 coins buy no unit: " + key(r));
        } finally {
            set(d, "supply", supply);
            restore(d, day, ledger);
            forget(d, a.getUUID());
            forget(d, b.getUUID());
        }
        h.succeed();
    }

    // ------------------------------------------------------------------ fund

    /** Subscription (paid at once, a lot at the next market day), cancel, redemption booked 7 days ahead and paid less 1 %. */
    @GameTest(batch = "round1143_exchange", template = "empty", timeoutTicks = 20)
    public static void fundSubscribeCancelRedeem(GameTestHelper h) {
        MinecraftServer server = h.getLevel().getServer();
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        UUID me = player.getUUID();
        Class<?> bf = common("exchange.BondsFund"), fr = common("exchange.FundRules");
        Object d = data(server), fund = get(d, "fund");
        long day = today(server);
        long[] ledger = columns(d, day);
        long weightWeek = l(get(fund, "weightWeek"));
        int weightDays = i(get(fund, "weightDays"));
        try {
            setCoins(d, me, 1_000_000L);
            Object r = call(bf, "subscribe", player, 0L);
            h.assertTrue(!ok(r) && key(r).endsWith("bad_quantity"), "0 units: " + key(r));
            long[] before = columns(d, day);
            r = call(bf, "subscribe", player, 50L);
            Object inv = ((Map<?, ?>) get(fund, "investors")).get(me);
            h.assertTrue(ok(r) && coins(d, me) == 950_000L && inv != null && l(get(inv, "pending")) == 50 && delta(d, day, before, "frozenIn") == 50_000L,
                "50 units: 50,000 paid and frozen, waiting for the next market day: " + key(r));
            before = columns(d, day);
            r = call(bf, "cancel", player);
            h.assertTrue(ok(r) && coins(d, me) == 1_000_000L && delta(d, day, before, "frozenOut") == 50_000L, "cancelled: the coins back");
            h.assertTrue(!ok(call(bf, "cancel", player)), "nothing left to cancel");
            h.assertTrue(ok(call(bf, "subscribe", player, 50L)), "50 units again");
            inv = ((Map<?, ?>) get(fund, "investors")).get(me);
            r = call(bf, "redeem", player, 1L);
            h.assertTrue(!ok(r) && key(r).endsWith("fund_not_enough"), "units waiting cannot be redeemed: " + key(r));
            call(fr, "settle", fund, day);
            List<?> lots = (List<?>) get(inv, "lots");
            h.assertTrue(lots.size() == 1 && l(get(lots.get(0), "units")) == 50 && l(get(lots.get(0), "principal")) == 50_000L && l(get(inv, "pending")) == 0,
                "settled: one lot of 50 units");
            r = call(bf, "redeem", player, 20L);
            h.assertTrue(ok(r) && lots.size() == 2 && l(call(inv, "free")) == 30, "20 of 50 booked (the lot split): " + key(r));
            Object booked = null;
            for (Object x : lots) if (l(get(x, "redeemDay")) >= 0) booked = x;
            h.assertTrue(booked != null && l(get(booked, "redeemDay")) == day + 7 && l(get(booked, "principal")) == 20_000L, "paid in 7 market days");
            r = call(bf, "redeem", player, 31L);
            h.assertTrue(!ok(r) && key(r).endsWith("fund_not_enough"), "more than the free units: " + key(r));
            // the pay day: 20,000 less the 1 % fee
            set(booked, "redeemDay", day);
            before = columns(d, day);
            call(bf, "fundDay", server, d, day, day);
            h.assertTrue(coins(d, me) == 950_000L + 19_800L && lots.size() == 1 && l(call(inv, "units")) == 30, "paid 19,800: " + coins(d, me));
            h.assertTrue(delta(d, day, before, "frozenOut") == 20_000L && delta(d, day, before, "reserveIn") + delta(d, day, before, "burned") == 200L,
                "20,000 unfrozen, the 200 fee into the reserve (above its cap destroyed)");
        } finally {
            set(fund, "weightWeek", weightWeek);
            set(fund, "weightDays", weightDays);
            restore(d, day, ledger);
            forget(d, me);
        }
        h.succeed();
    }

    /** A loss week takes the investors' share from the principal; a profit week pays half of it as new coins. */
    @GameTest(batch = "round1143_exchange", template = "empty", timeoutTicks = 20)
    @SuppressWarnings("unchecked")
    public static void fundLossAndProfitWeeks(GameTestHelper h) {
        MinecraftServer server = h.getLevel().getServer();
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        UUID me = player.getUUID();
        Class<?> bf = common("exchange.BondsFund"), fr = common("exchange.FundRules");
        Object d = data(server), fund = get(d, "fund");
        long day = today(server);
        long[] ledger = columns(d, day);
        Map<UUID, Object> investors = (Map<UUID, Object>) get(fund, "investors");
        Map<UUID, Object> others = new LinkedHashMap<>(investors);
        TreeMap<Long, double[]> pl = (TreeMap<Long, double[]>) get(fund, "pl");
        TreeMap<Long, double[]> plBefore = new TreeMap<>();
        pl.forEach((k, v) -> plBefore.put(k, v.clone()));
        java.util.ArrayDeque<Object> history = (java.util.ArrayDeque<Object>) get(fund, "history");
        List<Object> historyBefore = new ArrayList<>(history);
        long weightWeek = l(get(fund, "weightWeek"));
        int weightDays = i(get(fund, "weightDays"));
        try {
            investors.clear();
            pl.clear();
            set(fund, "weightWeek", Long.MIN_VALUE);
            setCoins(d, me, 0L);
            Object inv = call(fund, "investor", me);
            Object lot = make("exchange.FundRules$Lot");
            set(lot, "units", 100L);
            set(lot, "principal", 100_000L);
            set(lot, "settled", day - 20);
            ((List<Object>) get(inv, "lots")).add(lot);
            int sale = i(get(fr, "SALE_PROFIT")), loss = i(get(fr, "SALE_LOSS"));
            call(fr, "record", fund, day - 14, loss, 4_000.0);
            call(fr, "record", fund, day - 7, sale, 10_000.0);
            double stockCost = dbl(call(get(d, "records"), "totalCost"));
            // the loss week (closing day - 14)
            double share = dbl(call(fr, "share", 100_000L, 1, stockCost));
            long taken = (long) Math.ceil(4_000 * share);
            long[] before = columns(d, day);
            call(bf, "fundDay", server, d, day, day - 14);
            h.assertTrue(taken > 0 && l(get(lot, "principal")) == 100_000L - taken && l(get(inv, "lost")) == taken && delta(d, day, before, "fundLoss") == taken,
                "a loss of 4,000 at a share of " + share + ": " + taken + " taken from the principal (" + get(lot, "principal") + ")");
            h.assertTrue(coins(d, me) == 0 && delta(d, day, before, "minted") == 0, "a loss pays nothing");
            // the profit week (closing day - 7)
            long principal = l(get(lot, "principal"));
            double share2 = dbl(call(fr, "share", principal, 1, stockCost));
            long dividend = (long) Math.floor(10_000 * share2 * 0.5);
            before = columns(d, day);
            call(bf, "fundDay", server, d, day, day - 7);
            h.assertTrue(dividend > 0 && coins(d, me) == dividend && delta(d, day, before, "minted") == dividend && delta(d, day, before, "fundDividend") == dividend
                && l(get(inv, "paid")) == dividend && l(get(lot, "principal")) == principal,
                "a profit of 10,000 at a share of " + share2 + ": a dividend of " + dividend + " (new coins), the principal unchanged");
            h.assertTrue(history.size() >= 2 && pl.isEmpty(), "both weeks recorded and their results removed");
        } finally {
            investors.clear();
            investors.putAll(others);
            pl.clear();
            pl.putAll(plBefore);
            history.clear();
            history.addAll(historyBefore);
            set(fund, "weightWeek", weightWeek);
            set(fund, "weightDays", weightDays);
            restore(d, day, ledger);
            ((Map<?, ?>) get(d, "mail")).remove(me);
        }
        h.succeed();
    }

    // ------------------------------------------------------------------ the treasury's realised profit and loss

    /** Stock sold above / below its cost, the import premium, treasury items paid as bounty rewards, reservations back unpaid. */
    @GameTest(batch = "round1143_exchange", template = "empty", timeoutTicks = 20)
    @SuppressWarnings("unchecked")
    public static void treasuryResults(GameTestHelper h) {
        MinecraftServer server = h.getLevel().getServer();
        Class<?> bf = common("exchange.BondsFund"), fr = common("exchange.FundRules"), svc = common("exchange.ExchangeService");
        Object d = data(server), fund = get(d, "fund"), records = get(d, "records");
        long day = today(server);
        String id = commodity();
        Map<String, double[]> cost = (Map<String, double[]>) get(records, "cost");
        double[] costBefore = cost.get(id) == null ? null : cost.get(id).clone();
        Map<String, Long> treasury = (Map<String, Long>) get(d, "treasury");
        Long stockBefore = treasury.get(id);
        TreeMap<Long, double[]> pl = (TreeMap<Long, double[]>) get(fund, "pl");
        TreeMap<Long, double[]> plBefore = new TreeMap<>();
        pl.forEach((k, v) -> plBefore.put(k, v.clone()));
        List<Object> returned = (List<Object>) get(get(d, "board"), "returned");
        try {
            pl.clear();
            cost.remove(id);
            Object m = call(svc, "market", server, id);
            long price = l(call(m, "price"));
            double spread = dbl(call(common("exchange.EconomySettings"), "spread"));
            Class<?> quote = common("future.MarketQuote");
            long sb2 = l(call(quote, "buyCost", (int) price, 2, spread)), b5 = l(call(quote, "buyCost", (int) price, 5, spread));
            // a cost of u an item, above what the market sells it for
            long u = 10 * Math.max(1, price) + 1;
            call(records, "costIn", id, 0L, 10L, (double) (10 * u));
            long week = l(call(common("exchange.BountyMath"), "week", day));
            call(bf, "recordSale", server, d, m, id, 10L, 4, 4, 4 * u + 500, day);
            double[] w = pl.get(week);
            h.assertTrue(w != null && w[i(get(fr, "SALE_PROFIT"))] == 500 && dbl(call(records, "costOf", id)) == 6 * u,
                "4 items that cost " + 4 * u + " sold for 500 more: +500, the record keeps 6 items");
            call(bf, "recordSale", server, d, m, id, 6L, 2, 2, sb2, day);
            h.assertTrue(w[i(get(fr, "SALE_LOSS"))] == 2 * u - sb2 && dbl(call(records, "costOf", id)) == 4 * u,
                "2 items that cost " + 2 * u + " sold at the price for " + sb2 + ": a loss");
            call(bf, "recordSale", server, d, m, id, 4L, 0, 5, b5 + 33, day);
            h.assertTrue(w[i(get(fr, "IMPORT_FEE"))] == 33 && dbl(call(records, "costOf", id)) == 4 * u, "5 items imported for 33 above the price: +33");
            Object item = constant("exchange.BountyTables$Kind", "ITEM"), source = constant("exchange.BountyTables$Source", "TREASURY");
            call(bf, "bountyCost", d, make("exchange.BountyBoard$Payout", null, item, source, id, 5L, 12.5), day);
            call(bf, "bountyCost", d, make("exchange.BountyBoard$Payout", null, item, source, id, 2L, 0.0), day);
            h.assertTrue(w[i(get(fr, "BOUNTY_COST"))] == 62.5 + 2 * u, "bounty rewards: 5 at 12.5 and 2 at the record's average of " + u);
            treasury.put(id, 7L);
            returned.add(make("exchange.BountyBoard$Payout", null, item, source, id, 3L, 7.0));
            call(bf, "restoreCost", d);
            h.assertTrue(returned.isEmpty() && dbl(call(records, "costOf", id)) == 4 * u + 21
                && Math.abs(dbl(call(bf, "unitCost", server, d, id)) - (4 * u + 21) / 7.0) < 1e-9, "3 reserved items back unpaid at 7: the record holds 7");
        } finally {
            pl.clear();
            pl.putAll(plBefore);
            if (costBefore == null) cost.remove(id); else cost.put(id, costBefore);
            if (stockBefore == null) treasury.remove(id); else treasury.put(id, stockBefore);
            returned.clear();
        }
        h.succeed();
    }

    // ------------------------------------------------------------------ ranking, commands, settings, packets

    /** The ranking's sixth part, the two commands, the settings' defaults, the packet ids. */
    @GameTest(batch = "round1143_exchange", template = "empty", timeoutTicks = 20)
    @SuppressWarnings("unchecked")
    public static void rankingCommandsSettings(GameTestHelper h) {
        MinecraftServer server = h.getLevel().getServer();
        Class<?> wr = common("exchange.WealthRanking"), rules = common("exchange.WealthRules"), es = common("exchange.EconomySettings");
        Object d = data(server), fund = get(d, "fund");
        UUID a = new UUID(1143, 1);
        long day = today(server);
        Map<UUID, String> names = (Map<UUID, String>) get(d, "names");
        try {
            names.put(a, "Gamma1143");
            setCoins(d, a, 5_000L);
            call(common("exchange.BondRules"), "add", get(d, "bonds"), 991_143L, a, "Gamma1143", 7L, 7, 5_000, day);
            Object inv = call(fund, "investor", a);
            set(inv, "pending", 2L);
            Object lot = make("exchange.FundRules$Lot");
            set(lot, "units", 3L);
            set(lot, "principal", 2_900L);
            set(lot, "settled", day);
            ((List<Object>) get(inv, "lots")).add(lot);
            call(wr, "invalidate");
            Object va = call(wr, "view", server, a, 0);
            List<?> parts = (List<?>) call(va, "parts");
            h.assertTrue(i(get(rules, "PARTS")) == 6 && parts.size() == 6 && l(parts.get(i(get(rules, "PART_BONDS")))) == 7_000L + 2_900L + 2_000L
                && l(call(va, "myValue")) == 5_000L + 11_900L, "assets: 5,000 coins + bonds 7,000 + fund 2,900 + 2 units waiting = 16,900: " + parts);
            CommandNode<CommandSourceStack> market = server.getCommands().getDispatcher().getRoot().getChild("mio_icif").getChild("market");
            for (String c : new String[] {"bonds", "fund"})
                h.assertTrue(market.getChild(c) != null && market.getChild(c).getChild("all") != null, "/mio_icif market " + c + " [all]");
            CommandSourceStack op = server.createCommandSourceStack().withSuppressedOutput().withPermission(4);
            try {
                int bonds = server.getCommands().getDispatcher().execute("mio_icif market bonds all", op);
                int investors = server.getCommands().getDispatcher().execute("mio_icif market fund all", op);
                h.assertTrue(bonds >= 1 && investors >= 1, "bonds all: " + bonds + " holdings, fund all: " + investors + " investors");
                h.assertTrue(server.getCommands().getDispatcher().execute("mio_icif market bonds", op) == 1
                    && server.getCommands().getDispatcher().execute("mio_icif market fund", op) == 1, "bonds and fund from the console");
            } catch (CommandSyntaxException e) {
                throw new AssertionError(e.getMessage(), e);
            }
            h.assertTrue((boolean) call(es, "bondsEnabled") && (boolean) call(es, "treasuryFund"), "bonds and the fund on by default");
            Class<?> p = common("exchange.ExchangePackets");
            h.assertTrue(i(get(p, "BOND_BUY")) == 36 && i(get(p, "BOND_EXIT")) == 37 && i(get(p, "FUND_SUBSCRIBE")) == 38 && i(get(p, "FUND_CANCEL")) == 39
                && i(get(p, "FUND_REDEEM")) == 40, "packet ids 36 .. 40");
        } finally {
            names.remove(a);
            forget(d, a);
            call(wr, "invalidate");
        }
        h.succeed();
    }

    /**
     * {@code bonds all}: "maturing in the next 7 market days" are the 7 days after today (the bonds due today were paid when the
     * day began) and "the interest of the last 7 days" is today and the 6 days before it (before the fix: 6 and 8 days).
     */
    @GameTest(batch = "round1143_exchange", template = "empty", timeoutTicks = 20)
    @SuppressWarnings("unchecked")
    public static void bondsAllWindows(GameTestHelper h) {
        MinecraftServer server = h.getLevel().getServer();
        Object d = data(server);
        UUID a = new UUID(1143, 7);
        long day = today(server);
        ArrayDeque<Object> ledger = (ArrayDeque<Object>) get(d, "ledger");
        Object sixAgo = make("exchange.ExchangeData$LedgerDay"), sevenAgo = make("exchange.ExchangeData$LedgerDay");
        try {
            long[] before = bondsAll(server);
            Class<?> br = common("exchange.BondRules");
            Object bonds = get(d, "bonds");
            call(br, "add", bonds, 1_143_071L, a, "Delta1143", 2L, 7, 5_000, day - 6);   // due tomorrow
            call(br, "add", bonds, 1_143_072L, a, "Delta1143", 3L, 7, 5_000, day);       // due in 7 market days
            call(br, "add", bonds, 1_143_073L, a, "Delta1143", 5L, 7, 5_000, day + 1);   // due in 8 market days
            set(sixAgo, "day", day - 6);
            set(sixAgo, "bondInterest", 100L);
            set(sixAgo, "bondReserve", 40L);
            set(sevenAgo, "day", day - 7);
            set(sevenAgo, "bondInterest", 1_000L);
            set(sevenAgo, "bondReserve", 400L);
            ledger.addFirst(sixAgo);
            ledger.addFirst(sevenAgo);
            long[] after = bondsAll(server);
            h.assertTrue(after[0] - before[0] == 5_000L, "maturing in the next 7 market days: day + 1 and day + 7, not day + 8: " + (after[0] - before[0]));
            h.assertTrue(after[2] - before[2] == 100L && after[3] - before[3] == 40L,
                "interest of the last 7 days: 6 days ago counts, 7 days ago does not: " + (after[2] - before[2]) + " / " + (after[3] - before[3]));
        } finally {
            ledger.remove(sixAgo);
            ledger.remove(sevenAgo);
            forget(d, a);
        }
        h.succeed();
    }

    /** The numbers of {@code bonds all}: principal and interest maturing, interest paid and its part from the reserve. */
    private static long[] bondsAll(MinecraftServer server) {
        List<Component> got = new ArrayList<>();
        CommandSource rec = new CommandSource() {
            @Override public void sendSystemMessage(Component c) { got.add(c); }
            @Override public boolean acceptsSuccess() { return true; }
            @Override public boolean acceptsFailure() { return true; }
            @Override public boolean shouldInformAdmins() { return false; }
        };
        try {
            server.getCommands().getDispatcher().execute("mio_icif market bonds all", server.createCommandSourceStack().withSource(rec).withPermission(4));
        } catch (CommandSyntaxException e) {
            throw new AssertionError(e.getMessage(), e);
        }
        long[] out = {-1, -1, -1, -1};
        for (Component c : got) {
            if (!(c.getContents() instanceof TranslatableContents t)) continue;
            int at = t.getKey().equals("command.mio_icif.bonds.all_due") ? 0 : t.getKey().equals("command.mio_icif.bonds.all_paid") ? 2 : -1;
            if (at < 0) continue;
            out[at] = Long.parseLong(String.valueOf(t.getArgs()[0]).replace(",", ""));
            out[at + 1] = Long.parseLong(String.valueOf(t.getArgs()[1]).replace(",", ""));
        }
        if (out[0] < 0 || out[2] < 0) throw new AssertionError("bonds all: no maturing / paid line in " + got);
        return out;
    }
}
