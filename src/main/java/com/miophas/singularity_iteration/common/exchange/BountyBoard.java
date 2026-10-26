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

    /** The market as the board sees it. */
    public interface Prices {
        /** Coins one item costs at the market's ask now (price x (1 + spread / 2)), or 0 when it has no price. */
        double ask(String item);

        /** A datapack commodity (delivered items go to the treasury; others go to the vault). */
        boolean isCommodity(String item);
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

    public void rollWeek(long day) {
        long w = BountyMath.week(day);
        if (w == week) return;
        week = w;
        java.util.Arrays.fill(issued, 0);
        uuIssued = 0;
        advancedPosted = 0;
    }

    public long issueLeft(Tier t, Rules r, long day) {
        return BountyMath.left(r.issueCap(t), issued[t.ordinal()], week, BountyMath.week(day));
    }

    public long uuLeft(Rules r, long day) { return BountyMath.left(r.uuPerWeek(), uuIssued, week, BountyMath.week(day)); }

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
            n.count = q.count();
            double unit = q.value() > 0 ? q.value() : prices.ask(q.item());
            n.unit = Math.max(1, Math.round(unit));
            d.needs.add(n);
        }
        String pool = t.pool().isEmpty() ? r.pool(t.tier()) : t.pool();
        d.lines.addAll(rewards(tables.pools().get(pool), t.rolls(), d.value(), d.multiplier, t.tier(), r, treasury, prices, day, salt,
            needItems(d)));
        return d;
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
        b.expiry = day + Math.min(60, d.days);
        b.split = d.split == null ? Split.FIRST : d.split;
        b.share = d.share > 0 && d.share <= 1 ? d.share : 1.0;
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
            if (b.expiry > day) continue;
            it.remove();
            payouts.addAll(settleExpired(b, treasury));
            if (expired != null) expired.add(b);
        }
        rollWeek(day);
        // NORMAL: keep the day's target open
        int target = BountyMath.normalTarget(day, r.normalMin(), r.normalMax(), r.seed());
        List<String> pool = new ArrayList<>();
        for (String item : lacking) if (!blocks(item) && prices.ask(item) > 0) pool.add(item);
        for (int attempt = 0; open(Tier.NORMAL) < target && attempt < target * 4 && bounties.size() < MAX_OPEN; attempt++) {
            List<Table> normal = new ArrayList<>();
            for (Table t : tables.tier(Tier.NORMAL)) if (eligible(t, day) && hooks.eligible(t)) normal.add(t);
            int[] weights = new int[normal.size() + 1];
            boolean[] available = new boolean[weights.length];
            weights[0] = r.normalGeneratedWeight();
            available[0] = !pool.isEmpty();
            for (int i = 0; i < normal.size(); i++) { weights[i + 1] = normal.get(i).weight(); available[i + 1] = true; }
            long salt = 0x4E4F524DL + attempt;
            int pick = BountyMath.pick(weights, available, BountyMath.roll(r.seed() ^ salt, day));
            if (pick < 0) break;
            Draft d;
            if (pick == 0) {
                int k = (int) Math.min(pool.size() - 1, Math.floor(BountyMath.roll(r.seed() ^ (salt * 31), day) * pool.size()));
                String item = pool.remove(k);
                double ask = prices.ask(item);
                double v = r.normalValue() * (0.5 + BountyMath.roll(r.seed() ^ (salt * 131), day));
                int q = BountyMath.quantity(v, ask, r.normalMinQuantity(), r.normalMaxQuantity());
                if (q < 0) continue;
                d = draftGenerated(item, q, ask, r, tables, treasury, prices, day, salt);
            } else {
                d = draft(normal.get(pick - 1), r, tables, treasury, prices, day, salt);
            }
            post(d, day, r, treasury, hooks, posted);
        }
        // ADVANCED: the week's posts, spread over its days
        int weekly = BountyMath.advancedTarget(week, r.advancedMin(), r.advancedMax(), r.seed());
        if (BountyMath.advancedDue(day, weekly, advancedPosted)) {
            Table t = pickTable(tables.tier(Tier.ADVANCED), day, hooks, r, 0x41445643L);
            if (t != null) post(draft(t, r, tables, treasury, prices, day, 0x41445643L), day, r, treasury, hooks, posted);
        }
        // SPECIAL: by chance
        if (open(Tier.SPECIAL) < r.specialMaxOpen() && BountyMath.roll(r.seed() ^ 0x5350434CL, day) < r.specialChance()) {
            Table t = pickTable(tables.tier(Tier.SPECIAL), day, hooks, r, 0x5350434DL);
            if (t != null) post(draft(t, r, tables, treasury, prices, day, 0x5350434DL), day, r, treasury, hooks, posted);
        }
        return payouts;
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
