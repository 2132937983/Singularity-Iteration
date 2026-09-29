// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.api.energy;

/** Cable metadata contract reconstructed from the admitted SI CableTier implementation. */
public interface ICableTier {
    long getPowerRating();
    int getTier();
    String getName();
    String getDisplayName();
    String getFullName();
    float getElectricDamage();
    long getConductorBreakdownEnergy();
    long getInsulationBreakdownEnergy();
}
