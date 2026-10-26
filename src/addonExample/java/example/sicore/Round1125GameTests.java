package example.sicore;

import io.netty.buffer.Unpooled;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * GameTests of 0.1.10.25 (X1): selling to the market follows the two-level sell depth instead of the daily limit (any
 * amount, the payout of the closed form); the sell box sells now or into the daily auction, which pays a lone seller
 * what an instant sale pays and two equal sellers the same; buying has its own limit (none by default); the industrial
 * commodities, the per-commodity market fields and the recipe fixes are loaded; the operator tools answer; the sell page
 * slots and the new saved data work. The exchange is internal: reached by reflection.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
@SuppressWarnings("unchecked")
public final class Round1125GameTests {
    private Round1125GameTests() {}

    private static Object call(Object target, String name, Object... args) { return Round33GameTests.call(target, name, args); }

    private static Class<?> common(String name) { return Round33GameTests.common(name); }

    private static Class<?> service() { return common("exchange.ExchangeService"); }

    private static Class<?> depth() { return common("exchange.SellDepth"); }

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

    private static <T> T field(Object target, String name) { return (T) get(target, name); }

    private static Object data(MinecraftServer server) { return call(common("exchange.ExchangeData"), "get", server); }

    private static ResourceLocation rl(String path) { return ResourceLocation.fromNamespaceAndPath("mio_icif", path); }

    private static Item coin() { return BuiltInRegistries.ITEM.get(rl("normal/item_coin")); }

    private static int count(ServerPlayer player, Item item) {
        int n = 0;
        for (ItemStack s : player.getInventory().items) if (s.is(item)) n += s.getCount();
        return n;
    }

    /** Coins in the inventory plus coins in the exchange mailbox. */
    private static long coins(Object d, ServerPlayer player) {
        Object box = ((Map<UUID, Object>) field(d, "mail")).get(player.getUUID());
        return count(player, coin()) + (box == null ? 0 : (long) get(box, "coins"));
    }

    private static void expect(GameTestHelper h, Object result, boolean ok, String key, String what) {
        String got = (String) call(result, "key");
        h.assertTrue((boolean) call(result, "ok") == ok && got.endsWith("." + key), what + ": " + got);
    }

    private static void leave(MinecraftServer server, ServerPlayer... players) {
        for (ServerPlayer p : players) server.getPlayerList().remove(p);
    }

    private static long today(MinecraftServer server) { return (long) call(service(), "today", server); }

    private static double payout(Object curve, double mine, double pressure, double q) {
        return (double) call(depth(), "payout", curve, mine, pressure, q);
    }

    /**
     * A datapack commodity the player has unlocked, priced at most {@code maxPrice} and at least half its base price (so
     * a few hundred items stay above the lowest accepted price), with no resting bid of players (a sale meets the
     * market only).
     */
    private static Object pick(MinecraftServer server, ServerPlayer player, long maxPrice) {
        Class<?> manager = common("future.FutureCommodityManager");
        Object d = data(server);
        for (Object c : (List<?>) call(manager, "getCommodities")) {
            if (!(boolean) call(manager, "isUnlocked", player, c)) continue;
            Object m = call(service(), "market", server, (String) call(c, "getItemId"));
            if (m == null || !(boolean) call(m, "system")) continue;
            long price = (long) call(m, "price");
            if (price > maxPrice || price * 2 < (int) call(c, "getBasePrice")) continue;
            Object book = call(d, "book", (String) call(m, "id"), constant("exchange.ExchangeData$Currency", "COIN"));
            if ((long) call(get(book, "book"), "best", true) != 0) continue;
            return m;
        }
        return null;
    }

    /** Puts back what a test changed in the exchange data of the server. */
    private static void restore(Object d, String id, Double pressure, long stock, ServerPlayer... players) {
        Map<String, Double> p = field(d, "pressure");
        if (pressure == null) p.remove(id); else p.put(id, pressure);
        ((Map<String, Long>) field(d, "treasury")).put(id, stock);
        for (ServerPlayer player : players) {
            ((Map<?, ?>) field(d, "sold")).remove(player.getUUID());
            ((Map<?, ?>) field(d, "allowance")).remove(player.getUUID());
            ((Map<?, ?>) field(d, "mail")).remove(player.getUUID());
            ((List<Object>) field(d, "auction")).removeIf(a -> player.getUUID().equals(get(a, "owner")));
        }
    }

