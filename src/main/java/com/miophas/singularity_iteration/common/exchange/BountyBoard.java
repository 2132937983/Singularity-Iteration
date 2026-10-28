// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.exchange;

import com.miophas.singularity_iteration.common.exchange.BountyTables.Entry;
import com.miophas.singularity_iteration.common.exchange.BountyTables.Kind;
import com.miophas.singularity_iteration.common.exchange.BountyTables.Parsed;
import com.miophas.singularity_iteration.common.exchange.BountyTables.Pool;
import com.miophas.singularity_iteration.common.exchange.BountyTables.Requirement;
import com.miophas.singularity_iteration.common.exchange.BountyTables.Source;
import com.miophas.singularity_iteration.common.exchange.BountyTables.Split;
import com.miophas.singularity_iteration.common.exchange.BountyTables.Table;
import com.miophas.singularity_iteration.common.exchange.BountyTables.Tier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

/**
 * 0.1.10.26 (X3): the system bounties of the exchange in three tiers, as a pure state machine (no Minecraft types; the
 * offline harness runs it). {@link ExchangeData} keeps one board; {@link Bounties} connects it to the game.
 *
 * <h2>Tiers</h2>
 * <ul>
 *   <li><b>NORMAL</b>: the board keeps {@code normalMin..normalMax} open (fixed per day). A request is generated from a
 *       commodity the market lacks (no unlock, treasury below {@code treasuryLow}): its value is {@code normalValue x
 *       U[0.5, 1.5]}, the quantity {@code round(value / ask)} within {@code normalMinQuantity..normalMaxQuantity}; or a
 *       datapack table of the normal tier, by weight. 3 days, reward {@code 1.15 x} the value.</li>
 *   <li><b>ADVANCED</b>: {@code advancedMin..advancedMax} a market week from the datapack tables, spread over the week.
 *       7 days, reward {@code 1.3 x} the value; in multiplayer one player may deliver at most {@code advancedShare} of the
 *       value until the last day.</li>
 *   <li><b>SPECIAL</b>: a chance of {@code specialChance} a day, at most {@code specialMaxOpen} open, from the datapack
 *       tables (rare items); rewards set by the table's pool, never coins by default.</li>
 * </ul>
 *
 * <h2>Rewards (conservation)</h2>
 * Resolved when the bounty is posted, from the table's pool: coins (minted on payment, value x multiplier), items from the
 * treasury (the market's stock, reserved at posting), items from the vault (rare items delivered to earlier bounties,
 * reserved), items and UU-matter issued at payment (counted against the tier's weekly cap at posting). An entry that cannot
 * be paid is not drawn; with none left, NORMAL and ADVANCED pay coins and SPECIAL is not posted. Delivered commodities go to
 * the treasury, other items to the vault, so rare items circulate. At the expiry the unpaid reserved items go back; unpaid
 * coins and issued items are never created.
 *
 * <h2>Payment</h2>
 * {@link Split#FIRST}: each delivery is paid at once, every reward line in proportion to the delivered value.
 * {@link Split#CONTRIBUTION}: paid at completion or expiry, every line split by the delivered value with the largest
 * remainder ({@link BountyMath#apportion}).
 */
public final class BountyBoard {

    /** The settings of the bounty tiers ({@link EconomySettings} reads them from the config and the datapacks). */
    public record Rules(int normalMin, int normalMax, double normalValue, int normalDays, double normalMultiplier,
                        int normalMinQuantity, int normalMaxQuantity, int normalGeneratedWeight, String normalPool,
                        int advancedMin, int advancedMax, int advancedDays, double advancedMultiplier, double advancedShare,
                        String advancedPool, double specialChance, int specialMaxOpen, int specialDays, String specialPool,
                        long issueNormal, long issueAdvanced, long issueSpecial, long uuPerWeek, long treasuryLow, long seed) {

        public static Rules defaults(long seed) {
            return new Rules(3, 5, 4000, 3, 1.15, 16, 256, 100, "mio_icif:normal", 2, 3, 7, 1.3, 0.5, "mio_icif:advanced",
                0.05, 1, 7, "mio_icif:special", 0, 16, 1, 4000, 64, seed);
        }

        public int days(Tier t) { return switch (t) { case NORMAL -> normalDays; case ADVANCED -> advancedDays; case SPECIAL -> specialDays; }; }

        public double multiplier(Tier t) { return switch (t) { case NORMAL -> normalMultiplier; case ADVANCED -> advancedMultiplier; case SPECIAL -> 1.0; }; }

        public long issueCap(Tier t) { return switch (t) { case NORMAL -> issueNormal; case ADVANCED -> issueAdvanced; case SPECIAL -> issueSpecial; }; }

        public String pool(Tier t) { return switch (t) { case NORMAL -> normalPool; case ADVANCED -> advancedPool; case SPECIAL -> specialPool; }; }

        public double share(Tier t) { return t == Tier.ADVANCED ? advancedShare : 1.0; }
    }

    /**
     * 0.1.10.34: the pace settings and the players of the server, set by the game before the board posts: random days by
     * rarity ({@code randomDays}, within {@code minDays..maxDays}; off = the fixed days of the tiers), claims a player may
     * hold, the active players (online, or used the exchange in the last three market days) and the players online.
     */
    public record Pace(boolean randomDays, int minDays, int maxDays, int acceptMax, int active, int online) {
        public static final Pace DEFAULT = new Pace(true, 1, 7, 1, 1, 1);
    }

    /** The market as the board sees it. */
    public interface Prices {
        /** Coins one item costs at the market's ask now (price x (1 + spread / 2)), or 0 when it has no price. */
        double ask(String item);

        /** A datapack commodity (delivered items go to the treasury; others go to the vault). */
        boolean isCommodity(String item);

