// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.exchange;

import com.miophas.singularity_iteration.common.exchange.BountyBoard.Bounty;
import com.miophas.singularity_iteration.common.exchange.BountyBoard.Line;
import com.miophas.singularity_iteration.common.exchange.BountyBoard.Need;
import com.miophas.singularity_iteration.common.exchange.BountyTables.Kind;
import com.miophas.singularity_iteration.common.exchange.BountyTables.Source;
import com.miophas.singularity_iteration.common.exchange.BountyTables.Split;
import com.miophas.singularity_iteration.common.exchange.BountyTables.Tier;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

/**
 * 0.1.10.26 (X3): the {@link BountyBoard} in the exchange save ({@code mio_icif_exchange}, key {@code Bounties}). An
 * older save has no key and loads an empty board; a damaged bounty is dropped (its reserved items with it, as for a
 * damaged order).
 */
final class BountyNbt {
    private BountyNbt() {}

    static CompoundTag save(BountyBoard board) {
        CompoundTag tag = new CompoundTag();
        ListTag list = new ListTag();
        for (Bounty b : board.bounties) {
            CompoundTag x = new CompoundTag();
            x.putLong("Id", b.id);
            x.putString("Tier", b.tier.name());
            x.putString("Table", b.table);
            x.putLong("Day", b.day);
            x.putLong("Expiry", b.expiry);
            x.putString("Split", b.split.name());
            x.putDouble("Share", b.share);
            ListTag needs = new ListTag();
            for (Need n : b.needs) {
                CompoundTag t = new CompoundTag();
                t.putString("Item", n.item); t.putLong("Count", n.count); t.putLong("Delivered", n.delivered); t.putLong("Unit", n.unit);
                needs.add(t);
            }
            x.put("Needs", needs);
            ListTag lines = new ListTag();
            for (Line l : b.lines) {
                CompoundTag t = new CompoundTag();
                t.putString("Kind", l.kind.name()); t.putString("Source", l.source.name()); t.putString("Item", l.item);
                t.putLong("Amount", l.amount); t.putLong("Paid", l.paid);
                lines.add(t);
            }
            x.put("Lines", lines);
            ListTag by = new ListTag();
            b.contribution.forEach((id, value) -> {
                CompoundTag t = new CompoundTag();
                t.putString("Player", id.toString()); t.putLong("Value", value); t.putString("Name", b.names.getOrDefault(id, ""));
                by.add(t);
            });
            x.put("Contribution", by);
            // 0.1.10.34: rarity, rush bonus and the claim
            x.putString("Rarity", b.rarity.name());
            x.putInt("Rush", b.rush);
            if (b.claimant != null) {
                CompoundTag c = new CompoundTag();
                c.putString("Player", b.claimant.toString()); c.putString("Name", b.claimName); c.putString("Team", b.claimTeam);
                c.putLong("Day", b.claimDay);
                x.put("Claim", c);
            }
            x.putBoolean("Extended", b.extended);
            list.add(x);
        }
        tag.put("List", list);
        CompoundTag vault = new CompoundTag();
        board.vault.forEach((k, v) -> { if (v > 0) vault.putLong(k, v); });
        tag.put("Vault", vault);
        CompoundTag last = new CompoundTag();
        board.lastPosted.forEach(last::putLong);
        tag.put("LastPosted", last);
        tag.putLong("Week", board.week);
        tag.putLongArray("Issued", board.issued);
        tag.putLong("UuIssued", board.uuIssued);
        tag.putInt("AdvancedPosted", board.advancedPosted);
        // 0.1.10.34
        CompoundTag bans = new CompoundTag();
        board.bans.forEach((id, day) -> bans.putLong(id.toString(), day));
        tag.put("Bans", bans);
        CompoundTag refreshes = new CompoundTag();
        board.refreshes.forEach((id, v) -> refreshes.putLongArray(id.toString(), v));
        tag.put("Refreshes", refreshes);
        tag.putLong("PostedDay", board.postedDay);
        tag.putInt("PostedToday", board.postedToday);
        tag.putLong("LastPostTick", board.lastPostTick);
        tag.putLong("Serial", board.serial);
        return tag;
    }

