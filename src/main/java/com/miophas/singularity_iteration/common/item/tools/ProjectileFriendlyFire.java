// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.item.tools;

import com.miophas.singularity_iteration.common.armory.WingmanEntity;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

/**
 * Which entities a weapon projectile passes through instead of hitting, 0.1.11.193 (T0, W-13).
 *
 * <p>Vanilla only spares the shooter until the projectile has left it. A Wingman's bolt flying past its
 * owner hit the owner; now a projectile passes through the shooter's allies (same team), and a Wingman's
 * projectile also passes through its owner and the owner's other Wingmen.
 */
final class ProjectileFriendlyFire {
    private ProjectileFriendlyFire() {}

    static boolean spares(@Nullable Entity shooter, Entity target) {
        if (shooter == null) return false;
        if (target == shooter || shooter.isAlliedTo(target)) return true;
        if (shooter instanceof WingmanEntity wingman && wingman.owner() != null) {
            if (wingman.owner().equals(target.getUUID())) return true;
            return target instanceof WingmanEntity other && wingman.owner().equals(other.owner());
        }
        return false;
    }
}
