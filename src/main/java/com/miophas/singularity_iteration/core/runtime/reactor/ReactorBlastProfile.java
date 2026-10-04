// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.runtime.reactor;

/** Pure reactor accident scaling. The legacy SI radius is 8 + 4 times active fuel cells.
 * One power unit maps to two blocks of maximum radius; multiplier applies before the power cap.
 * Thermal reactor behavior and the two IC2 plating factors remain independent of this radius policy.
 */
public record ReactorBlastProfile(int fuelCells, int powerMillis, float heatEffectModifier) {
    public static ReactorBlastProfile calculate(ReactorCycle.Part[] parts, double multiplier, int powerLimit) {
        if (!Double.isFinite(multiplier) || multiplier < 0 || multiplier > 16 || powerLimit < 0 || powerLimit > 1000)
            throw new IllegalArgumentException("Invalid reactor explosion configuration");
        int cells = 0;
        double containment = 1;
        for (var part : parts) {
            if (part == null) continue;
            var profile = part.profile();
            if (profile.kind() == ReactorCycle.Kind.FUEL && part.remaining() > 0)
                cells = Math.addExact(cells, profile.cells());
            else if (profile.kind() == ReactorCycle.Kind.PLATING)
                containment *= (100 - profile.explosionReduction()) / 100.0;
        }
        if (multiplier == 0 || powerLimit == 0) return new ReactorBlastProfile(cells, 0, (float) containment);
        double power = (4 + 2.0 * cells) * multiplier * containment * containment;
        int millis = (int) Math.round(Math.min(powerLimit, Math.max(1, power)) * 1000);
        return new ReactorBlastProfile(cells, millis, (float) containment);
    }
    public boolean disabled() { return powerMillis == 0; }
    public float power() { return powerMillis / 1000F; }
    public int radius() { return disabled() ? 0 : Math.min(2000, (int) Math.ceil(power() * 2)); }
}
