package example.sicore;

import io.netty.buffer.Unpooled;
import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * GameTests of 0.1.10.29: the quantum suit, the MFSU upgrade kit and the mass fabricator take the Lapotron Crystal; the block cutter
 * cuts every vanilla wood (and the rubber wood) into its own planks, 6 a log, 3 a bamboo block; the coin account (payments from the
 * inventory and the account in the mode's order, deposit, withdraw, bank mode receipts and Collect); the minimum-price sale of the
 * sell box (Q* items, one payout); a percent minimum of a terminal order; the curves and the bank mode in the panel view. Internals
 * are reached by reflection.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
@SuppressWarnings({"unchecked", "rawtypes"})
public final class Round1129GameTests {
    private Round1129GameTests() {}

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

    private static Object data(MinecraftServer server) { return call(common("exchange.ExchangeData"), "get", server); }

    private static ResourceLocation rl(String path) { return ResourceLocation.fromNamespaceAndPath("mio_icif", path); }

    private static Item mio(String path) { return BuiltInRegistries.ITEM.get(rl(path)); }

    private static Item coin() { return mio("normal/item_coin"); }

    private static int count(ServerPlayer player, Item item) {
        int n = 0;
        for (ItemStack s : player.getInventory().items) if (s.is(item)) n += s.getCount();
        return n;
    }

    private static Object box(Object d, UUID id) { return call(d, "mailbox", id); }

    private static long account(Object d, ServerPlayer p) { return (long) get(box(d, p.getUUID()), "coins"); }

    private static void give(ServerPlayer player, Item item, int n) {
        while (n > 0) {
            int s = Math.min(n, item.getDefaultMaxStackSize());
            player.getInventory().add(new ItemStack(item, s));
            n -= s;
        }
    }

    private static void forget(MinecraftServer server, Object d, ServerPlayer... players) {
        for (ServerPlayer p : players) {
            ((Map<?, ?>) get(d, "mail")).remove(p.getUUID());
            ((Map<?, ?>) get(d, "sold")).remove(p.getUUID());
            ((Map<?, ?>) get(d, "allowance")).remove(p.getUUID());
            ((Map<?, ?>) get(d, "lastSale")).remove(p.getUUID());
            ((Set<?>) get(d, "bankMode")).remove(p.getUUID());
            server.getPlayerList().remove(p);
        }
    }

    /** An unlocked datapack market of coins with a price in [low, high], or null. */
    private static Object market(MinecraftServer server, long low, long high) {
        for (Object c : (List<?>) call(common("future.FutureCommodityManager"), "getCommodities")) {
            if (call(c, "getUnlockAdvancement") != null) continue;
            Object m = call(service(), "market", server, (String) call(c, "getItemId"));
            if (m == null || !(boolean) call(m, "system") || !"COIN".equals(call(call(m, "currency"), "name"))) continue;
            long price = (long) call(m, "price");
            if (price >= low && price <= high) return m;
        }
        return null;
    }

    // ------------------------------------------------------------------ item 1: the recipes

    /** The quantum armour, the MFSU upgrade kit and the mass fabricator take the Lapotron Crystal, not the Lapotron Crystal Battery. */
    @GameTest(batch = "round1129_recipes", template = "empty", timeoutTicks = 20)
    public static void lapotronRecipesTakeTheCrystal(GameTestHelper h) {
        var manager = h.getLevel().getServer().getRecipeManager();
        Item crystal = mio("normal/item_lapotron_crystal_lev0"), battery = mio("normal/item_lamacrystal_chargebat_lev0");
        for (String id : new String[] {"shaped_101_quantum_boots", "shaped_102_quantum_chestplate", "shaped_103_quantum_helmet",
                "shaped_104_quantum_leggings", "shaped_11_mfsu_upgrade_kit", "shaped_64_mass_fabricator"}) {
            Optional<RecipeHolder<?>> r = manager.byKey(rl(id));
            h.assertTrue(r.isPresent(), "recipe " + id + " is missing");
            boolean takesCrystal = false, takesBattery = false;
            for (Ingredient i : r.get().value().getIngredients()) {
                takesCrystal |= i.test(new ItemStack(crystal));
                takesBattery |= i.test(new ItemStack(battery)) && !i.test(new ItemStack(crystal));
            }
            h.assertTrue(takesCrystal && !takesBattery, id + ": crystal " + takesCrystal + ", battery " + takesBattery);
        }
        h.succeed();
    }

    // ------------------------------------------------------------------ item 2: the block cutter

    /** Every vanilla wood (logs, stems, stripped logs, bamboo blocks) and the rubber wood are cut into their own planks. */
    @GameTest(batch = "round1129_cutter", template = "empty", timeoutTicks = 20)
    public static void blockCutterCutsEveryWood(GameTestHelper h) {
        var level = h.getLevel();
        RecipeType type = (RecipeType) call(get(common("recipe.block_cutter.mio_icif_BlockCutterRecipes"), "BLOCK_CUTTER_TYPE"), "get");
        Object[][] cases = {
            {Items.SPRUCE_LOG, Items.SPRUCE_PLANKS, 6}, {Items.CHERRY_LOG, Items.CHERRY_PLANKS, 6}, {Items.MANGROVE_WOOD, Items.MANGROVE_PLANKS, 6},
            {Items.CRIMSON_STEM, Items.CRIMSON_PLANKS, 6}, {Items.STRIPPED_WARPED_STEM, Items.WARPED_PLANKS, 6}, {Items.STRIPPED_OAK_LOG, Items.OAK_PLANKS, 6},
            {Items.DARK_OAK_LOG, Items.DARK_OAK_PLANKS, 6}, {Items.BAMBOO_BLOCK, Items.BAMBOO_PLANKS, 3},
            {mio("block_rub_wood"), mio("build/rubber_planks"), 6}};
        for (Object[] c : cases) {
            ItemStack in = new ItemStack((Item) c[0]);
            RecipeInput input = (RecipeInput) make("recipe.block_cutter.mio_icif_BlockCutterRecipeInput", in);
            Optional<RecipeHolder<Recipe<RecipeInput>>> r = level.getRecipeManager().getRecipeFor(type, input, level);
            h.assertTrue(r.isPresent(), "no cutter recipe for " + in);
            ItemStack out = (ItemStack) call(r.get().value(), "resultFor", level, in);
            h.assertTrue(out.is((Item) c[1]) && out.getCount() == (int) c[2], in + " -> " + out + ", expected " + c[2] + " " + c[1]);
        }
        RecipeInput dirt = (RecipeInput) make("recipe.block_cutter.mio_icif_BlockCutterRecipeInput", new ItemStack(Items.DIRT));
        h.assertTrue(level.getRecipeManager().getRecipeFor(type, dirt, level).isEmpty(), "dirt has a cutter recipe");
        h.succeed();
    }

    // ------------------------------------------------------------------ item 4: the coin account

    /** Payments take the inventory first (bank mode: the account first); deposit, withdraw, receipts and Collect in bank mode. */
    @GameTest(batch = "round1129_account", template = "empty", timeoutTicks = 20)
    public static void coinAccountPaysAndReceives(GameTestHelper h) {
        MinecraftServer server = h.getLevel().getServer();
        Object d = data(server);
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        UUID id = player.getUUID();
        try {
            give(player, coin(), 100);
            set(box(d, id), "coins", 5_000L);
            Object plan = make("exchange.InventoryPlan", player.getInventory());
            h.assertTrue((boolean) call(service(), "takeCoins", plan, d, id, 3_000L), "a payment of 3,000 from 100 carried + 5,000 on the account failed");
            call(plan, "commit");
            h.assertTrue(count(player, coin()) == 0 && account(d, player) == 2_100, "normal mode: inventory first; left " + count(player, coin())
                + " carried, " + account(d, player) + " on the account");
            Object short1 = make("exchange.InventoryPlan", player.getInventory());
            h.assertTrue(!(boolean) call(service(), "takeCoins", short1, d, id, 2_101L) && account(d, player) == 2_100, "a payment above both was taken");
            give(player, coin(), 400);
            call(service(), "setBankMode", player, true);
            Object plan2 = make("exchange.InventoryPlan", player.getInventory());
            h.assertTrue((boolean) call(service(), "takeCoins", plan2, d, id, 1_000L), "bank mode payment failed");
            call(plan2, "commit");
            h.assertTrue(count(player, coin()) == 400 && account(d, player) == 1_100, "bank mode: account first; left " + count(player, coin()) + " / " + account(d, player));
            Object plan3 = make("exchange.InventoryPlan", player.getInventory());
            call(service(), "pay", plan3, d, id, get(common("exchange.ExchangeData$Currency"), "COIN"), 777L);
            call(plan3, "commit");
            h.assertTrue(count(player, coin()) == 400 && account(d, player) == 1_877, "bank mode receipt went to the inventory");
            Object dep = call(service(), "depositCoins", player, 0L);
            h.assertTrue((boolean) call(dep, "ok") && count(player, coin()) == 0 && account(d, player) == 2_277, "deposit of all coins: " + call(dep, "key"));
            Object wd = call(service(), "withdrawCoins", player, 500L);
            h.assertTrue((boolean) call(wd, "ok") && count(player, coin()) == 500 && account(d, player) == 1_777, "withdraw 500: " + call(wd, "key"));
            call(service(), "collect", player);
            h.assertTrue(count(player, coin()) == 500 && account(d, player) == 1_777, "Collect took coins out in bank mode");
            call(service(), "setBankMode", player, false);
            call(service(), "collect", player);
            h.assertTrue(count(player, coin()) == 2_277 && account(d, player) == 0, "Collect after bank mode: " + count(player, coin()) + " / " + account(d, player));
        } finally {
            forget(server, d, player);
        }
        h.succeed();
    }

    // ------------------------------------------------------------------ item 5: the minimum-price sale

    /** A minimum of 90 % sells of the box the Q* items paid at least the minimum, in one payout; the rest stays in the box. */
    @GameTest(batch = "round1129_minimum", template = "empty", timeoutTicks = 20)
    public static void sellBoxSellsAtTheMinimum(GameTestHelper h) {
        MinecraftServer server = h.getLevel().getServer();
        Object d = data(server);
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        Object m = market(server, 20, 1_000_000);
        if (m == null) { forget(server, d, player); h.succeed(); return; }
        String id = (String) call(m, "id");
        Item item = (Item) call(m, "item");
        Map<String, Double> pressure = (Map<String, Double>) get(d, "pressure");
        Double p0 = pressure.get(id);
        long stock = ((Map<String, Long>) get(d, "treasury")).getOrDefault(id, 0L);
        try {
            SimpleContainer sellBox = new SimpleContainer(18);
            for (int i = 0; i < 18; i++) sellBox.setItem(i, new ItemStack(item, Math.min(64, item.getDefaultMaxStackSize())));
            long offered = 0;
            for (int i = 0; i < 18; i++) offered += sellBox.getItem(i).getCount();
            Object curve = call(service(), "curve", m);
            double p = p0 == null ? 0 : p0;
            long limit = (long) call(common("exchange.MinPrice"), "limit", (double) call(curve, "unit"), true, 90L);
            long q = (long) call(common("exchange.SellDepth"), "maxForItemPrice", curve, 0.0, p, (double) limit, offered);
            long allowance = (long) call(service(), "allowanceLeft", server, d, player.getUUID(), (long) call(service(), "today", server));
            if (allowance != Long.MAX_VALUE) q = (long) call(common("exchange.SellDepth"), "maxForCoins", curve, 0.0, p, (double) allowance, q);
            long expected = (long) Math.floor((double) call(common("exchange.SellDepth"), "payout", curve, 0.0, p, (double) q));
            long before = count(player, coin()) + account(d, player);
            Object r = call(service(), "sellBox", player, sellBox, false, 90, false, 0L);
            long left = 0;
            for (int i = 0; i < 18; i++) if (sellBox.getItem(i).is(item)) left += sellBox.getItem(i).getCount();
            if (q == 0) {
                h.assertTrue(!(boolean) call(r, "ok") && left == offered, "nothing is paid 90 % but items sold");
            } else {
                h.assertTrue((boolean) call(r, "ok"), "sale: " + call(r, "key"));
                h.assertTrue(left == offered - q, "sold " + (offered - left) + ", Q* = " + q);
                h.assertTrue(count(player, coin()) + account(d, player) - before == expected, "paid " + (count(player, coin()) + account(d, player) - before)
                    + ", one payout of Q* items " + expected);
                double last = (double) call(common("exchange.SellDepth"), "itemPrice", curve, 0.0, p, q);
                h.assertTrue(last >= limit, "the last item was paid " + last + " under the minimum " + limit);
            }
        } finally {
            if (p0 == null) pressure.remove(id); else pressure.put(id, p0);
            ((Map<String, Long>) get(d, "treasury")).put(id, stock);
            forget(server, d, player);
        }
        h.succeed();
    }

    /** A terminal sell order with a percent minimum keeps it and sells at the percent of the bid without pressure in coins. */
    @GameTest(batch = "round1129_minimum", template = "empty", timeoutTicks = 20)
    public static void terminalOrderPercentMinimum(GameTestHelper h) {
        MinecraftServer server = h.getLevel().getServer();
        Object m = market(server, 1, Long.MAX_VALUE);
        if (m == null) { h.succeed(); return; }
        Object order = make("exchange.TerminalRules$Order", get(common("exchange.TerminalRules$Side"), "SELL"), call(m, "id"), 0L,
            get(common("exchange.TerminalRules$Mode"), "INSTANT"), 90L, true, true);
        h.assertTrue((boolean) call(order, "percent") && (boolean) call(call(order, "withEnabled", false), "percent"), "the order lost its percent");
        long limit = (long) call(common("exchange.TerminalOrders"), "limitOf", order, m);
        long expected = (long) call(common("exchange.MinPrice"), "limit", (double) call(call(service(), "curve", m), "unit"), true, 90L);
        h.assertTrue(limit == expected, "limit " + limit + ", 90 % of the bid without pressure " + expected);
        h.succeed();
    }

    // ------------------------------------------------------------------ the panel view

    /** The view carries the curve of the selected market and the bank mode, and decodes to the same. */
    @GameTest(batch = "round1129_account", template = "empty", timeoutTicks = 20)
    public static void viewCarriesCurvesAndBankMode(GameTestHelper h) {
        MinecraftServer server = h.getLevel().getServer();
        Object d = data(server);
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        Object m = market(server, 1, Long.MAX_VALUE);
        try {
            call(service(), "setBankMode", player, true);
            String selected = m == null ? "" : (String) call(m, "id");
            Object view = call(common("exchange.ExchangeViews"), "build", player, selected, true);
            h.assertTrue((boolean) call(view, "bankMode"), "the view does not show bank mode");
            List<?> curves = (List<?>) call(view, "curves");
            if (m != null) h.assertTrue(curves.size() == 1 && selected.equals(call(curves.get(0), "id")), "curves " + curves);
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
            call(view, "write", buf);
            Object back = call(common("exchange.ExchangeView"), "read", buf);
            h.assertTrue(buf.readableBytes() == 0 && call(back, "curves").equals(curves) && (boolean) call(back, "bankMode"), "the codec changed the view");
        } finally {
            forget(server, d, player);
        }
        h.succeed();
    }
}
