package com.miophas.singularity_iteration.common.item.tools;

import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.Tiers;

/**
 * Iron drill: an iron pickaxe and shovel on electric power (IC2: 30,000 EU, 100 EU/t, 50 EU a block, speed 8).
 * 0.1.11.196 (tool overhaul T3): on {@link AbstractDrill}; drops like an iron tool, 1×1 only, attack 6 (an iron sword)
 * that costs a use and needs energy.
 */
public class mio_icif_iron_driller extends AbstractDrill {
    public static final int IRON_DRILLER_MAX_ENERGY = 30000;
    public static final int IRON_DRILLER_TRANSFER_LIMIT = 100;
    public static final int IRON_DRILLER_ENERGY_PER_USE = 50;
    /** Added to the wielder's base damage: a player hits for 6, an iron sword. It was 8 (9) and cost nothing. */
    public static final float IRON_DRILLER_ATTACK_DAMAGE = 5.0F;
    public static final float IRON_DRILLER_EFFICIENCY = 8.0F;

    public mio_icif_iron_driller(Properties properties) {
        super(properties, IRON_DRILLER_MAX_ENERGY, IRON_DRILLER_TRANSFER_LIMIT, IRON_DRILLER_ENERGY_PER_USE, 1,
            BlockTags.INCORRECT_FOR_IRON_TOOL, IRON_DRILLER_EFFICIENCY, IRON_DRILLER_ATTACK_DAMAGE, 0, Tiers.IRON.getEnchantmentValue());
    }
}
