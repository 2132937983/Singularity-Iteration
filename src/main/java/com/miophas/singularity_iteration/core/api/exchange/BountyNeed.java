// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.api.exchange;

/**
 * <p><b>EN:</b> One item a bounty asks for (public API since 0.1.10.26): the item id, the count and the value of one item in
 * coins. A value of 0 means the market's ask when the bounty is posted (commodities), or 1 coin for other items. The value
 * weighs a delivery for proportional payment, the contribution split and the per-player share.
 * <p><b>ZH:</b> 悬赏要求的一种物品（0.1.10.26 起的公共 API）：物品 ID、数量与单个物品的货币价值。价值为 0 表示发布时市场的卖价（商品），
 * 其他物品为 1 货币。价值决定一次交付在按比例支付、按贡献分配与每名玩家份额中的权重。
 * <p><b>JA:</b> 懸賞が求める品物 1 種（0.1.10.26 以降の公開 API）：アイテム ID、数量、1 個あたりの硬貨の価値。価値 0 は、掲示時の市場の
 * 売値（市場品目）、その他のアイテムでは 1 コインを意味する。価値は、比例払い・貢献分配・1 人あたりの割合で納品の重みになる。
 */
public record BountyNeed(String item, long count, long value) {
    public BountyNeed {
        if (item == null || item.isEmpty()) throw new IllegalArgumentException("item");
        if (count < 1) throw new IllegalArgumentException("count must be at least 1");
        if (value < 0) throw new IllegalArgumentException("value must be 0 or more");
    }
}
