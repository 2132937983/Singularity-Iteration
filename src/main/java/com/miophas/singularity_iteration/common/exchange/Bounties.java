// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.exchange;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.common.exchange.BountyBoard.Bounty;
import com.miophas.singularity_iteration.common.exchange.BountyBoard.Delivery;
import com.miophas.singularity_iteration.common.exchange.BountyBoard.Draft;
import com.miophas.singularity_iteration.common.exchange.BountyBoard.Line;
import com.miophas.singularity_iteration.common.exchange.BountyBoard.Need;
import com.miophas.singularity_iteration.common.exchange.BountyBoard.Payout;
import com.miophas.singularity_iteration.common.exchange.BountyTables.Kind;
import com.miophas.singularity_iteration.common.exchange.BountyTables.Source;
import com.miophas.singularity_iteration.common.exchange.BountyTables.Table;
import com.miophas.singularity_iteration.common.exchange.BountyTables.Tier;
import com.miophas.singularity_iteration.common.exchange.ExchangeData.Currency;
import com.miophas.singularity_iteration.common.exchange.ExchangeService.Result;
import com.miophas.singularity_iteration.common.future.FutureCommodity;
import com.miophas.singularity_iteration.common.future.FutureCommodityManager;
import com.miophas.singularity_iteration.common.future.FutureMarketData;
import com.miophas.singularity_iteration.common.future.MarketQuote;
import com.miophas.singularity_iteration.core.api.exchange.BountyInfo;
import com.miophas.singularity_iteration.core.api.exchange.BountyNeed;
import com.miophas.singularity_iteration.core.api.exchange.BountyPostingEvent;
import com.miophas.singularity_iteration.core.api.exchange.BountyReward;
import com.miophas.singularity_iteration.core.api.exchange.BountySpec;
import com.miophas.singularity_iteration.core.api.exchange.BountySplit;
import com.miophas.singularity_iteration.core.api.exchange.BountyTier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

/**
 * 0.1.10.26 (X3): the system bounties in the game: the {@link BountyBoard} of the exchange data with the market's prices,
 * the datapack tables ({@link ExchangeTables}), the settings ({@link EconomySettings}), the posting event, deliveries from
 * the inventory, payouts (inventory, mailbox, ledger) and the datapack listings. Server thread only.
 *
 * <p>The system commissions of 0.1.10.21-0.1.10.25 ({@code postSystemCommissions}) are no longer posted; those in a save
 * stay on the commission board until they expire.
 */
@EventBusSubscriber(modid = Singularity_Iteration.MOD_ID)
public final class Bounties {
    private Bounties() {}

    // ------------------------------------------------------------------ settings and market

    static long seed(MinecraftServer server) { return server.overworld().getSeed() ^ 0x5349424F554E5459L; }

    public static BountyBoard.Rules rules(MinecraftServer server) { return EconomySettings.rules(seed(server)); }

    /** Multiplayer for the per-player share: a dedicated server or a world opened to LAN. */
    public static boolean multiplayer(MinecraftServer server) { return !(server.isSingleplayer() && !server.isPublished()); }

