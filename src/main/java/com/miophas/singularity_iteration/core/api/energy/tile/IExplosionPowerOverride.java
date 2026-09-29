// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.api.energy.tile;

/** SI storage and cable callbacks; the implementing block owns both decisions. */
public interface IExplosionPowerOverride {
    boolean shouldExplode();
    float getExplosionPower(int tier, float basePower);
}
