// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.api.suit;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * What an addon unit does (0.1.8.0, public API). Server side, main thread.
 */
@FunctionalInterface
public interface SuitModuleBehavior {
    /** A behavior that does nothing (a unit that other code only queries with {@link SuitModuleRegistry#isActive}). */
    SuitModuleBehavior NONE = (player, piece) -> { };

    /**
     * Called once per server tick for each player on whom the unit is active (installed, switched
     * on, powered, visor present when needed).
     *
     * @param player the wearer
     * @param piece  the worn suit piece that carries the unit (do not replace it; change its data only)
     */
    void activeTick(Player player, ItemStack piece);
}
