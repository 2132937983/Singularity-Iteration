// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.exchange;

import com.miophas.singularity_iteration.common.future.FutureCommodityManager;
import com.miophas.singularity_iteration.common.future.FutureMarketData;
import com.miophas.singularity_iteration.common.item.normal.mio_icif_normal;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/** 0.1.10.21 (futures M4): builds the {@link ExchangeView} of one player (server thread). */
public final class ExchangeViews {
    private ExchangeViews() {}

    public static ExchangeView build(ServerPlayer player, String selected, boolean energy) {
        return build(player, selected, energy, "");
    }

    /** 0.1.10.35: with the shops part for {@code shopQuery} ({@link Shops#view}). */
    public static ExchangeView build(ServerPlayer player, String selected, boolean energy, String shopQuery) {
        MinecraftServer server = player.server;
        ExchangeData d = ExchangeData.get(server);
        FutureMarketData fm = FutureMarketData.get(server.overworld());
        long day = ExchangeService.today(server);
        long gameTime = server.overworld().getGameTime();
        String me = player.getUUID().toString();
        long coins = 0;
        for (var stack : player.getInventory().items) if (stack.is(mio_icif_normal.COIN.get())) coins += stack.getCount();
        ExchangeData.Mailbox box = d.mail.getOrDefault(player.getUUID(), new ExchangeData.Mailbox());

        List<ExchangeService.Market> markets = ExchangeService.markets(server);
        List<ExchangeView.Row> rows = new ArrayList<>();
        for (ExchangeService.Market m : markets) {
            long[] spark = spark(d, fm, m);
            long previous = spark.length >= 2 ? spark[spark.length - 2] : m.price();
            int change = previous <= 0 ? 0 : (int) Math.max(-9999, Math.min(9999, (m.price() - previous) * 1000 / previous));
            rows.add(new ExchangeView.Row(m.id(), m.currency().ordinal(), m.system(), m.price(), change, todayVolume(d, m.id(), day), spark,
                m.system() && !FutureCommodityManager.isUnlocked(player, m.commodity())));
        }

        ExchangeView.Detail detail = null;
        ExchangeService.Market m = selected == null || selected.isEmpty() ? null : ExchangeService.market(server, selected);
        if (m != null) {
            long ask = 0, bid = 0, supply = 0, pressure = 0, mine = 0;
            if (m.system()) {
                supply = ExchangeService.supply(d, m.id(), day);
                if (supply > 0) ask = ExchangeService.systemCost(d, m, 1);
                // 0.1.10.25 (X1): the bid of this player's next item on the sell curve
                bid = ExchangeService.nextBid(d, m, player.getUUID(), day);
                pressure = Math.round(ExchangeService.pressure(d, m.id()));
                mine = ExchangeService.soldToday(d, player.getUUID(), m.id(), day);
            }
            ExchangeData.Book book = d.books.get(m.id());
            List<ExchangeView.Level> bids = new ArrayList<>(), asks = new ArrayList<>();
            if (book != null) {
                for (var l : book.book.depth(true, ExchangeView.MAX_LEVELS)) bids.add(new ExchangeView.Level(l.price(), l.quantity(), l.orders()));
                for (var l : book.book.depth(false, ExchangeView.MAX_LEVELS)) asks.add(new ExchangeView.Level(l.price(), l.quantity(), l.orders()));
            }
            List<ExchangeView.TradeLine> trades = new ArrayList<>();
            var q = d.trades.get(m.id());
            if (q != null) {
                var it = q.descendingIterator();
                while (it.hasNext() && trades.size() < ExchangeView.MAX_TRADES) {
                    var t = it.next();
                    trades.add(new ExchangeView.TradeLine(t.price(), t.quantity(), t.time(), me.equals(t.maker()) || me.equals(t.taker())));
                }
            }
            long l3 = 0, s3 = 0, l7 = 0, s7 = 0;
            if (m.system()) {
                var q3 = ExchangeService.futuresQuote(server, m, 3);
                var q7 = ExchangeService.futuresQuote(server, m, 7);
                l3 = q3.longPrice(); s3 = q3.shortPrice(); l7 = q7.longPrice(); s7 = q7.shortPrice();
            }
            int buyLeft = ExchangeService.buyLeft(server, player.getUUID());
            detail = new ExchangeView.Detail(m.id(), m.currency().ordinal(), m.system(), m.price(), ask, bid, supply, pressure,
                Math.round(m.fundamental()), m.depth(), m.low(), Math.min(m.high(), Long.MAX_VALUE / 8), bars(d, fm, m, day), bids, asks, trades,
                l3, s3, l7, s7, mine, buyLeft == Integer.MAX_VALUE ? -1 : buyLeft);
        }

        List<ExchangeView.MyOrder> orders = new ArrayList<>();
        for (ExchangeData.Book b : d.books.values())
            for (OrderBook.Order o : b.book.orders())
                if (o.owner().equals(me) && orders.size() < ExchangeView.MAX_ORDERS) {
                    // 0.1.10.35: the time left of an order with an end in game time
                    Long at = b.expires.get(o.id());
                    orders.add(new ExchangeView.MyOrder(o.id(), b.commodity, o.buy(), o.price(), o.remaining(), o.day(),
                        at == null ? -1 : CommissionHours.secondsLeft(gameTime, at)));
                }

        List<ExchangeView.MailLine> mail = new ArrayList<>();
        for (Map.Entry<String, Long> e : box.items.entrySet())
            if (mail.size() < ExchangeView.MAX_MAIL) mail.add(new ExchangeView.MailLine(e.getKey(), e.getValue()));

        List<ExchangeData.Commission> open = new ArrayList<>(d.commissions);
        // 0.1.10.35: the ones that end first come first (game time; an older commission by its market day)
        open.sort(Comparator.comparing((ExchangeData.Commission c) -> !c.system)
            .thenComparingLong(c -> c.expiresAt > 0 ? CommissionHours.secondsLeft(gameTime, c.expiresAt) : (c.expiry - day) * 1200));
        List<ExchangeView.CommissionLine> commissions = new ArrayList<>();
        for (ExchangeData.Commission c : open) {
            if (commissions.size() >= ExchangeView.MAX_COMMISSIONS) break;
            commissions.add(new ExchangeView.CommissionLine(c.id, c.system ? "" : c.requesterName, c.item, c.quantity, c.delivered,
                c.kind.ordinal(), c.rewardItem, c.rewardTotal, c.expiry, c.system, player.getUUID().equals(c.requester),
                c.expiresAt > 0 ? CommissionHours.secondsLeft(gameTime, c.expiresAt) : -1));
        }

        List<ExchangeView.PositionLine> positions = new ArrayList<>();
        for (ExchangeData.Position p : d.positions) {
            if (!p.owner.equals(player.getUUID())) continue;
            ExchangeService.Market pm = ExchangeService.market(server, p.commodity);
            long now = Forwards.settlement(p.isLong, p.price, p.quantity, pm == null ? p.price : pm.price());
            positions.add(new ExchangeView.PositionLine(p.id, p.commodity, p.isLong, p.price, p.quantity, p.expiry, now));
        }

        List<ExchangeView.LedgerLine> ledger = new ArrayList<>();
        var led = new ArrayList<>(d.ledger);
        for (int i = Math.max(0, led.size() - ExchangeView.MAX_LEDGER); i < led.size(); i++)
            ledger.add(new ExchangeView.LedgerLine(led.get(i).day, led.get(i).created(), led.get(i).destroyed()));   // 0.1.10.35: circulation

        List<Map.Entry<String, Long>> stock = new ArrayList<>(d.treasury.entrySet());
        stock.sort((a, b) -> Long.compare(b.getValue(), a.getValue()));
        List<ExchangeView.StockLine> stocks = new ArrayList<>();
        for (var e : stock) if (stocks.size() < ExchangeView.MAX_STOCKS && e.getValue() > 0) stocks.add(new ExchangeView.StockLine(e.getKey(), e.getValue()));

        List<ExchangeView.Candidate> candidates = new ArrayList<>();
        for (Map.Entry<String, List<ListingRules.Evidence>> e : d.evidence.entrySet()) {
            Set<String> who = new HashSet<>(), by = new HashSet<>(), days = new HashSet<>();
            int fulfilled = 0;
            for (ListingRules.Evidence ev : e.getValue()) {
                if (day - ev.day() >= ListingRules.WINDOW_DAYS) continue;
                who.add(ev.requester()); by.addAll(ev.deliverers()); days.add(Long.toString(ev.day())); fulfilled++;
            }
            if (fulfilled == 0) continue;
            candidates.add(new ExchangeView.Candidate(e.getKey(), server.isSingleplayer() ? days.size() : who.size(), fulfilled, by.size()));
        }
        candidates.sort((a, b) -> Integer.compare(b.fulfilled(), a.fulfilled()));
        if (candidates.size() > ExchangeView.MAX_CANDIDATES) candidates = candidates.subList(0, ExchangeView.MAX_CANDIDATES);

        // 0.1.10.25 (X1): the sell box of the open menu and the player's auction orders
        long allowance = ExchangeService.allowanceLeft(server, d, player.getUUID(), day);
        List<ExchangeView.BoxLine> boxLines = new ArrayList<>();
        ExchangeView.SettleView settle = ExchangeView.SettleView.NONE;
        if (player.containerMenu instanceof com.miophas.singularity_iteration.common.menu.producer.FutureElcMenu menu) {
            boxLines = quoteLines(player, d, ExchangeService.boxContents(player, menu.sellBox()), day, ExchangeView.MAX_BOX);
            // 0.1.10.27 (X2): the settlement slot
            settle = settleView(player, d, menu.settlementSlot(), day);
        }
        List<ExchangeView.AuctionLine> auctions = new ArrayList<>();
        for (ExchangeData.AuctionOrder a : d.auction) {
            if (!a.owner.equals(player.getUUID()) || auctions.size() >= ExchangeView.MAX_AUCTIONS) continue;
            SellDepth.Fill est = ExchangeService.auctionEstimate(server, d, a, day);
            auctions.add(new ExchangeView.AuctionLine(a.id, a.item, a.quantity, a.minPercent, est.quantity(), est.coins()));
        }
        // 0.1.10.26 (X3): the bounties and their weekly budgets
        BountyBoard.Rules rules = Bounties.rules(server);
        BountyBoard board = d.board;
        // 0.1.10.34: the pace for the players now (the caps grow with the active players)
        BountyBoard.Pace pace = Bounties.pace(server, d, day);
        long vaultItems = 0;
        for (long v : board.vault.values()) vaultItems += v;
        boolean thisWeek = board.week == BountyMath.week(day);
        long now = server.overworld().getGameTime();
        int target = board.normalTarget(day, rules), openNormal = board.open(BountyTables.Tier.NORMAL);
        int postedToday = board.postedDay == day ? board.postedToday : 0, dailyPosts = BountyPacing.dailyPosts(pace.active());
        long nextRefill = -1;
        if (openNormal < target && postedToday < dailyPosts && pace.online() > 0) {
            long wait = BountyPacing.refillWait(pace.online());
            long left = board.lastPostTick == Long.MIN_VALUE ? 0 : Math.max(0, board.lastPostTick + wait - now);
            nextRefill = (left + 19) / 20;   // seconds (the board checks every 10 seconds)
        }
        int refreshesLeft = Math.max(0, BountyPacing.REFRESH_PER_DAY - board.refreshesToday(player.getUUID(), day));
        ExchangeView.BountyBudget budget = new ExchangeView.BountyBudget(
            thisWeek ? board.issued[BountyTables.Tier.ADVANCED.ordinal()] : 0, board.issueCap(BountyTables.Tier.ADVANCED, rules),
            thisWeek ? board.issued[BountyTables.Tier.SPECIAL.ordinal()] : 0, board.issueCap(BountyTables.Tier.SPECIAL, rules),
            thisWeek ? board.uuIssued : 0, board.uuCap(rules), board.vault.size(), vaultItems,
            board.refreshCost(player.getUUID(), day, Bounties.prices(server)), refreshesLeft, (board.refreshWait(player.getUUID(), now) + 19) / 20,
            board.refreshable().size(), pace.active(), pace.online(), target, openNormal, postedToday, dailyPosts, nextRefill);
        // 0.1.10.29: the curves of the selected market, the box and the container (the panel's minimum-price previews)
        List<ExchangeView.CurveLine> curves = new ArrayList<>();
        if (m != null && m.system() && m.currency() == ExchangeData.Currency.COIN) addCurve(curves, server, d, player.getUUID(), m.id(), day);
        for (ExchangeView.BoxLine l : boxLines) addCurve(curves, server, d, player.getUUID(), l.id(), day);
        for (ExchangeView.BoxLine l : settle.lines()) addCurve(curves, server, d, player.getUUID(), l.id(), day);
        return new ExchangeView(day, coins, box.uu, box.coins, box.items.size(), energy, rows, detail, orders, mail, commissions, positions,
            ledger, stocks, List.copyOf(candidates), allowance == Long.MAX_VALUE ? -1 : allowance, boxLines, auctions,
            Bounties.lines(player, d, day), budget, settle, stabilizerLine(d, day), d.bankMode.contains(player.getUUID()), curves,
            m != null && m.system() ? m.bountyFloor() : 0, reserveLine(d, day), Shops.view(player, d, shopQuery));
    }

