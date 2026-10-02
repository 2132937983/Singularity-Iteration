// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.item;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.core.api.item.IBatteryItem;
import com.miophas.singularity_iteration.core.api.item.IEnergyPackItem;
import com.miophas.singularity_iteration.core.api.item.electric.IElectricItem;
import com.miophas.singularity_iteration.core.platform.neoforge.CoreDataComponents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.ModifyDefaultComponentsEvent;

/**
 * Anything that stores charge is a single item: two tools with different charge must never merge
 * into one stack (the energy would be lost or duplicated). Applied to every SI item that is a
 * battery, electric tool, electric armor, energy pack or carries a default charge component.
 */
@EventBusSubscriber(modid = Singularity_Iteration.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class ChargeableStackRules {
    private ChargeableStackRules() {}

    public static boolean isChargeable(Item item) {
        if (item instanceof IBatteryItem || item instanceof IEnergyPackItem || item instanceof IElectricItem) return true;
        return item.components().has(CoreDataComponents.BATTERY_ENERGY.get());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    static void modify(ModifyDefaultComponentsEvent event) {
        for (Item item : BuiltInRegistries.ITEM) {
            if (!Singularity_Iteration.MOD_ID.equals(BuiltInRegistries.ITEM.getKey(item).getNamespace())) continue;
            boolean charge = item instanceof IBatteryItem || item instanceof IEnergyPackItem || item instanceof IElectricItem;
            if (!charge) continue;
            event.modify(item, b -> b.set(DataComponents.MAX_STACK_SIZE, 1));
        }
    }
}
