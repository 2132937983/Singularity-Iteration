// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.api.exchange;

import net.minecraft.server.MinecraftServer;
import net.neoforged.bus.api.Event;

/**
 * <p><b>EN:</b> A system bounty was completed and paid (public API since 0.1.10.26), on {@code NeoForge.EVENT_BUS}, on the
 * server thread, after the delivery action. {@link BountyInfo#contribution} holds the value each player delivered (for a
 * quest reward or a statistic). Not cancelable. Expired bounties post no event.
 * <p><b>ZH:</b> 系统悬赏已完成并支付（0.1.10.26 起的公共 API），在交付操作之后于服务器线程发布到 {@code NeoForge.EVENT_BUS}。
 * {@link BountyInfo#contribution} 为每名玩家交付的价值（可用于任务奖励或统计）。不可取消。到期的悬赏不发布事件。
 * <p><b>JA:</b> システム懸賞が完了し、支払われた（0.1.10.26 以降の公開 API）。納品の操作の後、サーバースレッドで {@code NeoForge.EVENT_BUS} に投稿する。
 * {@link BountyInfo#contribution} は各プレイヤーが納品した価値（クエストの報酬や統計に使える）。キャンセル不可。期限切れの懸賞はイベントを出さない。
 */
public final class BountyCompletedEvent extends Event {
    private final MinecraftServer server;
    private final BountyInfo bounty;

    public BountyCompletedEvent(MinecraftServer server, BountyInfo bounty) {
        this.server = server;
        this.bounty = bounty;
    }

    /** EN: The server. ZH: 服务器。JA: サーバー。 */
    public MinecraftServer getServer() { return server; }
    /** EN: The completed bounty. ZH: 已完成的悬赏。JA: 完了した懸賞。 */
    public BountyInfo getBounty() { return bounty; }
}
