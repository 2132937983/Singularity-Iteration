// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.exchange;

import com.miophas.singularity_iteration.common.exchange.ExchangeData.Currency;
import com.miophas.singularity_iteration.common.future.FutureCommodity;
import com.miophas.singularity_iteration.common.future.FutureCommodityManager;
import com.miophas.singularity_iteration.common.future.FutureMarketData;
import com.miophas.singularity_iteration.common.item.normal.mio_icif_normal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * 0.1.10.42: the wealth ranking of the server ({@link WealthRules}): the total assets of every player the exchange knows, by
 * part, and the weekly results of {@link MarketRecords}. The assets are computed on request and kept for
 * {@link #CACHE_TICKS} ticks (panels ask once a second).
 *
 * <p>Values: commodities at the market's bid without pressure ({@link ExchangeService#bidUnit}), the items of a coin listing at
 * its last price, UU-matter at the UU board's last price per mB (else the calibrated reference); other items count nothing.
 */
public final class WealthRanking {
    private WealthRanking() {}

    /** Ticks the computed assets stay valid. */
    public static final int CACHE_TICKS = 200;

    private static MinecraftServer cachedServer;
    private static long cachedAt = Long.MIN_VALUE;
    private static Map<UUID, long[]> cached = Map.of();

    /** The assets by part of every known player (cached for {@link #CACHE_TICKS} ticks). */
    public static synchronized Map<UUID, long[]> assets(MinecraftServer server) {
        long now = server.getTickCount();
        if (cachedServer != server || cachedAt == Long.MIN_VALUE || now - cachedAt >= CACHE_TICKS || now < cachedAt) {
            cached = compute(server);
            cachedServer = server;
            cachedAt = now;
        }
        return cached;
    }

    /** Forgets the cached assets (an admin command recomputes at once). */
    public static synchronized void invalidate() { cachedAt = Long.MIN_VALUE; }

    /** The ranking of {@code category} for {@code viewer} ({@link RankingView#NONE} for an unknown category). */
    public static RankingView view(MinecraftServer server, UUID viewer, int category) {
        if (category < 0 || category >= WealthRules.CATEGORIES) return RankingView.NONE;
        ExchangeData d = ExchangeData.get(server);
        long day = ExchangeService.today(server);
        boolean enabled = EconomySettings.wealthRanking();
        boolean hidden = d.rankingHidden.contains(viewer);
        if (!enabled) return new RankingView(false, category, List.of(), 0, 0, 0, hidden, List.of(), day);
        WealthRules.Ranked r = rank(server, viewer, category, WealthRules.TOP);
        List<RankingView.Entry> top = new ArrayList<>();
        for (WealthRules.Place p : r.top()) top.add(new RankingView.Entry(p.rank(), p.name(), p.value(), p.me()));
        List<Long> parts = new ArrayList<>();
        if (category == WealthRules.TOTAL) {
            long[] mine = assets(server).get(viewer);
            for (int i = 0; i < WealthRules.PARTS; i++) parts.add(mine == null ? 0L : mine[i]);
        }
        return new RankingView(true, category, top, r.myRank(), r.myValue(), r.players(), hidden, parts, day);
    }

    /** The ranking of {@code category} for {@code viewer} (null = nobody's own), the top {@code top}. */
    public static WealthRules.Ranked rank(MinecraftServer server, UUID viewer, int category, int top) {
        ExchangeData d = ExchangeData.get(server);
        long day = ExchangeService.today(server);
        Map<UUID, long[]> all = assets(server);
        Set<UUID> ids = new LinkedHashSet<>(all.keySet());
        if (category != WealthRules.TOTAL) {
            d.records.rollWeek(day);
            ids.addAll(d.records.thisWeek.keySet());
        }
        List<WealthRules.Player> players = new ArrayList<>();
        for (UUID id : ids) {
            long[] parts = all.get(id);
            long total = parts == null ? 0 : WealthRules.total(parts);
            long value = WealthRules.value(category, total, d.records.thisWeek.get(id));
            players.add(new WealthRules.Player(id.toString(), name(server, d, id), value, d.rankingHidden.contains(id)));
        }
        if (viewer != null && !ids.contains(viewer)) players.add(new WealthRules.Player(viewer.toString(), name(server, d, viewer), 0, d.rankingHidden.contains(viewer)));
        return WealthRules.rank(players, viewer == null ? "" : viewer.toString(), top);
    }

    /** The last known name of a player (the profile cache, else the start of the UUID). */
    public static String name(MinecraftServer server, ExchangeData d, UUID id) {
        ServerPlayer online = server.getPlayerList().getPlayer(id);
        if (online != null) return online.getGameProfile().getName();
        String known = d.names.get(id);
        if (known != null && !known.isEmpty()) return known;
        var cache = server.getProfileCache();
        if (cache != null) {
            var profile = cache.get(id);
            if (profile.isPresent()) return profile.get().getName();
        }
        return id.toString().substring(0, 8);
    }

    // ------------------------------------------------------------------ the players' coins

    /** Coins in a player's inventory (all compartments). */
    public static long carried(ServerPlayer player) {
        Item coin = mio_icif_normal.COIN.get();
        long n = 0;
        var inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.is(coin)) n += s.getCount();
        }
        return n;
    }

    /** A player logged in: their name. */
    public static void onLogin(ServerPlayer player) {
        ExchangeData d = ExchangeData.get(player.server);
        String name = player.getGameProfile().getName();
        if (!name.equals(d.names.get(player.getUUID())) && (d.names.containsKey(player.getUUID()) || d.names.size() < MarketRecords.MAX_PLAYERS)) {
            d.names.put(player.getUUID(), name);
            d.setDirty();
        }
    }

    /** A player logged out: the coins they carry count for them while they are away. */
    public static void onLogout(ServerPlayer player) {
        ExchangeData d = ExchangeData.get(player.server);
        UUID id = player.getUUID();
        if (!d.heldCoins.containsKey(id) && d.heldCoins.size() >= MarketRecords.MAX_PLAYERS) return;
        long held = carried(player);
        Long before = held > 0 ? d.heldCoins.put(id, held) : d.heldCoins.remove(id);
        if (before == null || before != held) d.setDirty();
        if (d.names.containsKey(id) || d.names.size() < MarketRecords.MAX_PLAYERS) d.names.put(id, player.getGameProfile().getName());
    }

    /** Hides or shows the player's name in the ranking. */
    public static ExchangeService.Result setHidden(ServerPlayer player, boolean hidden) {
        ExchangeData d = ExchangeData.get(player.server);
        boolean changed = hidden ? d.rankingHidden.add(player.getUUID()) : d.rankingHidden.remove(player.getUUID());
        if (changed) d.setDirty();
        return ExchangeService.Result.done(hidden ? "ranking_hidden" : "ranking_shown");
    }

    // ------------------------------------------------------------------ the assets

    /** Values of items in coins, computed once per {@link #compute}. */
    static final class Prices {
        final MinecraftServer server;
        final ExchangeData d;
        final FutureMarketData fm;
        final Map<String, Double> unit = new HashMap<>();
        final double uuPerMb;

        Prices(MinecraftServer server, ExchangeData d) {
            this.server = server;
            this.d = d;
            this.fm = FutureMarketData.get(server.overworld());
            long reference = UuBoard.reference(ExchangeService.coinsPerBucket(server));
            this.uuPerMb = ExchangeService.lastPrice(d, UuBoard.MARKET, reference) / (double) UuBoard.LOT_MB;
        }

        /** Coins of one item of {@code id}: the bid of a commodity, the last price of a coin listing (UU listing: in mB), else 0. */
        double item(String id) {
            Double known = unit.get(id);
            if (known != null) return known;
            double v = 0;
            FutureCommodity c = FutureCommodityManager.getCommodityById(id);
            if (c != null) v = ExchangeService.bidUnit(server, c, fm.getPrice(id, c.getBasePrice()));
            else {
                ExchangeData.Listing l = d.listings.get(id);
                if (l != null) {
                    long last = ExchangeService.lastPrice(d, id, l.reference);
                    v = l.currency == Currency.UU ? last * uuPerMb : last;
                }
            }
            if (!(v > 0) || !Double.isFinite(v)) v = 0;
            unit.put(id, v);
            return v;
        }

        long items(String id, long count) { return count <= 0 ? 0 : (long) Math.min(Long.MAX_VALUE / 4.0, Math.floor(item(id) * count)); }

        long uu(long mb) { return mb <= 0 ? 0 : (long) Math.min(Long.MAX_VALUE / 4.0, Math.floor(uuPerMb * mb)); }
    }

    static long[] parts(Map<UUID, long[]> all, UUID id) {
        if (id == null) return new long[WealthRules.PARTS];
        return all.computeIfAbsent(id, k -> new long[WealthRules.PARTS]);
    }

    static void add(Map<UUID, long[]> all, UUID id, int part, long value) {
        if (id == null || value == 0) return;
        long[] p = parts(all, id);
        p[part] = WealthRules.add(p[part], value);
    }

    /** The assets by part of every player with anything on the exchange. */
    static Map<UUID, long[]> compute(MinecraftServer server) {
        ExchangeData d = ExchangeData.get(server);
        Prices prices = new Prices(server, d);
        Map<UUID, long[]> all = new HashMap<>();
        // the coin account, the UU account, the mailbox items
        for (Map.Entry<UUID, ExchangeData.Mailbox> e : d.mail.entrySet()) {
            ExchangeData.Mailbox box = e.getValue();
            add(all, e.getKey(), WealthRules.PART_COINS, box.coins);
            add(all, e.getKey(), WealthRules.PART_UU, prices.uu(box.uu));
            for (Map.Entry<String, Long> it : box.items.entrySet()) add(all, e.getKey(), WealthRules.PART_GOODS, prices.items(it.getKey(), it.getValue()));
        }
        // carried coins: online players now, the others at their logout
        Set<UUID> online = new java.util.HashSet<>();
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            online.add(p.getUUID());
            long carried = carried(p);
            if (carried > 0) add(all, p.getUUID(), WealthRules.PART_COINS, carried);
            else parts(all, p.getUUID());
        }
        d.heldCoins.forEach((id, v) -> { if (!online.contains(id)) add(all, id, WealthRules.PART_COINS, v); });
        // resting orders of the book: a buy order holds its coins (or UU-matter), a sell order its goods
        for (ExchangeData.Book b : d.books.values()) {
            for (OrderBook.Order o : b.book.orders()) {
                UUID owner = uuid(o.owner());
                if (owner == null) continue;
                long left = o.remaining();
                if (o.buy()) {
                    long escrow = WealthRules.add(0, o.price() * left);
                    if (b.currency == Currency.UU) add(all, owner, WealthRules.PART_UU, prices.uu(escrow));
                    else add(all, owner, WealthRules.PART_ESCROW, escrow);
                } else if (UuBoard.is(b.commodity)) {
                    add(all, owner, WealthRules.PART_UU, prices.uu(UuBoard.mb(left)));
                } else {
                    add(all, owner, WealthRules.PART_GOODS, prices.items(b.commodity, left));
                }
            }
        }
        // commission rewards held for the requesters
        for (ExchangeData.Commission c : d.commissions) {
            if (c.system || c.requester == null) continue;
            long left = c.rewardTotal - c.rewardPaid;
            if (left <= 0) continue;
            switch (c.kind) {
                case COIN -> add(all, c.requester, WealthRules.PART_ESCROW, left);
                case UU -> add(all, c.requester, WealthRules.PART_UU, prices.uu(left));
                case ITEM -> add(all, c.requester, WealthRules.PART_GOODS, prices.items(c.rewardItem, left));
            }
        }
        // the daily auction holds the items
        for (ExchangeData.AuctionOrder a : d.auction) add(all, a.owner, WealthRules.PART_GOODS, prices.items(a.item, a.quantity));
        // shops: goods in stock, coins of buy offers, the proceeds box
        for (ShopRules.Shop s : d.shops.values()) {
            for (ShopRules.Offer o : s.offers) {
                if (o.kind == ShopRules.Kind.BUY) add(all, s.owner, WealthRules.PART_ESCROW, o.stockCoins());
                else add(all, s.owner, WealthRules.PART_GOODS, prices.items(o.goods, o.stockItems()));
            }
            for (Map.Entry<String, Long> e : s.proceeds.entrySet()) add(all, s.owner, WealthRules.PART_GOODS, prices.items(e.getKey(), e.getValue()));
        }
        // futures: what each lot would pay at today's price
        for (ExchangeData.Position p : d.positions) {
            FutureCommodity c = FutureCommodityManager.getCommodityById(p.commodity);
            long settle = c == null ? p.price : prices.fm.getPrice(p.commodity, c.getBasePrice());
            add(all, p.owner, WealthRules.PART_FUTURES, Forwards.settlement(p.isLong, p.price, p.quantity, settle));
        }
        // 0.1.10.43: bond principal; fund principal and the units waiting for the next market day
        for (BondRules.Bond b : d.bonds) add(all, b.owner, WealthRules.PART_BONDS, b.principal());
        d.fund.investors.forEach((id, i) -> add(all, id, WealthRules.PART_BONDS, WealthRules.add(i.principal(), i.pending * FundRules.UNIT)));
        return all;
    }

    private static UUID uuid(String s) {
        try { return s == null || s.isEmpty() ? null : UUID.fromString(s); } catch (IllegalArgumentException e) { return null; }
    }
}
