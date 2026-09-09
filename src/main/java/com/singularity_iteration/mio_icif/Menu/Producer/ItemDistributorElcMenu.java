package com.singularity_iteration.mio_icif.Menu.Producer;

import com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_item_distributor_elc;
import com.singularity_iteration.mio_icif.Menu.Base.mio_icif_machine_menu;
import com.singularity_iteration.mio_icif.Menu.mio_icif_menus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("null")
public class ItemDistributorElcMenu extends mio_icif_machine_menu {

    public static final int BUFFER_SLOT_START = 0;
    public static final int BUFFER_SLOT_COUNT = 9;
    public static final int SLOT_COUNT = 9;

    @Nullable
    private BlockPos blockPos;

    public ItemDistributorElcMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, (mio_icif_item_distributor_elc) null);
    }

    public ItemDistributorElcMenu(int containerId, Inventory playerInventory, @Nullable mio_icif_item_distributor_elc blockEntity) {
        super(mio_icif_menus.ITEM_DISTRIBUTOR_ELC_MENU_TYPE.get(), containerId, SLOT_COUNT, playerInventory,
            blockEntity != null ? blockEntity.getItemHandler() : new ItemStackHandler(SLOT_COUNT),
            blockEntity != null ? blockEntity.getPriorityData() : new SimpleContainerData(mio_icif_item_distributor_elc.PRIORITY_DATA_SIZE),
            mio_icif_item_distributor_elc.PRIORITY_DATA_SIZE,
            blockEntity);
        if (blockEntity != null) {
            this.blockPos = blockEntity.getBlockPos();
        }
    }

    public ItemDistributorElcMenu(int containerId, Inventory playerInventory, FriendlyByteBuf data) {
        this(containerId, playerInventory, (mio_icif_item_distributor_elc) null);
        if (data != null) {
            this.blockPos = data.readBlockPos();
        }
    }

    @Nullable
    public BlockPos getBlockPos() {
        return blockPos;
    }

    public int getPriorityDirection(int index) {
        return data.get(index);
    }

    @Override
    protected void addMachineSlots() {
        for (int i = 0; i < BUFFER_SLOT_COUNT; i++) {
            final int slotIndex = BUFFER_SLOT_START + i;
            this.addSlot(new SlotItemHandler(itemHandler, slotIndex, 8 + i * 18, 108) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return itemHandler.isItemValid(slotIndex, stack);
                }
            });
        }
    }

    @Override
    protected int getPlayerInventoryY() { return 129; }

    @Override
    protected int getPlayerHotbarY() { return 187; }

    @Override
    protected int getBatterySlotIndex() { return -1; }

    @Override
    protected int getUpgradeSlotStart() { return -1; }

    @Override
    protected int getUpgradeSlotCount() { return 0; }

    @Override
    public ItemStack quickMoveStack(net.minecraft.world.entity.player.Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        net.minecraft.world.inventory.Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack itemStack1 = slot.getItem();
            itemstack = itemStack1.copy();

            if (index < SLOT_COUNT) {
                if (!this.moveItemStackTo(itemStack1, SLOT_COUNT, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (!this.moveItemStackTo(itemStack1, BUFFER_SLOT_START, BUFFER_SLOT_START + BUFFER_SLOT_COUNT, false)) {
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
}

