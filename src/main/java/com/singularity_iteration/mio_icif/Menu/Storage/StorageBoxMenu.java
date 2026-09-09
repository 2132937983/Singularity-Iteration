package com.singularity_iteration.mio_icif.Menu.Storage;

import com.singularity_iteration.mio_icif.Menu.Base.mio_icif_base_menu;
import com.singularity_iteration.mio_icif.Menu.mio_icif_menus;
import com.singularity_iteration.mio_icif.Blocks.entity.build.mio_icif_storage_box_entity;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;

@SuppressWarnings({"null"})
public class StorageBoxMenu extends mio_icif_base_menu {

    private static final int COLS = 9;
    private static final int MAX_VISIBLE_ROWS = 6;

    private static final Field SLOT_X_FIELD;
    private static final Field SLOT_Y_FIELD;

    static {
        try {
            SLOT_X_FIELD = Slot.class.getDeclaredField("x");
            SLOT_X_FIELD.setAccessible(true);
            SLOT_Y_FIELD = Slot.class.getDeclaredField("y");
            SLOT_Y_FIELD.setAccessible(true);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException("Failed to access Slot.x or Slot.y fields", e);
        }
    }

    private final Container container;
    private final int totalRows;
    private final int visibleRows;
    private final int slotCount;
    private final boolean canScroll;
    private final String storageType;
    private int currentScrollOffset;

    private static MenuType<StorageBoxMenu> getMenuTypeForSlotCount(int slotCount) {
        return switch (slotCount) {
            case 45 -> mio_icif_menus.STORAGE_BOX_MENU_45.get();
            case 63 -> mio_icif_menus.STORAGE_BOX_MENU_63.get();
            case 84 -> mio_icif_menus.STORAGE_BOX_MENU_84.get();
            case 126 -> mio_icif_menus.STORAGE_BOX_MENU_126.get();
            default -> mio_icif_menus.STORAGE_BOX_MENU_27.get();
        };
    }

    public StorageBoxMenu(int containerId, Inventory playerInventory, int slotCount) {
        this(containerId, playerInventory, new SimpleContainer(slotCount), slotCount, "wood");
    }

    public StorageBoxMenu(int containerId, Inventory playerInventory, @Nullable mio_icif_storage_box_entity blockEntity) {
        this(containerId, playerInventory,
                blockEntity != null ? blockEntity : new SimpleContainer(27),
                blockEntity != null ? blockEntity.getContainerSize() : 27,
                blockEntity != null ? blockEntity.getStorageType() : "wood");
    }

    private StorageBoxMenu(int containerId, Inventory playerInventory, Container container, int slotCount, String storageType) {
        super(getMenuTypeForSlotCount(slotCount), containerId, slotCount, slotCount);
        this.container = container;
        this.slotCount = slotCount;
        this.storageType = storageType;
        this.totalRows = (int) Math.ceil((double) slotCount / COLS);
        this.visibleRows = Math.min(this.totalRows, MAX_VISIBLE_ROWS);
        this.canScroll = this.totalRows > MAX_VISIBLE_ROWS;

        for (int row = 0; row < this.totalRows; row++) {
            for (int col = 0; col < COLS; col++) {
                int slotIndex = row * COLS + col;
                if (slotIndex < slotCount) {
                    this.addSlot(new ScrollableSlot(container, slotIndex, 8 + col * 18, 18 + row * 18, row));
                }
            }
        }

        int imageWidth = COLS * 18 + 14;
        int playerInvX = (imageWidth - 160) / 2;
        int playerInventoryY = 18 + this.visibleRows * 18 + 13;
        int hotbarY = playerInventoryY + 58;

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, playerInvX + col * 18, playerInventoryY + row * 18));
            }
        }

        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, playerInvX + col * 18, hotbarY));
        }

        updateSlotPositions(0);
    }

    public StorageBoxMenu(int containerId, Inventory playerInventory, Container container) {
        this(containerId, playerInventory, container, container.getContainerSize(), "wood");
    }



    public void updateSlotPositions(int scrollOffset) {
        this.currentScrollOffset = scrollOffset;
        for (int i = 0; i < this.slotCount; i++) {
            Slot slot = this.slots.get(i);
            int row = i / COLS;
            int col = i % COLS;
            try {
                SLOT_X_FIELD.setInt(slot, 8 + col * 18);
                SLOT_Y_FIELD.setInt(slot, 18 + (row - scrollOffset) * 18);
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Failed to update slot position", e);
            }
        }
    }

    public int getTotalRows() {
        return totalRows;
    }

    public int getVisibleRows() {
        return visibleRows;
    }

    public String getStorageType() {
        return storageType;
    }

    @Override
    protected boolean isBattery(ItemStack stack) {
        return false;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.container.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack itemStack1 = slot.getItem();
            itemstack = itemStack1.copy();

            int containerSize = this.slotCount;
            int totalSlots = this.slots.size();

            if (index < containerSize) {
                if (!this.moveItemStackTo(itemStack1, containerSize, totalSlots, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (!this.moveItemStackTo(itemStack1, 0, containerSize, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (itemStack1.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setByPlayer(itemStack1);
            }
        }

        return itemstack;
    }

    private class ScrollableSlot extends Slot {
        private final int logicalRow;

        public ScrollableSlot(Container container, int slot, int x, int y, int logicalRow) {
            super(container, slot, x, y);
            this.logicalRow = logicalRow;
        }

        @Override
        public boolean isActive() {
            if (!canScroll) return true;
            return logicalRow >= currentScrollOffset && logicalRow < currentScrollOffset + visibleRows;
        }
    }
}