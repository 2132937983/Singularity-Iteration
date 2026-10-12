// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.api.suit;

import net.minecraft.world.item.ItemStack;

/**
 * An addon item that is a quantum suit upgrade unit (0.1.8.0, public API). The quantum
 * modification station accepts it when its {@link SuitModuleSpec} is registered in
 * {@link SuitModuleRegistry}.
 */
public interface ISuitModuleItem {
    /** The unit that this item installs. */
    SuitModuleSpec suitModule(ItemStack stack);
}
