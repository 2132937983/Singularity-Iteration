// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.runtime.energy.engine;

import com.miophas.singularity_iteration.core.api.util.BoundedUnits;

/** Integer consumable-to-energy accounting. Unemitted energy stays in the persisted buffer. */
public final class ConsumableGeneration {
    private ConsumableGeneration() { }
    public record Step(long fuelConsumed, long fuelRemaining, long bufferedEnergy,
                       long ambientGenerated, long fuelGenerated) {
        public long generated() { return ambientGenerated + fuelGenerated; }
    }
    /** Convert SI's existing reserved-unit counter and emitted partial unit into remaining credit. */
    public static long legacyCredit(long reservedUnits, long emittedPartial, long perUnit, long maximumUnits) {
        if (perUnit <= 0 || maximumUnits < 0 || maximumUnits > Long.MAX_VALUE / perUnit)
            throw new IllegalArgumentException("Invalid legacy fuel bounds");
        long units = BoundedUnits.clamp(reservedUnits, maximumUnits);
        long emitted = BoundedUnits.clamp(emittedPartial, perUnit - 1);
        return Math.max(0, units * perUnit - emitted);
    }
    public static Step plan(long fuel, long buffered, long perUnit, long rate, long room, long ambient) {
        // A migrated legacy generator may already have reserved an entire bucket's worth of energy.
        // Spend that credit before consuming fresh fuel; do not truncate it to one unit's remainder.
        if (fuel < 0 || perUnit <= 0 || buffered < 0 || rate < 0 || room < 0 || ambient < 0)
            throw new IllegalArgumentException("Invalid consumable generation state");
        long freeOutput = Math.min(room, ambient);
        long wanted = Math.min(room - freeOutput, rate);
        long fromBuffer = Math.min(buffered, wanted);
        long missing = wanted - fromBuffer;
        if (missing == 0) return new Step(0, fuel, buffered - fromBuffer, freeOutput, fromBuffer);
        long needed = (missing - 1) / perUnit + 1;
        long consumed = Math.min(fuel, needed);
        // Avoid multiplication overflow when the final consumed unit exceeds the requested output.
        long fromFuel = consumed == needed ? missing : consumed * perUnit;
        long remainder = consumed == needed && missing % perUnit != 0 ? perUnit - missing % perUnit : 0;
        return new Step(consumed, fuel - consumed, remainder, freeOutput, fromBuffer + fromFuel);
    }
}
