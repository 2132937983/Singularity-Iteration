// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.api.item;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.fml.ModList;

/**
 * Inventory auto-charge mode of portable batteries and energy packs (0.1.7.35).
 *
 * <p>Right click (energy packs: sneak + right click) switches the mode on and off. While the mode
 * is on, the source feeds the electric tools and electric armor in the player inventory, the worn
 * armor and the accessory slots, at its charge rate. While the mode is off, the source gives
 * nothing.
 *
 * <p>Loop guard: a source in auto mode never charges another source (battery, energy pack or a
 * second source in auto mode), and solar helmets skip every source in auto mode
 * ({@link #isChargeSource}). Thus energy cannot cycle between a helmet and a battery.
 *
 * <p>Cost: one pass every {@link #INTERVAL} ticks, staggered per slot; no collection is
 * allocated unless an accessory provider (Curios) is loaded.
 */
public final class BatteryAutoCharge {
    private BatteryAutoCharge() {}

    public static final String TAG = "SiAutoCharge";
    /** Ticks between two charge passes; each pass moves INTERVAL x the charge rate. */
    public static final int INTERVAL = 10;

    /** Accessory targets of one pass (server thread only, cleared after use). */
    private static final List<ItemStack> ACCESSORIES = new ArrayList<>();

    public static boolean isOn(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data != null && data.contains(TAG);
    }

    /** Switches the mode; returns the new state. */
    public static boolean toggle(ItemStack stack) {
        boolean next = !isOn(stack);
        CustomData.update(DataComponents.CUSTOM_DATA, stack, (CompoundTag tag) -> {
            if (next) tag.putBoolean(TAG, true);
            else tag.remove(TAG);
        });
        return next;
    }

    /**
     * True for items that give energy to other items: batteries, energy packs and any item in auto
     * mode. Solar helmets and auto sources do not charge these items.
     */
    public static boolean isChargeSource(ItemStack stack) {
        return !stack.isEmpty() && (isOn(stack) || stack.getItem() instanceof IEnergyPackItem
            || stack.getItem() instanceof IEnergyDistributable d && d.supportsAutoCharge());
    }

    /**
     * One charge pass from {@code source} (it must be in the inventory of {@code player}).
     * Returns the EU moved. Call it every tick: it runs only on its own slot's interval tick.
     */
    public static long tick(Player player, ItemStack source, IBatteryItem battery, long ratePerTick, int slotId) {
        if (!(player.level() instanceof ServerLevel) || source.getCount() != 1 || !isOn(source)) return 0;
        if ((player.tickCount + slotId) % INTERVAL != 0) return 0;
        long budget = Math.min(battery.getEnergy(source), Math.max(0, ratePerTick) * INTERVAL);
        if (budget <= 0) return 0;
        long remaining = budget;
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize() && remaining > 0; i++) {
            remaining -= offer(source, battery, inv.getItem(i), remaining);
        }
        if (remaining > 0 && ModList.get().isLoaded("curios")) {
            try {
                BatteryTargetProviders.appendTargets(player, ACCESSORIES, source);
                for (int i = 0; i < ACCESSORIES.size() && remaining > 0; i++) {
                    remaining -= offer(source, battery, ACCESSORIES.get(i), remaining);
                }
            } finally {
                ACCESSORIES.clear();
            }
        }
        return budget - remaining;
    }

    private static long offer(ItemStack source, IBatteryItem battery, ItemStack target, long amount) {
        if (target == source || !BatteryTransfer.isEquipment(target) || isChargeSource(target)
                || !(target.getItem() instanceof IBatteryItem receiver)) return 0;
        if (receiver.getEnergy(target) >= receiver.getMaxEnergy(target)) return 0;
        return BatteryTransfer.move(source, battery, target, receiver, amount);
    }
}
