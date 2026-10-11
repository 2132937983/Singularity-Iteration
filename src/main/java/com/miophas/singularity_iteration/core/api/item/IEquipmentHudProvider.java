package com.miophas.singularity_iteration.core.api.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * Equipment HUD provider.
 *
 * <p>When an item is held (main hand or off hand) or worn (armor slot), it can give one line of status text for the action bar.
 * The client {@code EquipmentHudCycler} collects the text of all providers and shows the lines in turn, in a fixed order.
 * This prevents flicker when many items try to use the action bar each tick.
 *
 * <p>Note: these methods are called only on the client. Implementations can safely use client-side logic.
 */
public interface IEquipmentHudProvider {

    /**
     * Gets the HUD text to show now.
     *
     * @param stack the item stack of this item
     * @return the text to show; {@code null} means show nothing now
     */
    Component getEquipmentHudText(ItemStack stack);

    /**
     * Whether the line should stay on screen continuously (e.g. a jetpack while airborne). By
     * default a line is shown for a few seconds whenever its wording changes (mode switch, item
     * equipped) and then left to fade; only numbers changing does not re-show it.
     */
    default boolean keepEquipmentHudVisible(ItemStack stack, net.minecraft.world.entity.player.Player player) {
        return false;
    }
}
