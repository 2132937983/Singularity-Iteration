package com.singularity_iteration.mio_icif.Menu.Producer;

import com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_item_buffer_elc;
import com.singularity_iteration.mio_icif.Menu.mio_icif_menus;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("null")
public class ItemBufferMenu extends AbstractContainerMenu {

    public static final int SLOT_COUNT = 50;
    public static final int LEFT_CONTENT_SLOTS = 24;
    public static final int RIGHT_CONTENT_SLOTS = 24;
    public static final int UPGRADE_SLOT_1 = 48;
    public static final int UPGRADE_SLOT_2 = 49;

    private final ItemStackHandler itemHandler;
    @Nullable
    private final mio_icif_item_buffer_elc blockEntity;

    public ItemBufferMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, null, null);
    }

    public ItemBufferMenu(int containerId, Inventory playerInventory, @Nullable ItemStackHandler itemHandler, @Nullable mio_icif_item_buffer_elc blockEntity) {
        super(mio_icif_menus.ITEM_BUFFER_MENU_TYPE.get(), containerId);
        this.itemHandler = itemHandler != null ? itemHandler : new ItemStackHandler(SLOT_COUNT);
        this.blockEntity = blockEntity;

        addMachineSlots();
        addPlayerInventory(playerInventory);
    }

    private void addMachineSlots() {
        for (int y = 0; y < LEFT_CONTENT_SLOTS / 4; y++) {
            for (int x = 0; x < 4; x++) {
                int slotIndex = x + y * 4;
                this.addSlot(new SlotItemHandler(itemHandler, slotIndex, 8 + x * 18, 18 + y * 18));
            }
        }

        for (int y = 0; y < RIGHT_CONTENT_SLOTS / 4; y++) {
            for (int x = 0; x < 4; x++) {
                int slotIndex = LEFT_CONTENT_SLOTS + x + y * 4;
                this.addSlot(new SlotItemHandler(itemHandler, slotIndex, 98 + x * 18, 18 + y * 18));
            }
        }

        this.addSlot(new SlotItemHandler(itemHandler, UPGRADE_SLOT_1, 35, 128) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof com.singularity_iteration.mio_icif.Items.Upgrade.mio_icif_upgrade;
            }
            @Override
            public boolean mayPickup(net.minecraft.world.entity.player.Player player) {
                if (isTransformerUpgrade(getItem())) {
                    return com.singularity_iteration.mio_icif.network.mio_icif_KeyboardManager.isSafetyKeyDown(player);
                }
                return super.mayPickup(player);
            }
        });
        this.addSlot(new SlotItemHandler(itemHandler, UPGRADE_SLOT_2, 125, 128) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof com.singularity_iteration.mio_icif.Items.Upgrade.mio_icif_upgrade;
            }
            @Override
            public boolean mayPickup(net.minecraft.world.entity.player.Player player) {
                if (isTransformerUpgrade(getItem())) {
                    return com.singularity_iteration.mio_icif.network.mio_icif_KeyboardManager.isSafetyKeyDown(player);
                }
                return super.mayPickup(player);
            }
        });
    }

    private void addPlayerInventory(Inventory playerInventory) {
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.addSlot(new Slot(playerInventory, j + i * 9 + 9, 8 + j * 18, 150 + i * 18));
            }
        }
        for (int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(playerInventory, i, 8 + i * 18, 208));
        }
    }

    private static boolean isTransformerUpgrade(ItemStack stack) {
        if (com.singularity_iteration.mio_icif.api.MioIcifAPI.instance() == null) return false;
        return com.singularity_iteration.mio_icif.api.MioIcifAPI.instance().getUpgradeAPI().getUpgradeType(stack)
                == com.singularity_iteration.mio_icif.api.upgrade.IUpgradeAPI.UpgradeType.TRANSFORMER;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack itemStack1 = slot.getItem();
            itemstack = itemStack1.copy();

            int machineSlots = SLOT_COUNT;
            if (index < machineSlots) {
                if (!this.moveItemStackTo(itemStack1, machineSlots, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                boolean moved = false;
                if (itemStack1.getItem() instanceof com.singularity_iteration.mio_icif.Items.Upgrade.mio_icif_upgrade) {
                    if (this.moveItemStackTo(itemStack1, UPGRADE_SLOT_1, UPGRADE_SLOT_2 + 1, false)) {
                        moved = true;
                    }
                }
                if (!moved) {
                    if (!this.moveItemStackTo(itemStack1, 0, LEFT_CONTENT_SLOTS + RIGHT_CONTENT_SLOTS, false)) {
                        return ItemStack.EMPTY;
                    }
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

    @Override
    public boolean stillValid(Player player) {
        if (blockEntity == null) return true;
        return blockEntity.stillValid(player);
    }

    @Nullable
    public mio_icif_item_buffer_elc getBlockEntity() {
        return blockEntity;
    }
}