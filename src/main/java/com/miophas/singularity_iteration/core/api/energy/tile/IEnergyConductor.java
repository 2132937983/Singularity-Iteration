// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.api.energy.tile;

import com.miophas.singularity_iteration.core.api.energy.ICableTier;
import com.miophas.singularity_iteration.core.api.energy.storage.CableTier;

/** SI conductor view, with the low-voltage default used by admitted machine builders. */
public interface IEnergyConductor extends com.miophas.singularity_iteration.core.api.energy.grid.IEnergyConductor {
    ICableTier LV_TIER = CableTier.LV;
}