        /** 0.1.10.33: the stack size of the item (generated requests skip items that do not stack). */
        default int maxStack(String item) { return 64; }

        /** 0.1.10.33: coins of one mB of UU-matter (to value UU rewards; 0 = unknown). */
        default double uuPerMb() { return 0; }

        /** 0.1.10.34: the value of one item a request asks for (between the market's ask and bid); the ask by default. */
        default double value(String item) { return ask(item); }
    }

    /** What the game decides for the board while a day is posted. */
    public interface Hooks {
        /** A new bounty id. */
        long newId();

        /** The table's conditions that need the game (an online player completed its advancement). */
        boolean eligible(Table table);

        /** The draft about to be posted (the posting event may change it); null cancels the posting. */
        Draft posting(Draft draft);
    }

    public static final class Need {
        public String item = "";
        public long count, delivered;
        /** Coins per item at posting (at least 1): the weight of a delivery for proportional payment and the share cap. */
        public long unit = 1;

        public long left() { return Math.max(0, count - delivered); }
    }

    public static final class Line {
        public Kind kind = Kind.COINS;
        public Source source = Source.MINTED;
        public String item = "";
        public long amount, paid;

        public long unpaid() { return Math.max(0, amount - paid); }
    }

    public static final class Bounty {
        public long id;
        public Tier tier = Tier.NORMAL;
        public String table = "";
        public long day, expiry;
        public Split split = Split.FIRST;
        /** Share of the value one player may deliver (1 = no cap). */
        public double share = 1.0;
        public final List<Need> needs = new ArrayList<>();
        public final List<Line> lines = new ArrayList<>();
        /** 0.1.10.34: the rarity (its range of days) and the rush bonus (percent) of the posting. */
        public BountyPacing.Rarity rarity = BountyPacing.Rarity.COMMON;
        public int rush;
        /** 0.1.10.34: the claim: the player (null = none), their name and team, the market day; the one extra day given. */
        public UUID claimant;
        public String claimName = "", claimTeam = "";
        public long claimDay;
        public boolean extended;
        /** Delivered value per player, in the order of their first delivery. */
        public final LinkedHashMap<UUID, Long> contribution = new LinkedHashMap<>();
        public final Map<UUID, String> names = new HashMap<>();

        public long all() {
            long v = 0;
            for (Need n : needs) v = Math.addExact(v, Math.multiplyExact(n.count, n.unit));
            return v;
        }

        public long done() {
            long v = 0;
            for (Need n : needs) v = Math.addExact(v, Math.multiplyExact(Math.min(n.delivered, n.count), n.unit));
            return v;
        }

        public boolean complete() {
            for (Need n : needs) if (n.left() > 0) return false;
            return true;
        }

        public long mine(UUID player) { return contribution.getOrDefault(player, 0L); }
    }

    /** A bounty about to be posted. The lists may be changed (posting event). */
    public static final class Draft {
        public Tier tier = Tier.NORMAL;
        public String table = "";
        public int days;
        public Split split = Split.FIRST;
        public double share = 1.0;
        public final List<Need> needs = new ArrayList<>();
        public final List<Line> lines = new ArrayList<>();
        /** The bounty multiplier (coins of a fallback reward = value x multiplier). */
        public double multiplier = 1.0;
        /** Fall back to coins when no reward line can be paid (NORMAL and ADVANCED). */
        public boolean coinsFallback = true;
        /** 0.1.10.34: the rarity and the rush bonus (percent, already in {@link #multiplier}). */
        public BountyPacing.Rarity rarity = BountyPacing.Rarity.COMMON;
        public int rush;

        public long value() {
            long v = 0;
            for (Need n : needs) v = Math.addExact(v, Math.multiplyExact(n.count, Math.max(1, n.unit)));
            return v;
        }
    }

    /** What the game pays out: to a player, coins / items / UU-matter from a source. */
    public record Payout(UUID player, Kind kind, Source source, String item, long amount) { }

    /** A delivery: its payouts and whether it completed the bounty. */
    public record Delivery(List<Payout> payouts, boolean completed) { }

    public static final int MAX_OPEN = 64, MAX_NEEDS = BountyTables.MAX_REQUIREMENTS, MAX_LINES = 8;
    /** The longest a bounty stays open (days). */
    public static final int MAX_DAYS = 60;

    // ------------------------------------------------------------------ state

    public final List<Bounty> bounties = new ArrayList<>();
    /** Rare items delivered to bounties that are no commodities; they pay later bounties. */
    public final Map<String, Long> vault = new TreeMap<>();
    /** Table id to the day it was last posted (cooldown). */
    public final Map<String, Long> lastPosted = new HashMap<>();
    public long week = Long.MIN_VALUE;
    /** Items issued (counted at posting) this week, per tier. */
    public final long[] issued = new long[Tier.values().length];
    /** UU-matter (mB) issued (counted at posting) this week. */
    public long uuIssued;
    /** ADVANCED bounties posted this week. */
    public int advancedPosted;
    /** 0.1.10.34: players who abandoned a claim and the market day they did (no claim that day). */
    public final Map<UUID, Long> bans = new HashMap<>();
    /** 0.1.10.34: refreshes per player: {market day, refreshes that day, game tick of the last}. */
    public final Map<UUID, long[]> refreshes = new HashMap<>();
    /** 0.1.10.34: NORMAL postings of {@link #postedDay} (the daily cap), the game tick of the last one, a posting counter. */
    public long postedDay = Long.MIN_VALUE, lastPostTick = Long.MIN_VALUE, serial;
    public int postedToday;
    /** 0.1.10.34: the pace of the server, set by the game before the board posts (not saved). */
    public Pace pace = Pace.DEFAULT;

