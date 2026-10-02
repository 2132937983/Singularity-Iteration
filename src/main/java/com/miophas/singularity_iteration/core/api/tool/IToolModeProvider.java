// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.api.tool;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * A tool or weapon with named operating modes that a GUI (the equipment console)
 * can list and select directly. {@link ToggleableElectricTool#toggleActive} keeps
 * cycling to the next mode as before.
 */
public interface IToolModeProvider extends ToggleableElectricTool {
    /** Display names of all modes, in order. */
    List<Component> toolModes(ItemStack stack);

    /** Index of the current mode in {@link #toolModes}. */
    int toolModeIndex(ItemStack stack);

    /** Short detail lines (cost, range, output...) for a mode; empty by default. */
    default List<Component> toolModeDetails(ItemStack stack, int index) { return List.of(); }

    /**
     * Server side: select a mode. The default cycles with {@link #toggleActive} until the
     * index is reached (bounded), so tools with side effects on switching keep them.
     */
    default void selectToolMode(ItemStack stack, Player player, int index) {
        int count = toolModes(stack).size();
        if (index < 0 || index >= count) return;
        for (int i = 0; i < count && toolModeIndex(stack) != index; i++) toggleActive(stack, player);
    }
}
