// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.api.upgrade.tile;

/**
 * <p><b>EN:</b> The answer of an {@link IWorkCycleUpgrade} at the start of a work cycle (public API since 0.1.11.2).
 * {@link #proceed()} starts the cycle without a change. {@link #hold()} does not start the cycle in this tick: the
 * machine uses no energy, does not add progress and asks again in the next tick, the same as a machine that waits for
 * energy. {@link #proceed(float, float)} starts the cycle with a duration factor and an energy factor. Each factor is
 * clamped to {@link #MIN_FACTOR}..{@link #MAX_FACTOR}; a value that is not a finite number becomes 1.
 * <p><b>ZH:</b> {@link IWorkCycleUpgrade} 在工作周期开始时的答复（0.1.11.2 起的公共 API）。{@link #proceed()} 照常开工。
 * {@link #hold()} 本刻不开工：机器不耗电、不推进进度，下一刻再问，表现与等电的机器相同。{@link #proceed(float, float)}
 * 以耗时系数与耗电系数开工；每个系数钳制在 {@link #MIN_FACTOR}..{@link #MAX_FACTOR}，非有限数视为 1。
 * <p><b>JA:</b> 作業サイクル開始時の {@link IWorkCycleUpgrade} の答え（0.1.11.2 以降の公開 API）。{@link #proceed()} はそのまま開始する。
 * {@link #hold()} はこの tick では開始しない：機械は電力を使わず、進捗も進めず、次の tick に再び問う（電力待ちと同じ）。
 * {@link #proceed(float, float)} は所要時間係数と電力係数を付けて開始する。各係数は {@link #MIN_FACTOR}..{@link #MAX_FACTOR}
 * に制限され、有限数でない値は 1 になる。
 */
public final class CycleDecision {
    /** EN: Smallest factor. ZH: 系数下限。JA: 係数の下限。 */
    public static final float MIN_FACTOR = 0.25F;
    /** EN: Largest factor. ZH: 系数上限。JA: 係数の上限。 */
    public static final float MAX_FACTOR = 4.0F;

    private static final CycleDecision PROCEED = new CycleDecision(false, 1.0F, 1.0F);
    private static final CycleDecision HOLD = new CycleDecision(true, 1.0F, 1.0F);

    private final boolean hold;
    private final float durationFactor;
    private final float energyFactor;

    private CycleDecision(boolean hold, float durationFactor, float energyFactor) {
        this.hold = hold;
        this.durationFactor = durationFactor;
        this.energyFactor = energyFactor;
    }

    /** EN: Start the cycle without a change. ZH: 照常开工。JA: そのまま開始する。 */
    public static CycleDecision proceed() { return PROCEED; }

    /** EN: Do not start the cycle in this tick. ZH: 本刻不开工。JA: この tick では開始しない。 */
    public static CycleDecision hold() { return HOLD; }

    /**
     * EN: Start the cycle. {@code durationFactor} multiplies the ticks of this cycle (2 = half speed);
     * {@code energyFactor} multiplies the EU per tick of this cycle. Both are clamped.
     * ZH: 开工。{@code durationFactor} 乘本周期的刻数（2 = 半速）；{@code energyFactor} 乘本周期每刻的 EU。两者都钳制。
     * JA: 開始する。{@code durationFactor} はこのサイクルの tick 数に掛かる（2 = 半速）。{@code energyFactor} はこのサイクルの
     * tick あたり EU に掛かる。どちらも制限される。
     */
    public static CycleDecision proceed(float durationFactor, float energyFactor) {
        float d = clamp(durationFactor), e = clamp(energyFactor);
        return d == 1.0F && e == 1.0F ? PROCEED : new CycleDecision(false, d, e);
    }

    /** EN: The factor clamped to MIN_FACTOR..MAX_FACTOR (not finite: 1). ZH: 钳制后的系数（非有限数为 1）。JA: 制限後の係数（有限数でなければ 1）。 */
    public static float clamp(float factor) {
        if (!Float.isFinite(factor)) return 1.0F;
        return Math.max(MIN_FACTOR, Math.min(MAX_FACTOR, factor));
    }

    /** EN: True for {@link #hold()}. ZH: {@link #hold()} 时为真。JA: {@link #hold()} なら真。 */
    public boolean isHold() { return hold; }

    /** EN: Duration factor (1 for proceed() and hold()). ZH: 耗时系数（proceed() 与 hold() 为 1）。JA: 所要時間係数（proceed() と hold() は 1）。 */
    public float durationFactor() { return durationFactor; }

    /** EN: Energy factor (1 for proceed() and hold()). ZH: 耗电系数（proceed() 与 hold() 为 1）。JA: 電力係数（proceed() と hold() は 1）。 */
    public float energyFactor() { return energyFactor; }

    @Override
    public String toString() {
        return hold ? "CycleDecision[hold]" : "CycleDecision[proceed x" + durationFactor + " time, x" + energyFactor + " energy]";
    }
}
