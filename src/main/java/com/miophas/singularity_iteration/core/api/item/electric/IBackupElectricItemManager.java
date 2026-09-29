// SPDX-License-Identifier: Apache-2.0
// R133 independent SI public declaration; no predecessor body was read.
package com.miophas.singularity_iteration.core.api.item.electric;

import net.minecraft.world.item.ItemStack;

public interface IBackupElectricItemManager extends IElectricItemManager {
    boolean handles(ItemStack stack);
}
