// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.builder;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * 0.1.11.187: a block entity the Blueprint Builder's drones take building material from and bring the rest back to: the
 * {@link MaterialInterfaceBlockEntity Armory Material Interface} and the
 * {@link InfiniteMaterialInterfaceBlockEntity Infinite Armory Material Interface}. The builder uses those within
 * {@link BlueprintBuilderBlockEntity#INTERFACE_RANGE} blocks, nearest first.
 */
public interface MaterialSource {
    /** How many of {@code item} the drones can take here. */
    long count(Item item);

    /** Whether at least one of {@code item} is available here (allows early exit without counting all slots). */
    default boolean hasAny(Item item) { return count(item) > 0; }

    /** Takes up to {@code max} of {@code item} (empty when there is none). */
    ItemStack extract(Item item, int max);

    /** Takes back {@code stack}; returns what did not fit. */
    ItemStack insert(ItemStack stack);
}
