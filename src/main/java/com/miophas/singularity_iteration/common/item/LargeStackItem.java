// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.item;

/**
 * 0.1.10.16: an item of this mod that may stack above the vanilla container limit of 99 (the coin:
 * 9,999). Only SI items implement it, so the larger stacks never apply to the items of vanilla or
 * other mods. See {@link LargeStacks}.
 */
public interface LargeStackItem {
    /** The largest stack of this item in a container or slot that uses the vanilla default limit. */
    int largeStackLimit();
}
