// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.api.exchange;

import java.util.List;

/**
 * <p><b>EN:</b> A bounty to post through {@link ExchangeApi#postBounty(net.minecraft.server.MinecraftServer, BountySpec)}
 * (public API since 0.1.10.26). Empty {@code rewards}: they are drawn from the tier's reward pool as for a datapack table.
 * {@code days} of 0 and {@code playerShare} of 0 take the tier's settings. The rules of the market's own bounties apply.
 * Rewards from the treasury and the vault must be in stock. Issued items and UU-matter must fit the weekly caps. Lines
 * that cannot be paid are dropped.
 * <p><b>ZH:</b> 通过 {@link ExchangeApi#postBounty(net.minecraft.server.MinecraftServer, BountySpec)} 发布的悬赏（0.1.10.26 起的公共 API）。
 * {@code rewards} 为空时与数据包表一样从该等级的奖励池中抽取。{@code days} 与 {@code playerShare} 为 0 时使用该等级的设置。规则与市场自己的悬赏相同：
 * 来自国库与宝库的奖励必须有库存，发放的物品与 UU 物质必须在每周上限之内（无法支付的奖励项会被去掉）。
 * <p><b>JA:</b> {@link ExchangeApi#postBounty(net.minecraft.server.MinecraftServer, BountySpec)} で掲示する懸賞（0.1.10.26 以降の公開 API）。
 * {@code rewards} が空なら、データパックの表と同じくその段階の報酬プールから引く。{@code days} と {@code playerShare} が 0 なら段階の設定を使う。
 * 規則は市場自身の懸賞と同じ：国庫と宝物庫の報酬は在庫が必要、発行するアイテムと UU 物質は週上限の内側（払えない報酬行は外す）。
 */
public record BountySpec(BountyTier tier, List<BountyNeed> needs, List<BountyReward> rewards, int days, BountySplit split, double playerShare) {
    public BountySpec {
        if (tier == null || split == null) throw new IllegalArgumentException("tier and split");
        needs = List.copyOf(needs);
        rewards = List.copyOf(rewards);
        if (needs.isEmpty() || needs.size() > 8) throw new IllegalArgumentException("1..8 needs");
        if (rewards.size() > 8) throw new IllegalArgumentException("at most 8 rewards");
        if (days < 0 || days > 60) throw new IllegalArgumentException("days 0..60");
        if (!(playerShare >= 0 && playerShare <= 1)) throw new IllegalArgumentException("playerShare 0..1");
    }
}
