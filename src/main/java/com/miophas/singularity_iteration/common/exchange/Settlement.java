// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.exchange;

import com.miophas.singularity_iteration.common.exchange.ExchangeData.Currency;
import com.miophas.singularity_iteration.common.exchange.ExchangeService.Market;
import com.miophas.singularity_iteration.common.exchange.ExchangeService.Result;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * 0.1.10.27 (X2): the bulk actions of a settlement slot (proposal B5). The container's items are treated like the items of
 * the sell box or the inventory, under the same rules and as the player who uses the slot:
 * <ul>
 *   <li>sell now (sell depth and allowance, as the sell box) or into the daily auction, per commodity (unlock, plain items,
 *       a commodity's tag and {@code accept_components} as in {@link ExchangeService#sellable});</li>
 *   <li>deliver to a system bounty or a player commission (plain items);</li>
 *   <li>UU-matter of a fluid container or an AE2 fluid cell onto the account;</li>
 *   <li>buy into the container (a manual purchase now; what does not fit goes to the inventory, then the mailbox).</li>
 * </ul>
 * Items come out of the container before they are sold; what the market does not take goes back into it (and to the
 * player when it no longer fits), so an action never creates or loses items.
 */
public final class Settlement {
    private Settlement() {}

    /** The prototypes of the container per commodity they sell as (for {@code player}). */
    static Map<String, List<SettlementStorage.Entry>> byCommodity(Player player, SettlementStorage st) {
        Map<String, List<SettlementStorage.Entry>> out = new LinkedHashMap<>();
        for (SettlementStorage.Entry e : st.items()) {
            String id = ExchangeService.sellable(player, e.proto());
            if (id != null) out.computeIfAbsent(id, k -> new ArrayList<>()).add(e);
        }
        return out;
    }

    /** Items per commodity that the container would sell. */
    public static Map<String, Long> contents(Player player, SettlementStorage st) {
        Map<String, Long> out = new LinkedHashMap<>();
        byCommodity(player, st).forEach((id, list) -> {
            long n = 0;
            for (SettlementStorage.Entry e : list) n += e.count();
            out.put(id, n);
        });
        return out;
    }

    /** Items of the container that sell as no commodity. */
    public static long otherItems(Player player, SettlementStorage st) {
        long n = 0;
        for (SettlementStorage.Entry e : st.items()) if (ExchangeService.sellable(player, e.proto()) == null) n += e.count();
        return n;
    }

    private record Taken(ItemStack proto, long count) { }

    /** Takes up to {@code amount} items of the entries out of the container. */
    private static List<Taken> take(SettlementStorage st, List<SettlementStorage.Entry> entries, long amount) {
        List<Taken> out = new ArrayList<>();
        long left = amount;
        for (SettlementStorage.Entry e : entries) {
            if (left <= 0) break;
            long got = st.extract(e.proto(), Math.min(left, e.count()), false);
            if (got > 0) { out.add(new Taken(e.proto(), got)); left -= got; }
        }
        return out;
    }

    /** Puts {@code amount} of the taken items back (the last taken first); what does not fit goes to the player. */
    private static void putBack(ServerPlayer player, SettlementStorage st, List<Taken> taken, long amount) {
        for (int i = taken.size() - 1; i >= 0 && amount > 0; i--) {
            Taken t = taken.get(i);
            long n = Math.min(amount, t.count());
            amount -= n;
            long rest = n - st.insert(t.proto(), n, false);
            give(player, t.proto(), rest);
        }
    }

    /** Stacks of {@code proto} to the player's inventory; the rest to the mailbox (plain) or dropped. */
    static void give(ServerPlayer player, ItemStack proto, long amount) {
        while (amount > 0) {
            int n = (int) Math.min(amount, Math.max(1, proto.getMaxStackSize()));
            ItemStack s = proto.copyWithCount(n);
            amount -= n;
            if (!player.getInventory().add(s) && !s.isEmpty()) ExchangeService.toMailbox(player, s);
        }
        player.getInventory().setChanged();
    }

    /** Sells the container now or into the daily auction ({@code minPercent} as for the sell box). */
    public static Result sell(ServerPlayer player, SettlementStorage st, boolean toAuction, int minPercent) {
        return sell(player, st, toAuction, minPercent, false);
    }

    /** 0.1.10.28 (X4): {@link #sell} with the scrap sink for what an instant sale leaves ({@link ScrapSink}). */
    public static Result sell(ServerPlayer player, SettlementStorage st, boolean toAuction, int minPercent, boolean scrap) {
        MinecraftServer server = player.server;
        ExchangeData d = ExchangeService.data(server);
        UUID uuid = player.getUUID();
        long day = ExchangeService.today(server);
        Map<String, List<SettlementStorage.Entry>> groups = byCommodity(player, st);
        if (groups.isEmpty()) return Result.fail("settle_nothing");
        if (toAuction && d.auction.size() + groups.size() > ExchangeService.MAX_AUCTION_ORDERS) return Result.fail("auction_full");
        long items = 0, coins = 0;
        long[] scrapped = new long[2];
        for (Map.Entry<String, List<SettlementStorage.Entry>> g : groups.entrySet()) {
            Market m = ExchangeService.market(server, g.getKey());
            if (m == null || !m.system()) continue;
            long want = 0;
            for (SettlementStorage.Entry e : g.getValue()) want += e.count();
            List<Taken> taken = take(st, g.getValue(), want);
            long got = 0;
            for (Taken t : taken) got += t.count();
            if (got <= 0) continue;
            long sold;
            if (toAuction) {
                ExchangeService.addAuction(d, uuid, player.getGameProfile().getName(), m.id(), got, Math.max(0, Math.min(100, minPercent)), day);
                sold = got;
            } else {
                long[] r = ExchangeService.sellToMarket(server, d, uuid, m, got, 1, day);
                sold = r[0];
                coins += r[1];
            }
            if (got > sold && scrap && !toAuction && ScrapSink.eligible(d, m, uuid, day)) {
                // 0.1.10.28: the unsold (the last taken first) to the recycler's rate; what stays goes back
                long rest = got - sold;
                List<ItemStack> protos = new ArrayList<>();
                long[] counts = new long[taken.size()];
                for (int i = taken.size() - 1, k = 0; i >= 0; i--, k++) {
                    Taken t = taken.get(i);
                    protos.add(t.proto());
                    counts[k] = Math.min(rest, t.count());
                    rest -= counts[k];
                }
                long[] stay = ScrapSink.recycle(server, d, protos, counts, day, p -> give(player, p, p.getCount()), scrapped);
                for (int k = 0; k < protos.size(); k++) {
                    if (stay[k] <= 0) continue;
                    long back = stay[k] - st.insert(protos.get(k), stay[k], false);
                    give(player, protos.get(k), back);
                }
            } else if (got > sold) putBack(player, st, taken, got - sold);
            items += sold;
        }
        st.commit();
        if (coins > 0) {
            InventoryPlan plan = new InventoryPlan(player.getInventory());
            ExchangeService.pay(plan, d, uuid, Currency.COIN, coins);
            plan.commit();
        }
        d.setDirty();
        if (scrapped[0] > 0) return Result.done("box_sold_scrap", items, coins, scrapped[0], scrapped[1]);
        if (items == 0) return Result.fail(toAuction ? "settle_nothing" : "box_nothing_sold");
        return toAuction ? Result.done("auction_submitted", items) : Result.done("box_sold", items, coins);
    }

    /** Delivers the container's plain items to a system bounty. */
    public static Result deliverBounty(ServerPlayer player, SettlementStorage st, long id) {
        Result r = Bounties.deliver(player, id, st.source());
        st.commit();
        return r;
    }

    /** Delivers the container's plain items to a player commission. */
    public static Result deliverCommission(ServerPlayer player, SettlementStorage st, long id, int quantity) {
        Result r = ExchangeService.deliver(player, id, quantity, st.source());
        st.commit();
        return r;
    }

    /** UU-matter of the container onto the account. */
    public static Result depositUu(ServerPlayer player, SettlementStorage st) {
        long have = st.uu();
        if (have <= 0) return Result.fail("no_uu");
        long mb = st.extractUu(have, false);
        st.commit();
        if (mb <= 0) return Result.fail("no_uu");
        ExchangeData d = ExchangeService.data(player.server);
        ExchangeService.credit(d, player.getUUID(), Currency.UU, mb);
        d.setDirty();
        return Result.done("uu_deposited", mb);
    }

    /** Buys now into the container (a manual purchase: inventory coins, limit per item, the player's daily limit). */
    public static Result buyInto(ServerPlayer player, SettlementStorage st, String id, long limit, int quantity) {
        if (st.kind() == SettlementStorages.FLUID) return Result.fail("settle_no_items");
        return ExchangeService.place(player, id, true, false, limit, quantity, st);
    }

    // ------------------------------------------------------------------ Trade Terminal: between the container and the buffers

    /** Moves items of the container that {@code accepts} into {@code buffer}; returns the items moved. */
    public static long load(SettlementStorage st, Container buffer, Predicate<ItemStack> accepts) {
        long moved = 0;
        for (SettlementStorage.Entry e : st.items()) {
            if (!accepts.test(e.proto())) continue;
            long left = e.count();
            while (left > 0) {
                int room = room(buffer, e.proto());
                if (room <= 0) break;
                long got = st.extract(e.proto(), Math.min(left, room), false);
                if (got <= 0) break;
                long rest = add(buffer, e.proto(), got);
                if (rest > 0) st.insert(e.proto(), rest, false);   // cannot happen after room(); nothing is lost
                moved += got - rest;
                left -= got;
                if (rest > 0) break;
            }
        }
        st.commit();
        buffer.setChanged();
        return moved;
    }

    /** Moves the stacks of {@code from} into the container as far as they fit; returns the items moved. */
    public static long unload(SettlementStorage st, Container from) {
        long moved = 0;
        for (int i = 0; i < from.getContainerSize(); i++) {
            ItemStack s = from.getItem(i);
            if (s.isEmpty()) continue;
            long in = st.insert(s.copyWithCount(1), s.getCount(), false);
            if (in <= 0) continue;
            s.shrink((int) in);
            if (s.isEmpty()) from.setItem(i, ItemStack.EMPTY);
            moved += in;
        }
        st.commit();
        from.setChanged();
        return moved;
    }

    /** Items of {@code proto} that fit into {@code c}. */
    static int room(Container c, ItemStack proto) {
        long room = 0;
        int max = Math.min(c.getMaxStackSize(), proto.getMaxStackSize());
        for (int i = 0; i < c.getContainerSize(); i++) {
            ItemStack s = c.getItem(i);
            if (s.isEmpty()) room += max;
            else if (ItemStack.isSameItemSameComponents(s, proto)) room += Math.max(0, max - s.getCount());
        }
        return (int) Math.min(Integer.MAX_VALUE, room);
    }

    /** Adds {@code amount} of {@code proto} to {@code c}; returns what did not fit. */
    static long add(Container c, ItemStack proto, long amount) {
        int max = Math.min(c.getMaxStackSize(), proto.getMaxStackSize());
        for (int pass = 0; pass < 2 && amount > 0; pass++) {
            for (int i = 0; i < c.getContainerSize() && amount > 0; i++) {
                ItemStack s = c.getItem(i);
                if (pass == 0 && !s.isEmpty() && ItemStack.isSameItemSameComponents(s, proto) && s.getCount() < max) {
                    int n = (int) Math.min(amount, max - s.getCount());
                    s.grow(n);
                    amount -= n;
                } else if (pass == 1 && s.isEmpty()) {
                    int n = (int) Math.min(amount, max);
                    c.setItem(i, proto.copyWithCount(n));
                    amount -= n;
                }
            }
        }
        return amount;
    }

    /** Plain items of {@code item} in the container (for the delivery targets of the view). */
    static long plain(SettlementStorage st, Item item) { return st.plain(item); }
}
