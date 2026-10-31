package example.sicore;

import com.miophas.singularity_iteration.core.api.exchange.BountyCompletedEvent;
import com.miophas.singularity_iteration.core.api.exchange.BountyInfo;
import com.miophas.singularity_iteration.core.api.exchange.BountyNeed;
import com.miophas.singularity_iteration.core.api.exchange.BountyPostingEvent;
import com.miophas.singularity_iteration.core.api.exchange.BountyReward;
import com.miophas.singularity_iteration.core.api.exchange.BountySpec;
import com.miophas.singularity_iteration.core.api.exchange.BountySplit;
import com.miophas.singularity_iteration.core.api.exchange.BountyTier;
import com.miophas.singularity_iteration.core.api.exchange.ExchangeApi;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * GameTests of 0.1.10.26 (X3): system bounties through the public API (post a {@link BountySpec}, deliver from the
 * inventory, rewards to the inventory / mailbox / UU account, the completed event), the posting event (change, cancel),
 * the supply block, reserved treasury rewards coming back, the weekly cap of made rewards, the shipped datapack tables and
 * the saved data. Internals (delivery, exchange data) are reached by reflection.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
@SuppressWarnings("unchecked")
public final class Round1126GameTests {
    private Round1126GameTests() {}

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

    private static Object data(MinecraftServer server) { return call(common("exchange.ExchangeData"), "get", server); }

    private static long today(MinecraftServer server) { return (long) call(common("exchange.ExchangeService"), "today", server); }

