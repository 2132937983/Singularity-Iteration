// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.exchange;

import com.miophas.singularity_iteration.common.exchange.TerminalRules.Order;
import com.miophas.singularity_iteration.common.exchange.TerminalRules.Status;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * 0.1.10.42: one run of a Trade Box ({@link TradeBoxRules}; every {@link TerminalRules#RUN_TICKS} ticks): the advanced box
 * first moves what its filter does not take to the return slots and delivers to the bounties its owner claimed
 * ({@link Bounties#deliverClaimed}); then every commodity in the box is one sell order of {@link TerminalOrders} (the
 * Trade Terminal's trades: the owner's account, sell depth, allowance and unlocks). No energy.
 */
public final class TradeBoxes {
    private TradeBoxes() {}

    /** The box as a run sees it ({@link TerminalOrders.Host}: sell buffer = the box, output buffer = the return slots). */
    public interface Box extends TerminalOrders.Host {
        int tier();

        TradeBoxRules.Settings settings();

        /** The filter: item ids ("" = empty cell). */
        List<String> filter();

        /** Adds to a counter of the market day ({@link TradeBoxRules#SOLD} ..). */
        void count(int counter, long amount);
    }

    /** A fee of nothing (a box needs no energy). */
    public static final TerminalOrders.Fee FREE = new TerminalOrders.Fee() {
        @Override public void commit() { }

        @Override public void cancel() { }
    };

    /** Runs the box once; the status for the panel ({@link TradeBoxRules#summary}). */
    public static Status run(MinecraftServer server, Box box) {
        if (box.ownerId() == null) return Status.OFFLINE;
        boolean advanced = box.tier() == TradeBoxRules.ADVANCED;
        TradeBoxRules.Settings s = TradeBoxRules.of(box.tier(), box.settings());
        if (advanced) returnRejected(box);
        if (advanced && s.autoDeliver()) {
            long[] r = Bounties.deliverClaimed(server, box.ownerId(), box.ownerName(), source(box.sellBuffer()));
            if (r[0] > 0) {
                box.count(TradeBoxRules.DELIVERED, r[0]);
                box.count(TradeBoxRules.DELIVERED_COINS, r[1]);
                box.sellBuffer().setChanged();
            }
            ExchangeEvents.flush(server);
        }
        boolean online = server.getPlayerList().getPlayer(box.ownerId()) != null;
        int mode = TradeBoxRules.runMode(box.tier(), s, online);
        List<Order> orders = new ArrayList<>();
        for (String id : commodities(box.sellBuffer())) orders.add(TradeBoxRules.order(id, mode, s));
        if (orders.isEmpty()) return Status.NOTHING;
        return TradeBoxRules.summary(TerminalOrders.run(server, box, orders));
    }

    /** The commodities of the items in {@code c}, in slot order. */
    public static List<String> commodities(Container c) {
        Set<String> out = new LinkedHashSet<>();
        for (int i = 0; i < c.getContainerSize(); i++) {
            ItemStack st = c.getItem(i);
            if (st.isEmpty()) continue;
            String id = ExchangeService.sellableAs(st);
            if (id != null) out.add(id);
        }
        return new ArrayList<>(out);
    }

    /** The commodities of the filter items (an item that is no commodity counts as itself). */
    public static Set<String> filterCommodities(List<String> filter) {
        Set<String> out = new LinkedHashSet<>();
        for (String f : filter) {
            if (f == null || f.isEmpty()) continue;
            ResourceLocation rl = ResourceLocation.tryParse(f);
            Item item = rl == null ? null : BuiltInRegistries.ITEM.getOptional(rl).orElse(null);
            String c = item == null ? null : ExchangeService.sellableAs(new ItemStack(item));
            out.add(c != null ? c : f);
        }
        return out;
    }

    /** The box takes {@code stack}: a commodity its filter accepts (the basic box: every commodity). */
    public static boolean accepts(int tier, List<String> filter, ItemStack stack) {
        String c = ExchangeService.sellableAs(stack);
        if (c == null) return false;
        return tier != TradeBoxRules.ADVANCED || TradeBoxRules.accepts(filterCommodities(filter), c);
    }

    /** Moves the items the filter does not take (or no commodity) to the return slots; what does not fit stays. */
    static void returnRejected(Box box) {
        Container sell = box.sellBuffer(), back = box.outputBuffer();
        Set<String> allowed = filterCommodities(box.filter());
        boolean changed = false;
        for (int i = 0; i < sell.getContainerSize(); i++) {
            ItemStack st = sell.getItem(i);
            if (st.isEmpty() || TradeBoxRules.accepts(allowed, ExchangeService.sellableAs(st))) continue;
            long rest = Settlement.add(back, st.copyWithCount(1), st.getCount());
            if (rest == st.getCount()) continue;
            if (rest <= 0) sell.setItem(i, ItemStack.EMPTY); else st.setCount((int) rest);
            changed = true;
        }
        if (changed) { sell.setChanged(); back.setChanged(); }
    }

    /** The plain items of {@code c} as the source of a bounty delivery (the requested item first, then the items of its tags). */
    public static ItemSource source(Container c) {
        return new ItemSource() {
            @Override
            public long count(Item item) {
                long n = 0;
                for (int i = 0; i < c.getContainerSize(); i++) {
                    ItemStack s = c.getItem(i);
                    if (!s.isEmpty() && s.is(item) && InventoryPlan.plain(s, item)) n += s.getCount();
                }
                return n;
            }

            @Override
            public boolean take(Item item, long amount) {
                if (amount <= 0) return amount == 0;
                if (count(item) < amount) return false;
                for (int i = 0; i < c.getContainerSize() && amount > 0; i++) {
                    ItemStack s = c.getItem(i);
                    if (s.isEmpty() || !s.is(item) || !InventoryPlan.plain(s, item)) continue;
                    int n = (int) Math.min(amount, s.getCount());
                    s.shrink(n);
                    if (s.isEmpty()) c.setItem(i, ItemStack.EMPTY);
                    amount -= n;
                }
                c.setChanged();
                return true;
            }

            @Override
            public long count(ItemMatch match) {
                long n = 0;
                for (int i = 0; i < c.getContainerSize(); i++) {
                    ItemStack s = c.getItem(i);
                    if (match.test(s)) n += s.getCount();
                }
                return n;
            }

            @Override
            public boolean take(ItemMatch match, long amount) {
                if (amount <= 0) return amount == 0;
                if (count(match) < amount) return false;
                for (int pass = 0; pass < 2 && amount > 0; pass++) {
                    for (int i = 0; i < c.getContainerSize() && amount > 0; i++) {
                        ItemStack s = c.getItem(i);
                        if (!match.test(s) || match.exact(s) != (pass == 0)) continue;
                        int n = (int) Math.min(amount, s.getCount());
                        s.shrink(n);
                        if (s.isEmpty()) c.setItem(i, ItemStack.EMPTY);
                        amount -= n;
                    }
                }
                c.setChanged();
                return true;
            }
        };
    }
}
