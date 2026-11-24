// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.item.tools;

import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.Tiers;

/**
 * Diamond drill: drops like a diamond tool, speed 16, 80 EU a block. 0.1.11.196 (tool overhaul T3): on
 * {@link AbstractDrill}; areas 1×1 and 3×3 (G), attack 7 (a diamond sword) that costs a use and needs energy.
 */
public class mio_icif_diamond_driller extends AbstractDrill {
    public static final int DIAMOND_DRILLER_MAX_ENERGY = 30_000;
    /** Added to the wielder's base damage: a player hits for 7, a diamond sword. */
    public static final float DIAMOND_DRILLER_ATTACK_DAMAGE = 6.0F;
    public static final int DIAMOND_DRILLER_TRANSFER_LIMIT = 100;
    public static final int DIAMOND_DRILLER_ENERGY_PER_USE = 80;
    public static final float DIAMOND_DRILLER_EFFICIENCY = 16.0F;

    public mio_icif_diamond_driller(Properties properties) {
        super(properties, DIAMOND_DRILLER_MAX_ENERGY, DIAMOND_DRILLER_TRANSFER_LIMIT, DIAMOND_DRILLER_ENERGY_PER_USE, 1,
            BlockTags.INCORRECT_FOR_DIAMOND_TOOL, DIAMOND_DRILLER_EFFICIENCY, DIAMOND_DRILLER_ATTACK_DAMAGE, 1, Tiers.DIAMOND.getEnchantmentValue());
    }
}
