// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.item;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 0.1.10.16: stacks above 99 for SI items only ({@link LargeStackItem}: the coin).
 *
 * <p>Up to 0.1.10.15 a global mixin made every container that used the vanilla limit of 99 report
 * 9,999, so any item of any mod whose {@code max_stack_size} was above 99 (set by a data pack, a command
 * or another mod) stacked that high in every container. Now only an SI large-stack item
 * gets more than 99, and only in a container or slot that uses the vanilla default limit (a container
 * with its own limit keeps it) and that keeps large stacks ({@link #keepsLargeStacks}: vanilla
 * containers and SI {@link LargeStackContainer}s). Every other item keeps the vanilla rules.
 *
 * <p>Two codec ranges stay widened to 9,999, for save and load only: the count of an item stack
 * ({@code ItemStackMaxStackMixin}: a coin stack in any container) and the {@code max_stack_size}
 * component ({@code DataComponentMaxStackMixin}: a stack saved with a value above 99 in a world of an
 * earlier version still loads). Neither makes a stack larger in a container or slot.
 */
public final class LargeStacks {
    private LargeStacks() { }

    /** The vanilla default limit of a container and a slot. */
    public static final int VANILLA_LIMIT = 99;

    /**
     * Whether {@code container} keeps a stack up to its per-stack limit: a vanilla container (no vanilla
     * {@code setItem} cuts to the plain limit) or an SI {@link LargeStackContainer}. Other containers
     * (other mods, SI machines) may cut a stack to the plain {@code getMaxStackSize()} of 99 in
     * {@code setItem}; a large stack there would lose everything above 99, so they keep the vanilla limit.
     */
    public static boolean keepsLargeStacks(Container container) {
        return container instanceof LargeStackContainer || container.getClass().getName().startsWith("net.minecraft.");
    }

    /** {@code Container#getMaxStackSize(ItemStack)}: {@code original} unless an SI large-stack item meets the default limit. */
    public static int containerLimit(Container container, ItemStack stack, int original) {
        if (!(stack.getItem() instanceof LargeStackItem large) || container.getMaxStackSize() != VANILLA_LIMIT
                || !keepsLargeStacks(container)) return original;
        return Math.max(original, Math.min(stack.getMaxStackSize(), large.largeStackLimit()));
    }

    /** {@code Slot#getMaxStackSize(ItemStack)}: as {@link #containerLimit} for the limit of the slot and its container. */
    public static int slotLimit(Slot slot, ItemStack stack, int original) {
        if (!(stack.getItem() instanceof LargeStackItem large) || slot.getMaxStackSize() != VANILLA_LIMIT
                || !keepsLargeStacks(slot.container)) return original;
        return Math.max(original, Math.min(stack.getMaxStackSize(), large.largeStackLimit()));
    }

    /**
     * Start-up check (the mixins are optional so that a changed target can never stop the game): logs an
     * error when a coin does not get its large stack in a plain container and slot, and confirms that an
     * ordinary item keeps the vanilla limit.
     */
    public static void selfCheck() {
        try {
            ItemStack coin = new ItemStack(com.miophas.singularity_iteration.common.item.normal.mio_icif_normal.COIN.get());
            ItemStack plain = new ItemStack(net.minecraft.world.item.Items.STONE);
            SimpleContainer container = new SimpleContainer(1);
            Slot slot = new Slot(container, 0, 0, 0);
            int expected = Math.min(coin.getMaxStackSize(), ((LargeStackItem) coin.getItem()).largeStackLimit());
            boolean ok = container.getMaxStackSize(coin) == expected && slot.getMaxStackSize(coin) == expected
                && container.getMaxStackSize(plain) == plain.getMaxStackSize() && slot.getMaxStackSize(plain) == plain.getMaxStackSize();
            if (!ok) {
                com.miophas.singularity_iteration.common.Singularity_Iteration.LOGGER.error(
                    "Large coin stacks are not active (container {}, slot {}, expected {}); coins stack to 99 in containers",
                    container.getMaxStackSize(coin), slot.getMaxStackSize(coin), expected);
            }
        } catch (RuntimeException error) {
            com.miophas.singularity_iteration.common.Singularity_Iteration.LOGGER.error("Large stack self-check failed", error);
        }
    }
}
