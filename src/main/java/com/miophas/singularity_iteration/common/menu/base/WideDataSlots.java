// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.menu.base;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.DataSlot;

/**
 * Menu data slots that carry a full {@code int} (0.1.10.16: shared by {@link mio_icif_base_menu} and the
 * menus that do not extend it).
 *
 * <p>Vanilla sends a container data slot as a signed 16-bit value, so energy, capacity, progress or
 * coordinates above 32767 wrapped on the client. Each logical value is sent as two 16-bit halves; the
 * client writes both halves back into its {@link ContainerData}, which then holds the full value.
 */
public final class WideDataSlots {
    private WideDataSlots() { }

    /** Two data slots (low and high 16 bits) for every value of {@code data}, in order. */
    public static List<DataSlot> of(ContainerData data) {
        List<DataSlot> slots = new ArrayList<>(data.getCount() * 2);
        for (int i = 0; i < data.getCount(); i++) {
            final int index = i;
            slots.add(new DataSlot() {
                @Override public int get() { return data.get(index) & 0xFFFF; }
                @Override public void set(int value) { data.set(index, (data.get(index) & 0xFFFF0000) | (value & 0xFFFF)); }
            });
            slots.add(new DataSlot() {
                @Override public int get() { return data.get(index) >>> 16; }
                @Override public void set(int value) { data.set(index, (data.get(index) & 0xFFFF) | ((value & 0xFFFF) << 16)); }
            });
        }
        return slots;
    }
}
