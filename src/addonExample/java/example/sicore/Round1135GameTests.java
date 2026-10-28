package example.sicore;

import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * GameTests of 0.1.10.35 (exchange follow-up, phase 2): the hours of commissions and orders, the treasury reserve, the rules
 * of player shops, the processing bids (made_from cap, sell-only raw ores) on a live server, and the save of the new state.
 * The panel and the simulations run in the offline harness. Internals are reached by reflection.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round1135GameTests {
    private Round1135GameTests() {}

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

    private static int i(Object o) { return ((Number) o).intValue(); }

    private static long l(Object o) { return ((Number) o).longValue(); }

    /** Hours of commissions, the reserve's cap / deposit / circulation, the shop rules. */
    @GameTest(batch = "round1135_rules", template = "empty", timeoutTicks = 20)
    public static void hoursReserveShopRules(GameTestHelper h) {
        Class<?> ch = common("exchange.CommissionHours");
        h.assertTrue(i(call(ch, "clamp", 200L, 168)) == 168 && i(call(ch, "clamp", 0L, 168)) == 1 && i(call(ch, "clamp", 30L, 24)) == 24, "clamp");
        h.assertTrue(l(call(ch, "expiresAt", 1000L, 24)) == 1000 + 24 * 72_000L && l(call(ch, "secondsLeft", 71_999L, 72_000L)) == 1
            && (boolean) call(ch, "expired", 72_000L, 72_000L) && !(boolean) call(ch, "expired", 9L, 0L), "end tick, seconds left, expired");
        h.assertTrue(i(call(ch, "step", 24, 1, 168)) == 48 && i(call(ch, "step", 24, -1, 168)) == 12 && i(call(ch, "step", 72, 1, 72)) == 72, "- / +");
        Class<?> rr = common("exchange.ReserveRules");
        h.assertTrue(l(call(rr, "cap", 4, 6, 4000L, 1.15)) == 772_800 && l(call(rr, "cap", 0, 6, 4000L, 1.15)) == 0, "reserve cap");
        long[] dep = (long[]) call(rr, "deposit", 772_500L, 500L, 772_800L);
        h.assertTrue(dep[0] == 300 && dep[1] == 200 && l(call(rr, "circulation", 100L, 30L, 20L, 50L)) == 100, "deposit and circulation");
        Class<?> sr = common("exchange.ShopRules");
        Object swap = constant("exchange.ShopRules$Kind", "EXCHANGE"), sell = constant("exchange.ShopRules$Kind", "SELL");
        h.assertTrue(call(sr, "check", swap, "a", 1, "b", 1, 0L) == null && call(sr, "check", swap, "a", 1, "a", 1, 0L) != null
            && call(sr, "check", sell, "a", 64, "", 0, 0L) != null, "offer checks");
        long packed = l(call(sr, "pack", 999_999_999L, 1_000_000L));
        h.assertTrue(l(call(sr, "packedPrice", packed)) == 999_999_999L && l(call(sr, "packedUnits", packed)) == 1_000_000L, "pack");
        long[] run = (long[]) call(sr, "runDay", 5L, 20_000L, 19_990L);
        h.assertTrue(run[0] == 5 && (boolean) call(sr, "idle", 7L, 7) && (boolean) call(sr, "dissolve", 30L, 30) && !(boolean) call(sr, "dissolve", 29L, 30),
            "run days, idle, close");
        h.succeed();
    }

    /** The shipped processing bids: made_from, the cap below the inputs' ask, raw ores sold only to the market. */
    @GameTest(batch = "round1135_exchange", template = "empty", timeoutTicks = 20)
    public static void processingBids(GameTestHelper h) {
        MinecraftServer server = h.getLevel().getServer();
        Class<?> manager = common("future.FutureCommodityManager");
        Object fm = call(common("future.FutureMarketData"), "get", server.overworld());
        double spread = (double) call(common("exchange.EconomySettings"), "spread");
        for (String id : new String[] {"mio_icif:resource/item_adviron_ingot", "mio_icif:normal/item_circuit", "mio_icif:resource/item_alloy_ingot"}) {
            Object c = call(manager, "getCommodityById", id);
            h.assertTrue(c != null, id);
            @SuppressWarnings("unchecked") Map<String, Double> from = (Map<String, Double>) call(c, "madeFrom");
            h.assertTrue(!from.isEmpty(), id + " made_from");
            double inputs = 0;
            for (Map.Entry<String, Double> e : from.entrySet()) {
                Object in = call(manager, "getCommodityById", e.getKey());
                h.assertTrue(in != null && !(boolean) call(in, "sellOnly"), e.getKey());
                int p = i(call(fm, "getPrice", e.getKey(), i(call(in, "getBasePrice"))));
                inputs += e.getValue() * p * (1 + spread / 2);
            }
            int price = i(call(fm, "getPrice", id, i(call(c, "getBasePrice"))));
            double bid = (double) call(common("exchange.ExchangeService"), "bidUnit", server, c, price);
            h.assertTrue(bid <= inputs + 1e-6, id + ": bid " + bid + " above the inputs' ask " + inputs);
        }
        Object iron = call(manager, "getCommodityById", "mio_icif:resource/item_adviron_ingot");
        h.assertTrue(i(call(iron, "getBidPrice")) == 205, "refined iron 400 / 205");
        Object raw = call(manager, "getCommodityById", "minecraft:raw_iron");
        Object d = call(common("exchange.ExchangeService"), "data", server);
        h.assertTrue(raw != null && (boolean) call(raw, "sellOnly")
            && l(call(common("exchange.ExchangeService"), "supply", d, "minecraft:raw_iron", 0L)) == 0, "raw iron: sold to the market only");
        h.succeed();
    }

    /** Fees into the reserve up to its cap, bounty coins out of it, the ledger's columns and the circulation. */
    @GameTest(batch = "round1135_exchange", template = "empty", timeoutTicks = 20)
    public static void reserveFlow(GameTestHelper h) {
        MinecraftServer server = h.getLevel().getServer();
        Class<?> svc = common("exchange.ExchangeService");
        Object d = make("exchange.ExchangeData");
        long cap = l(call(svc, "reserveCap", d));
        h.assertTrue(cap > 0, "a reserve with the default settings: cap " + cap);
        call(svc, "toReserve", server, d, 5L, 1_000L);
        h.assertTrue(l(get(d, "reserve")) == 1_000, "the fee is kept");
        long paid = l(call(svc, "fromReserve", d, 5L, 1_500L));
        h.assertTrue(paid == 1_000 && l(get(d, "reserve")) == 0, "a bounty of 1,500 takes the 1,000 of the reserve");
        set(d, "reserve", cap - 10);
        call(svc, "toReserve", server, d, 5L, 50L);
        Object day = call(d, "ledger", 5L);
        h.assertTrue(l(get(d, "reserve")) == cap && l(get(day, "reserveIn")) == 1_010 && l(get(day, "reserveOut")) == 1_000 && l(get(day, "burned")) == 40,
            "at the cap the rest is destroyed");
        h.assertTrue(l(call(day, "created")) == 1_000 && l(call(day, "destroyed")) == 1_050 && l(call(day, "circulation")) == -50, "ledger columns");
        h.succeed();
    }

    /** Order and commission end ticks, the reserve, the quiet players, the run days and a shop through the save. */
    @GameTest(batch = "round1135_exchange", template = "empty", timeoutTicks = 20)
    public static void saveRoundTrip(GameTestHelper h) {
        MinecraftServer server = h.getLevel().getServer();
        Class<?> data = common("exchange.ExchangeData");
        Object d = make("exchange.ExchangeData");
        UUID a = new UUID(35, 11), b = new UUID(35, 12);
        set(d, "reserve", 4_321L);
        @SuppressWarnings("unchecked") Set<UUID> quiet = (Set<UUID>) get(d, "quietCommissions");
        quiet.add(b);
        set(d, "runDays", 17L);
        Object shop = make("exchange.ShopRules$Shop");
        set(shop, "owner", a);
        set(shop, "ownerName", "Alice");
        set(shop, "seenRunDay", 15L);
        Object offer = make("exchange.ShopRules$Offer");
        set(offer, "id", 77L);
        set(offer, "kind", constant("exchange.ShopRules$Kind", "SELL"));
        set(offer, "goods", "minecraft:iron_ingot");
        set(offer, "goodsCount", 16);
        set(offer, "price", 3_000L);
        set(offer, "stock", 4L);
        @SuppressWarnings("unchecked") List<Object> offers = (List<Object>) get(shop, "offers");
        offers.add(offer);
        @SuppressWarnings("unchecked") Map<UUID, Object> shops = (Map<UUID, Object>) get(d, "shops");
        shops.put(a, shop);
        CompoundTag tag = (CompoundTag) call(d, "save", new CompoundTag(), server.registryAccess());
        Object e = call(data, "load", tag, server.registryAccess());
        h.assertTrue(l(get(e, "reserve")) == 4_321 && ((Set<?>) get(e, "quietCommissions")).contains(b) && l(get(e, "runDays")) == 17, "reserve, quiet, run days");
        Object s = ((Map<?, ?>) get(e, "shops")).get(a);
        h.assertTrue(s != null && "Alice".equals(get(s, "ownerName")) && l(get(s, "seenRunDay")) == 15, "the shop");
        Object o = call(s, "offer", 77L);
        h.assertTrue(o != null && i(get(o, "goodsCount")) == 16 && l(get(o, "price")) == 3_000 && l(get(o, "stock")) == 4, "its offer");
        h.succeed();
    }
}
