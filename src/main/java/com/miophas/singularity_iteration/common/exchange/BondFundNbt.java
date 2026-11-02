// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.exchange;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

/**
 * 0.1.10.43 (Q9): the treasury bonds and the treasury fund in the exchange save ({@code mio_icif_exchange}, key
 * {@code BondsFund}). An older save has no key and loads none; a damaged holding or lot is dropped.
 */
final class BondFundNbt {
    private BondFundNbt() {}

    /** Holdings of all players at the most (a bound for a damaged save; purchases stop there too). */
    static final int MAX_BONDS = 262_144;
    static final String[] PL_KEYS = {"SaleProfit", "ImportFee", "SaleLoss", "BountyCost"};

    static CompoundTag save(List<BondRules.Bond> bonds, FundRules.Fund fund) {
        CompoundTag tag = new CompoundTag();
        ListTag list = new ListTag();
        for (BondRules.Bond b : bonds) {
            CompoundTag x = new CompoundTag();
            x.putLong("Id", b.id); x.putString("Owner", b.owner.toString()); x.putString("Name", b.ownerName);
            x.putLong("Units", b.units); x.putInt("Term", b.term); x.putInt("Rate", b.ratePpm);
            x.putLong("Day", b.day); x.putLong("Maturity", b.maturity);
            list.add(x);
        }
        tag.put("Bonds", list);
        CompoundTag f = new CompoundTag();
        ListTag investors = new ListTag();
        for (Map.Entry<UUID, FundRules.Investor> e : fund.investors.entrySet()) {
            FundRules.Investor i = e.getValue();
            CompoundTag x = new CompoundTag();
            x.putString("Player", e.getKey().toString()); x.putString("Name", i.name);
            x.putLong("Pending", i.pending); x.putLong("Paid", i.paid); x.putLong("Lost", i.lost);
            ListTag lots = new ListTag();
            for (FundRules.Lot l : i.lots) {
                CompoundTag t = new CompoundTag();
                t.putLong("Units", l.units); t.putLong("Principal", l.principal); t.putLong("Settled", l.settled);
                t.putLong("Redeem", l.redeemDay); t.putLong("Weight", l.weight);
                lots.add(t);
            }
            x.put("Lots", lots);
            ListTag hist = new ListTag();
            for (long[] h : i.history) {
                CompoundTag t = new CompoundTag();
                t.putLong("Week", h[0]); t.putLong("Coins", h[1]);
                hist.add(t);
            }
            x.put("History", hist);
            investors.add(x);
        }
        f.put("Investors", investors);
        ListTag pl = new ListTag();
        fund.pl.forEach((week, v) -> {
            CompoundTag t = new CompoundTag();
            t.putLong("Week", week);
            for (int k = 0; k < FundRules.KINDS; k++) t.putDouble(PL_KEYS[k], v[k]);
            pl.add(t);
        });
        f.put("Results", pl);
        f.putLong("WeightWeek", fund.weightWeek);
        f.putInt("WeightDays", fund.weightDays);
        ListTag weeks = new ListTag();
        for (FundRules.Week w : fund.history) {
            CompoundTag t = new CompoundTag();
            t.putLong("Week", w.week()); t.putLong("Profit", w.profit()); t.putLong("Loss", w.loss());
            t.putLong("Invested", w.invested()); t.putLong("StockCost", w.stockCost()); t.putInt("Share", w.sharePpm());
            t.putLong("Dividends", w.dividends()); t.putLong("Losses", w.losses()); t.putLong("Kept", w.kept());
            weeks.add(t);
        }
        f.put("Weeks", weeks);
        tag.put("Fund", f);
        return tag;
    }

    static void load(CompoundTag tag, List<BondRules.Bond> bonds, FundRules.Fund fund) {
        for (Tag t : tag.getList("Bonds", Tag.TAG_COMPOUND)) {
            CompoundTag x = (CompoundTag) t;
            BondRules.Bond b = new BondRules.Bond();
            b.id = x.getLong("Id"); b.owner = uuid(x.getString("Owner")); b.ownerName = x.getString("Name");
            b.units = x.getLong("Units"); b.term = x.getInt("Term"); b.ratePpm = x.getInt("Rate");
            b.day = x.getLong("Day"); b.maturity = x.getLong("Maturity");
            if (b.owner != null && b.units > 0 && b.units <= Long.MAX_VALUE / BondRules.UNIT && BondRules.validTerm(b.term)
                && b.ratePpm >= 0 && b.ratePpm <= BondRules.MAX_PPM && bonds.size() < MAX_BONDS) bonds.add(b);
        }
        CompoundTag f = tag.getCompound("Fund");
        for (Tag t : f.getList("Investors", Tag.TAG_COMPOUND)) {
            CompoundTag x = (CompoundTag) t;
            UUID id = uuid(x.getString("Player"));
            if (id == null || fund.investors.size() >= MarketRecords.MAX_PLAYERS) continue;
            FundRules.Investor i = new FundRules.Investor();
            i.name = x.getString("Name");
            i.pending = Math.max(0, Math.min(FundRules.MAX_UNITS, x.getLong("Pending")));
            i.paid = Math.max(0, x.getLong("Paid"));
            i.lost = Math.max(0, x.getLong("Lost"));
            for (Tag u : x.getList("Lots", Tag.TAG_COMPOUND)) {
                CompoundTag y = (CompoundTag) u;
                FundRules.Lot l = new FundRules.Lot();
                l.units = y.getLong("Units"); l.principal = Math.max(0, y.getLong("Principal")); l.settled = y.getLong("Settled");
                l.redeemDay = y.contains("Redeem") ? y.getLong("Redeem") : -1;
                l.weight = Math.max(0, y.getLong("Weight"));
                if (l.units > 0 && l.units <= FundRules.MAX_UNITS && i.lots.size() < FundRules.MAX_LOTS) i.lots.add(l);
            }
            for (Tag u : x.getList("History", Tag.TAG_COMPOUND)) {
                CompoundTag y = (CompoundTag) u;
                if (i.history.size() < FundRules.HISTORY) i.history.addLast(new long[] {y.getLong("Week"), y.getLong("Coins")});
            }
            if (!i.empty() || !i.history.isEmpty()) fund.investors.put(id, i);
        }
        for (Tag t : f.getList("Results", Tag.TAG_COMPOUND)) {
            CompoundTag x = (CompoundTag) t;
            double[] v = new double[FundRules.KINDS];
            for (int k = 0; k < FundRules.KINDS; k++) {
                double c = x.getDouble(PL_KEYS[k]);
                v[k] = Double.isFinite(c) ? Math.max(0, c) : 0;
            }
            if (fund.pl.size() < 3) fund.pl.put(x.getLong("Week"), v);
        }
        fund.weightWeek = f.contains("WeightWeek") ? f.getLong("WeightWeek") : Long.MIN_VALUE;
        fund.weightDays = Math.max(0, Math.min(366, f.getInt("WeightDays")));
        for (Tag t : f.getList("Weeks", Tag.TAG_COMPOUND)) {
            CompoundTag x = (CompoundTag) t;
            if (fund.history.size() >= FundRules.HISTORY) break;
            fund.history.addLast(new FundRules.Week(x.getLong("Week"), x.getLong("Profit"), x.getLong("Loss"), x.getLong("Invested"),
                x.getLong("StockCost"), x.getInt("Share"), x.getLong("Dividends"), x.getLong("Losses"), x.getLong("Kept")));
        }
    }

    private static UUID uuid(String s) {
        try { return s == null || s.isEmpty() ? null : UUID.fromString(s); } catch (IllegalArgumentException e) { return null; }
    }
}