    /** 0.1.10.35 (item 4): the reserve now, its cap, and this market week's fees in, bounty coins paid from it, new bounty coins. */
    static ExchangeView.ReserveLine reserveLine(ExchangeData d, long day) {
        long week = BountyMath.week(day), in = 0, out = 0, made = 0;
        for (ExchangeData.LedgerDay l : d.ledger) {
            if (BountyMath.week(l.day) != week) continue;
            in += l.reserveIn;
            out += l.reserveOut;
            made += Math.max(0, l.bountyCoins - l.reserveOut);
        }
        return new ExchangeView.ReserveLine(d.reserve, ExchangeService.reserveCap(d), in, out, made);
    }

    /** 0.1.10.29: adds the curve of {@code id} for {@code player} (once; datapack commodities only). */
    static void addCurve(List<ExchangeView.CurveLine> out, MinecraftServer server, ExchangeData d, java.util.UUID player, String id, long day) {
        if (out.size() >= ExchangeView.MAX_CURVES || ExchangeView.CurveLine.find(out, id) != null) return;
        ExchangeService.Market m = ExchangeService.market(server, id);
        if (m == null || !m.system()) return;
        SellDepth.Curve c = ExchangeService.curve(m);
        out.add(new ExchangeView.CurveLine(id, c.unit(), c.lp(), c.lg(), ExchangeService.soldToday(d, player, id, day), ExchangeService.pressure(d, id)));
    }