    private static void give(ServerPlayer player, Item item, int n) {
        while (n > 0) {
            int s = Math.min(n, item.getDefaultMaxStackSize());
            player.getInventory().add(new ItemStack(item, s));
            n -= s;
        }
    }

    // ------------------------------------------------------------------ sell depth instead of the daily limit

    /**
     * 300 items in one sale (the old rule took 100 a day): every item above the order's limit is sold, the coins are the
     * closed-form payout of the curve, and the sale adds to the seller's volume today, the sell pressure and the treasury.
     */
    @GameTest(batch = "round1125_sell", template = "empty", timeoutTicks = 20)
    public static void bulkSaleFollowsTheSellDepth(GameTestHelper h) {
        MinecraftServer server = h.getLevel().getServer();
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        Object d = data(server);
        Object m = pick(server, player, 160);
        if (m == null) { leave(server, player); h.succeed(); return; }
        String id = (String) call(m, "id");
        Item item = (Item) call(m, "item");
        long day = today(server);
        Double p0 = ((Map<String, Double>) field(d, "pressure")).get(id);
        long stock = ((Map<String, Long>) field(d, "treasury")).getOrDefault(id, 0L);
        try {
            Object curve = call(service(), "curve", m);
            double p = p0 == null ? 0 : p0;
            int quantity = 300;
            long low = (long) call(m, "low");
            long sold = (long) call(depth(), "maxForMarginal", curve, 0.0, p, (double) low, (long) quantity);
            long allowance = (long) call(service(), "allowanceLeft", server, d, player.getUUID(), day);
            if (allowance != Long.MAX_VALUE) sold = (long) call(depth(), "maxForCoins", curve, 0.0, p, (double) allowance, sold);
            long paid = (long) Math.floor(payout(curve, 0, p, sold));
            h.assertTrue(sold > 100, "the curve sells only " + sold + " of 300 at a limit of " + low);
            long bidBefore = (long) call(service(), "nextBid", d, m, player.getUUID(), day);
            give(player, item, quantity);
            expect(h, call(service(), "place", player, id, false, false, low, quantity), true, "sold", "bulk sale");
            h.assertTrue(count(player, item) == quantity - sold, "items left " + count(player, item) + ", expected " + (quantity - sold));
            h.assertTrue(coins(d, player) == paid, "paid " + coins(d, player) + ", the curve pays " + paid);
            h.assertTrue((long) call(service(), "soldToday", d, player.getUUID(), id, day) == sold, "volume today");
            h.assertTrue(Math.abs((double) call(service(), "pressure", d, id) - (p + sold)) < 1e-6, "sell pressure");
            h.assertTrue(((Map<String, Long>) field(d, "treasury")).getOrDefault(id, 0L) == stock + sold, "treasury");
            long bidAfter = (long) call(service(), "nextBid", d, m, player.getUUID(), day);
            h.assertTrue(bidAfter < bidBefore, "the bid did not fall: " + bidBefore + " -> " + bidAfter);
        } finally {
            restore(d, id, p0, stock, player);
            leave(server, player);
        }
        h.succeed();
    }

    /** Buying has its own limit, none by default; the defaults of the new settings are those of the design. */
    @GameTest(batch = "round1125_sell", template = "empty", timeoutTicks = 20)
    public static void buyingHasItsOwnLimit(GameTestHelper h) {
        Class<?> config = common("Singularity_Iteration_Config");
        Object[][] defaults = {{"FUTURE_BUY_ITEMS_PER_DAY", 0}, {"EXCHANGE_SELL_DEPTH_PLAYER", 256}, {"EXCHANGE_SELL_DEPTH_SERVER", 1024},
            {"EXCHANGE_SELL_EXPORT", 0.5}, {"EXCHANGE_SELL_ALLOWANCE", 50_000}, {"EXCHANGE_SELL_ALLOWANCE_DAYS", 3},
            {"EXCHANGE_SELL_ALLOWANCE_SINGLEPLAYER", false}};
        for (Object[] row : defaults) {
            Object value = call(get(config, (String) row[0]), "getDefault");
            h.assertTrue(row[1].equals(value), row[0] + " defaults to " + value);
        }
        MinecraftServer server = h.getLevel().getServer();
        Object market = call(common("future.FutureMarketData"), "get", server.overworld());
        UUID player = UUID.randomUUID();
        int cap = (int) call(get(config, "FUTURE_BUY_ITEMS_PER_DAY"), "get");
        call(market, "addPlayerVolume", player, 5000);
        int left = (int) call(service(), "buyLeft", server, player);
        h.assertTrue(cap > 0 ? left == Math.max(0, cap - 5000) : left == Integer.MAX_VALUE, "buy limit " + cap + ", left " + left);
        h.succeed();
    }