    private static Item coin() { return BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("mio_icif", "normal/item_coin")); }

    private static int count(ServerPlayer player, Item item) {
        int n = 0;
        for (ItemStack s : player.getInventory().items) if (s.is(item)) n += s.getCount();
        return n;
    }

    private static BountyInfo find(MinecraftServer server, long id) {
        for (BountyInfo b : ExchangeApi.bounties(server)) if (b.id() == id) return b;
        return null;
    }

    // a marker in the needs tells the test listener what to do: 7 dirt = cancel, 9 dirt = pay 1 coin instead
    private static boolean listening;
    private static final AtomicInteger COMPLETED = new AtomicInteger();

    private static void listen() {
        if (listening) return;
        listening = true;
        NeoForge.EVENT_BUS.addListener((BountyPostingEvent e) -> {
            for (BountyNeed n : e.getNeeds()) {
                if (!n.item().equals("minecraft:dirt")) continue;
                if (n.count() == 7) e.setCanceled(true);
                if (n.count() == 9) { e.getRewards().clear(); e.getRewards().add(BountyReward.coins(1)); e.setDays(2); }
            }
        });
        NeoForge.EVENT_BUS.addListener((BountyCompletedEvent e) -> COMPLETED.incrementAndGet());
    }

    private static BountySpec spec(BountyTier tier, String item, long count, List<BountyReward> rewards) {
        return spec(tier, item, count, rewards, 0);
    }

    /** {@code share}: the most one player may deliver (0 = the tier's setting; the GameTest server counts as multiplayer). */
    private static BountySpec spec(BountyTier tier, String item, long count, List<BountyReward> rewards, double share) {
        return new BountySpec(tier, List.of(new BountyNeed(item, count, 0)), rewards, 0, BountySplit.FIRST, share);
    }

    // ------------------------------------------------------------------ post, deliver, pay

    /** A bounty of the API is delivered from the inventory: coins to the inventory, UU-matter to the account, the event fires. */
    @GameTest(batch = "round1126_bounty", template = "empty", timeoutTicks = 20)
    public static void apiBountyIsDeliveredAndPaid(GameTestHelper h) {
        listen();
        MinecraftServer server = h.getLevel().getServer();
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        Object d = data(server);
        // a player share of 1: on the multiplayer GameTest server an advanced bounty takes at most half from one player otherwise
        long id = ExchangeApi.postBounty(server, spec(BountyTier.ADVANCED, "minecraft:cobblestone", 8,
            List.of(BountyReward.coins(500), BountyReward.uu(100)), 1.0));
        try {
            h.assertTrue(id > 0 && find(server, id) != null, "the API bounty was not posted: " + id);
            player.getInventory().add(new ItemStack(Items.COBBLESTONE, 8));
            int before = COMPLETED.get();
            Object result = call(common("exchange.Bounties"), "deliver", player, id);
            call(common("exchange.ExchangeEvents"), "flush", server);
            h.assertTrue((boolean) call(result, "ok"), "delivery refused: " + call(result, "key"));
            h.assertTrue(count(player, Items.COBBLESTONE) == 0, "cobblestone left in the inventory");
            Object box = ((Map<UUID, Object>) get(d, "mail")).get(player.getUUID());
            long coins = count(player, coin()) + (box == null ? 0 : (long) get(box, "coins"));
            h.assertTrue(coins == 500, "coins paid " + coins);
            h.assertTrue(box != null && (long) get(box, "uu") == 100, "UU-matter paid " + (box == null ? 0 : get(box, "uu")));
            h.assertTrue(find(server, id) == null, "the bounty is still open");
            h.assertTrue(COMPLETED.get() == before + 1, "BountyCompletedEvent did not fire");
        } finally {
            ExchangeApi.cancelBounty(server, id);
            ((Map<UUID, Object>) get(d, "mail")).remove(player.getUUID());
            server.getPlayerList().remove(player);
        }
        h.succeed();
    }

    /** The posting event can cancel a bounty or change its rewards and days. */
    @GameTest(batch = "round1126_bounty", template = "empty", timeoutTicks = 20)
    public static void postingEventChangesOrCancels(GameTestHelper h) {
        listen();
        MinecraftServer server = h.getLevel().getServer();
        long cancelled = ExchangeApi.postBounty(server, spec(BountyTier.ADVANCED, "minecraft:dirt", 7, List.of(BountyReward.coins(100))));
        h.assertTrue(cancelled == -1, "a cancelled posting returned " + cancelled);
        long changed = ExchangeApi.postBounty(server, spec(BountyTier.ADVANCED, "minecraft:dirt", 9, List.of(BountyReward.coins(100))));
        try {
            BountyInfo b = find(server, changed);
            h.assertTrue(b != null, "the changed bounty was not posted");
            h.assertTrue(b.rewards().size() == 1 && b.rewards().get(0).amount() == 1, "rewards not changed: " + b.rewards());
            h.assertTrue(b.expiry() - b.day() == 2, "days not changed: " + (b.expiry() - b.day()));
        } finally {
            ExchangeApi.cancelBounty(server, changed);
        }
        h.succeed();
    }

    // ------------------------------------------------------------------ market rules

    /**
     * While a normal bounty needs a commodity the market sells it no cheaper than the bounty pays per item (0.1.10.33; it stopped
     * selling it before); reserved treasury rewards come back on cancel.
     */
    @GameTest(batch = "round1126_bounty", template = "empty", timeoutTicks = 20)
    public static void supplyBlockAndReservedRewards(GameTestHelper h) {
        MinecraftServer server = h.getLevel().getServer();
        Object d = data(server);
        Class<?> service = common("exchange.ExchangeService");
        Map<String, Long> treasury = (Map<String, Long>) get(d, "treasury");
        String commodity = "minecraft:wheat", reward = "mio_icif:normal/item_circuit";
        long day = today(server);
        Long rewardStock = treasury.get(reward);
        treasury.put(reward, 10L);
        long supplyBefore = (long) call(service, "supply", d, commodity, day);
        // the market may run its own bounty for wheat (a daily normal bounty): compare with the floor it already sets
        long floorBefore = (long) call(call(service, "market", server, commodity), "bountyFloor");
        long id = ExchangeApi.postBounty(server, spec(BountyTier.NORMAL, commodity, 32,
            List.of(BountyReward.item(reward, 4, BountyReward.Source.TREASURY), BountyReward.coins(320))));
        try {
            h.assertTrue(id > 0, "not posted");
            Object open = call(service, "market", server, commodity);
            long floor = (long) call(open, "bountyFloor");
            h.assertTrue(floor >= 10, "the bounty sets no price floor: " + floor + " (the coins alone pay 10 per item)");
            h.assertTrue((long) call(service, "askOne", d, open) >= floor, "the market sells below the bounty's floor");
            h.assertTrue((long) call(service, "supply", d, commodity, day) == supplyBefore, "the bounty stopped the supply");
            h.assertTrue(treasury.getOrDefault(reward, 0L) == 6, "4 circuits were not reserved: " + treasury.get(reward));
        } finally {
            ExchangeApi.cancelBounty(server, id);
        }
        h.assertTrue(treasury.getOrDefault(reward, 0L) == 10, "the reserved circuits did not come back: " + treasury.get(reward));
        long floorAfter = (long) call(call(service, "market", server, commodity), "bountyFloor");
        h.assertTrue(floorAfter == floorBefore, "the floor is " + floorAfter + " after the cancel, " + floorBefore + " before the bounty");
        if (rewardStock == null) treasury.remove(reward); else treasury.put(reward, rewardStock);
        h.succeed();
    }

    /** A special bounty whose only reward would pass the weekly cap of made items is not posted (no coins for specials). */
    @GameTest(batch = "round1126_bounty", template = "empty", timeoutTicks = 20)
    public static void weeklyCapRefusesMadeRewards(GameTestHelper h) {
        MinecraftServer server = h.getLevel().getServer();
        long id = ExchangeApi.postBounty(server, spec(BountyTier.SPECIAL, "minecraft:nether_star", 1,
            List.of(BountyReward.item("mio_icif:generator/block_hybrid_solar_panel", 1_000_000, BountyReward.Source.ISSUE))));
        if (id > 0) ExchangeApi.cancelBounty(server, id);
        h.assertTrue(id == -1, "a reward above the weekly cap was accepted: " + id);
        h.succeed();
    }

    // ------------------------------------------------------------------ datapack and save

    /** The shipped si_exchange tables and pools are loaded without problems, and a table can be posted by id. */
    @GameTest(batch = "round1126_bounty", template = "empty", timeoutTicks = 20)
    public static void shippedTablesLoad(GameTestHelper h) {
        MinecraftServer server = h.getLevel().getServer();
        Object snapshot = call(common("exchange.ExchangeTables"), "current");
        Object tables = call(snapshot, "tables");
        Map<String, ?> byId = (Map<String, ?>) call(tables, "tables");
        Map<String, ?> pools = (Map<String, ?>) call(tables, "pools");
        List<String> errors = (List<String>) call(snapshot, "errors");
        h.assertTrue(byId.containsKey("mio_icif:electronics") && byId.containsKey("mio_icif:dragon_head"), "tables " + byId.keySet());
        h.assertTrue(pools.containsKey("mio_icif:normal") && pools.containsKey("mio_icif:advanced") && pools.containsKey("mio_icif:special"), "pools " + pools.keySet());
        h.assertTrue(errors.isEmpty(), "datapack problems: " + errors);
        long id = ExchangeApi.postBounty(server, ResourceLocation.fromNamespaceAndPath("mio_icif", "metals"));
        try {
            BountyInfo b = find(server, id);
            h.assertTrue(b != null && b.tier() == BountyTier.ADVANCED && b.needs().size() == 3 && !b.rewards().isEmpty(), "metals: " + b);
        } finally {
            ExchangeApi.cancelBounty(server, id);
        }
        h.succeed();
    }

    /** A bounty with a contribution and the vault survive a save and load of the exchange data. */
    @GameTest(batch = "round1126_bounty", template = "empty", timeoutTicks = 20)
    public static void bountySaveRoundTrip(GameTestHelper h) {
        MinecraftServer server = h.getLevel().getServer();
        Object d = data(server);
        Object board = get(d, "board");
        Map<String, Long> vault = (Map<String, Long>) get(board, "vault");
        Long elytra = vault.get("minecraft:elytra");
        vault.put("minecraft:elytra", 3L);
        long id = ExchangeApi.postBounty(server, spec(BountyTier.ADVANCED, "minecraft:cobblestone", 64, List.of(BountyReward.coins(640))));
        try {
            Object bounty = call(board, "find", id);
            ((Map<UUID, Long>) get(bounty, "contribution")).put(new UUID(1, 2), 10L);
            CompoundTag tag = (CompoundTag) call(d, "save", new CompoundTag(), server.registryAccess());
            Object loaded = call(common("exchange.ExchangeData"), "load", tag, server.registryAccess());
            Object board2 = get(loaded, "board");
            Object copy = call(board2, "find", id);
            h.assertTrue(copy != null, "the bounty is gone after loading");
            h.assertTrue(((Map<UUID, Long>) get(copy, "contribution")).get(new UUID(1, 2)) == 10L, "contribution lost");
            h.assertTrue(((Map<String, Long>) get(board2, "vault")).get("minecraft:elytra") == 3L, "vault lost");
        } finally {
            ExchangeApi.cancelBounty(server, id);
            if (elytra == null) vault.remove("minecraft:elytra"); else vault.put("minecraft:elytra", elytra);
        }
        h.succeed();
    }

    /** Coins paid in and taken out through the API go to the ledger under their reason code. */
    @GameTest(batch = "round1126_bounty", template = "empty", timeoutTicks = 20)
    public static void apiLedgerReasons(GameTestHelper h) {
        MinecraftServer server = h.getLevel().getServer();
        UUID who = new UUID(11, 26);
        long after = ExchangeApi.deposit(server, who, 250, "test:quest");
        h.assertTrue(after == 250 && ExchangeApi.mailboxCoins(server, who) == 250, "deposit " + after);
        h.assertTrue(!ExchangeApi.withdraw(server, who, 251, "test:shop"), "took more than the mailbox holds");
        h.assertTrue(ExchangeApi.withdraw(server, who, 250, "test:shop"), "withdraw refused");
        List<ExchangeApi.LedgerDay> ledger = ExchangeApi.ledger(server, 1);
        ExchangeApi.LedgerDay today = ledger.get(ledger.size() - 1);
        h.assertTrue(today.deposits().getOrDefault("test:quest", 0L) >= 250 && today.withdrawals().getOrDefault("test:shop", 0L) >= 250,
            "reasons " + today.deposits() + " / " + today.withdrawals());
        ((Map<UUID, Object>) get(data(server), "mail")).remove(who);
        h.succeed();
    }
}
