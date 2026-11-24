// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.item.tools;

import java.util.function.Predicate;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Shared rules for the self-acting carried devices (nutrition supply, first-aid life support), 0.1.11.193 (T0, U-01).
 *
 * <p>Before, each copy acted every tick from any inventory slot, so the first-aid unit healed 20 health a
 * second and several copies stacked. Now only one copy of a kind acts for a player (the first switched-on,
 * charged one: main hand, off hand, then inventory order), and it acts on a fixed interval.
 */
public final class PassiveDeviceRules {
    private PassiveDeviceRules() {}

    /** Ticks between two health points from the first-aid life support (1 health a second). */
    public static final int FIRST_AID_INTERVAL = 20;
    /** Ticks between two food points from the nutrition supply. */
    public static final int NUTRITION_INTERVAL = 10;

    /** Is it this device's turn: the interval has come round for {@code player}? */
    public static boolean onInterval(Player player, int interval) {
        return player.tickCount % interval == 0;
    }

    /**
     * Is {@code stack} the copy that acts for {@code player}? The first stack of the same item that passes
     * {@code active} wins; stacks are compared by identity.
     */
    public static boolean isActingCopy(Player player, ItemStack stack, Predicate<ItemStack> active) {
        Inventory inv = player.getInventory();
        ItemStack first = firstActive(inv.getSelected(), stack, active);
        if (first == null) first = firstActive(player.getOffhandItem(), stack, active);
        for (int i = 0; first == null && i < inv.items.size(); i++) first = firstActive(inv.items.get(i), stack, active);
        return first == stack;
    }

    private static ItemStack firstActive(ItemStack candidate, ItemStack self, Predicate<ItemStack> active) {
        return candidate.is(self.getItem()) && active.test(candidate) ? candidate : null;
    }
}