    // ------------------------------------------------------------------ sell box and daily auction

    /**
     * The sell box sells its plain commodity stacks now at the curve's payout and keeps a renamed stack; the next box
     * goes to the auction, whose estimate for a lone seller is the instant payout, and the clearing pays that to the
     * mailbox.
     */
    @GameTest(batch = "round1125_box", template = "empty", timeoutTicks = 20)
    public static void sellBoxSellsNowAndAtTheAuction(GameTestHelper h) {
        MinecraftServer server = h.getLevel().getServer();
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        Object d = data(server);
        Object m = pick(server, player, 160);
        if (m == null) { leave(server, player); h.succeed(); return; }
        String id = (String) call(m, "id");
        Item item = (Item) call(m, "item");
        long day = today(server);
        Double p0 = ((Map<String, Double>) field(d, "pressure")).get(id);
        long stock = ((Map<String, Long>) field(d, "treasury")).getOrDefault(id, 0L);
        List<Object> auction = field(d, "auction");
        try {
            h.assertTrue(auction.isEmpty(), "auction orders left by another test");
            Object curve = call(service(), "curve", m);
            double p = p0 == null ? 0 : p0;
            SimpleContainer box = new SimpleContainer(18);
            box.setItem(0, new ItemStack(item, 64));
            box.setItem(1, new ItemStack(item, 36));
            ItemStack named = new ItemStack(item, 5);
            named.set(DataComponents.CUSTOM_NAME, Component.literal("keep"));
            box.setItem(2, named);
            h.assertTrue(call(service(), "sellable", player, named) == null, "a renamed stack is sellable");
            long now = (long) call(depth(), "maxForMarginal", curve, 0.0, p, 1.0, 100L);
            long paid = (long) Math.floor(payout(curve, 0, p, now));
            expect(h, call(service(), "sellBox", player, box, false, 0), true, "box_sold", "box sold now");
            h.assertTrue(coins(d, player) == paid, "paid " + coins(d, player) + ", the curve pays " + paid);
            int left = box.getItem(0).getCount() + box.getItem(1).getCount();
            h.assertTrue(left == 100 - now, "box keeps " + left + " of the plain stacks, expected " + (100 - now));
            h.assertTrue(box.getItem(2).getCount() == 5 && box.getItem(2).has(DataComponents.CUSTOM_NAME), "the renamed stack was sold");

            box.setItem(0, new ItemStack(item, 50));
            box.setItem(1, ItemStack.EMPTY);
            double p1 = (double) call(service(), "pressure", d, id);
            long mine1 = (long) call(service(), "soldToday", d, player.getUUID(), id, day);
            expect(h, call(service(), "sellBox", player, box, true, 0), true, "auction_submitted", "box to the auction");
            h.assertTrue(box.getItem(0).isEmpty() && box.getItem(2).getCount() == 5, "the box after the submission");
            Object order = null;
            for (Object a : auction) if (player.getUUID().equals(get(a, "owner"))) order = a;
            h.assertTrue(order != null && (long) get(order, "quantity") == 50, "no auction order of 50");
            Object estimate = call(service(), "auctionEstimate", server, d, order, day);
            long alone = (long) Math.floor(payout(curve, mine1, p1, 50));
            long estCoins = (long) call(estimate, "coins");
            h.assertTrue((long) call(estimate, "quantity") == 50 && Math.abs(estCoins - alone) <= 1,
                "estimate " + call(estimate, "quantity") + " / " + estCoins + ", alone now " + alone);
            long before = coins(d, player);
            call(service(), "clearAuction", server, d, day);
            h.assertTrue(auction.isEmpty(), "the clearing kept orders");
            h.assertTrue(coins(d, player) - before == estCoins, "the clearing paid " + (coins(d, player) - before) + ", estimated " + estCoins);
            h.assertTrue(Math.abs((double) call(service(), "pressure", d, id) - (p1 + 50)) < 1e-6, "pressure after the clearing");
            h.assertTrue((long) call(service(), "soldToday", d, player.getUUID(), id, day) == mine1 + 50, "volume after the clearing");
        } finally {
            restore(d, id, p0, stock, player);
            leave(server, player);
        }
        h.succeed();
    }

