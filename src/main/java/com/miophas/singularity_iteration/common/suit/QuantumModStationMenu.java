// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.suit;

import com.miophas.singularity_iteration.common.menu.base.mio_icif_machine_menu;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.Nullable;

/** Quantum modification station GUI: piece slot, unit slot, battery, installed-unit list. */
public class QuantumModStationMenu extends mio_icif_machine_menu {
    public static final int GUI_WIDTH = 176, GUI_HEIGHT = 206;
    public static final int SUIT_X = 30, SUIT_Y = 22;
    public static final int UNIT_X = 30, UNIT_Y = 72;
    public static final int BATTERY_X = 8, BATTERY_Y = 92;
    /** Unit list panel. */
    public static final int LIST_X = 60, LIST_Y = 18, LIST_W = 108, LIST_H = 92, ROW_H = 14, LIST_TOP = 14;

    public QuantumModStationMenu(int containerId, Inventory inventory, @Nullable RegistryFriendlyByteBuf buf) {
        this(containerId, inventory, null, null);
    }

    public QuantumModStationMenu(int containerId, Inventory inventory, @Nullable QuantumModStationBlockEntity station, @Nullable ContainerData data) {
        super(SuitRegistry.STATION_MENU.get(), containerId, QuantumModStationBlockEntity.SLOT_COUNT, inventory,
            station != null ? station.getItemHandler() : null, data, QuantumModStationBlockEntity.DATA_COUNT, station);
    }

    @Override
    protected void addMachineSlots() {
        addBatterySlot(QuantumModStationBlockEntity.BATTERY_SLOT, BATTERY_X, BATTERY_Y);
        addSlot(new SlotItemHandler(itemHandler, QuantumModStationBlockEntity.SUIT_SLOT, SUIT_X, SUIT_Y) {
            @Override public boolean mayPlace(ItemStack stack) { return SuitModules.isSuitPiece(stack); }
            @Override public int getMaxStackSize() { return 1; }
            @Override public int getMaxStackSize(ItemStack stack) { return 1; }
        }.setBackground(InventoryMenu.BLOCK_ATLAS, InventoryMenu.EMPTY_ARMOR_SLOT_CHESTPLATE));
        addSlot(new SlotItemHandler(itemHandler, QuantumModStationBlockEntity.MODULE_SLOT, UNIT_X, UNIT_Y) {
            @Override public boolean mayPlace(ItemStack stack) { return stack.getItem() instanceof SuitModuleItem; }
        });
    }

    @Override protected int getPlayerInventoryY() { return 124; }
    @Override protected int getPlayerHotbarY() { return 182; }
    @Override protected int getBatterySlotIndex() { return QuantumModStationBlockEntity.BATTERY_SLOT; }
    @Override protected int getDataSlotCount() { return QuantumModStationBlockEntity.DATA_COUNT; }

    public long energy() {
        return (data.get(0) & 0xFFFFL) | ((long) (data.get(1) & 0xFFFF) << 16) | ((long) data.get(2) << 32);
    }
    public long capacity() { return Math.max(1, data.get(5)); }
    public int installProgress() { return data.get(3); }
    public QuantumModStationBlockEntity.Status status() {
        QuantumModStationBlockEntity.Status[] values = QuantumModStationBlockEntity.Status.values();
        return values[Math.floorMod(data.get(4), values.length)];
    }

    public ItemStack suit() { return getSlot(1).getItem(); }
    public ItemStack unit() { return getSlot(2).getItem(); }
    public List<SuitModuleType> installed() { return SuitModules.installed(suit()).types(); }

    /** Button id n = remove the n-th installed unit. */
    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (blockEntity instanceof QuantumModStationBlockEntity station) return station.removeUnit(id, player);
        return false;
    }
}