    public void rollWeek(long day) {
        long w = BountyMath.week(day);
        if (w == week) return;
        week = w;
        java.util.Arrays.fill(issued, 0);
        uuIssued = 0;
        advancedPosted = 0;
    }

    /** 0.1.10.34: the weekly caps grow with the square root of the active players. */
    public long issueLeft(Tier t, Rules r, long day) {
        return BountyMath.left(BountyPacing.issueCap(r.issueCap(t), pace.active()), issued[t.ordinal()], week, BountyMath.week(day));
    }

    public long uuLeft(Rules r, long day) {
        return BountyMath.left(BountyPacing.issueCap(r.uuPerWeek(), pace.active()), uuIssued, week, BountyMath.week(day));
    }

    /** 0.1.10.34: the weekly cap of a tier for the active players. */
    public long issueCap(Tier t, Rules r) { return BountyPacing.issueCap(r.issueCap(t), pace.active()); }

    /** 0.1.10.34: the weekly UU cap for the active players. */
    public long uuCap(Rules r) { return BountyPacing.issueCap(r.uuPerWeek(), pace.active()); }

    public int open(Tier t) {
        int n = 0;
        for (Bounty b : bounties) if (b.tier == t) n++;
        return n;
    }

    public int open(String table) {
        int n = 0;
        for (Bounty b : bounties) if (b.table.equals(table)) n++;
        return n;
    }

    public Bounty find(long id) {
        for (Bounty b : bounties) if (b.id == id) return b;
        return null;
    }

    /** An open NORMAL or ADVANCED bounty still needs {@code item}: the market does not sell it meanwhile. */
    public boolean blocks(String item) {
        for (Bounty b : bounties) {
            if (b.tier == Tier.SPECIAL) continue;
            for (Need n : b.needs) if (n.item.equals(item) && n.left() > 0) return true;
        }
        return false;
    }

    /**
     * 0.1.10.33: the most an open NORMAL or ADVANCED bounty pays per item of {@code item} it still needs (coins; reward items
     * at the market's ask, UU-matter at {@link Prices#uuPerMb}; 0 = none). While it is open the market sells the item at this
     * price or more, so buying from the market to deliver earns nothing; before, the market did not sell it at all (an emerald
     * bounty stopped the emerald market).
     */
    public long rewardPerItem(String item, Prices prices) {
        double best = 0;
        for (Bounty b : bounties) {
            if (b.tier == Tier.SPECIAL) continue;
            for (Need n : b.needs) {
                if (!n.item.equals(item) || n.left() <= 0) continue;
                long all = b.all();
                if (all <= 0) continue;
                double value = 0;
                for (Line l : b.lines) value += lineValue(l, prices);
                best = Math.max(best, value * Math.max(1, n.unit) / all);
            }
        }
        return best >= Long.MAX_VALUE / 4.0 ? Long.MAX_VALUE / 4 : (long) Math.ceil(best);
    }

    /** 0.1.10.33: the coins a reward line is worth (items at the market's ask, 0 when it has none). */
    static double lineValue(Line l, Prices prices) {
        return switch (l.kind) {
            case COINS -> l.amount;
            case ITEM -> l.amount * Math.max(0, prices.ask(l.item));
            case UU -> l.amount * Math.max(0, prices.uuPerMb());
        };
    }

    /** The table may be posted on {@code day}: its minimum day, cooldown and open count (the game checks the advancement). */
    public boolean eligible(Table t, long day) {
        if (t.weight() <= 0 || day < t.minDay() || open(t.id()) >= t.maxOpen()) return false;
        Long last = lastPosted.get(t.id());
        return last == null || t.cooldownDays() <= 0 || day - last >= t.cooldownDays();
    }

    // ------------------------------------------------------------------ drafting

    /** A NORMAL request generated for a commodity: {@code quantity} at {@code ask} coins each. */
    public Draft draftGenerated(String item, long quantity, double ask, Rules r, Parsed tables, Map<String, Long> treasury, Prices prices,
                                long day, long salt) {
        Draft d = new Draft();
        d.tier = Tier.NORMAL;
        d.days = r.normalDays();
        d.multiplier = r.normalMultiplier();
        Need n = new Need();
        n.item = item;
        n.count = quantity;
        n.unit = Math.max(1, Math.round(ask));
        d.needs.add(n);
        pace(d, Integer.MAX_VALUE, "", -1, -1, -1, r, day, salt);
        d.lines.addAll(rewards(tables.pools().get(r.normalPool()), 1, d.value(), d.multiplier, Tier.NORMAL, r, treasury, prices, day, salt,
            needItems(d)));
        return d;
    }

    /** A draft from a datapack table: its requirements valued at the ask (or their {@code value}), the rewards from its pool. */
    public Draft draft(Table t, Rules r, Parsed tables, Map<String, Long> treasury, Prices prices, long day, long salt) {
        Draft d = new Draft();
        d.tier = t.tier();
        d.table = t.id();
        d.days = t.days() > 0 ? t.days() : r.days(t.tier());
        d.split = t.split();
        d.share = t.playerShare() > 0 ? t.playerShare() : r.share(t.tier());
        d.multiplier = t.multiplier() > 0 ? t.multiplier() : r.multiplier(t.tier());
        d.coinsFallback = t.tier() != Tier.SPECIAL;
        for (Requirement q : t.requirements()) {
            Need n = new Need();
            n.item = q.item();
            // 0.1.10.33: at most eight inventory slots of the item (the loader warns about such tables)
            n.count = BountyMath.tableCount(q.count(), prices.maxStack(q.item()));
            // 0.1.10.34: valued between the ask and the bid (Prices.value)
            double unit = q.value() > 0 ? q.value() : prices.value(q.item());
            n.unit = Math.max(1, Math.round(unit));
            d.needs.add(n);
        }
        pace(d, t.weight(), t.rarity(), t.days(), t.minDays(), t.maxDays(), r, day, salt);
        String pool = t.pool().isEmpty() ? r.pool(t.tier()) : t.pool();
        d.lines.addAll(rewards(tables.pools().get(pool), t.rolls(), d.value(), d.multiplier, t.tier(), r, treasury, prices, day, salt,
            needItems(d)));
        return d;
    }