    /**
     * Two players give the same amount to the auction: the clearing pays both the same, and each gets less than a lone
     * instant sale of that amount (the other's items weigh on the bid too).
     */
    @GameTest(batch = "round1125_auction", template = "empty", timeoutTicks = 20)
    public static void auctionPaysEqualSellersTheSame(GameTestHelper h) {
        MinecraftServer server = h.getLevel().getServer();
        ServerPlayer a = h.makeMockServerPlayerInLevel(), b = h.makeMockServerPlayerInLevel();
        Object d = data(server);
        Object m = pick(server, a, 160);
        if (m == null || !(boolean) call(common("future.FutureCommodityManager"), "isUnlocked", b, call(m, "commodity"))) {
            leave(server, a, b);
            h.succeed();
            return;
        }
        String id = (String) call(m, "id");
        Item item = (Item) call(m, "item");
        long day = today(server);
        Double p0 = ((Map<String, Double>) field(d, "pressure")).get(id);
        long stock = ((Map<String, Long>) field(d, "treasury")).getOrDefault(id, 0L);
        try {
            double p = p0 == null ? 0 : p0;
            long alone = (long) Math.floor(payout(call(service(), "curve", m), 0, p, 40));
            for (ServerPlayer player : List.of(a, b)) {
                SimpleContainer box = new SimpleContainer(18);
                box.setItem(0, new ItemStack(item, 40));
                expect(h, call(service(), "sellBox", player, box, true, 0), true, "auction_submitted", "submission");
            }
            call(service(), "clearAuction", server, d, day);
            long ca = coins(d, a), cb = coins(d, b);
            h.assertTrue(ca == cb, "equal sellers got " + ca + " and " + cb);
            h.assertTrue(ca > 0 && ca <= alone, "each got " + ca + ", a lone instant sale pays " + alone);
        } finally {
            restore(d, id, p0, stock, a, b);
            leave(server, a, b);
        }
        h.succeed();
    }

    // ------------------------------------------------------------------ sell page of the panel