    /**
     * The market's ask of one item now (price x (1 + spread / 2)), 0 for an item that is no commodity; 0.1.10.34: the value of
     * an item a request asks for is halfway between the ask and the bid.
     */
    public static BountyBoard.Prices prices(MinecraftServer server) {
        FutureMarketData fm = FutureMarketData.get(server.overworld());
        double spread = EconomySettings.spread();
        Map<String, Double> memo = new HashMap<>(), valueMemo = new HashMap<>();
        return new BountyBoard.Prices() {
            @Override public double ask(String item) {
                return memo.computeIfAbsent(item, id -> {
                    FutureCommodity c = FutureCommodityManager.getCommodityById(id);
                    return c == null ? 0.0 : (double) MarketQuote.buyCost(fm.getPrice(id, c.getBasePrice()), 1, spread);
                });
            }

            @Override public boolean isCommodity(String item) { return FutureCommodityManager.hasCommodity(item); }

            // 0.1.10.33: generated requests skip items that do not stack; UU rewards are valued for the ask floor
            @Override public int maxStack(String item) { return ExchangeTables.maxStack(item); }

            @Override public double uuPerMb() { return ExchangeService.coinsPerBucket(server) / 1000.0; }

            // 0.1.10.34 (item 10): a request is valued halfway between the market's ask and bid
            @Override public double value(String item) {
                return valueMemo.computeIfAbsent(item, id -> {
                    FutureCommodity c = FutureCommodityManager.getCommodityById(id);
                    if (c == null) return 0.0;
                    int price = fm.getPrice(id, c.getBasePrice());
                    // 0.1.10.35: the bid under the made_from cap
                    return MarketQuote.midValue(ask(id), ExchangeService.bidUnit(server, c, price));
                });
            }
        };
    }

    /** Commodities a generated NORMAL request may ask for: no unlock, the market owns fewer than the low mark. */
    static List<String> lacking(ExchangeData d, BountyBoard.Rules r) {
        List<String> out = new ArrayList<>();
        for (FutureCommodity c : FutureCommodityManager.getCommodities())
            if (c.getUnlockAdvancement() == null && d.treasury.getOrDefault(c.getItemId(), 0L) < r.treasuryLow()) out.add(c.getItemId());
        return out;
    }

    /** The table's advancement is held by an online player (no advancement: always). */
    static boolean advancementHeld(MinecraftServer server, String advancement) {
        if (advancement == null || advancement.isEmpty()) return true;
        ResourceLocation id = ResourceLocation.tryParse(advancement);
        var holder = id == null ? null : server.getAdvancements().get(id);
        if (holder == null) return false;
        for (ServerPlayer p : server.getPlayerList().getPlayers()) if (p.getAdvancements().getOrStartProgress(holder).isDone()) return true;
        return false;
    }

    static BountyBoard.Hooks hooks(MinecraftServer server, ExchangeData d, BountyBoard.Prices prices) {
        return new BountyBoard.Hooks() {
            @Override public long newId() { return d.newId(); }

            @Override public boolean eligible(Table table) { return advancementHeld(server, table.advancement()); }

            @Override public Draft posting(Draft draft) { return postingEvent(server, draft, prices); }
        };
    }

    /** Fires {@link BountyPostingEvent}: null when it was cancelled or left the draft invalid. */
    static Draft postingEvent(MinecraftServer server, Draft draft, BountyBoard.Prices prices) {
        List<BountyNeed> needs = new ArrayList<>();
        for (Need n : draft.needs) needs.add(new BountyNeed(n.item, n.count, n.unit));
        List<BountyReward> rewards = new ArrayList<>();
        for (Line l : draft.lines) rewards.add(reward(l));
        BountyPostingEvent event = new BountyPostingEvent(server, tier(draft.tier), draft.table, needs, rewards, draft.days, split(draft.split));
        NeoForge.EVENT_BUS.post(event);
        if (event.isCanceled()) return null;
        Draft out = new Draft();
        out.tier = draft.tier;
        out.table = draft.table;
        out.days = event.getDays();
        out.split = event.getSplit() == BountySplit.CONTRIBUTION ? BountyTables.Split.CONTRIBUTION : BountyTables.Split.FIRST;
        out.share = draft.share;
        out.multiplier = draft.multiplier;
        out.coinsFallback = draft.coinsFallback;
        out.rarity = draft.rarity;   // 0.1.10.34
        out.rush = draft.rush;
        for (BountyNeed n : event.getNeeds()) {
            if (n == null || !ExchangeTables.itemExists(n.item())) return null;
            Need x = new Need();
            x.item = n.item();
            // 0.1.10.36: a need the event kept keeps the accepted tag of its table requirement
            for (Need before : draft.needs) if (before.item.equals(x.item)) x.tag = before.tag;
            x.count = n.count();
            x.unit = n.value() > 0 ? n.value() : Math.max(1, Math.round(prices.value(n.item())));
            out.needs.add(x);
        }
        for (BountyReward r : event.getRewards()) {
            if (r == null || r.kind() == BountyReward.Kind.ITEM && !ExchangeTables.itemExists(r.item())) continue;
            out.lines.add(line(r));
        }
        return out;
    }

