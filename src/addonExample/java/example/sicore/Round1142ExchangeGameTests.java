package example.sicore;

import com.mojang.brigadier.tree.CommandNode;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * GameTests of 0.1.10.42 (exchange follow-up): the Basic and the Advanced Trade Box (sale for the owner, upgrade kit, filter,
 * return slots, contents to the mailbox), the futures limits on a live server (16 rows, the per-order cap, the open collateral
 * cap), the wealth ranking, the shop upkeep period and the recipes. The panels run in the offline harness. Internals are
 * reached by reflection; every test removes what it put into the exchange's data.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round1142ExchangeGameTests {
    private Round1142ExchangeGameTests() {}

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

    private static int i(Object o) { return ((Number) o).intValue(); }

    private static long l(Object o) { return ((Number) o).longValue(); }

    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath("mio_icif", path); }

    private static Block block(String path) { return BuiltInRegistries.BLOCK.get(id(path)); }

    private static Item item(String id) { return BuiltInRegistries.ITEM.get(ResourceLocation.parse(id)); }

    private static Object data(MinecraftServer server) { return call(common("exchange.ExchangeService"), "data", server); }

    /** The first {@code n} commodities anyone trades (no unlock advancement; {@code buyable}: not sell-only) sold as their item. */
    private static List<String> commodities(int n, boolean buyable) {
        Class<?> svc = common("exchange.ExchangeService");
        List<String> out = new ArrayList<>();
        for (Object c : (List<?>) call(common("future.FutureCommodityManager"), "getCommodities")) {
            if (call(c, "getUnlockAdvancement") != null || (buyable && (boolean) call(c, "sellOnly"))) continue;
            String id = (String) call(c, "getItemId");
            ResourceLocation rl = ResourceLocation.tryParse(id);
            if (rl == null || !BuiltInRegistries.ITEM.containsKey(rl)) continue;
            if (!id.equals(call(svc, "sellableAs", new ItemStack(item(id))))) continue;
            out.add(id);
            if (out.size() == n) break;
        }
        if (out.size() < n) throw new AssertionError("only " + out.size() + " plain commodities");
        return out;
    }

    /** The mailbox of {@code owner} (null when none). */
    private static Object mailbox(Object d, UUID owner) { return ((Map<?, ?>) get(d, "mail")).get(owner); }

    /** The quantity of the owner's auction order of {@code item} (0 = none). */
    private static long auctioned(Object d, UUID owner, String item) {
        for (Object a : (List<?>) get(d, "auction"))
            if (owner.equals(get(a, "owner")) && item.equals(get(a, "item"))) return l(get(a, "quantity"));
        return 0;
    }

    /** Removes what a test player left in the exchange: auction orders, futures, the mailbox. */
    private static void forget(Object d, UUID owner) {
        for (String list : new String[] {"auction", "positions"})
            for (Iterator<?> it = ((List<?>) get(d, list)).iterator(); it.hasNext(); ) if (owner.equals(get(it.next(), "owner"))) it.remove();
        ((Map<?, ?>) get(d, "mail")).remove(owner);
    }

    // ------------------------------------------------------------------ Trade Boxes

    /** A basic box takes commodities from pipes (nothing else, nothing out) and puts them into its owner's daily auction. */
    @GameTest(batch = "round1142_exchange", template = "empty", timeoutTicks = 20)
    public static void basicTradeBoxAuctionsForItsOwner(GameTestHelper h) {
        MinecraftServer server = h.getLevel().getServer();
        ServerPlayer owner = h.makeMockServerPlayerInLevel();
        Object d = data(server);
        String id = commodities(1, false).get(0);
        BlockPos pos = new BlockPos(1, 1, 1);
        h.setBlock(pos, block("producer/block_trade_box"));
        Object box = h.getBlockEntity(pos);
        call(box, "setOwnerFromPlacer", owner);
        IItemHandler in = h.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, h.absolutePos(pos), Direction.UP);
        h.assertTrue(in != null && in.getSlots() == 27, "27 slots for hoppers and pipes");
        h.assertTrue(in.insertItem(0, new ItemStack(item(id), 40), false).isEmpty(), "a commodity goes in");
        h.assertTrue(call(common("exchange.ExchangeService"), "sellableAs", new ItemStack(Items.BARRIER)) == null
            && in.insertItem(1, new ItemStack(Items.BARRIER), false).getCount() == 1, "an item that is no commodity stays out");
        h.assertTrue(in.extractItem(0, 64, false).isEmpty(), "nothing comes out of a basic box");
        call(box, "run", h.getLevel());
        h.assertTrue(((SimpleContainer) call(box, "sellContainer")).isEmpty(), "the box is empty after its run");
        h.assertTrue(auctioned(d, owner.getUUID(), id) == 40, "the 40 items joined the owner's auction order: " + auctioned(d, owner.getUUID(), id));
        for (Object a : (List<?>) get(d, "auction"))
            if (owner.getUUID().equals(get(a, "owner"))) h.assertTrue(i(get(a, "minPercent")) == 50, "at the box's minimum of 50 %");
        h.assertTrue("OK".equals(String.valueOf(call(box, "lastStatus"))), "status " + call(box, "lastStatus"));
        forget(d, owner.getUUID());
        h.succeed();
    }

    /** The upgrade kit turns a placed basic box into an advanced one with its contents, owner and settings. */
    @GameTest(batch = "round1142_exchange", template = "empty", timeoutTicks = 20)
    public static void upgradeKitKeepsTheBox(GameTestHelper h) {
        MinecraftServer server = h.getLevel().getServer();
        ServerPlayer owner = h.makeMockServerPlayerInLevel();
        Object d = data(server);
        String id = commodities(1, false).get(0);
        BlockPos pos = new BlockPos(1, 1, 1);
        h.setBlock(pos, block("producer/block_trade_box"));
        Object box = h.getBlockEntity(pos);
        call(box, "setOwnerFromPlacer", owner);
        call(box, "setSettings", make("exchange.TradeBoxRules$Settings", 0, 35, 1L, false, false, true));
        ((SimpleContainer) call(box, "sellContainer")).setItem(3, new ItemStack(item(id), 17));
        owner.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(BuiltInRegistries.ITEM.get(id("normal/item_trade_box_upgrade"))));
        h.useBlock(pos, owner);
        h.assertBlockPresent(block("producer/block_trade_box_advanced"), pos);
        Object up = h.getBlockEntity(pos);
        SimpleContainer sell = (SimpleContainer) call(up, "sellContainer");
        h.assertTrue((boolean) call(up, "advanced") && sell.getContainerSize() == 54, "an advanced box of 54 slots");
        h.assertTrue(sell.getItem(3).is(item(id)) && sell.getItem(3).getCount() == 17, "the contents stay in their slot");
        h.assertTrue(owner.getUUID().equals(call(call(up, "owner"), "uuid")), "the owner stays");
        Object s = call(up, "settings");
        h.assertTrue(i(call(s, "percent")) == 35 && !(boolean) call(s, "autoDeliver"), "the settings stay");
        Object mail = mailbox(d, owner.getUUID());
        h.assertTrue(mail == null || ((Map<?, ?>) get(mail, "items")).isEmpty(), "nothing went to the mailbox");
        forget(d, owner.getUUID());
        h.succeed();
    }

    /** The advanced box: the filter, the return slots (pipes take them out), the auction, its contents to the mailbox when broken. */
    @GameTest(batch = "round1142_exchange", template = "empty", timeoutTicks = 20)
    public static void advancedTradeBoxFilterReturnsMailbox(GameTestHelper h) {
        MinecraftServer server = h.getLevel().getServer();
        ServerPlayer owner = h.makeMockServerPlayerInLevel();
        Object d = data(server);
        List<String> ids = commodities(2, false);
        String a = ids.get(0), b = ids.get(1);
        BlockPos pos = new BlockPos(1, 1, 1);
        h.setBlock(pos, block("producer/block_trade_box_advanced"));
        Object box = h.getBlockEntity(pos);
        call(box, "setOwnerFromPlacer", owner);
        call(box, "setSettings", make("exchange.TradeBoxRules$Settings", 0, 50, 1L, false, false, true));
        call(box, "setFilter", 0, a);
        IItemHandler io = h.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, h.absolutePos(pos), Direction.NORTH);
        h.assertTrue(io != null && io.getSlots() == 63, "54 slots and 9 return slots");
        h.assertTrue(io.insertItem(0, new ItemStack(item(b), 5), true).getCount() == 5, "the filter keeps " + b + " out");
        h.assertTrue(io.insertItem(0, new ItemStack(item(a), 20), false).isEmpty(), a + " goes in");
        SimpleContainer sell = (SimpleContainer) call(box, "sellContainer");
        sell.setItem(5, new ItemStack(item(b), 9));
        call(box, "run", h.getLevel());
        h.assertTrue(auctioned(d, owner.getUUID(), a) == 20, "the filtered commodity joined the auction");
        ItemStack back = io.extractItem(54, 64, false);
        h.assertTrue(back.is(item(b)) && back.getCount() == 9 && sell.isEmpty(), "the rest went to the return slots, pipes take it");
        sell.setItem(0, new ItemStack(item(a), 4));
        h.destroyBlock(pos);
        Object mail = mailbox(d, owner.getUUID());
        h.assertTrue(mail != null && l(((Map<?, ?>) get(mail, "items")).get(a)) == 4, "the contents of a broken box go to the owner's mailbox");
        forget(d, owner.getUUID());
        h.succeed();
    }

    // ------------------------------------------------------------------ futures

    /** 16 rows; an order joining a row; the cap of one order; the open collateral cap of a multiplayer server. */
    @GameTest(batch = "round1142_exchange", template = "empty", timeoutTicks = 20)
    public static void futuresRowsAndCaps(GameTestHelper h) {
        MinecraftServer server = h.getLevel().getServer();
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        UUID me = player.getUUID();
        Class<?> svc = common("exchange.ExchangeService"), fl = common("exchange.FuturesLimits");
        Object d = data(server);
        h.assertTrue(i(get(fl, "MAX_ROWS")) == 16 && i(get(svc, "MAX_POSITIONS")) == 16, "16 rows");
        h.assertTrue(i(call(fl, "maxQuantity", 70L)) == 14_285 && i(call(fl, "maxQuantity", 976L)) == 1_024
            && i(call(fl, "maxQuantity", 975L)) == 1_025 && i(call(fl, "maxQuantity", 10L)) == 65_536, "max(1,024, 1,000,000 / price), at most 65,536");
        set(call(d, "mailbox", me), "coins", 100_000_000L);
        List<String> ids = commodities(5, true);
        int rows = 0;
        for (int c = 0; c < 4; c++)
            for (boolean isLong : new boolean[] {true, false})
                for (int days : new int[] {3, 7}) {
                    Object r = call(svc, "openFuture", player, ids.get(c), isLong, 1, days);
                    h.assertTrue((boolean) call(r, "ok"), ids.get(c) + " " + isLong + " " + days + ": " + call(r, "key"));
                    rows++;
                }
        Object r = call(svc, "openFuture", player, ids.get(4), true, 1, 3);
        h.assertTrue(!(boolean) call(r, "ok") && String.valueOf(call(r, "key")).endsWith("too_many_positions"), rows + " rows: the 17th is refused");
        r = call(svc, "openFuture", player, ids.get(0), true, 1, 3);
        h.assertTrue((boolean) call(r, "ok"), "an order joining a row is taken: " + call(r, "key"));
        Object m = call(svc, "market", server, ids.get(0));
        long price = l(call(svc, "futuresPrice", server, m, true, 3));
        int cap = i(call(fl, "maxQuantity", price));
        r = call(svc, "openFuture", player, ids.get(0), true, cap + 1, 3);
        String over = cap < 65_536 ? "future_quantity_cap" : "bad_quantity";
        h.assertTrue(!(boolean) call(r, "ok") && String.valueOf(call(r, "key")).endsWith(over), "above the cap of " + cap + " at " + price + ": " + call(r, "key"));
        long collateralCap = l(call(svc, "futuresCollateralCap", server));
        boolean multiplayer = (boolean) call(common("exchange.Bounties"), "multiplayer", server);
        h.assertTrue(collateralCap == (multiplayer ? 5_000_000L : -1L), "collateral cap " + collateralCap + " (multiplayer " + multiplayer + ")");
        if (collateralCap > 0) {
            String last = "";
            for (int k = 0; k < 12; k++) {
                r = call(svc, "openFuture", player, ids.get(0), true, cap, 3);
                last = String.valueOf(call(r, "key"));
                if (!(boolean) call(r, "ok")) break;
            }
            long open = l(call(fl, "openCollateral", call(svc, "lots", d, me)));
            h.assertTrue(last.endsWith("future_collateral_cap") && open <= collateralCap, "the open collateral stops at the cap: " + open + ", " + last);
        }
        forget(d, me);
        h.succeed();
    }

    // ------------------------------------------------------------------ wealth ranking

    /** Places by total assets, a hidden name, a weekly category, the command. */
    @GameTest(batch = "round1142_exchange", template = "empty", timeoutTicks = 20)
    public static void wealthRanking(GameTestHelper h) {
        MinecraftServer server = h.getLevel().getServer();
        Class<?> wr = common("exchange.WealthRanking"), rules = common("exchange.WealthRules");
        Object d = data(server);
        UUID a = new UUID(1142, 1), b = new UUID(1142, 2);
        @SuppressWarnings("unchecked") Map<UUID, String> names = (Map<UUID, String>) get(d, "names");
        @SuppressWarnings("unchecked") Set<UUID> hidden = (Set<UUID>) get(d, "rankingHidden");
        names.put(a, "Alpha1142");
        names.put(b, "Beta1142");
        set(call(d, "mailbox", a), "coins", 4_000_000_000_000L);
        set(call(d, "mailbox", b), "coins", 3_000_000_000_000L);
        hidden.add(b);
        call(wr, "invalidate");
        try {
            Object va = call(wr, "view", server, a, 0);
            List<?> top = (List<?>) call(va, "top");
            h.assertTrue((boolean) call(va, "enabled") && i(call(va, "myRank")) == 1 && l(call(va, "myValue")) == 4_000_000_000_000L, "Alpha first");
            h.assertTrue(top.size() >= 2 && "Alpha1142".equals(call(top.get(0), "name")) && "".equals(call(top.get(1), "name"))
                && i(call(top.get(1), "rank")) == 2, "Beta second, name hidden from others");
            h.assertTrue(l(((List<?>) call(va, "parts")).get(0)) == 4_000_000_000_000L, "the coins part");
            Object vb = call(wr, "view", server, b, 0);
            Object own = ((List<?>) call(vb, "top")).get(1);
            h.assertTrue("Beta1142".equals(call(own, "name")) && (boolean) call(own, "me") && (boolean) call(vb, "hidden"), "a hidden player sees their own name");
            long day = l(call(common("exchange.ExchangeService"), "today", server));
            call(get(d, "records"), "add", a, day, i(get(common("exchange.MarketRecords"), "BOUNTY")), 777L);
            Object ranked = call(wr, "rank", server, a, i(get(rules, "BOUNTY")), 10);
            h.assertTrue(l(call(ranked, "myValue")) == 777 && i(call(ranked, "myRank")) >= 1, "this week's bounty value");
            CommandNode<CommandSourceStack> wealth = server.getCommands().getDispatcher().getRoot().getChild("mio_icif").getChild("market").getChild("wealth");
            h.assertTrue(wealth != null, "/mio_icif market wealth");
            for (String sub : new String[] {"total", "trade", "futures", "bounty", "hide", "show", "player", "refresh"})
                h.assertTrue(wealth.getChild(sub) != null, "/mio_icif market wealth " + sub);
        } finally {
            names.remove(a);
            names.remove(b);
            hidden.remove(b);
            ((Map<?, ?>) get(d, "mail")).remove(a);
            ((Map<?, ?>) get(d, "mail")).remove(b);
            ((Map<?, ?>) get(get(d, "records"), "thisWeek")).remove(a);
            call(wr, "invalidate");
        }
        h.succeed();
    }

    // ------------------------------------------------------------------ shop upkeep

    /** A shop pays its upkeep once a period (24 h), at most 60 coins shared by its offers; a shop of an older version waits. */
    @GameTest(batch = "round1142_exchange", template = "empty", timeoutTicks = 20)
    public static void shopUpkeepPerPeriod(GameTestHelper h) {
        MinecraftServer server = h.getLevel().getServer();
        Class<?> es = common("exchange.EconomySettings"), sr = common("exchange.ShopRules");
        h.assertTrue(i(call(es, "shopUpkeepHours")) == 24 && l(call(es, "shopUpkeepMaxPerShop")) == 60 && l(call(es, "shopUpkeepPerWeek")) == 10,
            "defaults: 24 h, 60 a shop, 10 an offer");
        Object d = data(server);
        @SuppressWarnings("unchecked") Map<UUID, Object> shops = (Map<UUID, Object>) get(d, "shops");
        UUID twelve = new UUID(1142, 12), seven = new UUID(1142, 7), old = new UUID(1142, 41);
        long now = server.overworld().getGameTime(), period = l(call(sr, "upkeepPeriod", 24));
        h.assertTrue(period == 1_728_000L, "24 h = 1,728,000 ticks");
        long day = l(call(common("exchange.ExchangeService"), "today", server));
        try {
            for (Object[] spec : new Object[][] {{twelve, 12, 100L, now - 1}, {seven, 7, 20L, now - 1}, {old, 3, 100L, Long.MIN_VALUE}}) {
                UUID owner = (UUID) spec[0];
                Object shop = make("exchange.ShopRules$Shop");
                set(shop, "owner", owner);
                set(shop, "ownerName", "shop1142");
                set(shop, "seenRunDay", l(get(d, "runDays")));
                set(shop, "upkeepDue", spec[3]);
                @SuppressWarnings("unchecked") List<Object> offers = (List<Object>) get(shop, "offers");
                for (int k = 0; k < (int) spec[1]; k++) {
                    Object o = make("exchange.ShopRules$Offer");
                    set(o, "id", 1_142_000L + k);
                    offers.add(o);
                }
                shops.put(owner, shop);
                set(call(d, "mailbox", owner), "coins", spec[2]);
            }
            call(common("exchange.Shops"), "tick", server, d, day);
            long[] paid = new long[3];
            int[] unpaid = new int[3];
            UUID[] owners = {twelve, seven, old};
            for (int k = 0; k < 3; k++) {
                paid[k] = l(get(call(d, "mailbox", owners[k]), "coins"));
                for (Object o : (List<?>) get(shops.get(owners[k]), "offers")) if ((boolean) get(o, "unpaid")) unpaid[k]++;
            }
            h.assertTrue(paid[0] == 40 && unpaid[0] == 0, "12 offers pay 60 (the cap), not 120: left " + paid[0]);
            h.assertTrue(paid[1] == 2 && unpaid[1] == 5, "7 offers share 60 (9, 9, 9, 9, 8, 8, 8): 20 coins pay two, five pause: " + paid[1] + ", " + unpaid[1]);
            h.assertTrue(paid[2] == 100 && unpaid[2] == 0, "a shop of an older version pays nothing yet");
            for (UUID owner : owners)
                h.assertTrue(l(get(shops.get(owner), "upkeepDue")) == now + period, "the next payment one period later");
        } finally {
            for (UUID owner : new UUID[] {twelve, seven, old}) {
                shops.remove(owner);
                ((Map<?, ?>) get(d, "mail")).remove(owner);
            }
        }
        h.succeed();
    }

    // ------------------------------------------------------------------ recipes

    /** The boxes, the kit and the Trade Terminal made of an Advanced Trade Box. */
    @GameTest(batch = "round1142_recipes", template = "empty", timeoutTicks = 20)
    public static void tradeBoxRecipes(GameTestHelper h) {
        var manager = h.getLevel().getRecipeManager();
        var registries = h.getLevel().registryAccess();
        String[][] expect = {
            {"shaped_360_trade_box", "producer/block_trade_box"},
            {"shaped_361_trade_box_advanced", "producer/block_trade_box_advanced"},
            {"shaped_362_trade_box_upgrade", "normal/item_trade_box_upgrade"},
            {"shaped_359_trade_terminal", "producer/block_trade_terminal"}};
        for (String[] e : expect) {
            var holder = manager.byKey(id(e[0])).orElse(null);
            h.assertTrue(holder != null && holder.value().getType() == RecipeType.CRAFTING, e[0]);
            ItemStack result = holder.value().getResultItem(registries);
            h.assertTrue(result.is(BuiltInRegistries.ITEM.get(id(e[1]))), e[0] + " makes " + e[1]);
        }
        Item advanced = BuiltInRegistries.ITEM.get(id("producer/block_trade_box_advanced"));
        boolean terminal = false, upgrade = false;
        for (Ingredient in : manager.byKey(id("shaped_359_trade_terminal")).orElseThrow().value().getIngredients()) terminal |= in.test(new ItemStack(advanced));
        for (Ingredient in : manager.byKey(id("shaped_361_trade_box_advanced")).orElseThrow().value().getIngredients())
            upgrade |= in.test(new ItemStack(BuiltInRegistries.ITEM.get(id("producer/block_trade_box"))));
        h.assertTrue(terminal, "the Trade Terminal takes an Advanced Trade Box");
        h.assertTrue(upgrade, "the Advanced Trade Box is made of a basic one");
        h.succeed();
    }
}