    /**
     * The menu has the machine slot, 18 sell box slots and the 36 inventory slots; the access transformer opened the slot
     * position for the panel; Fill moves the sellable stacks of the main inventory (not the hotbar, not other items), the
     * view lists the box, and the box comes back to the inventory.
     */
    @GameTest(batch = "round1125_box", template = "empty", timeoutTicks = 20)
    public static void sellPageSlots(GameTestHelper h) throws ReflectiveOperationException {
        h.assertTrue(Modifier.isPublic(Slot.class.getField("x").getModifiers()) && !Modifier.isFinal(Slot.class.getField("x").getModifiers())
            && !Modifier.isFinal(Slot.class.getField("y").getModifiers()), "the access transformer did not open Slot.x / Slot.y");
        MinecraftServer server = h.getLevel().getServer();
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        Object m = pick(server, player, Long.MAX_VALUE);
        if (m == null) { leave(server, player); h.succeed(); return; }
        Item item = (Item) call(m, "item");
        AbstractContainerMenu before = player.containerMenu;
        try {
            Class<?> type = common("menu.producer.FutureElcMenu");
            AbstractContainerMenu menu = (AbstractContainerMenu) type.getConstructor(int.class, Inventory.class).newInstance(1, player.getInventory());
            h.assertTrue(menu.slots.size() == 1 + 18 + 36, "menu slots " + menu.slots.size());
            player.getInventory().setItem(0, new ItemStack(item, 7));                 // hotbar
            player.getInventory().setItem(9, new ItemStack(item, 20));                // main inventory
            player.getInventory().setItem(10, new ItemStack(Items.BARRIER, 3));
            call(menu, "fillSellBox", player);
            SimpleContainer box = (SimpleContainer) call(menu, "sellBox");
            int inBox = 0;
            for (int i = 0; i < box.getContainerSize(); i++) if (box.getItem(i).is(item)) inBox += box.getItem(i).getCount();
            h.assertTrue(inBox == 20, "Fill moved " + inBox + " items");
            h.assertTrue(player.getInventory().getItem(0).getCount() == 7 && player.getInventory().getItem(10).is(Items.BARRIER),
                "Fill took the hotbar or an item that is not a commodity");
            h.assertTrue(!menu.slots.get(1).mayPlace(new ItemStack(Items.BARRIER)), "the box takes an item that is not a commodity");
            player.containerMenu = menu;
            Object view = call(common("exchange.ExchangeViews"), "build", player, "", true);
            List<?> lines = (List<?>) call(view, "box");
            h.assertTrue(lines.size() == 1 && (long) call(lines.get(0), "quantity") == 20, "box lines " + lines);
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
            call(view, "write", buf);
            Object back = call(common("exchange.ExchangeView"), "read", buf);
            h.assertTrue(buf.readableBytes() == 0 && call(back, "box").equals(lines), "the box lines changed in the codec");
            call(menu, "returnSellBox", player);
            h.assertTrue(box.isEmpty() && count(player, item) == 27, "the box did not come back: " + count(player, item));
        } finally {
            player.containerMenu = before;
            ((Map<?, ?>) field(data(server), "mail")).remove(player.getUUID());
            leave(server, player);
        }
        h.succeed();
    }

    // ------------------------------------------------------------------ saved data

    /** Sell pressure, the volume of today, allowances and auction orders survive a save / load round trip. */
    @GameTest(batch = "round1125_data", template = "empty", timeoutTicks = 20)
    public static void sellStateRoundTrip(GameTestHelper h) {
        var provider = h.getLevel().registryAccess();
        Class<?> type = common("exchange.ExchangeData");
        Object d = call(type, "load", new CompoundTag(), provider);
        UUID someone = UUID.randomUUID();
        ((Map<String, Double>) field(d, "pressure")).put("minecraft:iron_ingot", 123.5);
        set(d, "pressureDay", 6L);
        ((Map<String, Long>) call(d, "sold", someone, 6L)).put("minecraft:iron_ingot", 40L);
        ((Map<UUID, long[]>) field(d, "allowance")).put(someone, new long[] {1234L, 6L});
        Object a = make("exchange.ExchangeData$AuctionOrder");
        set(a, "id", 9L); set(a, "owner", someone); set(a, "ownerName", "someone"); set(a, "item", "minecraft:iron_ingot");
        set(a, "quantity", 77L); set(a, "minPercent", 40); set(a, "day", 6L);
        ((List<Object>) field(d, "auction")).add(a);
        CompoundTag first = (CompoundTag) call(d, "save", new CompoundTag(), provider);
        Object again = call(type, "load", first.copy(), provider);
        CompoundTag second = (CompoundTag) call(again, "save", new CompoundTag(), provider);
        h.assertTrue(first.equals(second), "the exchange data changed in a save / load round trip");
        h.assertTrue(((Map<String, Double>) field(again, "pressure")).get("minecraft:iron_ingot") == 123.5, "pressure lost");
        h.assertTrue(((Map<String, Long>) call(again, "sold", someone, 6L)).getOrDefault("minecraft:iron_ingot", 0L) == 40L, "volume of the day lost");
        h.assertTrue(((Map<UUID, long[]>) field(again, "allowance")).get(someone)[0] == 1234L, "allowance lost");
        List<?> orders = field(again, "auction");
        h.assertTrue(orders.size() == 1 && (int) get(orders.get(0), "minPercent") == 40 && (long) get(orders.get(0), "quantity") == 77L,
            "auction order lost");
        h.succeed();
    }

    // ------------------------------------------------------------------ industrial goods, market fields, recipe fixes

