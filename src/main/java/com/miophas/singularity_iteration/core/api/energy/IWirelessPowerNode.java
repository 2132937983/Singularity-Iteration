// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.api.energy;

import net.minecraft.core.BlockPos;

/** Configuration surface of the admitted SI wireless power node and its manager item. */
public interface IWirelessPowerNode {
    BlockPos getTargetPosition();
    void setTargetPosition(BlockPos target);
    boolean isTransmitting();
}
