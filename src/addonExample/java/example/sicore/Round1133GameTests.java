package example.sicore;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * GameTests of 0.1.10.33 (exchange follow-up, phase 0): the market day (a world day set back moves it one day on; the repair
 * moves a market set back up to its latest record and changes nothing the second time), commission lots (every lot pays whole
 * reward items), the stack rules of the bounty quantities, the limit of an instant sale under the band. The panel layout runs
 * on the client and is checked by the offline harness. Internals are reached by reflection.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round1133GameTests {
    private Round1133GameTests() {}

    private static Object call(Object target, String name, Object... args) { return Round33GameTests.call(target, name, args); }

    private static Class<?> common(String name) { return Round33GameTests.common(name); }

    private static Object make(Class<?> type, Object... args) {
        for (var c : type.getDeclaredConstructors()) {
            if (c.getParameterCount() != args.length) continue;
            try {
                c.setAccessible(true);
                return c.newInstance(args);
            } catch (ReflectiveOperationException e) {
                throw new AssertionError(type.getSimpleName(), e);
            }
        }
        throw new AssertionError("no constructor " + type);
    }

    private static long day(Class<?> md, String name, Object... args) { return (long) call(md, name, args); }

    /** A day set back counts one day on; forward counts all; the repair takes the latest stamp; shifts keep old records. */
    @GameTest(batch = "round1133_rules", template = "empty", timeoutTicks = 20)
    public static void marketDayRules(GameTestHelper h) {
        Class<?> md = common("future.MarketDay");
        long unseen = Long.MIN_VALUE;
        h.assertTrue(day(md, "next", 100L, 40L, 43L) == 103, "forward");
        h.assertTrue(day(md, "next", 100L, 40L, 2L) == 101, "a day set back counts one day");
        h.assertTrue(day(md, "next", 100L, 40L, 40L) == 100, "same day");
        h.assertTrue(day(md, "next", 100L, unseen, 7L) == 100, "unseen keeps the market day");
        h.assertTrue((boolean) call(md, "wentBack", 40L, 2L) && !(boolean) call(md, "wentBack", unseen, 2L), "wentBack");
        h.assertTrue(day(md, "repaired", 12L, 299L) == 299 && day(md, "repaired", 300L, 299L) == 300 && day(md, "repaired", 12L, unseen) == 12,
            "repaired");
        h.assertTrue(day(md, "shift", 15L, 12L, 12L, 299L) == 302, "a record made after the jump keeps its remaining days");
        h.assertTrue(day(md, "shift", 293L, 290L, 12L, 299L) == 293, "a record of the old timeline keeps its stamps");
        h.succeed();
    }

    /** Lots: every lot pays whole reward items; a delivery takes whole lots or all that is left; coin rewards take any amount. */
    @GameTest(batch = "round1133_rules", template = "empty", timeoutTicks = 40)
    public static void commissionLots(GameTestHelper h) {
        Class<?> lots = common("exchange.CommissionLots");
        h.assertTrue((long) call(lots, "lot", 64L, 16L, false) == 4 && (long) call(lots, "perLot", 64L, 16L, 4L) == 1, "64 for 16");
        h.assertTrue((long) call(lots, "lot", 64L, 16L, true) == 1, "coin rewards take any amount");
        h.assertTrue((long) call(lots, "take", 10L, 64L, 4L) == 8 && (long) call(lots, "take", 3L, 3L, 4L) == 3
            && (long) call(lots, "take", 3L, 64L, 4L) == 0, "take");
        for (long q = 1; q <= 64; q++) {
            for (long r = 1; r <= 64; r++) {
                long lot = (long) call(lots, "lot", q, r, false);
                long per = (long) call(lots, "perLot", q, r, lot);
                h.assertTrue(q % lot == 0 && per * q == r * lot, "lot " + lot + " of " + q + " for " + r);
            }
        }
        h.succeed();
    }

    /** Requests skip items that do not stack, ask two stacks at most below 64; tables ask eight slots at most. */
    @GameTest(batch = "round1133_rules", template = "empty", timeoutTicks = 20)
    public static void bountyStacks(GameTestHelper h) {
        Class<?> math = common("exchange.BountyMath");
        h.assertTrue((int) call(math, "generatedQuantity", 1000.0, 10.0, 8, 256, 1) == -1, "an item that does not stack");
        h.assertTrue((int) call(math, "generatedQuantity", 1000.0, 10.0, 8, 256, 16) == 32, "two stacks of 16");
        h.assertTrue((int) call(math, "generatedQuantity", 1000.0, 10.0, 8, 256, 64)
            == (int) call(math, "quantity", 1000.0, 10.0, 8, 256), "64 stacks as before");
        h.assertTrue((long) call(math, "tableCount", 1000L, 1) == 8 && (long) call(math, "tableCount", 1000L, 16) == 128
            && (long) call(math, "tableCount", 20L, 64) == 20, "table slots");
        h.succeed();
    }

    /** An instant sale of a datapack commodity may name a limit under the band; a resting order, a purchase may not. */
    @GameTest(batch = "round1133_exchange", template = "empty", timeoutTicks = 20)
    public static void saleUnderBand(GameTestHelper h) {
        MinecraftServer server = h.getLevel().getServer();
        Class<?> service = common("exchange.ExchangeService");
        Object live = call(service, "market", server, "minecraft:iron_ingot");
        h.assertTrue(live != null && (boolean) call(live, "system"), "iron is no datapack market");
        Object m = make(live.getClass(), call(live, "id"), call(live, "item"), call(live, "currency"), true, call(live, "commodity"),
            call(live, "price"), call(live, "fundamental"), call(live, "depth"), 100L, 200L, call(live, "depthFactor"), 0L);
        h.assertTrue((boolean) call(service, "limitAllowed", m, false, false, 5L), "an instant sale under the band");
        h.assertTrue(!(boolean) call(service, "limitAllowed", m, false, true, 5L), "a resting sale under the band");
        h.assertTrue(!(boolean) call(service, "limitAllowed", m, true, false, 5L), "a purchase under the band");
        h.assertTrue(!(boolean) call(service, "limitAllowed", m, false, false, 0L), "a limit of 0");
        h.assertTrue(!(boolean) call(service, "limitAllowed", m, false, false, 201L), "above the band");
        h.assertTrue((boolean) call(service, "limitAllowed", m, true, true, 150L), "within the band");
        h.succeed();
    }

    /** After the load the market day is not behind any record, and the repair changes nothing. */
    @GameTest(batch = "round1133_exchange", template = "empty", timeoutTicks = 20)
    public static void repairIsIdle(GameTestHelper h) {
        MinecraftServer server = h.getLevel().getServer();
        Object d = call(common("exchange.ExchangeData"), "get", server);
        Object fm = call(common("future.FutureMarketData"), "get", server.overworld());
        Class<?> repair = common("exchange.MarketDayRepair");
        Object first = call(repair, "repair", d, fm);
        Object second = call(repair, "repair", d, fm);
        h.assertTrue(!(boolean) call(second, "changed") && (int) call(second, "moved") == 0, "the second repair changed " + second);
        long now = (long) call(fm, "getCurrentDay");
        h.assertTrue(now >= (long) call(repair, "latestStamp", d), "market day " + now + " behind a record (" + first + ")");
        h.succeed();
    }
}
