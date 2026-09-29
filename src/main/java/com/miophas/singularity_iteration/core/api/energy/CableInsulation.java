package com.miophas.singularity_iteration.core.api.energy;

import com.miophas.singularity_iteration.core.api.energy.storage.CableTier;

/** IC2 metal insulation layers; extended tiers keep their registered material policy. */
public final class CableInsulation {
    private CableInsulation() { }
    public static int maximumLayers(CableTier tier) {
        if (tier == CableTier.LV || tier == CableTier.MV) return 1;
        if (tier == CableTier.HV) return 2;
        if (tier == CableTier.EV) return 3;
        return 0;
    }
    public static double absorption(CableTier tier, int layers) {
        if (layers < 0 || layers > Math.max(1, maximumLayers(tier))) throw new IllegalArgumentException("Insulation layers");
        if (maximumLayers(tier) == 0)
            return layers > 0 ? tier.insulatedInsulationEnergyAbsorption : tier.insulationEnergyAbsorption;
        return (tier == CableTier.LV ? 8L : 32L) << (2 * layers);
    }
}
