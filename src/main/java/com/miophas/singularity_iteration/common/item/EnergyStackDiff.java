package com.miophas.singularity_iteration.common.item;

import com.miophas.singularity_iteration.core.platform.neoforge.CoreDataComponents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;

/** Tells whether two stacks differ only in their stored charge. */
public final class EnergyStackDiff {
    private EnergyStackDiff() {}

    public static boolean energyOnly(ItemStack a, ItemStack b) {
        if (a.isEmpty() || b.isEmpty() || a.getItem() != b.getItem() || a.getCount() != b.getCount()) return false;
        var energy = CoreDataComponents.BATTERY_ENERGY.get();
        if (!a.has(energy) && !b.has(energy)) return false;
        ItemStack x = a.copy(), y = b.copy();
        x.remove(energy); y.remove(energy);
        x.remove(DataComponents.DAMAGE); y.remove(DataComponents.DAMAGE);   // charge shown as durability bar
        return ItemStack.isSameItemSameComponents(x, y);
    }
}
