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

    /** 0.1.11.201 (T5d): a device that acts for the player wearing it. */
    public interface WornDevice {
        void tickWorn(ItemStack stack, Player player);
    }

    /**
     * 0.1.11.201 (T5d, decision D7): where the devices work. With Curios installed they are accessories and work only from an
     * accessory slot (the bridge sets {@link #wornCopies}); without Curios they work from the inventory as before.
     */
    public static boolean fromInventory() {
        return wornCopies == null;
    }

    /** Set by the Curios bridge: the player's functional accessory stacks, in slot order. */
    public static volatile java.util.function.Function<Player, java.util.List<ItemStack>> wornCopies;

    /** 0.1.11.201 (T5d): the tooltip line that says where the device works (an accessory slot with Curios). */
    public static void appendWornHint(java.util.List<net.minecraft.network.chat.Component> tooltip, String curiosSlot) {
        if (fromInventory()) return;
        tooltip.add(net.minecraft.network.chat.Component.translatable("tooltip.mio_icif.passive_device.worn",
            net.minecraft.network.chat.Component.translatable("curios.identifier." + curiosSlot))
            .withStyle(net.minecraft.ChatFormatting.GOLD));
    }

    /** Is it this device's turn: the interval has come round for {@code player}? */
    public static boolean onInterval(Player player, int interval) {
        return player.tickCount % interval == 0;
    }

    /**
     * Is {@code stack} the copy that acts for {@code player}? The first stack of the same item that passes
     * {@code active} wins; stacks are compared by identity.
     */
    public static boolean isActingCopy(Player player, ItemStack stack, Predicate<ItemStack> active) {
        var worn = wornCopies;
        if (worn != null) {
            for (ItemStack copy : worn.apply(player)) {
                if (firstActive(copy, stack, active) != null) return copy == stack;
            }
            return false;
        }
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
