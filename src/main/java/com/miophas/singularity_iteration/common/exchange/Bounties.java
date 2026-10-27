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

    /** The market's ask of one item now (price x (1 + spread / 2)), 0 for an item that is no commodity. */
    public static BountyBoard.Prices prices(MinecraftServer server) {
        FutureMarketData fm = FutureMarketData.get(server.overworld());
        double spread = EconomySettings.spread();
        Map<String, Double> memo = new HashMap<>();
        return new BountyBoard.Prices() {
            @Override public double ask(String item) {
                return memo.computeIfAbsent(item, id -> {
                    FutureCommodity c = FutureCommodityManager.getCommodityById(id);
                    return c == null ? 0.0 : (double) MarketQuote.buyCost(fm.getPrice(id, c.getBasePrice()), 1, spread);
                });
            }

            @Override public boolean isCommodity(String item) { return FutureCommodityManager.hasCommodity(item); }
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
        for (BountyNeed n : event.getNeeds()) {
            if (n == null || !ExchangeTables.itemExists(n.item())) return null;
            Need x = new Need();
            x.item = n.item();
            x.count = n.count();
            x.unit = n.value() > 0 ? n.value() : Math.max(1, Math.round(prices.ask(n.item())));
            out.needs.add(x);
        }
        for (BountyReward r : event.getRewards()) {
            if (r == null || r.kind() == BountyReward.Kind.ITEM && !ExchangeTables.itemExists(r.item())) continue;
            out.lines.add(line(r));
        }
        return out;
    }

    // ------------------------------------------------------------------ the day

    /** Called by {@link ExchangeService#onNewDay}: expiries, the week, and the day's postings. */
    static void onNewDay(MinecraftServer server, ExchangeData d, long day) {
        applyListings(server, d, day);
        BountyBoard.Rules r = rules(server);
        BountyBoard.Prices p = prices(server);
        List<Bounty> posted = new ArrayList<>(), expired = new ArrayList<>();
        List<Payout> payouts = d.board.newDay(day, r, ExchangeTables.tables(), d.treasury, p, lacking(d, r), hooks(server, d, p), posted, expired);
        for (Payout x : payouts) toMailbox(d, x, day);
        for (Bounty b : posted) if (b.tier == Tier.SPECIAL) announce(server, b);
        d.setDirty();
    }

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
            Item item = ExchangeService.item(n.item);
            if (item == null || n.left() <= 0) continue;
            long can = d.board.canDeliver(b, i, uuid, day, mp);
            long have = src.count(item);
            if (can <= 0 && have > 0) capped = true;
            long take = Math.min(can, have);
            if (take <= 0 || !src.take(item, take)) continue;
            Delivery x = d.board.deliver(b, i, take, uuid, player.getGameProfile().getName(), d.treasury, prices);
            payouts.addAll(x.payouts());
            delivered += take;
            completed = x.completed();
        }
        if (delivered == 0) return capped ? Result.fail("bounty_share", Math.round(b.share * 100)) : Result.fail("not_enough_items");
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
        return completed ? Result.done("bounty_completed", delivered, coins) : Result.done("bounty_delivered", delivered, coins);
    }

    static void ledger(ExchangeData d, Payout x, long day) {
        ExchangeData.LedgerDay l = d.ledger(day);
        if (x.kind() == Kind.COINS) { l.minted += x.amount(); l.bountyCoins += x.amount(); }
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
        for (BountyNeed n : spec.needs()) {
            if (!ExchangeTables.itemExists(n.item())) return -1;
            Need x = new Need();
            x.item = n.item();
            x.count = n.count();
            x.unit = n.value() > 0 ? n.value() : Math.max(1, Math.round(p.ask(n.item())));
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
        List<Bounty> open = new ArrayList<>(d.board.bounties);
        open.sort(java.util.Comparator.comparingInt((Bounty b) -> -b.tier.ordinal()).thenComparingLong(b -> b.expiry).thenComparingLong(b -> b.id));
        List<ExchangeView.BountyLine> out = new ArrayList<>();
        for (Bounty b : open) {
            if (out.size() >= ExchangeView.MAX_BOUNTIES) break;
            List<ExchangeView.NeedLine> needs = new ArrayList<>();
            for (Need n : b.needs) needs.add(new ExchangeView.NeedLine(n.item, n.count, n.delivered));
            List<ExchangeView.RewardLine> rewards = new ArrayList<>();
            for (Line l : b.lines) rewards.add(new ExchangeView.RewardLine(l.kind.ordinal(), l.source.ordinal(), l.item, l.amount, l.paid));
            long room = d.board.room(b, player.getUUID(), day, mp);
            out.add(new ExchangeView.BountyLine(b.id, b.tier.ordinal(), b.expiry, b.split.ordinal(), (int) Math.round(b.share * 1000),
                needs, rewards, b.mine(player.getUUID()), b.all(), room == Long.MAX_VALUE ? -1 : room, b.contribution.size()));
        }
        return out;
    }
}
