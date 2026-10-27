// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.exchange;

import com.miophas.singularity_iteration.common.exchange.ExchangeService.Market;
import com.miophas.singularity_iteration.common.exchange.TerminalRules.Mode;
import com.miophas.singularity_iteration.common.exchange.TerminalRules.Order;
import com.miophas.singularity_iteration.common.exchange.TerminalRules.Side;
import com.miophas.singularity_iteration.common.exchange.TerminalRules.Status;
import com.miophas.singularity_iteration.common.future.FutureCommodity;
import com.miophas.singularity_iteration.common.future.FutureCommodityManager;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * 0.1.10.27 (X2): one run of the standing orders of a Trade Terminal (every {@link TerminalRules#RUN_TICKS} ticks). Every
 * trade is the owner's: a SELL INSTANT order is {@link ExchangeService#sellForAccount} (book, sell depth, allowance, ledger
 * and pressure of the owner's UUID), a SELL AUCTION order joins the owner's daily auction order, a BUY order is
 * {@link ExchangeService#buyForAccount} (book, treasury and import, the owner's daily buy limit, paid from the owner's
 * mailbox coins). The coins of sales go to the owner's mailbox. While the owner is offline only AUCTION orders run, and not
 * for commodities that need an unlock advancement (proposal Q3). One energy fee per order that trades.
 */
public final class TerminalOrders {
    private TerminalOrders() {}

    /** Log kinds. */
    public static final int LOG_SOLD = 0, LOG_AUCTION = 1, LOG_BOUGHT = 2, LOG_SCRAP = 3;

    /** The energy fee of one trade, reserved before it and kept only when it trades. */
    public interface Fee {
        void commit();

        void cancel();
    }

    /** The terminal as the orders see it. */
    public interface Host {
        UUID ownerId();

        String ownerName();

        Container sellBuffer();

        Container outputBuffer();

        boolean outputMode();

        /** Null when the terminal lacks the energy of a trade. */
        Fee reserveFee();

        void log(long day, int kind, String item, long quantity, long coins);

        /** A stack that fits nowhere (owner's mailbox when plain, else dropped at the terminal). */
        void overflow(ItemStack stack);
    }

    /** Runs all orders once; returns the status of each. */
    public static List<Status> run(MinecraftServer server, Host host, List<Order> orders) {
        List<Status> out = new ArrayList<>(orders.size());
        UUID owner = host.ownerId();
        if (owner == null) {
            for (int i = 0; i < orders.size(); i++) out.add(Status.OFFLINE);
            return out;
        }
        ServerPlayer online = server.getPlayerList().getPlayer(owner);
        String team = ExchangeService.team(server, host.ownerName());
        long day = ExchangeService.today(server);
        for (Order o : orders) {
            Status s;
            try {
                s = runOne(server, host, o, online, owner, team, day);
            } catch (ArithmeticException e) {
                s = Status.NO_COINS;
            }
            out.add(s);
        }
        ExchangeEvents.flush(server);
        return out;
    }

    static Status runOne(MinecraftServer server, Host host, Order o, ServerPlayer online, UUID owner, String team, long day) {
        if (!o.enabled()) return Status.DISABLED;
        FutureCommodity c = FutureCommodityManager.getCommodityById(o.item());
        Market m = c == null ? null : ExchangeService.market(server, o.item());
        if (m == null || !m.system() || m.currency() != ExchangeData.Currency.COIN) return Status.NO_MARKET;
        if (online != null && !FutureCommodityManager.isUnlocked(online, c)) return Status.LOCKED;
        boolean lockedOffline = online == null && c.getUnlockAdvancement() != null;
        ExchangeData d = ExchangeService.data(server);
        if (o.side() == Side.SELL) {
            long inBuffer = count(host.sellBuffer(), o.item());
            boolean full = full(host.sellBuffer());
            Status s = TerminalRules.sellStatus(o, inBuffer, online != null, lockedOffline, full);
            if (s != Status.OK) return s;
            long offer = Math.min(TerminalRules.sellOffer(o, inBuffer, online != null, lockedOffline, full), ExchangeService.MAX_QUANTITY);
            if (offer <= 0) return Status.NOTHING;
            Fee fee = host.reserveFee();
            if (fee == null) return Status.NO_ENERGY;
            boolean traded = false;
            try {
                List<ItemStack> taken = take(host.sellBuffer(), o.item(), offer);
                long got = 0;
                for (ItemStack t : taken) got += t.getCount();
                if (got <= 0) return Status.NOTHING;
                if (o.mode() == Mode.AUCTION) {
                    ExchangeService.addAuction(d, owner, host.ownerName(), o.item(), got, (int) Math.min(100, o.limit()), day);
                    d.setDirty();
                    host.log(day, LOG_AUCTION, o.item(), got, 0);
                    traded = true;
                    return Status.OK;
                }
                long[] r = ExchangeService.sellForAccount(server, owner, team, o.item(), limitOf(o, m), (int) got);
                long[] scrapped = new long[2];
                if (got > r[0] && o.mode() == Mode.SCRAP && ScrapSink.eligible(d, m, owner, day)) {
                    // 0.1.10.28 (X4): the unsold to the scrap sink; the products to the output buffer or the owner's mailbox
                    long rest = got - r[0];
                    List<ItemStack> protos = new ArrayList<>();
                    long[] counts = new long[taken.size()];
                    for (int i = taken.size() - 1, k = 0; i >= 0; i--, k++) {
                        protos.add(taken.get(i).copyWithCount(1));
                        counts[k] = Math.min(rest, taken.get(i).getCount());
                        rest -= counts[k];
                    }
                    long[] stay = ScrapSink.recycle(server, d, protos, counts, day, p -> product(host, d, owner, p), scrapped);
                    for (int k = 0; k < protos.size(); k++) {
                        long back = Settlement.add(host.sellBuffer(), protos.get(k), stay[k]);
                        for (ItemStack over : ScrapSink.stacks(protos.get(k), back)) host.overflow(over);
                    }
                    host.sellBuffer().setChanged();
                    if (scrapped[0] > 0) host.log(day, LOG_SCRAP, o.item(), scrapped[0], scrapped[1]);
                } else if (got > r[0]) putBack(host, taken, got - r[0]);
                if (r[0] <= 0 && scrapped[0] > 0) { traded = true; return Status.OK; }
                if (r[0] <= 0) return Status.PRICE;
                host.log(day, LOG_SOLD, o.item(), r[0], r[1]);
                traded = true;
                return Status.OK;
            } finally {
                if (traded) fee.commit(); else fee.cancel();
            }
        }
        // BUY
        ExchangeData.Mailbox box = d.mail.get(owner);
        long coins = box == null ? 0 : box.coins;
        long stock = host.outputMode() ? count(host.outputBuffer(), o.item()) : box == null ? 0 : box.items.getOrDefault(o.item(), 0L);
        Status s = TerminalRules.buyStatus(o, stock, coins, online != null);
        if (s != Status.OK) return s;
        long need = Math.min(TerminalRules.buyNeed(o, stock, coins, online != null), ExchangeService.MAX_QUANTITY);
        if (need <= 0) return Status.NO_COINS;
        Fee fee = host.reserveFee();
        if (fee == null) return Status.NO_ENERGY;
        boolean traded = false;
        try {
            long[] r = ExchangeService.buyForAccount(server, owner, team, o.item(), o.limit(), (int) need);
            if (r[0] <= 0) return Status.PRICE;
            Item item = ExchangeService.item(o.item());
            long rest = r[0];
            if (host.outputMode() && item != null) rest = Settlement.add(host.outputBuffer(), new ItemStack(item), rest);
            if (rest > 0) ExchangeService.addItems(d.mailbox(owner), o.item(), rest);
            host.outputBuffer().setChanged();
            d.setDirty();
            host.log(day, LOG_BOUGHT, o.item(), r[0], r[1]);
            traded = true;
            return Status.OK;
        } finally {
            if (traded) fee.commit(); else fee.cancel();
        }
    }

    /** 0.1.10.29: the lowest price per item of an instant sale order now: its coins, or its percent of the bid without pressure. */
    public static long limitOf(Order o, Market m) {
        return o.percent() ? MinPrice.limit(ExchangeService.curve(m).unit(), true, o.limit()) : Math.max(1, o.limit());
    }

    /** 0.1.10.28: a product of the scrap sink: the output buffer (output mode), else the owner's mailbox; what fits nowhere overflows. */
    static void product(Host host, ExchangeData d, UUID owner, ItemStack p) {
        long rest = host.outputMode() ? Settlement.add(host.outputBuffer(), p, p.getCount()) : p.getCount();
        if (rest <= 0) { host.outputBuffer().setChanged(); return; }
        ItemStack left = p.copyWithCount((int) rest);
        if (left.isComponentsPatchEmpty()) ExchangeService.addItems(d.mailbox(owner), ExchangeService.id(left.getItem()), rest);
        else host.overflow(left);
    }

    /** True when {@code c} has no empty slot. */
    static boolean full(Container c) {
        for (int i = 0; i < c.getContainerSize(); i++) if (c.getItem(i).isEmpty()) return false;
        return true;
    }

    /** Items in {@code c} that sell as commodity {@code id} (its item, its tag, components it accepts). */
    public static long count(Container c, String id) {
        long n = 0;
        for (int i = 0; i < c.getContainerSize(); i++) {
            ItemStack s = c.getItem(i);
            if (!s.isEmpty() && id.equals(ExchangeService.sellableAs(s))) n += s.getCount();
        }
        return n;
    }

    /** Takes {@code amount} items that sell as {@code id} out of {@code c}; returns the stacks taken. */
    static List<ItemStack> take(Container c, String id, long amount) {
        List<ItemStack> out = new ArrayList<>();
        for (int i = 0; i < c.getContainerSize() && amount > 0; i++) {
            ItemStack s = c.getItem(i);
            if (s.isEmpty() || !id.equals(ExchangeService.sellableAs(s))) continue;
            int n = (int) Math.min(amount, s.getCount());
            out.add(s.copyWithCount(n));
            s.shrink(n);
            if (s.isEmpty()) c.setItem(i, ItemStack.EMPTY);
            amount -= n;
        }
        c.setChanged();
        return out;
    }

    /** Puts {@code amount} of the taken stacks back (the last first); what does not fit overflows. */
    static void putBack(Host host, List<ItemStack> taken, long amount) {
        for (int i = taken.size() - 1; i >= 0 && amount > 0; i--) {
            ItemStack t = taken.get(i);
            int n = (int) Math.min(amount, t.getCount());
            amount -= n;
            long rest = Settlement.add(host.sellBuffer(), t.copyWithCount(1), n);
            while (rest > 0) {
                int k = (int) Math.min(rest, t.getMaxStackSize());
                host.overflow(t.copyWithCount(k));
                rest -= k;
            }
        }
        host.sellBuffer().setChanged();
    }
}
