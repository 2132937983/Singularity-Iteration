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
}