    /**
     * The industrial category is loaded with its per-commodity depths and period; raw ores are never imported; the
     * cookie costs what its ingredients are worth; the two recipes that multiplied items are fixed.
     */
    @GameTest(batch = "round1125_data", template = "empty", timeoutTicks = 20)
    public static void industrialGoodsAndRecipeFixes(GameTestHelper h) {
        Class<?> manager = common("future.FutureCommodityManager");
        Object tin = call(manager, "getCommodityById", "mio_icif:resource/item_ingot_tin");
        h.assertTrue(tin != null && "INDUSTRIAL".equals(((Enum<?>) call(tin, "getCategory")).name()), "tin is not an industrial commodity");
        for (String id : List.of("mio_icif:resource/item_ingot_bronze", "mio_icif:resource/item_rubber", "mio_icif:normal/item_circuit",
                "mio_icif:resource/item_alloy_ingot", "mio_icif:resource/item_carbon_plate", "mio_icif:resource/item_adviron_ingot")) {
            h.assertTrue((boolean) call(manager, "hasCommodity", id), "missing commodity " + id);
        }
        Object iridium = call(manager, "getCommodityById", "mio_icif:resource/item_iridium");
        h.assertTrue(iridium != null, "no iridium commodity");
        Object market = call(iridium, "getMarket");
        h.assertTrue((int) call(market, "depthPlayer") == 64 && (int) call(market, "depthServer") == 256
            && (int) call(iridium, "getPeriodDays") == 3, "iridium market " + market);
        Object rawIron = call(manager, "getCommodityById", "minecraft:raw_iron");
        if (rawIron != null) {
            h.assertTrue((int) call(call(rawIron, "getMarket"), "importPerDay") == 0, "raw iron is imported");
            h.assertTrue((int) call(service(), "importPerDay", "minecraft:raw_iron") == 0, "the service imports raw iron");
        }
        Object cookie = call(manager, "getCommodityById", "minecraft:cookie");
        if (cookie != null) h.assertTrue((int) call(cookie, "getBasePrice") == 11, "cookie price " + call(cookie, "getBasePrice"));

        MinecraftServer server = h.getLevel().getServer();
        var recipes = server.getRecipeManager();
        var can = recipes.byKey(rl("powder/tin_empty_can_to_tin_dust")).orElseThrow().value();
        h.assertTrue((int) call(can, "getIngredientCount") == 2, "tin cans per tin dust: " + call(can, "getIngredientCount"));
        var stairs = recipes.byKey(rl("powder/quartz_stairs_to_quartz")).orElseThrow().value();
        ItemStack quartz = (ItemStack) call(common("util.RecipeResults"), "of", stairs, server.registryAccess());
        h.assertTrue(quartz.is(Items.QUARTZ) && quartz.getCount() == 4, "quartz from stairs: " + quartz);
        h.succeed();
    }

    /** The audit finds a recipe pair that multiplies an item and a loop that buys, processes and sells with profit. */
    @GameTest(batch = "round1125_data", template = "empty", timeoutTicks = 20)
    public static void arbitrageAuditFindsLoopsAndCycles(GameTestHelper h) {
        Class<?> audit = common("exchange.ArbitrageAudit");
        List<Object> recipes = new ArrayList<>();
        // the bug of 0.1.10.24 (tin): ore -> 1 ingot, 1 ingot -> 10 cans, 1 can -> 1 dust, 1 dust -> 1 ingot
        recipes.add(make("exchange.ArbitrageAudit$Recipe", "test:smelt", Items.IRON_INGOT, 1, List.of(Set.of(Items.RAW_IRON)), 0.0, 0.0));
        recipes.add(make("exchange.ArbitrageAudit$Recipe", "test:dust", Items.GUNPOWDER, 1, List.of(Set.of(Items.BUCKET)), 0.0, 0.0));
        recipes.add(make("exchange.ArbitrageAudit$Recipe", "test:ingot", Items.IRON_INGOT, 1, List.of(Set.of(Items.GUNPOWDER)), 0.0, 0.0));
        List<Object> fixed = new ArrayList<>(recipes);
        recipes.add(make("exchange.ArbitrageAudit$Recipe", "test:cans", Items.BUCKET, 10, List.of(Set.of(Items.IRON_INGOT)), 0.0, 0.0));
        Set<?> cycles = (Set<?>) call(audit, "cycles", recipes);
        h.assertTrue(cycles.contains(Items.IRON_INGOT), "the multiplying cycle was not found: " + cycles);
        fixed.add(make("exchange.ArbitrageAudit$Recipe", "test:cans", Items.BUCKET, 1, List.of(Set.of(Items.IRON_INGOT)), 0.0, 0.0));
        h.assertTrue(((Set<?>) call(audit, "cycles", fixed)).isEmpty(), "a cycle without a gain was reported");
        // ore doubling: one raw iron (ask 100) makes two ingots (bid 80 each)
        List<Object> doubling = List.of(make("exchange.ArbitrageAudit$Recipe", "test:double", Items.IRON_INGOT, 2,
            List.of(Set.of(Items.RAW_IRON)), 0.0, 400.0));
        List<?> loops = (List<?>) call(audit, "loops", Map.of(Items.RAW_IRON, 100.0), Map.of(Items.IRON_INGOT, 80.0), doubling);
        h.assertTrue(loops.size() == 1 && Math.abs((double) call(loops.get(0), "profit") - 30.0) < 1e-9
            && Math.abs((double) call(loops.get(0), "energy") - 200.0) < 1e-9, "loops " + loops);
        h.succeed();
    }

