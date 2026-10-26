// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.exchange;

import com.miophas.singularity_iteration.core.api.exchange.BountyCompletedEvent;
import com.miophas.singularity_iteration.core.api.exchange.BountyInfo;
import com.miophas.singularity_iteration.core.api.exchange.ExchangeTradeEvent;
import com.miophas.singularity_iteration.core.api.exchange.MarketDayEvent;
import java.util.ArrayDeque;
import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.common.NeoForge;

/**
 * 0.1.10.26 (X3): the exchange events of the public API. Trades and completed bounties are queued while an action runs and
 * posted by {@link #flush} after it (the action may hold an inventory plan that a listener must not change); the market
 * day event is posted at the end of the day's work. Server thread only.
 */
public final class ExchangeEvents {
    private ExchangeEvents() {}

    record Trade(String item, long day, long price, long quantity, String currency, String seller, String buyer) { }

    static final int MAX_QUEUED = 4096;
    private static final ArrayDeque<Trade> TRADES = new ArrayDeque<>();
    private static final ArrayDeque<BountyInfo> COMPLETED = new ArrayDeque<>();
    private static boolean flushing;

    static void trade(String item, long day, long price, long quantity, ExchangeData.Currency currency, String seller, String buyer) {
        if (quantity <= 0) return;
        if (TRADES.size() >= MAX_QUEUED) TRADES.pollFirst();
        TRADES.addLast(new Trade(item, day, price, quantity, currency == ExchangeData.Currency.UU ? "uu" : "coin", seller, buyer));
    }

    static void completed(BountyInfo bounty) {
        if (COMPLETED.size() >= MAX_QUEUED) COMPLETED.pollFirst();
        COMPLETED.addLast(bounty);
    }

    /** Posts the queued events (a listener that trades again queues more; they are posted in the same flush). */
    public static void flush(MinecraftServer server) {
        if (flushing || server == null) return;
        flushing = true;
        try {
            for (int guard = 0; guard < 16 && (!TRADES.isEmpty() || !COMPLETED.isEmpty()); guard++) {
                while (!TRADES.isEmpty()) {
                    Trade t = TRADES.pollFirst();
                    NeoForge.EVENT_BUS.post(new ExchangeTradeEvent(server, t.item(), t.day(), t.price(), t.quantity(), t.currency(), t.seller(), t.buyer()));
                }
                while (!COMPLETED.isEmpty()) NeoForge.EVENT_BUS.post(new BountyCompletedEvent(server, COMPLETED.pollFirst()));
            }
        } finally {
            flushing = false;
        }
    }

    static void marketDay(MinecraftServer server, long day) {
        flush(server);
        NeoForge.EVENT_BUS.post(new MarketDayEvent(server, day));
        flush(server);
    }
}
