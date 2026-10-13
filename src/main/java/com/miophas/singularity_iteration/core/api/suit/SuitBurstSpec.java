// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.api.suit;

/**
 * <p><b>EN:</b> Costs of a burst ability of a nano unit (0.1.8.5). A burst uses the nano reserve
 * (0..100, refilled from EU) and adds heat (an overheated suit cannot burst until it cools down).
 * The wearer assigns up to two bursts to the burst keys (V and B by default).
 * <p><b>ZH:</b> 纳米单元爆发能力的消耗（0.1.8.5）。爆发消耗纳米储备（0..100，由 EU 回充）并增加热量（过热的套装在冷却前不能爆发）。
 * 穿戴者最多可把两个爆发分配给爆发键（默认 V 与 B）。
 * <p><b>JA:</b> ナノユニットのバースト能力のコスト（0.1.8.5）。バーストはナノ予備量（0..100、EU で回復）を使い、熱を加える
 * （過熱したスーツは冷えるまでバーストできない）。着用者は最大 2 つのバーストをバーストキー（初期値 V と B）に割り当てる。
 *
 * @param reserveCost   reserve points per use (a sustained burst: per second)
 * @param heat          heat added per use
 * @param cooldownTicks ticks before the next use
 * @param sustained     true for a toggle burst that drains the reserve while it runs
 */
public record SuitBurstSpec(int reserveCost, int heat, int cooldownTicks, boolean sustained) {
    public SuitBurstSpec {
        if (reserveCost < 0 || reserveCost > 100) throw new IllegalArgumentException("reserve cost out of 0..100: " + reserveCost);
        if (heat < 0 || heat > 200) throw new IllegalArgumentException("heat out of 0..200: " + heat);
        if (cooldownTicks < 0) throw new IllegalArgumentException("negative cooldown");
    }
}
