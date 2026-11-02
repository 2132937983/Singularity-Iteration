// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.exchange;

import java.util.List;
import net.minecraft.network.FriendlyByteBuf;

/**
 * 0.1.10.43 (Q9): the treasury bonds and the treasury fund of one player for the treasury tab ({@link BondsFund#view}), sent
 * with the exchange view. Bounded on both sides.
 */
public record BondFundView(Bonds bonds, Fund fund) {
    public static final int MAX_HOLDINGS = BondRules.MAX_HOLDINGS, MAX_LOTS = FundRules.MAX_LOTS, MAX_WEEKS = FundRules.HISTORY;

    /** A holding: id, units, term (days), weekly rate (ppm), bought and maturity (market days), interest at maturity. */
    public record Holding(long id, long units, int term, int ratePpm, long day, long maturity, long interest) { }

    /**
     * The bonds: sold, the stabiliser on, the weekly rate of a purchase now, the stabiliser's net issue of 7 days and target,
     * the player's principal and cap (0 = none), all bonds and the server's cap (-1 = none), the units the player may still
     * buy, the player's holdings (earliest maturity first).
     */
    public record Bonds(boolean enabled, boolean stabilizer, int ratePpm, long net7, long target, long mine, long maxPerPlayer,
                        long outstanding, long serverCap, long unitsRoom, List<Holding> holdings) {
        public static final Bonds NONE = new Bonds(false, false, BondRules.BASE_PPM, 0, 0, 0, 0, 0, -1, 0, List.of());
    }

    /** A lot of the player's fund units: units, principal now, settled on, paid on (-1 = not booked). */
    public record Lot(long units, long principal, long settled, long redeemDay) { }

    /** A distributed week of the fund ({@link FundRules.Week}); {@code mine}: the player's dividend (+) or loss (-). */
    public record Week(long week, long profit, long loss, long invested, long stockCost, int sharePpm, long dividends, long losses,
                       long kept, long mine) { }

    /**
     * The fund: subscriptions taken, the player's units waiting for the next market day, lots, dividends and losses in total;
     * all invested principal, all waiting units, the cost of the treasury stock, the investors' share now (ppm), this market
     * week's realised profit and loss so far ({@link FundRules#KINDS}), the distributed weeks (newest first), the investors.
     */
    public record Fund(boolean enabled, long pending, List<Lot> lots, long paid, long lost, long invested, long allPending, long stockCost,
                       int sharePpm, List<Long> thisWeek, List<Week> weeks, int investors) {
        public static final Fund NONE = new Fund(false, 0, List.of(), 0, 0, 0, 0, 0, 0, List.of(), List.of(), 0);

        public long units() { long s = 0; for (Lot l : lots) s += l.units(); return s; }

        public long principal() { long s = 0; for (Lot l : lots) s = WealthRules.add(s, l.principal()); return s; }

        /** Units not booked for redemption. */
        public long free() { long s = 0; for (Lot l : lots) if (l.redeemDay() < 0) s += l.units(); return s; }

        /** This week's realised profit and loss of the treasury. */
        public long week(int kind) { return kind >= 0 && kind < thisWeek.size() ? thisWeek.get(kind) : 0; }
    }

    public static final BondFundView NONE = new BondFundView(Bonds.NONE, Fund.NONE);

    static void write(FriendlyByteBuf b, BondFundView v) {
        BondFundView x = v == null ? NONE : v;
        Bonds s = x.bonds() == null ? Bonds.NONE : x.bonds();
        b.writeBoolean(s.enabled()); b.writeBoolean(s.stabilizer()); b.writeVarInt(s.ratePpm()); b.writeLong(s.net7()); b.writeVarLong(s.target());
        b.writeVarLong(s.mine()); b.writeVarLong(s.maxPerPlayer()); b.writeVarLong(s.outstanding()); b.writeLong(s.serverCap());
        b.writeVarLong(s.unitsRoom());
        ExchangeView.list(b, s.holdings(), MAX_HOLDINGS, h -> { b.writeVarLong(h.id()); b.writeVarLong(h.units()); b.writeVarInt(h.term());
            b.writeVarInt(h.ratePpm()); b.writeVarLong(h.day()); b.writeVarLong(h.maturity()); b.writeVarLong(h.interest()); });
        Fund f = x.fund() == null ? Fund.NONE : x.fund();
        b.writeBoolean(f.enabled()); b.writeVarLong(f.pending());
        ExchangeView.list(b, f.lots(), MAX_LOTS, l -> { b.writeVarLong(l.units()); b.writeVarLong(l.principal()); b.writeVarLong(l.settled());
            b.writeLong(l.redeemDay()); });
        b.writeVarLong(f.paid()); b.writeVarLong(f.lost()); b.writeVarLong(f.invested()); b.writeVarLong(f.allPending()); b.writeVarLong(f.stockCost());
        b.writeVarInt(f.sharePpm());
        ExchangeView.list(b, f.thisWeek(), FundRules.KINDS, b::writeVarLong);
        ExchangeView.list(b, f.weeks(), MAX_WEEKS, w -> { b.writeLong(w.week()); b.writeVarLong(w.profit()); b.writeVarLong(w.loss());
            b.writeVarLong(w.invested()); b.writeVarLong(w.stockCost()); b.writeVarInt(w.sharePpm()); b.writeVarLong(w.dividends());
            b.writeVarLong(w.losses()); b.writeVarLong(w.kept()); b.writeLong(w.mine()); });
        b.writeVarInt(f.investors());
    }

    static BondFundView read(FriendlyByteBuf b) {
        boolean enabled = b.readBoolean(), stabilizer = b.readBoolean();
        int rate = b.readVarInt();
        long net7 = b.readLong(), target = b.readVarLong(), mine = b.readVarLong(), max = b.readVarLong(), outstanding = b.readVarLong();
        long serverCap = b.readLong(), room = b.readVarLong();
        List<Holding> holdings = ExchangeView.read(b, MAX_HOLDINGS, () -> new Holding(b.readVarLong(), b.readVarLong(), b.readVarInt(),
            b.readVarInt(), b.readVarLong(), b.readVarLong(), b.readVarLong()));
        Bonds bonds = new Bonds(enabled, stabilizer, rate, net7, target, mine, max, outstanding, serverCap, room, holdings);
        boolean fundOn = b.readBoolean();
        long pending = b.readVarLong();
        List<Lot> lots = ExchangeView.read(b, MAX_LOTS, () -> new Lot(b.readVarLong(), b.readVarLong(), b.readVarLong(), b.readLong()));
        long paid = b.readVarLong(), lost = b.readVarLong(), invested = b.readVarLong(), allPending = b.readVarLong(), stockCost = b.readVarLong();
        int share = b.readVarInt();
        List<Long> thisWeek = ExchangeView.read(b, FundRules.KINDS, b::readVarLong);
        List<Week> weeks = ExchangeView.read(b, MAX_WEEKS, () -> new Week(b.readLong(), b.readVarLong(), b.readVarLong(), b.readVarLong(),
            b.readVarLong(), b.readVarInt(), b.readVarLong(), b.readVarLong(), b.readVarLong(), b.readLong()));
        Fund fund = new Fund(fundOn, pending, lots, paid, lost, invested, allPending, stockCost, share, thisWeek, weeks, b.readVarInt());
        return new BondFundView(bonds, fund);
    }
}
