// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.api.item;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * A ranged weapon item that tells the ballistic calculator how its projectile flies
 * (0.1.8.0, public API). The suit fire-control HUD, the Smart Trajectory Guidance module and the
 * Wingman drones use it. For an item of a different mod, register a provider in
 * {@link BallisticProfiles} instead.
 */
public interface IBallisticWeapon {
    /**
     * The launch profile of a shot fired now.
     *
     * @param stack    the weapon
     * @param shooter  the entity that holds it
     * @param useTicks ticks of use so far (draw or charge time; 0 when the item is not in use)
     * @return the profile, or null when the weapon cannot fire now
     */
    @Nullable
    BallisticProfile ballistics(ItemStack stack, LivingEntity shooter, int useTicks);
}
