package com.miophas.singularity_iteration.common.menu.producer;

import com.miophas.singularity_iteration.common.menu.MergedSlots;
import com.miophas.singularity_iteration.common.registry.mio_icif_menus;
import com.miophas.singularity_iteration.common.blockentity.producer.mio_icif_centrifuge_elc;
import com.miophas.singularity_iteration.common.menu.base.mio_icif_machine_menu;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings({"null"}) public class CentrifugeElcMenu extends mio_icif_machine_menu {
    public static final int INPUT_SLOT = 0;
    public static final int OUTPUT_SLOT_1 = 1;
    public static final int OUTPUT_SLOT_2 = 2;
    public static final int OUTPUT_SLOT_3 = 3;
    public static final int BATTERY_SLOT = 4;
    public static final int UPGRADE_SLOT_START = 5;
    public static final int SLOT_COUNT = 9;

    public CentrifugeElcMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, null);
    }

    public CentrifugeElcMenu(int containerId, Inventory playerInventory, @Nullable mio_icif_centrifuge_elc blockEntity) {
        super(mio_icif_menus.CENTRIFUGE_ELC_MENU_TYPE.get(), containerId, playerInventory, blockEntity, SLOT_COUNT, 11);
        registerMachineMoveRules();
    }

    @Override
    protected void addMachineSlots() {
        addInputSlot(INPUT_SLOT, MergedSlots.Centrifuge.IN_X, MergedSlots.Centrifuge.IN_Y);   // 0.1.11.114: two columns on the left of the instrument screen
        this.addSlot(new SlotItemHandler(itemHandler, OUTPUT_SLOT_1, MergedSlots.Centrifuge.OUT_X, MergedSlots.Centrifuge.OUT_TOP_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) { return false; }
        });
        this.addSlot(new SlotItemHandler(itemHandler, OUTPUT_SLOT_2, MergedSlots.Centrifuge.OUT_X, MergedSlots.Centrifuge.OUT_TOP_Y + MergedSlots.Centrifuge.OUT_STEP) {
            @Override
            public boolean mayPlace(ItemStack stack) { return false; }
        });
        this.addSlot(new SlotItemHandler(itemHandler, OUTPUT_SLOT_3, MergedSlots.Centrifuge.OUT_X, MergedSlots.Centrifuge.OUT_TOP_Y + 2 * MergedSlots.Centrifuge.OUT_STEP) {
            @Override
            public boolean mayPlace(ItemStack stack) { return false; }
        });
        addBatterySlot(BATTERY_SLOT, MergedSlots.BATTERY_X, MergedSlots.BATTERY_Y);   // 0.1.11.118: the furnace block
        this.addUpgradeSlot(UPGRADE_SLOT_START, 152, 8);
        this.addUpgradeSlot(UPGRADE_SLOT_START + 1, 152, 26);
        this.addUpgradeSlot(UPGRADE_SLOT_START + 2, 152, 44);
        this.addUpgradeSlot(UPGRADE_SLOT_START + 3, 152, 62);
    }

    @Override
    protected int getBatterySlotIndex() { return BATTERY_SLOT; }

    @Override
    protected int getInputSlotStart() {
        return INPUT_SLOT;
    }

    @Override
    protected int getInputSlotCount() {
        return 1;
    }

    @Override
    protected int getOutputSlotStart() {
        return OUTPUT_SLOT_1;
    }

    @Override
    protected int getOutputSlotCount() {
        return 3;
    }

    @Override
    protected int getUpgradeSlotStart() {
        return UPGRADE_SLOT_START;
    }

    @Override
    protected int getUpgradeSlotCount() {
        return 4;
    }

    @Override
    protected int getDataSlotCount() { return 11; }

    /** 0.1.11.114: data 7 status (ProcessStatus), 8 heat the input needs HU, 9 rotor speed ‰, 10 stored energy ‰ (0-6 as before). */
    public int getStatus() { return data.get(7); }
    public int getRequiredHeat() { return data.get(8); }
    public int getRotorSpeedPermille() { return data.get(9); }
    public int getEnergyPermille() { return data.get(10); }

    // ---- 0.1.11.114: the instrument screen layout (248 x 204; docs/gui_processing_machines.md) ----

    public static final int GUI_WIDTH = 176, GUI_HEIGHT = 240, INVENTORY_X = 8, INVENTORY_Y = 160, HOTBAR_Y = 218;   // 0.1.11.180: the vanilla width

    @Override
    protected int getPlayerInventoryY() { return INVENTORY_Y; }

    @Override
    protected int getPlayerHotbarY() { return HOTBAR_Y; }

    @Override
    protected void addPlayerInventory(Inventory playerInventory, int startY, int hotbarY) {
        for (int i = 0; i < 3; ++i) for (int j = 0; j < 9; ++j)
            this.addSlot(new net.minecraft.world.inventory.Slot(playerInventory, j + i * 9 + 9, INVENTORY_X + j * 18, startY + i * 18));
        for (int i = 0; i < 9; ++i) this.addSlot(new net.minecraft.world.inventory.Slot(playerInventory, i, INVENTORY_X + i * 18, hotbarY));
    }


    public int getProgress() { return data.get(0); }
    public int getMaxProgress() { return data.get(1); }
    public boolean isWorking() { return data.get(2) == 1; }
    public int getEnergy() { return data.get(3); }
    public int getMaxEnergy() { return data.get(4); }
    public int getHeat() { return data.get(5); }
    public int getMaxHeat() { return data.get(6); }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if (blockEntity instanceof mio_icif_centrifuge_elc centrifuge) {
            this.setSyncData(0, centrifuge.getProgress());
            this.setSyncData(1, centrifuge.getMaxProgress());
            this.setSyncData(2, centrifuge.isWorking() ? 1 : 0);
            this.setSyncData(3, (int) centrifuge.getEnergyStorage().getAmount());
            this.setSyncData(4, (int) centrifuge.getEnergyStorage().getCapacity());
            this.setSyncData(5, centrifuge.getHeatStorage());
            this.setSyncData(6, mio_icif_centrifuge_elc.MAX_HEAT);
            this.setSyncData(7, centrifuge.processStatus());
            this.setSyncData(8, centrifuge.requiredHeat());
            this.setSyncData(9, (int) Math.round(centrifuge.getRotorSpeed() * 1000));
            this.setSyncData(10, (int) Math.round(1000.0 * centrifuge.getEnergyStorage().getAmount() / Math.max(1, centrifuge.getEnergyStorage().getCapacity())));
        }
    }
}
