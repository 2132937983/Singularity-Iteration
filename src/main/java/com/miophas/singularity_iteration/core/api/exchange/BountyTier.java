// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.api.exchange;

/**
 * <p><b>EN:</b> Tier of a system bounty of the SI exchange (public API since 0.1.10.26). NORMAL: everyday requests for
 * commodities the market lacks (3 days, reward 1.15 x the value). ADVANCED: sets of goods from the datapack tables, a few a
 * week (7 days, reward 1.3 x). In multiplayer one player may deliver at most half of it until the last day. SPECIAL: rare items,
 * by chance, rewards from the table's pool (rare equipment, items of the vault).
 * <p><b>ZH:</b> SI 交易所系统悬赏的等级（0.1.10.26 起的公共 API）。NORMAL：市场缺少的商品的日常委托（3 天，奖励为价值的 1.15 倍）。
 * ADVANCED：来自数据包表的成套物资，每周数件（7 天，奖励 1.3 倍；多人游戏中最后一天之前一名玩家最多交付一半）。SPECIAL：稀有物品，
 * 随机出现，奖励由表的奖励池决定（稀有设备、宝库中的物品）。
 * <p><b>JA:</b> SI 取引所のシステム懸賞の段階（0.1.10.26 以降の公開 API）。NORMAL：市場に足りない品目の日常の依頼（3 日、報酬は価値の
 * 1.15 倍）。ADVANCED：データパックの表から出る品物のセット、週に数件（7 日、1.3 倍。マルチプレイでは最終日まで 1 人が納品できるのは
 * 半分まで）。SPECIAL：希少品、確率で出現し、報酬は表の報酬プール（豪華な設備、宝物庫の品）。
 */
public enum BountyTier {
    NORMAL, ADVANCED, SPECIAL
}
