// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.api.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/**
 * Per-item energy allocation priority, used when a charge pad (or any other shared
 * charger) splits its energy between a player's items: higher priority items are
 * filled first, {@link #OFF} items are never charged. Stored in the item's custom
 * data, default {@link #NORMAL} (the historical order: armor, then hands).
 */
public enum ChargePriority {
    OFF, LOW, NORMAL, HIGH;

    public static final String KEY = "si_charge_priority";

    public static ChargePriority of(ItemStack stack) {
        if (stack.isEmpty()) return NORMAL;
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null || !data.contains(KEY)) return NORMAL;
        int v = data.copyTag().getInt(KEY);
        return v >= 0 && v < values().length ? values()[v] : NORMAL;
    }

    public static void set(ItemStack stack, ChargePriority priority) {
        if (stack.isEmpty()) return;
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            if (priority == NORMAL) tag.remove(KEY);
            else tag.putInt(KEY, priority.ordinal());
        });
    }
}
