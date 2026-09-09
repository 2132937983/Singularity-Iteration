package com.singularity_iteration.mio_icif.Menu.Producer;

import com.singularity_iteration.mio_icif.Menu.mio_icif_menus;
import com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_centrifuge_elc;
import com.singularity_iteration.mio_icif.Menu.Base.mio_icif_machine_menu;
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
        super(mio_icif_menus.CENTRIFUGE_ELC_MENU_TYPE.get(), containerId, playerInventory, blockEntity, SLOT_COUNT, 7);
        registerMachineMoveRules();
    }

    @Override
    protected void addMachineSlots() {
        this.addSlot(new SlotItemHandler(itemHandler, INPUT_SLOT, 11, 21) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return true;
            }
        });
        this.addSlot(new SlotItemHandler(itemHandler, OUTPUT_SLOT_1, 124, 18) {
            @Override
            public boolean mayPlace(ItemStack stack) { return false; }
        });
        this.addSlot(new SlotItemHandler(itemHandler, OUTPUT_SLOT_2, 124, 36) {
            @Override
            public boolean mayPlace(ItemStack stack) { return false; }
        });
        this.addSlot(new SlotItemHandler(itemHandler, OUTPUT_SLOT_3, 124, 54) {
            @Override
            public boolean mayPlace(ItemStack stack) { return false; }
        });
        this.addSlot(new SlotItemHandler(itemHandler, BATTERY_SLOT, 11, 60) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof com.singularity_iteration.mio_icif.Items.Normal.mio_icif_bat;
            }
        });
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
    protected int getDataSlotCount() { return 7; }

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
        }
    }
}