    // ------------------------------------------------------------------ the day

    // ------------------------------------------------------------------ 0.1.10.34: players and pace

    /**
     * Sets the pace of the board for now: the settings, the active players (online, or seen at the exchange in the last
     * {@link BountyPacing#ACTIVE_DAYS} market days) and the players online.
     */
    static BountyBoard.Pace pace(MinecraftServer server, ExchangeData d, long day) {
        java.util.Set<UUID> active = new java.util.HashSet<>();
        int online = 0;
        for (ServerPlayer p : server.getPlayerList().getPlayers()) { active.add(p.getUUID()); online++; }
        for (Map.Entry<UUID, Long> e : d.seen.entrySet()) if (BountyPacing.active(e.getValue(), day)) active.add(e.getKey());
        d.board.pace = EconomySettings.pace(active.size(), online);
        return d.board.pace;
    }

    /** {@code player} logged in or used the exchange today (an active player; a claimant who is not away). */
    public static void seen(ServerPlayer player) {
        ExchangeData d = ExchangeData.get(player.server);
        long day = ExchangeService.today(player.server);
        Long before = d.seen.put(player.getUUID(), day);
        if (before == null || before != day) d.setDirty();
    }

    @SubscribeEvent
    public static void onLogin(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer p && FutureMarketData.get(p.server.overworld()).isInitialized()) {
            seen(p);
            Shops.onLogin(p);   // 0.1.10.35: the owner is back; a shop closed while away is told
        }
    }

    @SubscribeEvent
    public static void onLogout(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent event) {
        ExchangePackets.SHOP_QUERY.remove(event.getEntity().getUUID());   // 0.1.10.35: the shop page the player looked at
    }

    /** Ticks between two checks of the refill (10 seconds). */
    public static final int REFILL_CHECK_TICKS = 200;

    @SubscribeEvent
    public static void onServerTick(net.neoforged.neoforge.event.tick.ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server.getTickCount() % REFILL_CHECK_TICKS == 0) {
            refill(server);
            ExchangeService.tick(server);   // 0.1.10.35: commissions and orders in hours, the shops
        }
    }

    /**
     * 0.1.10.34 (item 11): one NORMAL request when the board is below the day's target and the wait for the players online
     * passed (BountyBoard#refill). Returns the postings.
     */
    static int refill(MinecraftServer server) {
        if (server.getPlayerList().getPlayerCount() == 0 || !FutureMarketData.get(server.overworld()).isInitialized()) return 0;
        ExchangeData d = ExchangeData.get(server);
        long day = ExchangeService.today(server);
        pace(server, d, day);
        BountyBoard.Rules r = rules(server);
        BountyBoard.Prices p = prices(server);
        long before = d.board.lastPostTick;
        List<Bounty> posted = new ArrayList<>();
        int n = d.board.refill(day, server.overworld().getGameTime(), r, ExchangeTables.tables(), d.treasury, p, lacking(d, r), hooks(server, d, p),
            posted);
        if (n > 0 || d.board.lastPostTick != before) d.setDirty();
        return n;
    }

    /** Called by {@link ExchangeService#onNewDay}: expiries, the week, and the day's postings. */
    static void onNewDay(MinecraftServer server, ExchangeData d, long day) {
        applyListings(server, d, day);
        // 0.1.10.34: the pace for the day; claims of players away for two whole market days are released
        pace(server, d, day);
        Map<UUID, Long> last = new HashMap<>(d.seen);
        for (ServerPlayer p : server.getPlayerList().getPlayers()) last.put(p.getUUID(), day);
        d.board.releaseAbsent(day, last);
        d.seen.values().removeIf(v -> day - v > SEEN_KEEP_DAYS || v > day);
        BountyBoard.Rules r = rules(server);
        BountyBoard.Prices p = prices(server);
        List<Bounty> posted = new ArrayList<>(), expired = new ArrayList<>();
        List<Payout> payouts = d.board.newDay(day, r, ExchangeTables.tables(), d.treasury, p, lacking(d, r), hooks(server, d, p), posted, expired);
        for (Payout x : payouts) toMailbox(d, x, day);
        for (Bounty b : posted) if (b.tier == Tier.SPECIAL) announce(server, b);
        d.setDirty();
    }

    /** Market days a player's last visit is kept (the active players need three, the absent claimants three). */
    static final int SEEN_KEEP_DAYS = 30;

    static void announce(MinecraftServer server, Bounty b) {
        Item item = ExchangeService.item(b.needs.get(0).item);
        Component what = item == null ? Component.literal(b.needs.get(0).item) : new ItemStack(item).getHoverName();
        server.getPlayerList().broadcastSystemMessage(Component.translatable("message.mio_icif.exchange.bounty_special_posted", what,
            b.expiry - b.day), false);
    }

    // ------------------------------------------------------------------ delivery

    /**
     * Delivers to a bounty everything it still needs that the player carries (plain stacks of the main inventory), within
     * the per-player share. The rewards due go to the inventory (coins, items; the rest to the mailbox) and UU-matter to
     * the account; a contribution split pays at completion, other contributors through their mailboxes.
     */
    public static Result deliver(ServerPlayer player, long id) {
        return deliver(player, id, null);
    }

    /** 0.1.10.27 (X2): {@link #deliver} with the items from {@code from} (a settlement container; null = the inventory). */
    public static Result deliver(ServerPlayer player, long id, ItemSource from) {
        MinecraftServer server = player.server;
        ExchangeData d = ExchangeData.get(server);
        Bounty b = d.board.find(id);
        if (b == null) return Result.fail("no_bounty");
        UUID uuid = player.getUUID();
        // 0.1.10.34: a claimed bounty takes deliveries from its claimant and their team only
        if (!BountyBoard.mayDeliver(b, uuid, team(player))) return Result.fail("bounty_claimed", b.claimName);
        long day = ExchangeService.today(server);
        boolean mp = multiplayer(server);
        BountyBoard.Prices prices = prices(server);
        InventoryPlan plan = new InventoryPlan(player.getInventory());
        ItemSource src = from != null ? from : ItemSource.of(plan);
        List<Payout> payouts = new ArrayList<>();
        long delivered = 0;
        boolean completed = false, capped = false;
        for (int i = 0; i < b.needs.size() && !completed; i++) {
            Need n = b.needs.get(i);
            // 0.1.10.36: the requested item or any item of its material tags (another mod's copper ingot for ours)
            ItemMatch match = ItemMatch.of(n.item, n.tag);
            if (match == null || n.left() <= 0) continue;
            long can = d.board.canDeliver(b, i, uuid, day, mp);
            long have = src.count(match);
            if (can <= 0 && have > 0) capped = true;
            long take = Math.min(can, have);
            if (take <= 0 || !src.take(match, take)) continue;
            // 0.1.10.33: the records (the value delivered; a commodity enters the treasury at its value)
            d.records.add(uuid, day, MarketRecords.BOUNTY, Math.multiplyExact(take, Math.max(1, n.unit)));
            if (prices.isCommodity(n.item)) d.records.costIn(n.item, d.treasury.getOrDefault(n.item, 0L), take, (double) take * Math.max(1, n.unit));
            Delivery x = d.board.deliver(b, i, take, uuid, player.getGameProfile().getName(), d.treasury, prices);
            payouts.addAll(x.payouts());
            delivered += take;
            completed = x.completed();
        }
        // 0.1.10.33: the messages name what is still needed (a need the panel did not show stopped deliveries silently)
        if (delivered == 0) return capped ? Result.fail("bounty_share", Math.round(b.share * 100)) : Result.fail("bounty_missing", missing(b));
        long coins = 0;
        for (Payout x : payouts) {
            if (x.player().equals(uuid)) { if (x.kind() == Kind.COINS) coins += x.amount(); toPlayer(plan, d, x, day); }
            else toMailbox(d, x, day);
        }
        if (!plan.unchanged()) throw new IllegalStateException("inventory changed during a bounty delivery");
        plan.commit();
        if (completed) {
            ExchangeEvents.completed(info(b));
            for (UUID who : b.contribution.keySet()) {
                ServerPlayer online = server.getPlayerList().getPlayer(who);
                if (online != null && !online.equals(player)) online.sendSystemMessage(Component.translatable("message.mio_icif.exchange.bounty_completed_mail"));
            }
        }
        d.setDirty();
        return completed ? Result.done("bounty_completed", delivered, coins) : Result.done("bounty_delivered_left", delivered, coins, missing(b));
    }

    /** 0.1.10.33: what a bounty still needs, as "name xN, name xN" (four at most, then "+N"). */
    static Component missing(Bounty b) {
        net.minecraft.network.chat.MutableComponent out = Component.empty();
        int shown = 0, more = 0;
        for (Need n : b.needs) {
            if (n.left() <= 0) continue;
            if (shown >= 4) { more++; continue; }
            Item item = ExchangeService.item(n.item);
            if (shown > 0) out.append(", ");
            out.append(item == null ? Component.literal(n.item) : new ItemStack(item).getHoverName()).append(" x" + n.left());
            shown++;
        }
        if (more > 0) out.append(" +" + more);
        return out;
    }

    static void ledger(ExchangeData d, Payout x, long day) {
        ExchangeData.LedgerDay l = d.ledger(day);
        // 0.1.10.35: the reserve pays first; only the rest is new coins
        if (x.kind() == Kind.COINS) { l.minted += x.amount() - ExchangeService.fromReserve(d, day, x.amount()); l.bountyCoins += x.amount(); }
        else if (x.kind() == Kind.UU) l.bountyUu += x.amount();
        else if (x.source() == Source.ISSUE) l.bountyItems += x.amount();
    }

    static void toPlayer(InventoryPlan plan, ExchangeData d, Payout x, long day) {
        ledger(d, x, day);
        switch (x.kind()) {
            case COINS -> ExchangeService.pay(plan, d, x.player(), Currency.COIN, x.amount());
            case UU -> ExchangeService.credit(d, x.player(), Currency.UU, x.amount());
            case ITEM -> {
                Item item = ExchangeService.item(x.item());
                if (item != null) ExchangeService.give(plan, d, x.player(), item, x.item(), x.amount());
                else ExchangeService.addItems(d.mailbox(x.player()), x.item(), x.amount());
            }
        }
    }

    static void toMailbox(ExchangeData d, Payout x, long day) {
        ledger(d, x, day);
        switch (x.kind()) {
            case COINS -> ExchangeService.credit(d, x.player(), Currency.COIN, x.amount());
            case UU -> ExchangeService.credit(d, x.player(), Currency.UU, x.amount());
            case ITEM -> ExchangeService.addItems(d.mailbox(x.player()), x.item(), x.amount());
        }
    }

    // ------------------------------------------------------------------ 0.1.10.34: claims and the refresh

    /** The player's vanilla team ("" = none): its members deliver to the bounties the player claimed. */
    static String team(ServerPlayer player) {
        var t = player.getTeam();
        return t == null ? "" : t.getName();
    }

    /** Claims a NORMAL or ADVANCED bounty nobody delivered to yet (one per player by default). */
    public static Result claim(ServerPlayer player, long id) {
        MinecraftServer server = player.server;
        ExchangeData d = ExchangeData.get(server);
        Bounty b = d.board.find(id);
        if (b == null) return Result.fail("no_bounty");
        long day = ExchangeService.today(server);
        BountyBoard.Pace pace = pace(server, d, day);
        String why = d.board.claimRefusal(b, player.getUUID(), day);
        if (why != null) return Result.fail(why, b.claimName, pace.acceptMax());
        d.board.claim(b, player.getUUID(), player.getGameProfile().getName(), team(player), day);
        d.setDirty();
        return Result.done("bounty_claim_done", Math.max(0, b.expiry - day));
    }

    /** Gives up the player's claim: no new claim for them until the next market day. */
    public static Result abandon(ServerPlayer player, long id) {
        MinecraftServer server = player.server;
        ExchangeData d = ExchangeData.get(server);
        Bounty b = d.board.find(id);
        if (b == null) return Result.fail("no_bounty");
        if (!d.board.abandon(b, player.getUUID(), ExchangeService.today(server))) return Result.fail("bounty_not_mine");
        d.setDirty();
        return Result.done("bounty_abandoned");
    }

    /**
     * Replaces the NORMAL bounties nobody claimed or delivered to with new requests, for coins that are destroyed (the
     * ledger's burned coins): max(500, 5 % of their rewards), doubled for each earlier refresh of the player that day; three
     * a day, five minutes apart.
     */
    public static Result refresh(ServerPlayer player) {
        MinecraftServer server = player.server;
        ExchangeData d = ExchangeData.get(server);
        long day = ExchangeService.today(server), now = server.overworld().getGameTime();
        UUID uuid = player.getUUID();
        pace(server, d, day);
        BountyBoard.Rules r = rules(server);
        BountyBoard.Prices p = prices(server);
        BountyBoard.Hooks hooks = hooks(server, d, p);
        List<String> lacking = lacking(d, r);
        if (d.board.refreshable().isEmpty()) return Result.fail("bounty_refresh_none");
        long cost = d.board.refreshCost(uuid, day, p);
        if (cost < 0) return Result.fail("bounty_refresh_limit", BountyPacing.REFRESH_PER_DAY);
        long wait = d.board.refreshWait(uuid, now);
        if (wait > 0) return Result.fail("bounty_refresh_wait", (wait + 19) / 20);
        if (!d.board.refreshPossible(day, ExchangeTables.tables(), p, lacking, hooks)) return Result.fail("bounty_refresh_nothing");
        InventoryPlan plan = new InventoryPlan(player.getInventory());
        if (!ExchangeService.takeCoins(plan, d, uuid, cost)) return Result.fail("not_enough_coins");
        plan.commit();
        ExchangeService.toReserve(server, d, day, cost);   // 0.1.10.35: into the treasury reserve (was destroyed)
        List<Bounty> posted = new ArrayList<>();
        int replaced = d.board.refresh(uuid, day, now, r, ExchangeTables.tables(), d.treasury, p, lacking, hooks, posted);
        d.setDirty();
        return Result.done("bounty_refreshed", replaced, posted.size(), cost);
    }

    // ------------------------------------------------------------------ posting by hand (command, API)

    /** Posts a datapack table now (weight, cooldown and conditions are not checked). Returns the id or -1. */
    public static long post(MinecraftServer server, String tableId) {
        ExchangeData d = ExchangeData.get(server);
        Table t = ExchangeTables.tables().tables().get(tableId);
        if (t == null) return -1;
        long day = ExchangeService.today(server);
        BountyBoard.Rules r = rules(server);
        BountyBoard.Prices p = prices(server);
        Draft draft = d.board.draft(t, r, ExchangeTables.tables(), d.treasury, p, day, System.nanoTime());
        Bounty b = d.board.post(draft, day, r, d.treasury, hooks(server, d, p), null);
        d.setDirty();
        ExchangeEvents.flush(server);
        if (b != null && b.tier == Tier.SPECIAL) announce(server, b);
        return b == null ? -1 : b.id;
    }

    /** Posts a bounty of the Java API. Returns the id or -1. */
    public static long post(MinecraftServer server, BountySpec spec) {
        ExchangeData d = ExchangeData.get(server);
        long day = ExchangeService.today(server);
        BountyBoard.Rules r = rules(server);
        BountyBoard.Prices p = prices(server);
        Tier tier = Tier.values()[spec.tier().ordinal()];
        Draft draft = new Draft();
        draft.tier = tier;
        draft.days = spec.days() > 0 ? spec.days() : r.days(tier);
        draft.split = spec.split() == BountySplit.CONTRIBUTION ? BountyTables.Split.CONTRIBUTION : BountyTables.Split.FIRST;
        draft.share = spec.playerShare() > 0 ? spec.playerShare() : r.share(tier);
        draft.multiplier = r.multiplier(tier);
        draft.coinsFallback = tier != Tier.SPECIAL;
        // 0.1.10.34: the rarity of the tier (the days stay as asked, no rush bonus)
        draft.rarity = BountyPacing.rarity(tier.ordinal(), tier == Tier.NORMAL ? 4 : 3);
        for (BountyNeed n : spec.needs()) {
            if (!ExchangeTables.itemExists(n.item())) return -1;
            Need x = new Need();
            x.item = n.item();
            x.count = n.count();
            x.unit = n.value() > 0 ? n.value() : Math.max(1, Math.round(p.value(n.item())));
            draft.needs.add(x);
        }
        if (spec.rewards().isEmpty()) {
            draft.lines.addAll(d.board.rewards(ExchangeTables.tables().pools().get(r.pool(tier)), 1, draft.value(), draft.multiplier, tier, r,
                d.treasury, p, day, System.nanoTime(), BountyBoard.needItems(draft)));
        } else {
            for (BountyReward x : spec.rewards()) if (x.kind() != BountyReward.Kind.ITEM || ExchangeTables.itemExists(x.item())) draft.lines.add(line(x));
        }
        Bounty b = d.board.post(draft, day, r, d.treasury, hooks(server, d, p), null);
        d.setDirty();
        ExchangeEvents.flush(server);
        return b == null ? -1 : b.id;
    }

    /** Removes an open bounty; unpaid reserved items go back. */
    public static boolean cancel(MinecraftServer server, long id) {
        ExchangeData d = ExchangeData.get(server);
        Bounty b = d.board.find(id);
        if (b == null) return false;
        d.board.cancel(b, d.treasury);
        d.setDirty();
        return true;
    }

    // ------------------------------------------------------------------ datapack listings

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) { applyListings(event.getServer()); }

    public static void applyListings(MinecraftServer server) {
        if (server == null) return;
        applyListings(server, ExchangeData.get(server), ExchangeService.today(server));
    }

    /**
     * The listings of {@code si_exchange/listings}: added when missing (not for commodities), marked so that they are not
     * delisted for lack of trades; a listing no datapack names any more becomes an ordinary one.
     */
    static void applyListings(MinecraftServer server, ExchangeData d, long day) {
        java.util.Set<String> named = new java.util.HashSet<>();
        for (ExchangeTables.DatapackListing l : ExchangeTables.current().listings()) {
            if (FutureCommodityManager.hasCommodity(l.item())) continue;
            named.add(l.item());
            ExchangeData.Listing x = d.listings.get(l.item());
            if (x == null) {
                x = new ExchangeData.Listing();
                x.item = l.item();
                x.currency = l.currency();
                x.reference = l.price();
                x.listedDay = day;
                x.lastTradeDay = day;
                d.listings.put(l.item(), x);
                d.setDirty();
            }
            if (!x.datapack) { x.datapack = true; d.setDirty(); }
        }
        for (ExchangeData.Listing x : d.listings.values()) {
            if (x.datapack && !named.contains(x.item)) { x.datapack = false; x.lastTradeDay = day; d.setDirty(); }
        }
    }

    // ------------------------------------------------------------------ API records

    static BountyTier tier(Tier t) { return BountyTier.values()[t.ordinal()]; }

    static BountySplit split(BountyTables.Split s) { return s == BountyTables.Split.CONTRIBUTION ? BountySplit.CONTRIBUTION : BountySplit.FIRST; }

    static BountyReward reward(Line l) {
        return new BountyReward(BountyReward.Kind.values()[l.kind.ordinal()], BountyReward.Source.values()[l.source.ordinal()], l.item,
            Math.max(1, l.amount));
    }

    static Line line(BountyReward r) {
        Line l = new Line();
        l.kind = Kind.values()[r.kind().ordinal()];
        l.source = Source.values()[r.source().ordinal()];
        l.item = r.item();
        l.amount = r.amount();
        return l;
    }

    public static BountyInfo info(Bounty b) {
        List<BountyNeed> needs = new ArrayList<>();
        List<Long> delivered = new ArrayList<>(), paid = new ArrayList<>();
        for (Need n : b.needs) { needs.add(new BountyNeed(n.item, n.count, n.unit)); delivered.add(n.delivered); }
        List<BountyReward> rewards = new ArrayList<>();
        for (Line l : b.lines) { rewards.add(reward(l)); paid.add(l.paid); }
        return new BountyInfo(b.id, tier(b.tier), b.table, b.day, b.expiry, split(b.split), needs, delivered, rewards, paid, b.contribution);
    }

    // ------------------------------------------------------------------ the panel

    /** The board for the panel of {@code player}: specials first, then advanced, then normal, by expiry. */
    static List<ExchangeView.BountyLine> lines(ServerPlayer player, ExchangeData d, long day) {
        boolean mp = multiplayer(player.server);
        UUID me = player.getUUID();
        String myTeam = team(player);
        List<Bounty> open = new ArrayList<>(d.board.bounties);
        open.sort(java.util.Comparator.comparingInt((Bounty b) -> -b.tier.ordinal()).thenComparingLong(b -> b.expiry).thenComparingLong(b -> b.id));
        List<ExchangeView.BountyLine> out = new ArrayList<>();
        for (Bounty b : open) {
            if (out.size() >= ExchangeView.MAX_BOUNTIES) break;
            List<ExchangeView.NeedLine> needs = new ArrayList<>();
            for (Need n : b.needs) needs.add(new ExchangeView.NeedLine(n.item, n.count, n.delivered, n.tag));
            List<ExchangeView.RewardLine> rewards = new ArrayList<>();
            for (Line l : b.lines) rewards.add(new ExchangeView.RewardLine(l.kind.ordinal(), l.source.ordinal(), l.item, l.amount, l.paid));
            long room = d.board.room(b, player.getUUID(), day, mp);
            // 0.1.10.34: the claim as this player sees it, and why they may not claim (a message key, "" = they may)
            int claim = me.equals(b.claimant) ? ExchangeView.CLAIM_MINE
                : b.claimant != null ? (BountyBoard.mayDeliver(b, me, myTeam) ? ExchangeView.CLAIM_TEAM : ExchangeView.CLAIM_OTHER)
                : ExchangeView.CLAIM_NONE;
            String refusal = claim == ExchangeView.CLAIM_NONE ? d.board.claimRefusal(b, me, day) : null;
            out.add(new ExchangeView.BountyLine(b.id, b.tier.ordinal(), b.expiry, b.split.ordinal(), (int) Math.round(b.share * 1000),
                needs, rewards, b.mine(player.getUUID()), b.all(), room == Long.MAX_VALUE ? -1 : room, b.contribution.size(),
                b.rarity.ordinal(), b.rush, claim, b.claimName, refusal == null ? "" : refusal));
        }
        return out;
    }
}
