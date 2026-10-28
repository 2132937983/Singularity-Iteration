package example.sicore;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * GameTests of 0.1.10.34 (exchange follow-up, phase 1): the pace rules (days by rarity, the rush bonus, slots by players, the
 * refill wait, the refresh cost), claims on the board (refusals, the extra day, abandon, team delivery, absent claimants), the
 * bid prices of the shipped commodities and the bounty value between ask and bid, the pace of a live server and the save of
 * the new state. The panel and the simulations run in the offline harness. Internals are reached by reflection.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round1134GameTests {
    private Round1134GameTests() {}

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

    private static Object constant(String type, String name) {
        for (Object o : common(type).getEnumConstants()) if (((Enum<?>) o).name().equals(name)) return o;
        throw new AssertionError(type + "." + name);
    }

    private static String name(Object e) { return ((Enum<?>) e).name(); }

    private static int i(Object o) { return ((Number) o).intValue(); }

    private static long l(Object o) { return ((Number) o).longValue(); }

    /** Rarity ranges, the effort minimum, the rush bonus, slots by players, the refill wait, the refresh cost. */
    @GameTest(batch = "round1134_rules", template = "empty", timeoutTicks = 20)
    public static void pacingRules(GameTestHelper h) {
        Class<?> p = common("exchange.BountyPacing");
        h.assertTrue(name(call(p, "rarity", 0, 100)).equals("COMMON") && name(call(p, "rarity", 0, 3)).equals("UNCOMMON")
            && name(call(p, "rarity", 1, 2)).equals("RARE") && name(call(p, "rarity", 2, 9)).equals("LEGENDARY"), "rarity by tier and weight");
        h.assertTrue(i(call(p, "effortDays", 4000.0)) == 1 && i(call(p, "effortDays", 8000.0)) == 2 && i(call(p, "effortDays", 64000.0)) == 5, "effort");
        for (double r = 0; r < 1; r += 0.05) {
            int d = i(call(p, "days", 3, 7, 1000.0, 1, 7, r));
            h.assertTrue(d >= 3 && d <= 7, "common days " + d);
            h.assertTrue(i(call(p, "days", 1, 3, 20000.0, 1, 7, r)) == 3, "a special worth 20,000 stays 3 days");
        }
        h.assertTrue(i(call(p, "rushPercent", 1)) == 15 && i(call(p, "rushPercent", 3)) == 5 && i(call(p, "rushPercent", 4)) == 0, "rush");
        h.assertTrue(l(call(p, "claimExpiry", 11L, 10L, false)) == 12 && l(call(p, "claimExpiry", 11L, 10L, true)) == 11, "claim day");
        int[] four = (int[]) call(p, "normalRange", 3, 5, 4);
        int[] many = (int[]) call(p, "normalRange", 3, 5, 40);
        h.assertTrue(four[0] == 8 && four[1] == 10 && many[0] == 16 && many[1] == 18, "normal range for 4 and 40 players");
        h.assertTrue(i(call(p, "advancedWeekly", 3, 4)) == 8 && i(call(p, "specialOpen", 1, 5)) == 2 && l(call(p, "issueCap", 16L, 4)) == 32,
            "advanced, special, issue caps");
        h.assertTrue(l(call(p, "refillWait", 1)) == 7200 && l(call(p, "refillWait", 2)) == 4800 && l(call(p, "refillWait", 20)) == 1200
            && l(call(p, "refillWait", 0)) == Long.MAX_VALUE && i(call(p, "dailyPosts", 2)) == 10, "refill");
        h.assertTrue(l(call(p, "refreshCost", 0.0, 0)) == 500 && l(call(p, "refreshCost", 30000.0, 1)) == 3000 && l(call(p, "refreshCost", 0.0, 3)) == -1,
            "refresh cost");
        h.assertTrue((boolean) call(p, "active", 10L, 12L) && !(boolean) call(p, "active", 10L, 13L) && !(boolean) call(p, "absent", 11L, 13L)
            && (boolean) call(p, "absent", 11L, 14L), "active and absent");
        h.succeed();
    }

    private static Object bounty(long id, String tier, long day, long expiry) {
        Object b = make("exchange.BountyBoard$Bounty");
        set(b, "id", id);
        set(b, "tier", constant("exchange.BountyTables$Tier", tier));
        set(b, "day", day);
        set(b, "expiry", expiry);
        return b;
    }

    /** Claims: refusals, the extra day once, the ban after abandoning, delivery by the team, the release of absent claimants. */
    @GameTest(batch = "round1134_rules", template = "empty", timeoutTicks = 20)
    public static void boardClaims(GameTestHelper h) {
        Object board = make("exchange.BountyBoard");
        UUID a = new UUID(34, 1), b = new UUID(34, 2);
        Object n1 = bounty(1, "NORMAL", 10, 15), n2 = bounty(2, "NORMAL", 10, 11), sp = bounty(3, "SPECIAL", 10, 13);
        @SuppressWarnings("unchecked") List<Object> list = (List<Object>) get(board, "bounties");
        list.add(n1);
        list.add(n2);
        list.add(sp);
        h.assertTrue("bounty_claim_special".equals(call(board, "claimRefusal", sp, a, 10L)), "special");
        h.assertTrue(call(board, "claimRefusal", n1, a, 10L) == null, "free");
        call(board, "claim", n1, a, "Alice", "red", 10L);
        h.assertTrue(a.equals(get(n1, "claimant")) && l(get(n1, "expiry")) == 15, "claimed, no extra day");
        h.assertTrue("bounty_claimed".equals(call(board, "claimRefusal", n1, b, 10L)), "claimed by another");
        h.assertTrue("bounty_claim_limit".equals(call(board, "claimRefusal", n2, a, 10L)), "one claim at a time");
        call(board, "claim", n2, b, "Bob", "", 10L);
        h.assertTrue(l(get(n2, "expiry")) == 12 && (boolean) get(n2, "extended"), "one more day");
        h.assertTrue((boolean) call(board, "abandon", n2, b, 10L) && get(n2, "claimant") == null, "abandon");
        h.assertTrue("bounty_claim_banned".equals(call(board, "claimRefusal", n2, b, 10L)) && call(board, "claimRefusal", n2, b, 11L) == null,
            "no claim the rest of the day");
        Class<?> bb = common("exchange.BountyBoard");
        h.assertTrue((boolean) call(bb, "mayDeliver", n1, a, "") && (boolean) call(bb, "mayDeliver", n1, b, "red")
            && !(boolean) call(bb, "mayDeliver", n1, b, "") && (boolean) call(bb, "mayDeliver", n2, b, ""), "deliver");
        Map<UUID, Long> seen = new HashMap<>();
        seen.put(a, 10L);
        h.assertTrue(i(call(board, "releaseAbsent", 12L, seen)) == 0 && i(call(board, "releaseAbsent", 13L, seen)) == 1 && get(n1, "claimant") == null,
            "released after two whole days away");
        h.succeed();
    }

    /** The shipped bid prices load; the bounty value of a commodity lies between its bid and its ask. */
    @GameTest(batch = "round1134_exchange", template = "empty", timeoutTicks = 20)
    public static void bidPrices(GameTestHelper h) {
        Class<?> manager = common("future.FutureCommodityManager");
        Object carbon = call(manager, "getCommodityById", "mio_icif:resource/item_carbon_plate");
        Object iron = call(manager, "getCommodityById", "mio_icif:resource/item_adviron_ingot");
        h.assertTrue(carbon != null && i(call(carbon, "getBasePrice")) == 300 && i(call(carbon, "getBidPrice")) == 60
            && Math.abs((double) call(carbon, "bidRatio") - 0.2) < 1e-9, "carbon plate 300 / 60");
        h.assertTrue(iron != null && i(call(iron, "getBasePrice")) == 400 && i(call(iron, "getBidPrice")) == 220, "refined iron 400 / 220");
        Object diamond = call(manager, "getCommodityById", "minecraft:diamond");
        h.assertTrue(diamond == null || (double) call(diamond, "bidRatio") == 1.0, "no bid_price: ratio 1");
        Class<?> q = common("future.MarketQuote");
        h.assertTrue(Math.abs((double) call(q, "bidUnit", 300.0, 0.06, 0.2) - 58.2) < 1e-9 && (double) call(q, "midValue", 100.0, 150.0) == 100.0,
            "bid and mid");
        MinecraftServer server = h.getLevel().getServer();
        Object prices = call(common("exchange.Bounties"), "prices", server);
        double ask = (double) call(prices, "ask", "mio_icif:resource/item_carbon_plate");
        if (ask > 0) {
            double value = (double) call(prices, "value", "mio_icif:resource/item_carbon_plate");
            h.assertTrue(value < 0.7 * ask && value >= ask / 2, "carbon plate valued " + value + " at ask " + ask);
        }
        h.succeed();
    }

    /** The pace of the server (no player online in the test) and the save of the claims, refreshes, postings and seen players. */
    @GameTest(batch = "round1134_exchange", template = "empty", timeoutTicks = 20)
    public static void paceAndSave(GameTestHelper h) {
        MinecraftServer server = h.getLevel().getServer();
        Class<?> data = common("exchange.ExchangeData");
        Object d = make("exchange.ExchangeData");
        UUID a = new UUID(34, 3);
        @SuppressWarnings("unchecked") Map<UUID, Long> seen = (Map<UUID, Long>) get(d, "seen");
        seen.put(a, 40L);
        Object pace = call(common("exchange.Bounties"), "pace", server, d, 41L);
        int online = server.getPlayerList().getPlayerCount();
        h.assertTrue(i(call(pace, "online")) == online && i(call(pace, "active")) == online + 1 && get(get(d, "board"), "pace") == pace,
            "pace " + pace + " with " + online + " online and one seen yesterday");
        Object board = get(d, "board");
        Object x = bounty(9, "ADVANCED", 41, 44);
        Object need = make("exchange.BountyBoard$Need");
        set(need, "item", "minecraft:emerald");
        set(need, "count", 8L);
        set(need, "unit", 100L);
        @SuppressWarnings("unchecked") List<Object> needs = (List<Object>) get(x, "needs");
        needs.add(need);
        Object line = make("exchange.BountyBoard$Line");
        set(line, "amount", 920L);
        @SuppressWarnings("unchecked") List<Object> lines = (List<Object>) get(x, "lines");
        lines.add(line);
        set(x, "rarity", constant("exchange.BountyPacing$Rarity", "RARE"));
        set(x, "rush", 10);
        @SuppressWarnings("unchecked") List<Object> list = (List<Object>) get(board, "bounties");
        list.add(x);
        call(board, "claim", x, a, "Alice", "", 41L);
        set(board, "serial", 12L);
        set(board, "lastPostTick", 4321L);
        CompoundTag tag = (CompoundTag) call(d, "save", new CompoundTag(), server.registryAccess());
        Object e = call(data, "load", tag, server.registryAccess());
        Object y = call(get(e, "board"), "find", 9L);
        h.assertTrue(y != null && name(get(y, "rarity")).equals("RARE") && i(get(y, "rush")) == 10 && a.equals(get(y, "claimant")),
            "the claimed rare bounty");
        h.assertTrue(l(get(get(e, "board"), "serial")) == 12 && l(get(get(e, "board"), "lastPostTick")) == 4321
            && ((Map<?, ?>) get(e, "seen")).get(a).equals(40L), "the board state and the seen players");
        h.succeed();
    }
}
