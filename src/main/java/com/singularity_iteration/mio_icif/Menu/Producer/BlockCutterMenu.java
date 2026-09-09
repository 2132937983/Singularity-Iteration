package com.singularity_iteration.mio_icif.Menu.Producer;

import com.singularity_iteration.mio_icif.Menu.mio_icif_menus;
import com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_block_cutter;
import com.singularity_iteration.mio_icif.api.MioIcifAPI;
import com.singularity_iteration.mio_icif.Menu.Base.mio_icif_machine_menu;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings({"null"}) public class BlockCutterMenu extends mio_icif_machine_menu {
    public static final int INPUT_SLOT = 0;
    public static final int BLADE_SLOT = 1;
    public static final int OUTPUT_SLOT = 2;
    public static final int BATTERY_SLOT = 3;
    public static final int UPGRADE_SLOT_1 = 4;
    public static final int UPGRADE_SLOT_2 = 5;
    public static final int UPGRADE_SLOT_3 = 6;
    public static final int UPGRADE_SLOT_4 = 7;
    public static final int SLOT_COUNT = 8;

    public BlockCutterMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, null);
    }

    public BlockCutterMenu(int containerId, Inventory playerInventory, @Nullable mio_icif_block_cutter blockEntity) {
        super(mio_icif_menus.BLOCK_CUTTER_MENU_TYPE.get(), containerId, playerInventory, blockEntity, SLOT_COUNT);
    }

    @Override
    protected void addMachineSlots() {
        this.addSlot(new SlotItemHandler(itemHandler, INPUT_SLOT, 26, 17));
        this.addSlot(new SlotItemHandler(itemHandler, BLADE_SLOT, 71, 37));
        this.addSlot(new SlotItemHandler(itemHandler, OUTPUT_SLOT, 116, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
        this.addSlot(new SlotItemHandler(itemHandler, BATTERY_SLOT, 26, 53) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return MioIcifAPI.instance().getItemAPI().isBattery(stack);
            }
        });
        for (int i = 0; i < 4; i++) {
            addUpgradeSlot(itemHandler, UPGRADE_SLOT_1 + i, 152, 8 + i * 18);
        }
    }

    @Override
    protected int getBatterySlotIndex() { return BATTERY_SLOT; }

    @Override
    protected int getDataSlotCount() { return 5; }

    public int getProgress() { return data.get(0); }
    public int getMaxProgress() { return data.get(1); }
    public boolean isWorking() { return data.get(2) == 1; }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if (blockEntity instanceof mio_icif_block_cutter cutter) {
            this.setSyncData(0, cutter.getProgress());
            this.setSyncData(1, cutter.getMaxProgress());
            this.setSyncData(2, cutter.isWorking() ? 1 : 0);
            this.setSyncData(3, (int) cutter.getEnergyStorage().getAmount());
            this.setSyncData(4, (int) cutter.getEnergyStorage().getCapacity());
        }
    }
}