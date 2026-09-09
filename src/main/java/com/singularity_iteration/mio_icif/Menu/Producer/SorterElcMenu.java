package com.singularity_iteration.mio_icif.Menu.Producer;

import com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_sorter_elc;
import com.singularity_iteration.mio_icif.Menu.Base.mio_icif_machine_menu;
import com.singularity_iteration.mio_icif.Menu.Slot.PhantomSlot;
import com.singularity_iteration.mio_icif.Menu.mio_icif_menus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("null")
public class SorterElcMenu extends mio_icif_machine_menu {

    public static final int BATTERY_SLOT = 0;
    public static final int UPGRADE_SLOT_START = 1;
    public static final int UPGRADE_SLOT_COUNT = 3;
    public static final int BUFFER_SLOT_START = 4;
    public static final int BUFFER_SLOT_COUNT = 11;
    public static final int FILTER_SLOT_START = 15;
    public static final int FILTER_SLOT_COUNT = 42;
    public static final int SLOT_COUNT = 1 + UPGRADE_SLOT_COUNT + BUFFER_SLOT_COUNT + FILTER_SLOT_COUNT;

    @Nullable
    private BlockPos blockPos;

    public SorterElcMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf data) {
        this(containerId, playerInventory, (mio_icif_sorter_elc) null);
        if (data != null) {
            this.blockPos = data.readBlockPos();
        }
    }

    public SorterElcMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, (mio_icif_sorter_elc) null);
    }

    public SorterElcMenu(int containerId, Inventory playerInventory, @Nullable mio_icif_sorter_elc blockEntity) {
        super(mio_icif_menus.SORTER_ELC_MENU_TYPE.get(), containerId, playerInventory, blockEntity, SLOT_COUNT, 6);
        if (blockEntity != null) {
            this.blockPos = blockEntity.getBlockPos();
        }
    }

    @Nullable
    public BlockPos getBlockPos() {
        return blockPos;
    }

    @Override
    protected void addMachineSlots() {
        this.addSlot(new SlotItemHandler(itemHandler, BATTERY_SLOT, 188, 219) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                if (!(blockEntity instanceof mio_icif_sorter_elc sorter)) return isBattery(stack);
                return sorter.isSorterBatteryForGUI(stack);
            }

            @Override
            public int getMaxStackSize(ItemStack stack) {
                return 1;
            }
        });

        for (int i = 0; i < UPGRADE_SLOT_COUNT; i++) {
            final int slotIndex = UPGRADE_SLOT_START + i;
            this.addSlot(new SlotItemHandler(itemHandler, slotIndex, 188, 161 + i * 18) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return stack.getItem() instanceof com.singularity_iteration.mio_icif.Items.Upgrade.mio_icif_upgrade upgrade &&
                           upgrade.getLocalUpgradeType() == com.singularity_iteration.mio_icif.Items.Upgrade.mio_icif_upgrade.UpgradeType.TRANSFORMER;
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

        for (int i = 0; i < BUFFER_SLOT_COUNT; i++) {
            final int slotIndex = BUFFER_SLOT_START + i;
            this.addSlot(new SlotItemHandler(itemHandler, slotIndex, 8 + i * 18, 141) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return itemHandler.isItemValid(slotIndex, stack);
                }
            });
        }

        mio_icif_sorter_elc sorter = blockEntity instanceof mio_icif_sorter_elc s ? s : null;
        for (int dir = 0; dir < 6; dir++) {
            for (int j = 0; j < 7; j++) {
                final int filterIndex = dir * 7 + j;
                final mio_icif_sorter_elc sorterRef = sorter;
                PhantomSlot phantomSlot = new PhantomSlot(FILTER_SLOT_START + filterIndex, 80 + j * 18, 19 + dir * 20, 1) {
                    @Override
                    public ItemStack getItem() {
                        if (sorterRef != null) return sorterRef.getFilterStack(filterIndex);
                        return super.getItem();
                    }

                    @Override
                    public void set(ItemStack stack) {
                        if (sorterRef != null) {
                            sorterRef.setFilterStack(filterIndex, stack);
                        } else {
                            super.set(stack);
                        }
                    }
                };
                this.addSlot(phantomSlot);
            }
        }
    }

    @Override
    public void clicked(int slotId, int dragType, ClickType clickType, Player player) {
        if (slotId >= 0 && slotId < slots.size()) {
            Slot slot = slots.get(slotId);
            if (slot instanceof PhantomSlot phantomSlot) {
                if (clickType == ClickType.PICKUP && (dragType == 0 || dragType == 1)) {
                    phantomSlot.onSlotClick(dragType, player);
                }
                return;
            }
        }
        super.clicked(slotId, dragType, clickType, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (slot instanceof PhantomSlot) {
            return ItemStack.EMPTY;
        }

        ItemStack itemstack = ItemStack.EMPTY;
        if (slot != null && slot.hasItem()) {
            ItemStack itemStack1 = slot.getItem();
            itemstack = itemStack1.copy();

            if (index < FILTER_SLOT_START) {
                if (!this.moveItemStackTo(itemStack1, this.playerInventoryStart, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (index >= this.playerInventoryStart) {
                boolean moved = false;

                if (isBattery(itemStack1)) {
                    if (this.moveItemStackTo(itemStack1, BATTERY_SLOT, BATTERY_SLOT + 1, false)) {
                        moved = true;
                    }
                }

                if (!moved && isUpgrade(itemStack1)) {
                    if (this.moveItemStackTo(itemStack1, UPGRADE_SLOT_START, UPGRADE_SLOT_START + UPGRADE_SLOT_COUNT, false)) {
                        moved = true;
                    }
                }

                if (!moved) {
                    if (!this.moveItemStackTo(itemStack1, BUFFER_SLOT_START, BUFFER_SLOT_START + BUFFER_SLOT_COUNT, false)) {
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
    protected int getPlayerInventoryY() { return 161; }

    @Override
    protected int getPlayerHotbarY() { return 219; }

    @Override
    protected int getBatterySlotIndex() { return BATTERY_SLOT; }

    @Override
    protected int getUpgradeSlotStart() { return UPGRADE_SLOT_START; }

    @Override
    protected int getUpgradeSlotCount() { return UPGRADE_SLOT_COUNT; }

    public int getProgress() { return data.get(0); }
    public int getMaxProgress() { return data.get(1); }
    public boolean isWorking() { return data.get(2) == 1; }
    public int getEnergy() { return data.get(3); }
    public int getMaxEnergy() { return data.get(4); }
    public int getDefaultOutputDirection() { return data.get(5); }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if (blockEntity instanceof mio_icif_sorter_elc sorter) {
            this.setSyncData(0, sorter.getProgress());
            this.setSyncData(1, sorter.getMaxProgress());
            this.setSyncData(2, sorter.isWorking() ? 1 : 0);
            this.setSyncData(3, (int) sorter.getEnergyStorage().getAmount());
            this.setSyncData(4, (int) sorter.getEnergyStorage().getCapacity());
            this.setSyncData(5, sorter.getDefaultOutputDirection().get3DDataValue());
        }
    }
}