    /** 0.1.10.28 (X4): the stabiliser and the scrap sink of the last closed days, for the treasury tab. */
    static ExchangeView.StabilizerLine stabilizerLine(ExchangeData d, long day) {
        Stabilizer.Rules rules = EconomySettings.stabilizerRules();
        long net7 = 0, scrapIn = 0, scrapOut = 0;
        for (ExchangeData.LedgerDay l : d.ledger) {
            if (l.day >= day || day - l.day > Stabilizer.WINDOW_DAYS) continue;
            net7 += l.circulation();   // 0.1.10.35
            scrapIn += l.scrapIn;
            scrapOut += l.scrapOut;
        }
        int active = 0;
        for (long last : d.lastSale.values()) if (day - last <= Stabilizer.WINDOW_DAYS) active++;
        return new ExchangeView.StabilizerLine(rules.enabled(), rules.enabled() ? d.stabFactor : 1.0, net7,
            Math.round(Stabilizer.target(rules, d.supply, active)), d.supply, active, scrapIn, scrapOut);
    }

    /**
     * The quote of items per commodity (the sell box since 0.1.10.25; 0.1.10.27 also the settlement container): what an instant
     * sale would sell and pay now, and the share of the daily auction if the day changed now.
     */
    static List<ExchangeView.BoxLine> quoteLines(ServerPlayer player, ExchangeData d, Map<String, Long> contents, long day, int max) {
        MinecraftServer server = player.server;
        long allowance = ExchangeService.allowanceLeft(server, d, player.getUUID(), day);
        List<ExchangeView.BoxLine> boxLines = new ArrayList<>();
        for (Map.Entry<String, Long> e : contents.entrySet()) {
            if (boxLines.size() >= max) break;
            ExchangeService.Market bm = ExchangeService.market(server, e.getKey());
            if (bm == null || !bm.system()) continue;
            SellDepth.Curve c = ExchangeService.curve(bm);
            long before = ExchangeService.soldToday(d, player.getUUID(), bm.id(), day);
            double pressure = ExchangeService.pressure(d, bm.id());
            long now = SellDepth.maxForItemPrice(c, before, pressure, 1, e.getValue());
            if (allowance != Long.MAX_VALUE) now = SellDepth.maxForCoins(c, before, pressure, allowance, now);
            long nowCoins = (long) Math.floor(SellDepth.payout(c, before, pressure, now));
            ExchangeData.AuctionOrder probe = new ExchangeData.AuctionOrder();
            probe.owner = player.getUUID();
            probe.item = bm.id();
            probe.quantity = e.getValue();
            ExchangeData.AuctionOrder existing = null;
            for (ExchangeData.AuctionOrder a : d.auction) if (a.owner.equals(player.getUUID()) && a.item.equals(bm.id())) existing = a;
            if (existing != null) { probe.quantity += existing.quantity; probe.minPercent = existing.minPercent; }
            List<ExchangeData.AuctionOrder> others = new ArrayList<>();
            for (ExchangeData.AuctionOrder a : d.auction) if (a.item.equals(bm.id()) && a != existing) others.add(a);
            others.add(probe);
            SellDepth.Fill est = ExchangeService.clearing(server, d, bm, others, day).get(others.size() - 1);
            // the box's share: the estimate of the order with the box minus the estimate without it
            SellDepth.Fill without = existing == null ? new SellDepth.Fill(0, 0) : ExchangeService.auctionEstimate(server, d, existing, day);
            boxLines.add(new ExchangeView.BoxLine(bm.id(), e.getValue(), now, nowCoins, Math.max(0, est.quantity() - without.quantity()),
                Math.max(0, est.coins() - without.coins())));
        }
        return boxLines;
    }

