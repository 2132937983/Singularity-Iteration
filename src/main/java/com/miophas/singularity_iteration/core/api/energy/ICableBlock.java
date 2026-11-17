// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.api.energy;

import net.minecraft.world.level.block.Block;

/**
 * API interface for cable/wire blocks that expose their voltage tier.
 * Addon mods should implement this interface on their wire blocks
 * to participate in the SI energy net's fuse/overload system.
 */
public interface ICableBlock {
    ICableTier getCableTier();

    /**
     * 0.1.11.31: what is left when the energy net melts this cable in {@code state} (the cable itself is lost): for
     * example the scaffold or the set wall it was laid in. Nothing by default.
     */
    default java.util.List<net.minecraft.world.item.ItemStack> meltDrops(net.minecraft.world.level.block.state.BlockState state) {
        return java.util.List.of();
    }
}