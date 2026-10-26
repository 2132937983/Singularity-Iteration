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
        return b;
    }
}
