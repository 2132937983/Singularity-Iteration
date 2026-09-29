// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.runtime.uu;

import java.util.Map;

/** Processing machine families and their pricing policy, independent of any platform recipe system. */
public final class UuProcessingFamily {
    public enum Family { SMELTING, POWDER, COMPRESSOR, EXTRACTOR, ROLLING, CUTTING, EXTRUDING }

    /**
     * Each explicitly configured overhead is in buckets per operation, before dividing by output count.
     * An absent family is disabled except for the exact R93 sand-to-glass observation, if requested.
     * Callers must supply the provenance/configuration authority for any enabled family; this adapter
     * does not infer that one observed machine operation establishes every other machine's cost.
     */
    public record PricingPolicy(Map<Family, Double> explicitFamilyOverheads, boolean observedSandSmelting) {
        public PricingPolicy {
            explicitFamilyOverheads = Map.copyOf(explicitFamilyOverheads);
            for (double overhead : explicitFamilyOverheads.values()) {
                if (!Double.isFinite(overhead) || overhead <= 0 || overhead >= Long.MAX_VALUE / 1000.0)
                    throw new IllegalArgumentException("Invalid processing overhead");
            }
        }
        public static PricingPolicy observedOnly() { return new PricingPolicy(Map.of(), true); }
        public static PricingPolicy disabled() { return new PricingPolicy(Map.of(), false); }
    }

    private UuProcessingFamily() { }
}