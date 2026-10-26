// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.api.exchange;

import net.minecraft.server.MinecraftServer;
import net.neoforged.bus.api.Event;

/**
 * <p><b>EN:</b> A trade of the SI exchange happened (public API since 0.1.10.26): a purchase from or a sale to the market,
 * a fill between two players' orders, or a sale at the daily auction. Posted on {@code NeoForge.EVENT_BUS} on the server
 * thread after the action is complete (never in the middle of it). Not cancelable: a notification for quests and
 * statistics. {@code seller} and {@code buyer} are player UUIDs or "system" (the market).
 * <p><b>ZH:</b> SI 交易所发生了一笔成交（0.1.10.26 起的公共 API）：从市场买入或卖给市场、两名玩家订单之间的成交，或集中清算中的出售。
 * 在操作完成之后（绝不在中途）于服务器线程发布到 {@code NeoForge.EVENT_BUS}。不可取消：供任务与统计使用的通知。{@code seller} 与 {@code buyer}
 * 为玩家 UUID 或 "system"（市场）。
 * <p><b>JA:</b> SI 取引所で取引が成立した（0.1.10.26 以降の公開 API）：市場からの購入・市場への売却、2 人のプレイヤーの注文同士の約定、一括清算での売却。
 * 操作が終わった後（途中では決してない）に、サーバースレッドで {@code NeoForge.EVENT_BUS} に投稿する。キャンセル不可：クエストや統計のための通知。
 * {@code seller} と {@code buyer} はプレイヤーの UUID か "system"（市場）。
 */
public final class ExchangeTradeEvent extends Event {
    private final MinecraftServer server;
    private final String item, currency, seller, buyer;
    private final long day, price, quantity;

    public ExchangeTradeEvent(MinecraftServer server, String item, long day, long price, long quantity, String currency, String seller, String buyer) {
        this.server = server;
        this.item = item;
        this.day = day;
        this.price = price;
        this.quantity = quantity;
        this.currency = currency;
        this.seller = seller;
        this.buyer = buyer;
    }

    /** EN: The server. ZH: 服务器。JA: サーバー。 */
    public MinecraftServer getServer() { return server; }
    /** EN: The item id. ZH: 物品 ID。JA: アイテム ID。 */
    public String getItem() { return item; }
    /** EN: The market day. ZH: 交易日。JA: 取引日。 */
    public long getDay() { return day; }
    /** EN: Price of one item (average for a sale to the market). ZH: 单个物品的价格（卖给市场时为平均价）。JA: 1 個の価格（市場への売却では平均）。 */
    public long getPrice() { return price; }
    /** EN: Items traded. ZH: 成交数量。JA: 取引した個数。 */
    public long getQuantity() { return quantity; }
    /** EN: "coin", or "uu" (mB of UU-matter) for a UU listing. ZH: "coin"，UU 上市品为 "uu"（UU 物质 mB）。JA: "coin"、UU 上場品は "uu"（UU 物質 mB）。 */
    public String getCurrency() { return currency; }
    /** EN: The seller: a player UUID or "system". ZH: 卖方：玩家 UUID 或 "system"。JA: 売り手：プレイヤーの UUID か "system"。 */
    public String getSeller() { return seller; }
    /** EN: The buyer: a player UUID or "system". ZH: 买方：玩家 UUID 或 "system"。JA: 買い手：プレイヤーの UUID か "system"。 */
    public String getBuyer() { return buyer; }
}