    /** 0.1.10.27 (X2): the view of the container in a settlement slot ({@link ExchangeView.SettleView#NONE} when empty). */
    public static ExchangeView.SettleView settleView(ServerPlayer player, ExchangeData d, net.minecraft.world.Container slot, long day) {
        if (slot == null || slot.getItem(0).isEmpty()) return ExchangeView.SettleView.NONE;
        SettlementStorage st = SettlementStorages.open(slot, 0);
        if (st == null) return ExchangeView.SettleView.NONE;
        List<ExchangeView.BoxLine> lines = quoteLines(player, d, Settlement.contents(player, st), day, ExchangeView.MAX_SETTLE_LINES);
        List<ExchangeView.SettleTarget> targets = new ArrayList<>();
        boolean mp = Bounties.multiplayer(player.server);
        String team = Bounties.team(player);
        for (BountyBoard.Bounty b : d.board.bounties) {
            if (targets.size() >= ExchangeView.MAX_SETTLE_TARGETS) break;
            if (!BountyBoard.mayDeliver(b, player.getUUID(), team)) continue;   // 0.1.10.34: claimed by another player
            long n = 0;
            String first = "";
            for (int i = 0; i < b.needs.size(); i++) {
                BountyBoard.Need need = b.needs.get(i);
                net.minecraft.world.item.Item item = ExchangeService.item(need.item);
                if (item == null || need.left() <= 0) continue;
                long can = Math.min(d.board.canDeliver(b, i, player.getUUID(), day, mp), st.plain(item));
                if (can > 0 && first.isEmpty()) first = need.item;
                n += Math.max(0, can);
            }
            if (n > 0) targets.add(new ExchangeView.SettleTarget(0, b.id, first, n));
        }
        for (ExchangeData.Commission c : d.commissions) {
            if (targets.size() >= ExchangeView.MAX_SETTLE_TARGETS) break;
            if (player.getUUID().equals(c.requester)) continue;
            net.minecraft.world.item.Item item = ExchangeService.item(c.item);
            if (item == null) continue;
            // 0.1.10.33: whole reward lots (CommissionLots)
            long left = c.quantity - c.delivered;
            long n = CommissionLots.take(Math.min(left, st.plain(item)), left, CommissionLots.lot(c.quantity, c.rewardTotal,
                c.kind == ExchangeData.RewardKind.COIN));
            if (n > 0) targets.add(new ExchangeView.SettleTarget(1, c.id, c.item, n));
        }
        return new ExchangeView.SettleView(st.kind(), lines, Settlement.otherItems(player, st), st.uu(), targets,
            st.plain(com.miophas.singularity_iteration.common.item.normal.mio_icif_normal.COIN.get()));
    }

