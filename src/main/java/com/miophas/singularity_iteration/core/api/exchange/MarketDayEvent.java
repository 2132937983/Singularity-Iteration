// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.api.exchange;

import net.minecraft.server.MinecraftServer;
import net.neoforged.bus.api.Event;

/**
 * <p><b>EN:</b> The market day of the SI exchange changed (public API since 0.1.10.26), posted on {@code NeoForge.EVENT_BUS}
 * on the server thread after the day's work: the daily auction is cleared, orders and bounties expired, futures settled,
 * the day's bounties posted. Not cancelable.
 * <p><b>ZH:</b> SI 交易所的交易日已更替（0.1.10.26 起的公共 API），在当天的处理（集中清算、订单与悬赏到期、期货结算、发布当天的悬赏）之后，
 * 在服务器线程发布到 {@code NeoForge.EVENT_BUS}。不可取消。
 * <p><b>JA:</b> SI 取引所の取引日が変わった（0.1.10.26 以降の公開 API）。その日の処理（一括清算、注文と懸賞の期限切れ、先物の決済、その日の懸賞の掲示）
 * の後に、サーバースレッドで {@code NeoForge.EVENT_BUS} に投稿する。キャンセル不可。
 */
public final class MarketDayEvent extends Event {
    private final MinecraftServer server;
    private final long day;

    public MarketDayEvent(MinecraftServer server, long day) {
        this.server = server;
        this.day = day;
    }

    /** EN: The server. ZH: 服务器。JA: サーバー。 */
    public MinecraftServer getServer() { return server; }

    /** EN: The new market day (the panel shows day + 1). ZH: 新的交易日（界面显示为日 + 1）。JA: 新しい取引日（画面では日 + 1）。 */
    public long getDay() { return day; }
}
