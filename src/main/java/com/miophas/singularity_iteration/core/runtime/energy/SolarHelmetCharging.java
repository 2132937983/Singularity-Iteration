// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.runtime.energy;

import com.miophas.singularity_iteration.core.api.item.BatteryTransfer;

import com.miophas.singularity_iteration.core.api.item.IBatteryItem;
import com.miophas.singularity_iteration.core.api.item.IElectricArmorItem;
import java.util.WeakHashMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** One worn solar source per player tick. Cursor values retain no player, level or inventory references. */
public final class SolarHelmetCharging {
    public static final int INVENTORY_BUDGET = 12;
    public static final int IDLE_TICKS = 20;
    public static final int HEAD_SLOT = Inventory.INVENTORY_SIZE + EquipmentSlot.HEAD.getIndex();
    private static final int[] ARMOR_SLOTS = {Inventory.INVENTORY_SIZE + EquipmentSlot.CHEST.getIndex(),
        Inventory.INVENTORY_SIZE + EquipmentSlot.LEGS.getIndex(), Inventory.INVENTORY_SIZE + EquipmentSlot.FEET.getIndex()};
    private static final EquipmentSlot[] WORN_ARMOR_SLOTS = {
        EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };
    private static final WeakHashMap<Player, Cursor> PLAYERS = new WeakHashMap<>();
    private static final WeakHashMap<Player, Long> CHEST_TICKS = new WeakHashMap<>();
    private static final WeakHashMap<Player, Long> ARMOR_TICKS = new WeakHashMap<>();
    /** Sub-EU sky-light output carried between ticks for the base solar helmet. */
    private static final WeakHashMap<Player, Double> CHEST_CARRY = new WeakHashMap<>();
    private SolarHelmetCharging() { }

    public static void tick(ItemStack source, IBatteryItem battery, Level level, Player player, int generation, int limit) {
        if (!(level instanceof ServerLevel server) || !server.getServer().isSameThread()
                || player.level() != level || player.getItemBySlot(EquipmentSlot.HEAD) != source || source.getCount() != 1) return;
        PLAYERS.computeIfAbsent(player, ignored -> new Cursor()).step(player.getInventory(), source, battery, level.getGameTime(), generation, limit);
    }

    /**
     * Base helmet policy: generate the fractional getSkyLight() output into the helmet buffer, then offer only to
     * the worn chest item. Sub-EU output is carried between ticks per player.
     */
    public static long tickChestOnly(ItemStack source, IBatteryItem battery, Level level, Player player, double generation, long limit) {
        if (!(level instanceof ServerLevel server) || !server.getServer().isSameThread()
                || player.level() != level || player.getItemBySlot(EquipmentSlot.HEAD) != source
                || source.isEmpty() || source.getCount() != 1) return 0;
        long tick = level.getGameTime();
        Long previous = CHEST_TICKS.put(player, tick);
        if (previous != null && previous == tick) return 0;
        long whole = accumulateCarry(player, generation);
        if (whole > 0) battery.addEnergy(source, whole);
        long offered = Math.min(Math.max(0, limit), battery.getEnergy(source));
        if (offered <= 0) return 0;
        ItemStack target = player.getItemBySlot(EquipmentSlot.CHEST);
        if (!(target.getItem() instanceof IBatteryItem receiver)) return 0;
        return BatteryTransfer.move(source, battery, target, receiver, offered);
    }

    /** Carry the sub-EU part of a fractional generation rate so slow sky-light output is not lost. */
    private static long accumulateCarry(Player player, double generation) {
        double carried = CHEST_CARRY.getOrDefault(player, 0.0D);
        double total = carried + (Double.isFinite(generation) ? Math.max(0.0D, generation) : 0.0D);
        long whole = (long) Math.floor(total);
        double remainder = total - whole;
        if (remainder > 0.0D) CHEST_CARRY.put(player, remainder);
        else CHEST_CARRY.remove(player);
        return whole;
    }