    // ------------------------------------------------------------------ operator tools

    /** Collects what a command sends back. */
    private static final class Capture implements CommandSource {
        final List<Component> messages = new ArrayList<>();

        @Override
        public void sendSystemMessage(Component component) { messages.add(component); }

        @Override
        public boolean acceptsSuccess() { return true; }

        @Override
        public boolean acceptsFailure() { return true; }

        @Override
        public boolean shouldInformAdmins() { return false; }

        Object[] args(String key) {
            for (Component c : messages) {
                if (c.getContents() instanceof TranslatableContents t && t.getKey().equals("command.mio_icif.market_tools." + key)) return t.getArgs();
            }
            return null;
        }
    }

    private static double number(Object formatted) { return Double.parseDouble(String.valueOf(formatted).replace(",", "")); }

    /**
     * simulate: the auction estimate of a lone seller is the instant payout; arbitrage: the server's recipes multiply no
     * tin or quartz item any more; calibrate and ledger answer.
     */
    @GameTest(batch = "round1125_tools", template = "empty", timeoutTicks = 20)
    public static void operatorToolsAnswer(GameTestHelper h) {
        MinecraftServer server = h.getLevel().getServer();
        Capture capture = new Capture();
        CommandSourceStack source = server.createCommandSourceStack().withSource(capture).withPermission(4);
        List<?> commodities = (List<?>) call(common("future.FutureCommodityManager"), "getCommodities");
        if (!commodities.isEmpty()) {
            String id = (String) call(commodities.get(0), "getItemId");
            server.getCommands().performPrefixedCommand(source, "mio_icif market simulate " + id + " 1000");
            Object[] sale = capture.args("simulate_sale");
            h.assertTrue(sale != null, "simulate sent " + capture.messages);
            double coins = number(sale[1]);
            long alone = ((Number) sale[4]).longValue();
            h.assertTrue(Math.abs(Math.floor(coins) - alone) <= 1, "simulate: now " + coins + ", auction alone " + alone);
        }
        server.getCommands().performPrefixedCommand(source, "mio_icif market arbitrage 50");
        Object[] head = capture.args("arbitrage");
        h.assertTrue(head != null && ((Number) head[0]).intValue() > 0, "arbitrage sent " + capture.messages);
        Object[] cycles = capture.args("cycles");
        if (cycles != null) {
            String names = String.valueOf(cycles[0]);
            h.assertTrue(!names.contains("tin") && !names.contains("quartz"), "recipe cycles: " + names + " (" + head[3] + ")");
        }
        server.getCommands().performPrefixedCommand(source, "mio_icif market calibrate");
        h.assertTrue(capture.args("calibrate") != null, "calibrate did not answer");
        server.getCommands().performPrefixedCommand(source, "mio_icif market ledger 3");
        h.assertTrue(capture.args("ledger") != null, "ledger did not answer");
        h.succeed();
    }
}