    /**
     * 0.1.10.34: the days, rarity and rush bonus of a draft (before its rewards): a table's fixed {@code duration_days} as
     * before; else, with random days on, a range by the rarity (the table's, or by tier and weight) or the table's
     * {@code days}, raised to the effort of the value, drawn per bounty; the rush bonus raises the multiplier.
     */
    void pace(Draft d, int weight, String rarityName, int fixedDays, int minDays, int maxDays, Rules r, long day, long salt) {
        BountyPacing.Rarity named = BountyPacing.Rarity.of(rarityName);
        d.rarity = named != null ? named : BountyPacing.rarity(d.tier.ordinal(), weight);
        d.rush = 0;
        if (fixedDays > 0) { d.days = fixedDays; return; }
        if (!pace.randomDays()) return;
        int lo = minDays > 0 ? minDays : d.rarity.minDays, hi = maxDays > 0 ? maxDays : d.rarity.maxDays;
        d.days = BountyPacing.days(lo, hi, d.value(), pace.minDays(), pace.maxDays(), BountyMath.roll(r.seed() ^ salt ^ 0x44415953L, day));
        d.rush = BountyPacing.rushPercent(d.days);
        d.multiplier *= 1 + d.rush / 100.0;
    }

    /** The items a draft asks for (a reward never pays one of them). */
    public static java.util.Set<String> needItems(Draft d) {
        java.util.Set<String> out = new java.util.HashSet<>();
        for (Need n : d.needs) out.add(n.item);
        return out;
    }

    /**
     * Resolves {@code rolls} reward lines from a pool for a bounty worth {@code value}: each roll targets
     * {@code value x multiplier / rolls}. Entries that cannot be paid now are not drawn (stock, vault, weekly caps, a value
     * too small for one item), and never an item of {@code exclude} (what the bounty asks for). Lines of the same item and
     * source are merged.
     */
    List<Line> rewards(Pool pool, int rolls, long value, double multiplier, Tier tier, Rules r, Map<String, Long> treasury, Prices prices,
                       long day, long salt, java.util.Set<String> exclude) {
        List<Line> out = new ArrayList<>();
        if (pool == null || pool.entries().isEmpty()) return out;
        List<Entry> entries = pool.entries();
        double perRoll = value * multiplier / Math.max(1, rolls);
        for (int k = 0; k < rolls; k++) {
            int[] weights = new int[entries.size()];
            boolean[] available = new boolean[entries.size()];
            long[] amounts = new long[entries.size()];
            for (int i = 0; i < entries.size(); i++) {
                Entry e = entries.get(i);
                weights[i] = e.weight();
                amounts[i] = amount(e, perRoll, prices);
                available[i] = amounts[i] > 0 && !exclude.contains(e.item())
                    && canPay(e.kind(), e.source(), e.item(), amounts[i], out, tier, r, treasury, day);
            }
            int pick = BountyMath.pick(weights, available, BountyMath.roll(r.seed() ^ salt, day * 64 + k));
            if (pick < 0) continue;
            Entry e = entries.get(pick);
            add(out, e.kind(), e.source(), e.item(), amounts[pick]);
        }
        return out;
    }

    /** The amount an entry pays for a roll worth {@code value} (0 = cannot pay). */
    static long amount(Entry e, double value, Prices prices) {
        return switch (e.kind()) {
            case COINS -> e.count() > 0 ? e.count() : Math.max(0, Math.round(value * e.multiplier()));
            case UU -> e.count();
            case ITEM -> e.count() > 0 ? e.count() : BountyMath.rewardCount(value, e.multiplier(), prices.ask(e.item()));
        };
    }

    /** A line of {@code amount} could be reserved now, besides the lines already in {@code drafted}. */
    boolean canPay(Kind kind, Source source, String item, long amount, List<Line> drafted, Tier tier, Rules r, Map<String, Long> treasury, long day) {
        if (amount <= 0) return false;
        long taken = 0;
        for (Line l : drafted) if (l.kind == kind && l.source == source && l.item.equals(item)) taken += l.amount;
        long need = amount + taken;
        return switch (source) {
            case MINTED -> kind == Kind.COINS;
            case TREASURY -> kind == Kind.ITEM && treasury.getOrDefault(item, 0L) >= need;
            case VAULT -> kind == Kind.ITEM && vault.getOrDefault(item, 0L) >= need;
            case ISSUE -> {
                if (kind == Kind.UU) {
                    long uu = 0;
                    for (Line l : drafted) if (l.kind == Kind.UU) uu += l.amount;
                    yield uuLeft(r, day) >= amount + uu;
                }
                long items = 0;
                for (Line l : drafted) if (l.kind == Kind.ITEM && l.source == Source.ISSUE) items += l.amount;
                yield kind == Kind.ITEM && issueLeft(tier, r, day) >= amount + items;
            }
        };
    }

    static void add(List<Line> lines, Kind kind, Source source, String item, long amount) {
        for (Line l : lines) {
            if (l.kind == kind && l.source == source && l.item.equals(item)) { l.amount = Math.addExact(l.amount, amount); return; }
        }
        Line l = new Line();
        l.kind = kind;
        l.source = source;
        l.item = item;
        l.amount = amount;
        lines.add(l);
    }

    // ------------------------------------------------------------------ posting

