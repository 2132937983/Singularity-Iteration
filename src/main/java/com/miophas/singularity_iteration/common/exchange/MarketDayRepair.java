// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.exchange;

import com.miophas.singularity_iteration.common.future.FutureMarketData;
import com.miophas.singularity_iteration.common.future.MarketDay;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 0.1.10.33: repairs a market whose day went back under an older version ({@code /time set} moved the world's day back,
 * and the market day was the world's day).
 *
 * <p>The latest day stamp kept by the exchange and the futures market (a bounty or order posted, a position opened, a trade,
 * a ledger day, a price event; never an expiry) is a day the market already lived. When it is later than the market day,
 * the market day moves up to it ({@link MarketDay#repaired}). The records made after the day went back (their start
 * stamp at or before the old market day) move by the same number of days, so they keep their age and their remaining days
 * ({@link MarketDay#shift}); the records of the old timeline keep their stamps (a bounty posted on day 290 for 3 days
 * has 3 days left on the repaired day 290, not 293). Today's counters (the player's sales, the import, the pressure, the
 * allowance) stay today's.
 *
 * <p>The repair changes nothing when no stamp is later than the market day, so it may run at every load
 * ({@link com.miophas.singularity_iteration.common.future.MarketDayDriver}) and by command ({@code /mio_icif market day repair}).
 * Not moved: the ledger, bars, trades (history; a few days of the time between the jump and the repair keep their low days)
 * and delegate ID cards (their expiry is signed into the card item).
 */
public final class MarketDayRepair {
    private MarketDayRepair() {}

    /** What a repair did: the market day before and after, and the records it moved. */
    public record Report(long oldDay, long newDay, long latest, int moved) {
        public boolean changed() { return newDay > oldDay; }
    }

    /** The latest day stamp of the exchange ({@link MarketDay#UNSEEN}: none). */
    public static long latestStamp(ExchangeData d) {
        long m = MarketDay.UNSEEN;
        m = Math.max(m, d.lastDay);
        for (ExchangeData.Book b : d.books.values()) for (OrderBook.Order o : b.book.orders()) m = Math.max(m, o.day());
        for (ExchangeData.Commission c : d.commissions) m = Math.max(m, c.day);
        for (ExchangeData.Position p : d.positions) m = Math.max(m, p.openDay);
        for (BountyBoard.Bounty b : d.board.bounties) m = Math.max(m, b.day);
        for (long v : d.board.lastPosted.values()) m = Math.max(m, v);
        for (ExchangeData.LedgerDay l : d.ledger) m = Math.max(m, l.day);
        for (var q : d.bars.values()) for (ExchangeData.Bar b : q) m = Math.max(m, b.day);
        for (var q : d.trades.values()) for (ExchangeData.Trade t : q) m = Math.max(m, t.day());
        for (ExchangeData.Listing l : d.listings.values()) m = Math.max(m, Math.max(l.listedDay, l.lastTradeDay));
        for (List<ListingRules.Evidence> list : d.evidence.values()) for (ListingRules.Evidence e : list) m = Math.max(m, e.day());
        for (ExchangeData.AuctionOrder a : d.auction) m = Math.max(m, a.day);
        for (long[] a : d.allowance.values()) if (a != null && a.length > 1) m = Math.max(m, a[1]);
        for (long v : d.lastSale.values()) m = Math.max(m, v);
        m = Math.max(m, Math.max(d.soldDay, Math.max(d.pressureDay, Math.max(d.importedDay, d.supplyDay))));
        return m;
    }

    /** Moves the records made after the day went back (start at or before {@code oldDay}) to the repaired day; returns how many. */
    public static int shift(ExchangeData d, long oldDay, long newDay) {
        if (newDay <= oldDay) return 0;
        int[] moved = {0};
        for (ExchangeData.Book b : d.books.values()) {
            b.book.redate(day -> {
                long x = MarketDay.shift(day, day, oldDay, newDay);
                if (x != day) moved[0]++;
                return x;
            });
        }
        for (ExchangeData.Commission c : d.commissions) {
            if (c.day > oldDay) continue;
            c.expiry = MarketDay.shift(c.expiry, c.day, oldDay, newDay);
            c.day = MarketDay.shift(c.day, c.day, oldDay, newDay);
            moved[0]++;
        }
        for (ExchangeData.Position p : d.positions) {
            if (p.openDay > oldDay) continue;
            p.expiry = MarketDay.shift(p.expiry, p.openDay, oldDay, newDay);
            p.openDay = MarketDay.shift(p.openDay, p.openDay, oldDay, newDay);
            moved[0]++;
        }
        for (BountyBoard.Bounty b : d.board.bounties) {
            if (b.day > oldDay) continue;
            b.expiry = MarketDay.shift(b.expiry, b.day, oldDay, newDay);
            b.day = MarketDay.shift(b.day, b.day, oldDay, newDay);
            moved[0]++;
        }
        for (Map.Entry<String, Long> e : d.board.lastPosted.entrySet()) e.setValue(MarketDay.shift(e.getValue(), e.getValue(), oldDay, newDay));
        for (ExchangeData.Listing l : d.listings.values()) {
            l.lastTradeDay = MarketDay.shift(l.lastTradeDay, l.lastTradeDay, oldDay, newDay);
            l.listedDay = MarketDay.shift(l.listedDay, l.listedDay, oldDay, newDay);
        }
        for (Map.Entry<String, List<ListingRules.Evidence>> e : d.evidence.entrySet()) {
            List<ListingRules.Evidence> out = new ArrayList<>(e.getValue().size());
            for (ListingRules.Evidence x : e.getValue())
                out.add(new ListingRules.Evidence(MarketDay.shift(x.day(), x.day(), oldDay, newDay), x.requester(), x.deliverers(), x.unitPrice(), x.rewardValue()));
            e.setValue(out);
        }
        for (ExchangeData.AuctionOrder a : d.auction) {
            if (a.day > oldDay) continue;
            a.day = MarketDay.shift(a.day, a.day, oldDay, newDay);
            moved[0]++;
        }
        for (long[] a : d.allowance.values()) if (a != null && a.length > 1) a[1] = MarketDay.shift(a[1], a[1], oldDay, newDay);
        for (Map.Entry<java.util.UUID, Long> e : d.lastSale.entrySet()) e.setValue(MarketDay.shift(e.getValue(), e.getValue(), oldDay, newDay));
        // today's counters stay today's
        if (d.soldDay == oldDay) d.soldDay = newDay;
        if (d.pressureDay == oldDay) d.pressureDay = newDay;
        if (d.importedDay == oldDay) d.importedDay = newDay;
        if (d.lastDay != Long.MIN_VALUE && d.lastDay < newDay) d.lastDay = newDay;
        d.setDirty();
        return moved[0];
    }

    /**
     * The repair: the market day moves up to the latest stamp of the exchange and the futures market when that is later, and
     * the records made after the day went back move with it. Changes nothing when no stamp is later.
     */
    public static Report repair(ExchangeData d, FutureMarketData fm) {
        long old = fm.getCurrentDay();
        long latest = Math.max(latestStamp(d), fm.latestStamp());
        long day = MarketDay.repaired(old, latest);
        if (day <= old) return new Report(old, old, latest, 0);
        int moved = shift(d, old, day);
        fm.shiftForRepair(old, day);
        fm.setCurrentDay(day);
        return new Report(old, day, latest, moved);
    }
}
