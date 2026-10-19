// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.suit.nano;

/**
 * 0.1.10.8: a HUD value that eases toward the last synced value. The server sends the reserve, the
 * heat and the coherence when they changed by 0.5 or more (or once a second), so the bars moved in
 * small steps. A small change now glides over a few client ticks (interpolated within the frame); a
 * jump of more than {@link #SNAP} of the scale (a spent action, a refill) is shown at once.
 */
final class HudLerp {
    /** Share of the scale above which a change is shown at once. */
    static final float SNAP = 0.15F;
    /** Share of the remaining distance covered per client tick. */
    static final float RATE = 0.45F;

    private float prev = Float.NaN, now = Float.NaN;

    /** Once per client tick: move toward {@code target}; {@code scale} is the full value of the bar. */
    void tick(float target, float scale) {
        if (Float.isNaN(now) || Math.abs(target - now) > Math.max(1F, scale) * SNAP) {
            prev = now = target;
            return;
        }
        prev = now;
        now += (target - now) * RATE;
        if (Math.abs(target - now) < 0.02F) now = target;
    }

    /** The value to draw in this frame, or {@code fallback} before the first tick. */
    float get(float partial, float fallback) {
        return Float.isNaN(now) ? fallback : prev + (now - prev) * partial;
    }

    void reset() {
        prev = now = Float.NaN;
    }
}