    /**
     * Posts a draft: checks it (needs, lines), drops lines that cannot be paid any more, reserves the treasury and vault
     * items and counts issued items and UU-matter against the weekly caps. Without a line: coins (value x multiplier) when the
     * draft allows it, else nothing is posted (null).
     */
    public Bounty commit(Draft d, long id, long day, Rules r, Map<String, Long> treasury) {
        rollWeek(day);
        if (d == null || bounties.size() >= MAX_OPEN || d.needs.isEmpty() || d.needs.size() > MAX_NEEDS || d.days < 1) return null;
        for (int i = 0; i < d.needs.size(); i++) {
            Need n = d.needs.get(i);
            if (n.item == null || n.item.isEmpty() || n.count < 1 || n.count > BountyTables.MAX_COUNT) return null;
            for (int j = 0; j < i; j++) if (d.needs.get(j).item.equals(n.item)) return null;
            n.unit = Math.max(1, n.unit);
            n.delivered = 0;
        }
        List<Line> kept = new ArrayList<>();
        for (Line l : d.lines) {
            if (l == null || l.item == null || kept.size() >= MAX_LINES) continue;
            if (l.kind == Kind.COINS) { l.source = Source.MINTED; l.item = ""; }
            if (l.kind == Kind.UU) { l.source = Source.ISSUE; l.item = ""; }
            if (l.kind == Kind.ITEM && (l.item.isEmpty() || l.source == Source.MINTED)) continue;
            boolean asked = false;
            for (Need n : d.needs) if (n.item.equals(l.item)) asked = true;
            if (asked) continue;
            if (canPay(l.kind, l.source, l.item, l.amount, kept, d.tier, r, treasury, day)) add(kept, l.kind, l.source, l.item, l.amount);
        }
        if (kept.isEmpty()) {
            long coins = Math.round(d.value() * d.multiplier);
            if (!d.coinsFallback || coins <= 0) return null;
            add(kept, Kind.COINS, Source.MINTED, "", coins);
        }
        Bounty b = new Bounty();
        b.id = id;
        b.tier = d.tier;
        b.table = d.table == null ? "" : d.table;
        b.day = day;
        b.expiry = day + Math.min(MAX_DAYS, d.days);
        b.split = d.split == null ? Split.FIRST : d.split;
        b.share = d.share > 0 && d.share <= 1 ? d.share : 1.0;
        b.rarity = d.rarity == null ? BountyPacing.Rarity.COMMON : d.rarity;
        b.rush = Math.max(0, d.rush);
        for (Need n : d.needs) {
            Need c = new Need();
            c.item = n.item; c.count = n.count; c.unit = n.unit;
            b.needs.add(c);
        }
        for (Line l : kept) {
            switch (l.source) {
                case TREASURY -> treasury.merge(l.item, -l.amount, Long::sum);
                case VAULT -> vault.merge(l.item, -l.amount, Long::sum);
                case ISSUE -> { if (l.kind == Kind.UU) uuIssued += l.amount; else issued[b.tier.ordinal()] += l.amount; }
                case MINTED -> { }
            }
            b.lines.add(l);
        }
        treasury.values().removeIf(v -> v <= 0);
        vault.values().removeIf(v -> v <= 0);
        bounties.add(b);
        if (!b.table.isEmpty()) lastPosted.put(b.table, day);
        if (b.tier == Tier.ADVANCED) advancedPosted++;
        if (b.tier == Tier.NORMAL) {
            if (postedDay != day) { postedDay = day; postedToday = 0; }
            postedToday++;
        }
        return b;
    }

    /**
     * The work of a new market day: expires due bounties (payouts of a contribution split, reserved items back), then
     * posts NORMAL requests up to the day's target, an ADVANCED one when the week's rhythm asks for it, and a SPECIAL one by
     * chance. {@code lacking}: commodities a NORMAL request may ask for (no unlock, low treasury), in a fixed order.
     */
    public List<Payout> newDay(long day, Rules r, Parsed tables, Map<String, Long> treasury, Prices prices, List<String> lacking, Hooks hooks,
                               List<Bounty> posted, List<Bounty> expired) {
        List<Payout> payouts = new ArrayList<>();
        for (Iterator<Bounty> it = bounties.iterator(); it.hasNext(); ) {
            Bounty b = it.next();
            // 0.1.10.33: a bounty with more than 60 days left or posted after today is damaged (a market day that went back)
            if (b.expiry > day && b.expiry - day <= MAX_DAYS && b.day <= day) continue;
            it.remove();
            payouts.addAll(settleExpired(b, treasury));
            if (expired != null) expired.add(b);
        }
        rollWeek(day);
        if (postedDay != day) { postedDay = day; postedToday = 0; }
        // NORMAL: keep the day's target open (0.1.10.34: by the active players, plus the claimed ones; within the daily cap)
        int target = normalTarget(day, r);
        postNormal(target, BountyPacing.dailyPosts(pace.active()), day, r, tables, treasury, prices, lacking, hooks, posted, java.util.Set.of());
        // ADVANCED: the week's posts, spread over its days (0.1.10.34: more for more players)
        int weekly = BountyPacing.advancedWeekly(BountyMath.advancedTarget(week, r.advancedMin(), r.advancedMax(), r.seed()), pace.active());
        if (BountyMath.advancedDue(day, weekly, advancedPosted)) {
            Table t = pickTable(tables.tier(Tier.ADVANCED), day, hooks, r, 0x41445643L);
            if (t != null) post(draft(t, r, tables, treasury, prices, day, 0x41445643L), day, r, treasury, hooks, posted);
        }
        // SPECIAL: by chance
        if (open(Tier.SPECIAL) < BountyPacing.specialOpen(r.specialMaxOpen(), pace.active())
            && BountyMath.roll(r.seed() ^ 0x5350434CL, day) < r.specialChance()) {
            Table t = pickTable(tables.tier(Tier.SPECIAL), day, hooks, r, 0x5350434DL);
            if (t != null) post(draft(t, r, tables, treasury, prices, day, 0x5350434DL), day, r, treasury, hooks, posted);
        }
        return payouts;
    }

