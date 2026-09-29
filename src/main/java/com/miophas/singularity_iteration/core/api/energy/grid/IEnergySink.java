// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.api.energy.grid;

import net.minecraft.core.Direction;

/** SI receivers return the unaccepted remainder of an offered transfer. */
public interface IEnergySink extends IEnergyAcceptor {
    double getDemandedEnergy();
    int getSinkTier();
    double injectEnergy(Direction direction, double amount, double voltage);
}
