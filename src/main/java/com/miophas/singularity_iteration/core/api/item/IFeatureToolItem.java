// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.api.item;

import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * An electric tool or weapon with switchable traits. They are listed, toggled and stored exactly
 * like armor traits ({@link com.miophas.singularity_iteration.core.prefab.item.ArmorFeatures},
 * equipment console, tooltip); declare them with {@code EquipmentSlot.MAINHAND}. Every trait is
 * enabled until the player switches it off.
 */
public interface IFeatureToolItem {
    List<ArmorFeatureInfo> getFeatures(ItemStack stack);
}