    /** 0.1.10.34: NORMAL requests to keep open on {@code day}: the range for the active players, plus the claimed ones. */
    public int normalTarget(long day, Rules r) {
        int[] range = BountyPacing.normalRange(r.normalMin(), r.normalMax(), pace.active());
        int claimed = 0;
        for (Bounty b : bounties) if (b.tier == Tier.NORMAL && b.claimant != null) claimed++;
        return BountyMath.normalTarget(day, range[0], range[1], r.seed()) + claimed;
    }

    /** 0.1.10.34: NORMAL postings left today under the daily cap. */
    public int postsLeft(long day) {
        return Math.max(0, BountyPacing.dailyPosts(pace.active()) - (postedDay == day ? postedToday : 0));
    }

    /**
     * Posts NORMAL requests until {@code target} are open or {@code cap} were posted today ({@link Integer#MAX_VALUE}: no
     * cap): a generated request for a commodity the market lacks, or a NORMAL table, by weight. Items and tables of
     * {@code avoid} are not drawn (a refresh does not bring back what it replaced). Returns the postings.
     */
    int postNormal(int target, int cap, long day, Rules r, Parsed tables, Map<String, Long> treasury, Prices prices, List<String> lacking,
                   Hooks hooks, List<Bounty> posted, java.util.Set<String> avoid) {
        List<String> pool = new ArrayList<>();
        // 0.1.10.33: never an item that does not stack
        for (String item : lacking)
            if (!blocks(item) && !avoid.contains(item) && prices.ask(item) > 0 && prices.maxStack(item) > 1) pool.add(item);
        int done = 0;
        for (int attempt = 0; open(Tier.NORMAL) < target && attempt < target * 4 && bounties.size() < MAX_OPEN; attempt++) {
            if (cap != Integer.MAX_VALUE && (postedDay == day ? postedToday : 0) >= cap) break;
            List<Table> normal = new ArrayList<>();
            for (Table t : tables.tier(Tier.NORMAL)) if (!avoid.contains(t.id()) && eligible(t, day) && hooks.eligible(t)) normal.add(t);
            int[] weights = new int[normal.size() + 1];
            boolean[] available = new boolean[weights.length];
            weights[0] = r.normalGeneratedWeight();
            available[0] = !pool.isEmpty();
            for (int i = 0; i < normal.size(); i++) { weights[i + 1] = normal.get(i).weight(); available[i + 1] = true; }
            // 0.1.10.34: the salt counts every posting attempt (the board posts during the day too)
            long salt = 0x4E4F524DL + serial++;
            int pick = BountyMath.pick(weights, available, BountyMath.roll(r.seed() ^ salt, day));
            if (pick < 0) break;
            Draft d;
            if (pick == 0) {
                int k = (int) Math.min(pool.size() - 1, Math.floor(BountyMath.roll(r.seed() ^ (salt * 31), day) * pool.size()));
                String item = pool.remove(k);
                // 0.1.10.34: valued between the ask and the bid (Prices.value)
                double unit = prices.value(item);
                double v = r.normalValue() * (0.5 + BountyMath.roll(r.seed() ^ (salt * 131), day));
                int q = BountyMath.generatedQuantity(v, unit, r.normalMinQuantity(), r.normalMaxQuantity(), prices.maxStack(item));
                if (q < 0) continue;
                d = draftGenerated(item, q, unit, r, tables, treasury, prices, day, salt);
            } else {
                d = draft(normal.get(pick - 1), r, tables, treasury, prices, day, salt);
            }
            if (post(d, day, r, treasury, hooks, posted) != null) done++;
        }
        return done;
    }

    /**
     * 0.1.10.34: the refill during the day ({@code now} = game tick): one NORMAL request when the board is below the day's
     * target and the wait for the players online passed since the last posting, within the daily cap. Returns the postings.
     */
    public int refill(long day, long now, Rules r, Parsed tables, Map<String, Long> treasury, Prices prices, List<String> lacking, Hooks hooks,
                      List<Bounty> posted) {
        if (postedDay != day) { postedDay = day; postedToday = 0; }
        if (!BountyPacing.refillDue(open(Tier.NORMAL), normalTarget(day, r), now, lastPostTick, pace.online(), postedToday, pace.active()))
            return 0;
        int target = open(Tier.NORMAL) + 1;
        int n = postNormal(target, BountyPacing.dailyPosts(pace.active()), day, r, tables, treasury, prices, lacking, hooks, posted, java.util.Set.of());
        // the wait starts again after a posting; a refill that posted nothing (no table, no lacking commodity, draws that
        // did not fit) tries again at the next check
        if (n > 0) lastPostTick = now;
        return n;
    }

    /** A table among {@code all} that may be posted now, by weight; null when none. */
    Table pickTable(List<Table> all, long day, Hooks hooks, Rules r, long salt) {
        List<Table> ok = new ArrayList<>();
        for (Table t : all) if (eligible(t, day) && hooks.eligible(t)) ok.add(t);
        int[] weights = new int[ok.size()];
        boolean[] available = new boolean[ok.size()];
        for (int i = 0; i < ok.size(); i++) { weights[i] = ok.get(i).weight(); available[i] = true; }
        int pick = BountyMath.pick(weights, available, BountyMath.roll(r.seed() ^ salt, day));
        return pick < 0 ? null : ok.get(pick);
    }

