// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.item;

/**
 * 0.1.10.16: an SI container that keeps a stack up to {@code Container#getMaxStackSize(ItemStack)}, so
 * a {@link LargeStackItem} may stack above 99 in it (see {@link LargeStacks}).
 *
 * <p>Only a container whose {@code setItem} cuts a stack to the per-stack limit (or not at all) may
 * implement it. A container that cuts to the plain {@code getMaxStackSize()} would delete the part of
 * a large stack above 99, so it keeps the vanilla limit for every item.
 */
public interface LargeStackContainer {
}
