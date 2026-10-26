// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.exchange;

import com.miophas.singularity_iteration.common.Singularity_Iteration_Config;
import com.miophas.singularity_iteration.common.future.FutureCommodityManager;
import com.miophas.singularity_iteration.common.future.FutureMarketData;
import com.miophas.singularity_iteration.common.future.MarketQuote;
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
        MinecraftServer server = player.server;
        ExchangeData d = ExchangeData.get(server);
        FutureMarketData fm = FutureMarketData.get(server.overworld());
        long day = ExchangeService.today(server);
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
            long ask = 0, bid = 0, supply = 0;
            int budgetLeft = 0;
            if (m.system()) {
                supply = ExchangeService.supply(d, m.id(), day);
                if (supply > 0) ask = ExchangeService.systemCost(d, m, 1);
                budgetLeft = Math.min(Integer.MAX_VALUE, MarketQuote.budgetLeft(fm.getSystemBought(m.id()),
                    Singularity_Iteration_Config.FUTURE_SYSTEM_BUY_PER_DAY.get()));
                if (budgetLeft > 0) bid = Math.max(0, ExchangeService.systemPayout(server, m, 1));
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
            detail = new ExchangeView.Detail(m.id(), m.currency().ordinal(), m.system(), m.price(), ask, bid, supply, budgetLeft,
                Math.round(m.fundamental()), m.depth(), m.low(), Math.min(m.high(), Long.MAX_VALUE / 8), bars(d, fm, m, day), bids, asks, trades,
                l3, s3, l7, s7, ExchangeService.dailyLeft(server, player.getUUID()));
        }

        List<ExchangeView.MyOrder> orders = new ArrayList<>();
        for (ExchangeData.Book b : d.books.values())
            for (OrderBook.Order o : b.book.orders())
                if (o.owner().equals(me) && orders.size() < ExchangeView.MAX_ORDERS)
                    orders.add(new ExchangeView.MyOrder(o.id(), b.commodity, o.buy(), o.price(), o.remaining(), o.day()));

        List<ExchangeView.MailLine> mail = new ArrayList<>();
        for (Map.Entry<String, Long> e : box.items.entrySet())
            if (mail.size() < ExchangeView.MAX_MAIL) mail.add(new ExchangeView.MailLine(e.getKey(), e.getValue()));

        List<ExchangeData.Commission> open = new ArrayList<>(d.commissions);
        open.sort(Comparator.comparing((ExchangeData.Commission c) -> !c.system).thenComparingLong(c -> c.expiry));
        List<ExchangeView.CommissionLine> commissions = new ArrayList<>();
        for (ExchangeData.Commission c : open) {
            if (commissions.size() >= ExchangeView.MAX_COMMISSIONS) break;
            commissions.add(new ExchangeView.CommissionLine(c.id, c.system ? "" : c.requesterName, c.item, c.quantity, c.delivered,
                c.kind.ordinal(), c.rewardItem, c.rewardTotal, c.expiry, c.system, player.getUUID().equals(c.requester)));
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
            ledger.add(new ExchangeView.LedgerLine(led.get(i).day, led.get(i).minted, led.get(i).burned));

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

        return new ExchangeView(day, coins, box.uu, box.coins, box.items.size(), energy, rows, detail, orders, mail, commissions, positions,
            ledger, stocks, List.copyOf(candidates));
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