    /** Closing prices for the sparkline: the 20-day history of a datapack commodity, the daily bars of a listing. */
    static long[] spark(ExchangeData d, FutureMarketData fm, ExchangeService.Market m) {
        if (m.system()) {
            List<Integer> history = fm.getPriceHistory(m.id());
            long[] out = new long[Math.min(ExchangeView.MAX_SPARK, history.size())];
            for (int i = 0; i < out.length; i++) out[i] = history.get(history.size() - out.length + i);
            return out.length == 0 ? new long[] {m.price()} : out;
        }
        ArrayDeque<ExchangeData.Bar> q = d.bars.get(m.id());
        if (q == null || q.isEmpty()) return new long[] {m.price()};
        List<ExchangeData.Bar> bars = new ArrayList<>(q);
        int n = Math.min(ExchangeView.MAX_SPARK, bars.size());
        long[] out = new long[n];
        for (int i = 0; i < n; i++) out[i] = bars.get(bars.size() - n + i).close;
        return out;
    }

    static long todayVolume(ExchangeData d, String id, long day) {
        var q = d.bars.get(id);
        return q == null || q.isEmpty() || q.peekLast().day != day ? 0 : q.peekLast().volume;
    }

    /**
     * Daily bars for the chart. A datapack commodity: one bar per day of its price history (open = previous close), with
     * the high, low and volume of the exchange trades of that day when there were any. A listing: its exchange bars.
     */
    static List<ExchangeView.Bar> bars(ExchangeData d, FutureMarketData fm, ExchangeService.Market m, long day) {
        List<ExchangeView.Bar> out = new ArrayList<>();
        ArrayDeque<ExchangeData.Bar> q = d.bars.getOrDefault(m.id(), new ArrayDeque<>());
        if (m.system()) {
            List<Integer> history = fm.getPriceHistory(m.id());
            int n = history.size();
            for (int i = Math.max(0, n - ExchangeView.MAX_BARS); i < n; i++) {
                long d0 = day - (n - 1 - i);
                long close = history.get(i), open = i > 0 ? history.get(i - 1) : close;
                long high = Math.max(open, close), low = Math.min(open, close), volume = 0;
                for (ExchangeData.Bar b : q) if (b.day == d0) { high = Math.max(high, b.high); low = Math.min(low, b.low); volume = b.volume; }
                out.add(new ExchangeView.Bar(d0, open, high, low, close, volume));
            }
            return out;
        }
        List<ExchangeData.Bar> bars = new ArrayList<>(q);
        for (int i = Math.max(0, bars.size() - ExchangeView.MAX_BARS); i < bars.size(); i++) {
            ExchangeData.Bar b = bars.get(i);
            out.add(new ExchangeView.Bar(b.day, b.open, b.high, b.low, b.close, b.volume));
        }
        return out;
    }
}