    /** Posts a draft through the posting hook; returns the bounty or null. */
    public Bounty post(Draft d, long day, Rules r, Map<String, Long> treasury, Hooks hooks, List<Bounty> posted) {
        if (d == null) return null;
        Draft x = hooks.posting(d);
        if (x == null) return null;
        Bounty b = commit(x, hooks.newId(), day, r, treasury);
        if (b != null && posted != null) posted.add(b);
        return b;
    }

    // ------------------------------------------------------------------ claims (0.1.10.34)

    /** Claims {@code player} holds. */
    public int claims(UUID player) {
        int n = 0;
        for (Bounty b : bounties) if (player.equals(b.claimant)) n++;
        return n;
    }

    /**
     * Why {@code player} may not claim {@code b} on {@code day} (a language key), or null: NORMAL and ADVANCED only, nobody
     * delivered yet, not claimed, fewer than {@link Pace#acceptMax} claims, not abandoned today.
     */
    public String claimRefusal(Bounty b, UUID player, long day) {
        if (b.tier == Tier.SPECIAL) return "bounty_claim_special";
        if (b.claimant != null) return player.equals(b.claimant) ? "bounty_claim_mine" : "bounty_claimed";
        if (!b.contribution.isEmpty()) return "bounty_claim_started";
        if (claims(player) >= Math.max(1, pace.acceptMax())) return "bounty_claim_limit";
        Long ban = bans.get(player);
        if (ban != null && ban >= day) return "bounty_claim_banned";
        return null;
    }

    /** Claims {@code b} for {@code player} (check {@link #claimRefusal} first); one day left gets one more (once). */
    public void claim(Bounty b, UUID player, String name, String team, long day) {
        b.claimant = player;
        b.claimName = name == null ? "" : name;
        b.claimTeam = team == null ? "" : team;
        b.claimDay = day;
        long expiry = BountyPacing.claimExpiry(b.expiry, day, b.extended);
        if (expiry != b.expiry) { b.expiry = expiry; b.extended = true; }
    }

    /** Gives up the claim of {@code player}: no claim for them for the rest of the market day. */
    public boolean abandon(Bounty b, UUID player, long day) {
        if (b == null || !player.equals(b.claimant)) return false;
        release(b);
        bans.put(player, day);
        return true;
    }

    static void release(Bounty b) {
        b.claimant = null;
        b.claimName = "";
        b.claimTeam = "";
        b.claimDay = 0;
    }

    /**
     * Releases the claims of players not active for two market days ({@code lastActive}: market day of their last login or
     * use of the exchange; a claimant with none counts from the claim). Returns the released bounties.
     */
    public int releaseAbsent(long day, Map<UUID, Long> lastActive) {
        int n = 0;
        for (Bounty b : bounties) {
            if (b.claimant == null) continue;
            long seen = Math.max(b.claimDay, lastActive.getOrDefault(b.claimant, Long.MIN_VALUE));
            if (BountyPacing.absent(seen, day)) { release(b); n++; }
        }
        bans.values().removeIf(d -> d < day);
        return n;
    }

    /** Only the claimant and their team deliver to a claimed bounty ({@code team}: the deliverer's team, "" = none). */
    public static boolean mayDeliver(Bounty b, UUID player, String team) {
        if (b.claimant == null || b.claimant.equals(player)) return true;
        return !b.claimTeam.isEmpty() && b.claimTeam.equals(team);
    }

    // ------------------------------------------------------------------ refresh (0.1.10.34)

    /** The NORMAL bounties a refresh replaces: not claimed, nothing delivered. */
    public List<Bounty> refreshable() {
        List<Bounty> out = new ArrayList<>();
        for (Bounty b : bounties) if (b.tier == Tier.NORMAL && b.claimant == null && b.contribution.isEmpty()) out.add(b);
        return out;
    }

    /** Items and tables of the {@link #refreshable} bounties (a refresh draws none of them). */
    java.util.Set<String> refreshAvoid() {
        java.util.Set<String> avoid = new java.util.HashSet<>();
        for (Bounty b : refreshable()) {
            if (!b.table.isEmpty()) avoid.add(b.table);
            for (Need n : b.needs) avoid.add(n.item);
        }
        return avoid;
    }

    /**
     * A refresh can post at least one new request: a commodity the market lacks that is not asked for yet, or a NORMAL table
     * that may be posted, outside the replaced ones. Checked before the player pays.
     */
    public boolean refreshPossible(long day, Parsed tables, Prices prices, List<String> lacking, Hooks hooks) {
        if (refreshable().isEmpty()) return false;
        java.util.Set<String> avoid = refreshAvoid();
        for (String item : lacking)
            if (!avoid.contains(item) && !blocks(item) && prices.ask(item) > 0 && prices.maxStack(item) > 1) return true;
        for (Table t : tables.tier(Tier.NORMAL)) if (!avoid.contains(t.id()) && eligible(t, day) && hooks.eligible(t)) return true;
        return false;
    }

    /** Refreshes of {@code player} on {@code day}. */
    public int refreshesToday(UUID player, long day) {
        long[] x = refreshes.get(player);
        return x == null || x[0] != day ? 0 : (int) x[1];
    }

    /** The coins of the next refresh of {@code player}: by the coin value of the rewards of {@link #refreshable}; -1 when none is left. */
    public long refreshCost(UUID player, long day, Prices prices) {
        double value = 0;
        for (Bounty b : refreshable()) for (Line l : b.lines) value += lineValue(l, prices);
        return BountyPacing.refreshCost(value, refreshesToday(player, day));
    }

    /** Ticks until {@code player} may refresh again (0 = now). */
    public long refreshWait(UUID player, long now) {
        long[] x = refreshes.get(player);
        if (x == null || x[2] == Long.MIN_VALUE) return 0;
        return Math.max(0, x[2] + BountyPacing.REFRESH_WAIT_TICKS - now);
    }