    static void load(CompoundTag tag, BountyBoard board) {
        board.bounties.clear();
        for (Tag t : tag.getList("List", Tag.TAG_COMPOUND)) {
            try {
                Bounty b = bounty((CompoundTag) t);
                if (b != null) board.bounties.add(b);
            } catch (IllegalArgumentException | ArithmeticException ignored) {
                // a damaged bounty is dropped
            }
        }
        CompoundTag vault = tag.getCompound("Vault");
        for (String k : vault.getAllKeys()) { long v = vault.getLong(k); if (v > 0) board.vault.put(k, v); }
        CompoundTag last = tag.getCompound("LastPosted");
        for (String k : last.getAllKeys()) board.lastPosted.put(k, last.getLong(k));
        board.week = tag.contains("Week") ? tag.getLong("Week") : Long.MIN_VALUE;
        long[] issued = tag.getLongArray("Issued");
        for (int i = 0; i < Math.min(issued.length, board.issued.length); i++) board.issued[i] = Math.max(0, issued[i]);
        board.uuIssued = Math.max(0, tag.getLong("UuIssued"));
        board.advancedPosted = Math.max(0, tag.getInt("AdvancedPosted"));
        // 0.1.10.34
        board.bans.clear();
        CompoundTag bans = tag.getCompound("Bans");
        for (String k : bans.getAllKeys()) { UUID id = uuid(k); if (id != null) board.bans.put(id, bans.getLong(k)); }
        board.refreshes.clear();
        CompoundTag refreshes = tag.getCompound("Refreshes");
        for (String k : refreshes.getAllKeys()) {
            UUID id = uuid(k);
            long[] v = refreshes.getLongArray(k);
            if (id != null && v.length == 3 && v[1] >= 0) board.refreshes.put(id, v);
        }
        board.postedDay = tag.contains("PostedDay") ? tag.getLong("PostedDay") : Long.MIN_VALUE;
        board.postedToday = Math.max(0, tag.getInt("PostedToday"));
        board.lastPostTick = tag.contains("LastPostTick") ? tag.getLong("LastPostTick") : Long.MIN_VALUE;
        board.serial = Math.max(0, tag.getLong("Serial"));
    }

    static UUID uuid(String s) {
        try {
            return UUID.fromString(s);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    static Bounty bounty(CompoundTag x) {
        Bounty b = new Bounty();
        b.id = x.getLong("Id");
        b.tier = Tier.valueOf(x.getString("Tier"));
        b.table = x.getString("Table");
        b.day = x.getLong("Day");
        b.expiry = x.getLong("Expiry");
        b.split = Split.valueOf(x.getString("Split"));
        double share = x.getDouble("Share");
        b.share = share > 0 && share <= 1 ? share : 1.0;
        for (Tag t : x.getList("Needs", Tag.TAG_COMPOUND)) {
            CompoundTag n = (CompoundTag) t;
            Need need = new Need();
            need.item = n.getString("Item"); need.count = n.getLong("Count"); need.delivered = n.getLong("Delivered");
            need.unit = Math.max(1, n.getLong("Unit"));
            if (need.item.isEmpty() || need.count < 1 || need.delivered < 0 || need.delivered > need.count) return null;
            b.needs.add(need);
        }
        for (Tag t : x.getList("Lines", Tag.TAG_COMPOUND)) {
            CompoundTag l = (CompoundTag) t;
            Line line = new Line();
            line.kind = Kind.valueOf(l.getString("Kind")); line.source = Source.valueOf(l.getString("Source")); line.item = l.getString("Item");
            line.amount = l.getLong("Amount"); line.paid = l.getLong("Paid");
            if (line.amount < 0 || line.paid < 0 || line.paid > line.amount) return null;
            b.lines.add(line);
        }
        for (Tag t : x.getList("Contribution", Tag.TAG_COMPOUND)) {
            CompoundTag c = (CompoundTag) t;
            UUID id = UUID.fromString(c.getString("Player"));
            b.contribution.put(id, Math.max(0, c.getLong("Value")));
            if (!c.getString("Name").isEmpty()) b.names.put(id, c.getString("Name"));
        }
        if (b.needs.isEmpty() || b.lines.isEmpty() || b.complete()) return null;
        // 0.1.10.34 (an older save: common, no rush, no claim)
        BountyPacing.Rarity rarity = BountyPacing.Rarity.of(x.getString("Rarity"));
        b.rarity = rarity == null ? BountyPacing.Rarity.COMMON : rarity;
        b.rush = Math.max(0, Math.min(100, x.getInt("Rush")));
        if (x.contains("Claim")) {
            CompoundTag c = x.getCompound("Claim");
            b.claimant = uuid(c.getString("Player"));
            if (b.claimant != null) {
                b.claimName = c.getString("Name");
                b.claimTeam = c.getString("Team");
                b.claimDay = c.getLong("Day");
            }
        }
        b.extended = x.getBoolean("Extended");
        return b;
    }
}
