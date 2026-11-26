// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.integration;

import com.miophas.singularity_iteration.common.blockentity.producer.mio_icif_advanced_miner_elc;
import com.miophas.singularity_iteration.common.client.screen.mio_icif_gui_advanced_miner_elc;
import com.miophas.singularity_iteration.common.menu.producer.AdvancedMinerElcMenu;
import com.miophas.singularity_iteration.common.menu.slot.PhantomSlot;
import com.miophas.singularity_iteration.common.network.mio_icif_Network;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 0.1.11.209: dragging an item from the recipe viewer's list (JEI or EMI) onto a filter slot of the Advanced Miner sets
 * the filter, as a click with the item does; nothing is consumed (the slots only hold a marker) and the server is told.
 */
public final class AdvancedMinerFilters {
    private AdvancedMinerFilters() {}

    /** A filter slot and where it is on the screen (16 x 16 at {@code x, y}). */
    public record Target(Slot slot, int index, int x, int y) {}

    /** The filter slots of the open screen. */
    public static List<Target> targets(mio_icif_gui_advanced_miner_elc gui) {
        List<Target> targets = new ArrayList<>();
        for (Slot slot : gui.getMenu().slots) {
            if (!(slot instanceof PhantomSlot)) continue;
            int index = filterIndex(slot);
            if (index >= 0) targets.add(new Target(slot, index, gui.getGuiLeft() + slot.x, gui.getGuiTop() + slot.y));
        }
        return targets;
    }

    /** Sets {@code stack} (one of it) as the filter of {@code target}, on the screen and on the server. */
    public static void set(mio_icif_gui_advanced_miner_elc gui, Target target, ItemStack stack) {
        if (stack.isEmpty()) return;
        ItemStack filter = stack.copyWithCount(1);
        target.slot().set(filter);
        mio_icif_advanced_miner_elc miner = gui.getMenu().getMinerBlockEntity();
        if (miner != null) mio_icif_Network.sendMinerFilter(miner.getBlockPos(), target.index(), filter);
    }

    /**
     * The filter index (0 .. FILTER_COUNT-1) from the slot's place on the fixed 18 px grid that starts at
     * (FILTER_START_X, FILTER_START_Y); more reliable than {@code Slot.index}, which {@code addSlot} overwrites.
     */
    public static int filterIndex(Slot slot) {
        int col = (slot.x - AdvancedMinerElcMenu.FILTER_START_X) / 18;
        int row = (slot.y - AdvancedMinerElcMenu.FILTER_START_Y) / 18;
        if (col < 0 || col >= AdvancedMinerElcMenu.FILTER_COLS || row < 0 || row >= AdvancedMinerElcMenu.FILTER_ROWS) return -1;
        return row * AdvancedMinerElcMenu.FILTER_COLS + col;
    }
}
