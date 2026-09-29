// SPDX-License-Identifier: Apache-2.0
// R133 independent SI public declaration; intentionally no added parent interface.
package com.miophas.singularity_iteration.core.api.item.electric;

import net.minecraft.world.item.ItemStack;

public interface ISpecialElectricItem {
    IElectricItemManager getManager(ItemStack stack);
}
