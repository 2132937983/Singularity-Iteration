// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.exchange;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 0.1.10.43 (exchange proposal idea 5, Q9): treasury bonds. Pure (the offline harness runs it).
 *
 * <ul>
 *   <li><b>Terms and units</b>: {@link #TERMS} 7 or 28 market days, {@link #UNIT} coins a unit. At maturity the principal and
 *       the interest go to the holder's coin account.</li>
 *   <li><b>Rate</b> per market week, fixed at the purchase ({@link #weeklyRatePpm}): with the stabiliser on
 *       {@code 0.5 % + 1.5 % x (net issue of the last 7 closed days / target - 1)} within 0.2 % .. 2.0 %, so the rate rises
 *       when the server issues more coins than the target and bonds take coins out of circulation; with the stabiliser off a
 *       fixed 0.5 %. A 28-day bond earns four weeks of it.</li>
 *   <li><b>Early exit</b>: the principal less {@link #EARLY_PENALTY_PERMILLE} per mille (2 %), no interest; the penalty goes
 *       to the treasury reserve.</li>
 *   <li><b>Caps</b> ({@link #room}): a player holds at most {@code bondMaxPerPlayer} coins of principal (1,000,000); all bonds
 *       together at most {@code bondMaxSupplyPercent} (25 %) of the stabiliser's money supply estimate; above it no new bonds
 *       are sold. 0 = no cap.</li>
 *   <li><b>Ledger</b>: the principal is frozen (out of circulation, which the stabiliser reads) until it is paid back; the
 *       interest is booked as bond interest (from the reserve first, the rest new coins).</li>
 * </ul>
 */
public final class BondRules {
    private BondRules() {}

    public static final long UNIT = 1_000;
    public static final int[] TERMS = {7, 28};
    /** Rates are kept in parts per million of the principal per market week. */
    public static final int PPM = 1_000_000;
    public static final int BASE_PPM = 5_000, SLOPE_PPM = 15_000, MIN_PPM = 2_000, MAX_PPM = 20_000;
    public static final int EARLY_PENALTY_PERMILLE = 20;
    /** Holdings per player (purchases of the same term, rate and maturity add to one holding). */
    public static final int MAX_HOLDINGS = 32;
    /** Units of one purchase at the most (the caps usually stop it earlier). */
    public static final long MAX_UNITS = 1_000_000;

    /** A holding of bonds. */
    public static final class Bond {
        public long id;
        public UUID owner;
        public String ownerName = "";
        public long units;
        public int term;
        public int ratePpm;
        public long day, maturity;

        public long principal() { return BondRules.principal(units); }

        public long interest() { return BondRules.interest(principal(), ratePpm, term); }
    }

    public static boolean validTerm(int days) {
        for (int t : TERMS) if (t == days) return true;
        return false;
    }

    public static long principal(long units) { return Math.multiplyExact(Math.max(0, units), UNIT); }

    /**
     * The rate per market week in ppm: with the stabiliser on {@code BASE + SLOPE x (net7 / target - 1)} within MIN .. MAX
     * (rounded to the nearest ppm; a target of 0 gives the base rate); off: the base rate.
     */
    public static int weeklyRatePpm(boolean stabilizerOn, long net7, double target) {
        if (!stabilizerOn || !(target > 0) || !Double.isFinite(target)) return BASE_PPM;
        double r = BASE_PPM + SLOPE_PPM * (net7 / target - 1.0);
        if (!(r > MIN_PPM)) return MIN_PPM;
        if (r >= MAX_PPM) return MAX_PPM;
        return (int) Math.round(r);
    }

    /** The interest of {@code principal} at {@code ratePpm} a week for {@code termDays} market days (rounded down). */
    public static long interest(long principal, int ratePpm, int termDays) {
        if (principal <= 0 || ratePpm <= 0 || termDays <= 0) return 0;
        java.math.BigInteger v = java.math.BigInteger.valueOf(principal).multiply(java.math.BigInteger.valueOf(ratePpm))
            .multiply(java.math.BigInteger.valueOf(termDays)).divide(java.math.BigInteger.valueOf(7L * PPM));
        return v.bitLength() < 63 ? v.longValue() : Long.MAX_VALUE;
    }

    /** The penalty of an early exit (2 % of the principal, rounded up). */
    public static long penalty(long principal) {
        if (principal <= 0) return 0;
        return Math.addExact(Math.multiplyExact(principal / 1000, EARLY_PENALTY_PERMILLE),
            (principal % 1000 * EARLY_PENALTY_PERMILLE + 999) / 1000);
    }

    /** The server's cap of all bonds: {@code percent} of the supply estimate; {@link Long#MAX_VALUE} for 0 (no cap). */
    public static long serverCap(long supply, double percent) {
        if (!(percent > 0)) return Long.MAX_VALUE;
        double c = Math.max(0, supply) * Math.min(100.0, percent) / 100.0;
        return c >= Long.MAX_VALUE / 2.0 ? Long.MAX_VALUE / 2 : (long) Math.floor(c);
    }

    /**
     * Coins of principal a player may still buy: the rest of the player's cap ({@code maxPerPlayer}, 0 = none) and of the
     * server's cap ({@link #serverCap}), never below 0.
     */
    public static long room(long mine, long maxPerPlayer, long outstanding, long serverCap) {
        long a = maxPerPlayer <= 0 ? Long.MAX_VALUE : Math.max(0, maxPerPlayer - Math.max(0, mine));
        long b = serverCap == Long.MAX_VALUE ? Long.MAX_VALUE : Math.max(0, serverCap - Math.max(0, outstanding));
        return Math.min(a, b);
    }

    /** Units a player may still buy ({@link #room} in whole units, at most {@link #MAX_UNITS}). */
    public static long unitsRoom(long room) { return Math.min(MAX_UNITS, Math.max(0, room) / UNIT); }

    /** The principal of all bonds. */
    public static long outstanding(List<Bond> bonds) {
        long s = 0;
        for (Bond b : bonds) s = WealthRules.add(s, b.principal());
        return s;
    }

    /** The principal of {@code owner}'s bonds. */
    public static long held(List<Bond> bonds, UUID owner) {
        long s = 0;
        for (Bond b : bonds) if (b.owner.equals(owner)) s = WealthRules.add(s, b.principal());
        return s;
    }

    /** {@code owner}'s holdings, the earliest maturity first. */
    public static List<Bond> of(List<Bond> bonds, UUID owner) {
        List<Bond> out = new ArrayList<>();
        for (Bond b : bonds) if (b.owner.equals(owner)) out.add(b);
        out.sort((x, y) -> x.maturity != y.maturity ? Long.compare(x.maturity, y.maturity) : Long.compare(x.id, y.id));
        return out;
    }

    /**
     * Adds a purchase: to the owner's holding of the same term, rate and maturity, else as a new holding with {@code id}.
     * Null when the owner already has {@link #MAX_HOLDINGS} holdings and none matches (nothing is added).
     */
    public static Bond add(List<Bond> bonds, long id, UUID owner, String name, long units, int term, int ratePpm, long day) {
        long maturity = day + term;
        int mine = 0;
        for (Bond b : bonds) {
            if (!b.owner.equals(owner)) continue;
            mine++;
            if (b.term == term && b.ratePpm == ratePpm && b.maturity == maturity) {
                b.units = Math.addExact(b.units, units);
                if (name != null && !name.isEmpty()) b.ownerName = name;
                return b;
            }
        }
        if (mine >= MAX_HOLDINGS) return null;
        Bond b = new Bond();
        b.id = id;
        b.owner = owner;
        b.ownerName = name == null ? "" : name;
        b.units = units;
        b.term = term;
        b.ratePpm = ratePpm;
        b.day = day;
        b.maturity = maturity;
        bonds.add(b);
        return b;
    }

    /** Whether {@link #add} takes a purchase: the owner has a holding of the same term, rate and maturity or fewer than {@link #MAX_HOLDINGS}. */
    public static boolean canAdd(List<Bond> bonds, UUID owner, int term, int ratePpm, long maturity) {
        int mine = 0;
        for (Bond b : bonds) {
            if (!b.owner.equals(owner)) continue;
            if (b.term == term && b.ratePpm == ratePpm && b.maturity == maturity) return true;
            mine++;
        }
        return mine < MAX_HOLDINGS;
    }

    /** The holdings due on {@code day} (maturity reached), in the order of the list. */
    public static List<Bond> due(List<Bond> bonds, long day) {
        List<Bond> out = new ArrayList<>();
        for (Bond b : bonds) if (b.maturity <= day) out.add(b);
        return out;
    }

    /** "0.85%" for {@code ppm} (two decimals). */
    public static String percent(int ppm) {
        return String.format(java.util.Locale.ROOT, "%.2f%%", ppm / 10_000.0);
    }
}
