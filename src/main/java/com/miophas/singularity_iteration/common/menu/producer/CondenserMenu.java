package com.miophas.singularity_iteration.common.menu.producer;

import com.miophas.singularity_iteration.common.registry.mio_icif_menus;
import com.miophas.singularity_iteration.common.blockentity.producer.mio_icif_condenser;
import com.miophas.singularity_iteration.common.menu.base.mio_icif_machine_menu;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings({"null"}) public class CondenserMenu extends mio_icif_machine_menu {
    public static final int VENT_SLOT_1 = 0;
    public static final int VENT_SLOT_2 = 1;
    public static final int VENT_SLOT_3 = 2;
    public static final int VENT_SLOT_4 = 3;
    public static final int INPUT_BUCKET_SLOT = 4;
    public static final int EMPTY_BUCKET_SLOT = 5;
    public static final int BATTERY_SLOT = 6;
    public static final int UPGRADE_SLOT = 7;
    public static final int SLOT_COUNT = 8;

    public CondenserMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, null, null);
    }

    public CondenserMenu(int containerId, Inventory playerInventory, @Nullable IItemHandler itemHandler) {
        this(containerId, playerInventory, itemHandler, null);
    }

    public CondenserMenu(int containerId, Inventory playerInventory, @Nullable IItemHandler itemHandler, @Nullable ContainerData data) {
        super(mio_icif_menus.CONDENSER_MENU_TYPE.get(), containerId, SLOT_COUNT, playerInventory, itemHandler, data, 6);
    }

    public CondenserMenu(int containerId, Inventory playerInventory, @Nullable mio_icif_condenser blockEntity) {
        this(containerId, playerInventory,
              blockEntity != null ? blockEntity.getItemHandler() : null,
              blockEntity != null ? blockEntity.getContainerData() : null, blockEntity);
    }

    public CondenserMenu(int containerId, Inventory playerInventory, @Nullable IItemHandler itemHandler,
                         @Nullable ContainerData data, @Nullable mio_icif_condenser blockEntity) {
        super(mio_icif_menus.CONDENSER_MENU_TYPE.get(), containerId, SLOT_COUNT, playerInventory,
              itemHandler, data, 6, blockEntity);
    }

    @Override
    protected void addMachineSlots() {
        this.addSlot(new SlotItemHandler(itemHandler, VENT_SLOT_1, 10, 18));
        this.addSlot(new SlotItemHandler(itemHandler, VENT_SLOT_2, 30, 18));
        this.addSlot(new SlotItemHandler(itemHandler, VENT_SLOT_3, 10, 38));
        this.addSlot(new SlotItemHandler(itemHandler, VENT_SLOT_4, 30, 38));
        this.addSlot(new SlotItemHandler(itemHandler, INPUT_BUCKET_SLOT, 10, 62));
        this.addSlot(new SlotItemHandler(itemHandler, EMPTY_BUCKET_SLOT, 30, 62));
        this.addSlot(new SlotItemHandler(itemHandler, BATTERY_SLOT, 10, 88));
        this.addUpgradeSlot(UPGRADE_SLOT, 30, 88);
    }

    // The GUI height is 184. The player inventory position follows the original IC2 logic.
    // Inventory Y = 184 - 82 = 102, hotbar Y = 184 - 24 = 160

    public boolean isWorking() { return data.get(0) == 1; }
    public int getEnergy() { return data.get(1); }
    public int getMaxEnergy() { return data.get(2); }
    public int getProgress() { return data.get(3); }
    public int getSteamAmount() { return data.get(4); }
    public int getDistilledAmount() { return data.get(5); }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if (blockEntity instanceof mio_icif_condenser condenser) {
            setSyncData(0, condenser.isWorking() ? 1 : 0);
            setSyncData(1, (int) condenser.getEnergyStorage().getAmount());
            setSyncData(2, (int) condenser.getEnergyStorage().getCapacity());
            setSyncData(3, condenser.getProgress());
            setSyncData(4, condenser.getSteamTank().getFluidAmount());
            setSyncData(5, condenser.getDistilledTank().getFluidAmount());
        }
    }

    /** 0.1.10.6: the progress is at index 3 of the synced data. */
    @Override protected int progressDataIndex() { return 3; }

    // 0.1.11.16: the 248 x 204 mimic screen
    @Override
    protected int getPlayerInventoryY() { return 162; }   // 0.1.11.121: under the readout window

    @Override
    protected int getPlayerHotbarY() { return 220; }

    @Override
    protected void addPlayerInventory(net.minecraft.world.entity.player.Inventory playerInventory, int startY, int hotbarY) {
        for (int i = 0; i < 3; ++i) for (int j = 0; j < 9; ++j)
            this.addSlot(new net.minecraft.world.inventory.Slot(playerInventory, j + i * 9 + 9, 44 + j * 18, startY + i * 18));
        for (int i = 0; i < 9; ++i) this.addSlot(new net.minecraft.world.inventory.Slot(playerInventory, i, 44 + i * 18, hotbarY));
    }
}
