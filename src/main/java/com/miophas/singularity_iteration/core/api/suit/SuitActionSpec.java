// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.api.suit;

/**
 * <p><b>EN:</b> Costs of a quantum action of an addon unit (public API since 0.1.10.0). An action
 * uses quantum coherence (0..100, more with a coherence capacitor, refilled from EU). When the
 * coherence reaches 0 the suit decoheres: actions lock for a few seconds. The wearer assigns up to
 * two actions to the action keys (V and B by default; the same keys as the nano bursts).
 * <p><b>ZH:</b> 附属单元量子行动的消耗（0.1.10.0 起的公共 API）。行动消耗量子相干度（0..100，装有相干电容器时更多，由 EU 回充）。
 * 相干度降到 0 时套装退相干：行动锁定数秒。穿戴者最多可把两个行动分配给行动键（默认 V 与 B，与纳米爆发共用）。
 * <p><b>JA:</b> アドオンユニットの量子アクションのコスト（0.1.10.0 以降の公開 API）。アクションは量子コヒーレンス（0..100、
 * コヒーレンスキャパシタで増える、EU で回復）を使う。コヒーレンスが 0 になるとスーツはデコヒーレンスし、数秒間アクションがロックされる。
 * 着用者は最大 2 つのアクションをアクションキー（初期値 V と B、ナノバーストと共通）に割り当てる。
 *
 * @param coherenceCost      coherence per use (a sustained action: when it starts)
 * @param cooldownTicks      ticks before the next use (a sustained action: after it ends)
 * @param sustained          true for a toggle action that drains coherence while it runs
 * @param sustainPerSecond   coherence per second while a sustained action runs (0 for a single action)
 */
public record SuitActionSpec(int coherenceCost, int cooldownTicks, boolean sustained, int sustainPerSecond) {
    public SuitActionSpec {
        if (coherenceCost < 0 || coherenceCost > 100) throw new IllegalArgumentException("coherence cost out of 0..100: " + coherenceCost);
        if (cooldownTicks < 0) throw new IllegalArgumentException("negative cooldown");
        if (sustainPerSecond < 0 || sustainPerSecond > 100) throw new IllegalArgumentException("sustain cost out of 0..100: " + sustainPerSecond);
        if (!sustained && sustainPerSecond != 0) throw new IllegalArgumentException("a single action has no sustain cost");
    }

    /** EN: A single action. ZH: 单次行动。JA: 単発アクション。 */
    public static SuitActionSpec single(int coherenceCost, int cooldownTicks) {
        return new SuitActionSpec(coherenceCost, cooldownTicks, false, 0);
    }

    /** EN: A toggle action. ZH: 持续（开关）行动。JA: 持続（トグル）アクション。 */
    public static SuitActionSpec sustained(int startCost, int perSecond, int cooldownTicks) {
        return new SuitActionSpec(startCost, cooldownTicks, true, perSecond);
    }
}
