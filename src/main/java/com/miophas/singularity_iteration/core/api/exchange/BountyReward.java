// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.api.exchange;

/**
 * <p><b>EN:</b> One reward line of a bounty (public API since 0.1.10.26): what it pays ({@link Kind}), where it comes from
 * ({@link Source}), the item id (items only, else "") and the amount (coins, items, or mB of UU-matter). Coins are always
 * MINTED and UU-matter is always ISSUE.
 * <p><b>ZH:</b> 悬赏的一项奖励（0.1.10.26 起的公共 API）：支付什么（{@link Kind}）、来自何处（{@link Source}）、物品 ID（仅物品，其余为 ""）
 * 与数量（货币、物品个数或 UU 物质的 mB）。货币总是 MINTED，UU 物质总是 ISSUE。
 * <p><b>JA:</b> 懸賞の報酬 1 行（0.1.10.26 以降の公開 API）：何を払うか（{@link Kind}）、出どころ（{@link Source}）、アイテム ID（アイテムのみ、
 * それ以外は ""）、量（硬貨、個数、または UU 物質の mB）。硬貨は常に MINTED、UU 物質は常に ISSUE。
 */
public record BountyReward(Kind kind, Source source, String item, long amount) {
    /**
     * <p><b>EN:</b> COINS, ITEM or UU (mB of UU-matter to the exchange account).
     * <p><b>ZH:</b> COINS（货币）、ITEM（物品）或 UU（存入交易所账户的 UU 物质 mB）。
     * <p><b>JA:</b> COINS（硬貨）、ITEM（アイテム）、UU（取引所口座に入る UU 物質 mB）。
     */
    public enum Kind { COINS, ITEM, UU }

    /**
     * <p><b>EN:</b> MINTED: new coins at payment. TREASURY: the market's stock, reserved when the bounty is posted. VAULT:
     * rare items that earlier bounties received, reserved when posted. ISSUE: created at payment, counted against the tier's
     * weekly cap when posted.
     * <p><b>ZH:</b> MINTED：支付时发行的新货币。TREASURY：市场的国库库存，发布时预留。VAULT：此前悬赏收到的稀有物品（宝库），发布时预留。
     * ISSUE：支付时创建，发布时计入该等级的每周上限。
     * <p><b>JA:</b> MINTED：支払い時に発行される新しい硬貨。TREASURY：市場の国庫在庫（掲示時に確保）。VAULT：以前の懸賞が受け取った希少品
     * （宝物庫、掲示時に確保）。ISSUE：支払い時に生成し、掲示時にその段階の週上限に数える。
     */
    public enum Source { MINTED, TREASURY, VAULT, ISSUE }

    public BountyReward {
        if (kind == null || source == null) throw new IllegalArgumentException("kind and source");
        if (item == null) item = "";
        if (amount < 1) throw new IllegalArgumentException("amount must be at least 1");
        if (kind == Kind.COINS && source != Source.MINTED) throw new IllegalArgumentException("coins are MINTED");
        if (kind == Kind.UU && source != Source.ISSUE) throw new IllegalArgumentException("UU-matter is ISSUE");
        if (kind == Kind.ITEM && (item.isEmpty() || source == Source.MINTED)) throw new IllegalArgumentException("an item reward needs an item and a source");
    }

    /**
     * <p><b>EN:</b> Coins, minted at payment.
     * <p><b>ZH:</b> 货币，支付时发行。
     * <p><b>JA:</b> 硬貨。支払い時に発行する。
     */
    public static BountyReward coins(long amount) { return new BountyReward(Kind.COINS, Source.MINTED, "", amount); }

    /**
     * <p><b>EN:</b> Items from a source.
     * <p><b>ZH:</b> 来自某来源的物品。
     * <p><b>JA:</b> 出どころを指定したアイテム。
     */
    public static BountyReward item(String item, long count, Source source) { return new BountyReward(Kind.ITEM, source, item, count); }

    /**
     * <p><b>EN:</b> UU-matter in mB, issued at payment.
     * <p><b>ZH:</b> UU 物质（mB），支付时发放。
     * <p><b>JA:</b> UU 物質（mB）。支払い時に発行する。
     */
    public static BountyReward uu(long mb) { return new BountyReward(Kind.UU, Source.ISSUE, "", mb); }
}