    /** Alternative armor-only policy for addon helmets that only feed the worn armor slots. */
    public static long tickArmorOnly(ItemStack source, IBatteryItem battery, Level level, Player player, int generation, int limit) {
        if (!(level instanceof ServerLevel server) || !server.getServer().isSameThread()
                || player.level() != level || player.getItemBySlot(EquipmentSlot.HEAD) != source
                || source.isEmpty() || source.getCount() != 1) return 0;
        long tick = level.getGameTime();
        Long previous = ARMOR_TICKS.put(player, tick);
        if (previous != null && previous == tick) return 0;
        if (generation > 0) battery.addEnergy(source, generation);
        long remaining = Math.min(Math.max(0, limit), battery.getEnergy(source));
        long offered = remaining;
        for (EquipmentSlot slot : WORN_ARMOR_SLOTS) {
            if (remaining <= 0) break;
            ItemStack target = player.getItemBySlot(slot);
            if (target.getItem() instanceof IElectricArmorItem receiver) {
                remaining -= BatteryTransfer.move(source, battery, target, receiver, remaining);
            }
        }
        return offered - remaining;
    }

    public static final class Cursor {
        private boolean seen;
        private long lastTick, nextProbeTick = Long.MIN_VALUE;
        private int cursor, cachedSlot = -1, scannedSinceTransfer;
        private int inventoryProbes, targetAttempts;
        public int inventoryProbes() { return inventoryProbes; }
        public int targetAttempts() { return targetAttempts; }

        public long step(Container inventory, ItemStack source, IBatteryItem battery, long tick, int generation, int limit) {
            inventoryProbes = targetAttempts = 0;
            if (inventory.getContainerSize() <= HEAD_SLOT || source.isEmpty() || source.getCount() != 1
                    || inventory.getItem(HEAD_SLOT) != source || seen && lastTick == tick) return 0;
            if (seen && tick < lastTick) { nextProbeTick = Long.MIN_VALUE; scannedSinceTransfer = 0; }
            seen = true; lastTick = tick;
            if (generation > 0) battery.addEnergy(source, generation);
            long remaining = Math.min(Math.max(0, limit), battery.getEnergy(source));
            if (remaining <= 0 || tick < nextProbeTick) return 0;
            long offered = remaining;
            for (int slot : ARMOR_SLOTS) {
                if (remaining <= 0) break;
                remaining -= charge(inventory, slot, source, battery, remaining);
            }
            int reused = cachedSlot;
            if (remaining > 0 && reused >= 0 && reused < inventory.getContainerSize()) {
                long moved = charge(inventory, reused, source, battery, remaining);
                remaining -= moved;
                if (moved == 0) cachedSlot = -1;
            }
            int count = inventory.getContainerSize();
            while (remaining > 0 && inventoryProbes < Math.min(INVENTORY_BUDGET, count)) {
                int slot = Math.floorMod(cursor, count); cursor = (slot + 1) % count; inventoryProbes++;
                if (slot == reused || slot >= Inventory.INVENTORY_SIZE && slot <= HEAD_SLOT) continue;
                long moved = charge(inventory, slot, source, battery, remaining);
                remaining -= moved;
                if (moved > 0) cachedSlot = slot;
            }
            long moved = offered - remaining;
            if (moved > 0) { scannedSinceTransfer = 0; nextProbeTick = Long.MIN_VALUE; }
            else {
                scannedSinceTransfer += inventoryProbes;
                if (scannedSinceTransfer >= count) { nextProbeTick = tick + IDLE_TICKS; scannedSinceTransfer = 0; }
            }
            return moved;
        }

        private long charge(Container inventory, int slot, ItemStack source, IBatteryItem battery, long amount) {
            targetAttempts++;
            ItemStack target = inventory.getItem(slot);
            return target.getItem() instanceof IBatteryItem item ? BatteryTransfer.move(source, battery, target, item, amount) : 0;
        }
    }
}