    /**
     * Replaces the {@link #refreshable} bounties (their reserved items go back as at an expiry) with as many new NORMAL
     * requests, never the same items or tables, outside the daily cap (they replace unpaid bounties one for one); counts the
     * refresh of {@code player}. The caller took the coins. Returns the bounties replaced.
     */
    public int refresh(UUID player, long day, long now, Rules r, Parsed tables, Map<String, Long> treasury, Prices prices, List<String> lacking,
                       Hooks hooks, List<Bounty> posted) {
        List<Bounty> old = refreshable();
        java.util.Set<String> avoid = refreshAvoid();
        for (Bounty b : old) {
            bounties.remove(b);
            returnUnpaid(b, treasury);
        }
        refreshes.put(player, new long[] {day, refreshesToday(player, day) + 1L, now});
        if (old.isEmpty()) return 0;
        int keep = postedDay == day ? postedToday : 0;
        postNormal(open(Tier.NORMAL) + old.size(), Integer.MAX_VALUE, day, r, tables, treasury, prices, lacking, hooks, posted, avoid);
        if (postedDay == day) postedToday = keep;
        return old.size();
    }

    // ------------------------------------------------------------------ delivery

    /** Value {@code player} may still deliver ({@link Long#MAX_VALUE} without a cap). */
    public long room(Bounty b, UUID player, long day, boolean multiplayer) {
        boolean capped = BountyMath.capped(multiplayer, b.share, day, b.expiry);
        return BountyMath.room(b.all(), b.share, b.mine(player), capped);
    }

    /**
     * Items of need {@code index} the player may deliver now: what is left, within the share cap. A player below the cap may
     * deliver the item that crosses it (by less than one item): with whole items, two players at a half each could otherwise
     * leave a remainder that neither may deliver before the last day.
     */
    public long canDeliver(Bounty b, int index, UUID player, long day, boolean multiplayer) {
        Need n = b.needs.get(index);
        long room = room(b, player, day, multiplayer);
        long unit = Math.max(1, n.unit);
        long byRoom = room == Long.MAX_VALUE ? Long.MAX_VALUE : room <= 0 ? 0 : room / unit + (room % unit == 0 ? 0 : 1);
        return Math.max(0, Math.min(n.left(), byRoom));
    }

    /**
     * Records a delivery of {@code quantity} (the game already took the items; it must not exceed {@link #canDeliver}).
     * Commodities go to the treasury, other items to the vault. A first-come split pays each line in proportion now; the
     * completed bounty is removed and a contribution split pays out.
     */
    public Delivery deliver(Bounty b, int index, long quantity, UUID player, String name, Map<String, Long> treasury, Prices prices) {
        Need n = b.needs.get(index);
        long take = Math.min(quantity, n.left());
        if (take <= 0) return new Delivery(List.of(), false);
        long before = b.done(), all = b.all();
        n.delivered += take;
        b.contribution.merge(player, Math.multiplyExact(take, n.unit), Long::sum);
        if (name != null && !name.isEmpty()) b.names.put(player, name);
        if (prices.isCommodity(n.item)) treasury.merge(n.item, take, Long::sum); else vault.merge(n.item, take, Long::sum);
        List<Payout> payouts = new ArrayList<>();
        long done = b.done();
        if (b.split == Split.FIRST) {
            for (Line l : b.lines) {
                long due = BountyMath.due(l.amount, done, all, l.paid);
                if (due <= 0) continue;
                l.paid += due;
                payouts.add(new Payout(player, l.kind, l.source, l.item, due));
            }
        }
        boolean completed = b.complete();
        if (completed) {
            bounties.remove(b);
            if (b.split == Split.CONTRIBUTION) payouts.addAll(splitByContribution(b, all, all));
        }
        return new Delivery(payouts, completed);
    }

    /** The end of an expired bounty: a contribution split pays the delivered share; reserved items not paid go back. */
    public List<Payout> settleExpired(Bounty b, Map<String, Long> treasury) {
        List<Payout> payouts = new ArrayList<>();
        if (b.split == Split.CONTRIBUTION) payouts.addAll(splitByContribution(b, b.done(), b.all()));
        returnUnpaid(b, treasury);
        return payouts;
    }

    /** Removes a bounty (operator, or the API): unpaid reserved items go back, nothing more is paid. */
    public void cancel(Bounty b, Map<String, Long> treasury) {
        bounties.remove(b);
        returnUnpaid(b, treasury);
    }

    void returnUnpaid(Bounty b, Map<String, Long> treasury) {
        for (Line l : b.lines) {
            long left = l.unpaid();
            if (left <= 0) continue;
            if (l.source == Source.TREASURY) treasury.merge(l.item, left, Long::sum);
            else if (l.source == Source.VAULT) vault.merge(l.item, left, Long::sum);
            l.paid = l.amount;   // closed (issued items and coins are never created)
        }
    }

    /** Pays the share {@code done / all} of every line, split by contribution with the largest remainder. */
    List<Payout> splitByContribution(Bounty b, long done, long all) {
        List<Payout> payouts = new ArrayList<>();
        List<UUID> who = new ArrayList<>(b.contribution.keySet());
        long[] weights = new long[who.size()];
        for (int i = 0; i < who.size(); i++) weights[i] = b.contribution.get(who.get(i));
        for (Line l : b.lines) {
            long due = BountyMath.due(l.amount, done, all, l.paid);
            if (due <= 0) continue;
            long[] parts = BountyMath.apportion(due, weights);
            long given = 0;
            for (int i = 0; i < parts.length; i++) {
                if (parts[i] <= 0) continue;
                payouts.add(new Payout(who.get(i), l.kind, l.source, l.item, parts[i]));
                given += parts[i];
            }
            l.paid += given;
        }
        return payouts;
    }
}
