// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.api.exchange;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * <p><b>EN:</b> A snapshot of an open (or just completed) system bounty (public API since 0.1.10.26). {@code table} is the
 * datapack table id, or "" for a generated NORMAL request or a bounty of the Java API. {@code delivered} and {@code paid}
 * follow the order of {@code needs} and {@code rewards}. {@code contribution}: the value each player delivered, in coins.
 * Market days count from the start of the world (the panel shows day + 1).
 * <p><b>ZH:</b> 未结束（或刚完成）的系统悬赏的快照（0.1.10.26 起的公共 API）。{@code table} 为数据包表 ID，自动生成的 NORMAL 委托或 Java API
 * 发布的悬赏为 ""。{@code delivered} 与 {@code paid} 分别对应 {@code needs} 与 {@code rewards} 的顺序。{@code contribution}：每名玩家交付的价值（货币）。
 * 交易日从世界开始计数（界面显示为日 + 1）。
 * <p><b>JA:</b> 開いている（または完了したばかりの）システム懸賞のスナップショット（0.1.10.26 以降の公開 API）。{@code table} はデータパックの表の
 * ID。自動生成の NORMAL の依頼や Java API の懸賞では ""。{@code delivered} と {@code paid} は {@code needs} と {@code rewards} の順に並ぶ。
 * {@code contribution}：各プレイヤーが納品した価値（硬貨）。取引日はワールドの開始から数える（画面では日 + 1 と表示）。
 */
public record BountyInfo(long id, BountyTier tier, String table, long day, long expiry, BountySplit split, List<BountyNeed> needs,
                         List<Long> delivered, List<BountyReward> rewards, List<Long> paid, Map<UUID, Long> contribution) {
    public BountyInfo {
        needs = List.copyOf(needs);
        delivered = List.copyOf(delivered);
        rewards = List.copyOf(rewards);
        paid = List.copyOf(paid);
        contribution = Map.copyOf(contribution);
    }
}
