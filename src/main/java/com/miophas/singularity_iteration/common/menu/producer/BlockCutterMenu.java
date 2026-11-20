package com.miophas.singularity_iteration.common.menu.producer;

import com.miophas.singularity_iteration.common.registry.mio_icif_menus;
import com.miophas.singularity_iteration.common.blockentity.producer.mio_icif_block_cutter;
import com.miophas.singularity_iteration.core.api.MioIcifAPI;
import com.miophas.singularity_iteration.common.menu.base.mio_icif_machine_menu;
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
        super(mio_icif_menus.BLOCK_CUTTER_MENU_TYPE.get(), containerId, playerInventory, blockEntity, SLOT_COUNT, 9);
    }

    @Override
    protected void addMachineSlots() {
        this.addSlot(new SlotItemHandler(itemHandler, INPUT_SLOT, 10, 18));
        this.addSlot(new SlotItemHandler(itemHandler, BLADE_SLOT, 30, 18));
        this.addSlot(new SlotItemHandler(itemHandler, OUTPUT_SLOT, 61, 37) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
        this.addSlot(new SlotItemHandler(itemHandler, BATTERY_SLOT, 10, 56) {
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
    protected int getDataSlotCount() { return 9; }

    /** 0.1.11.115: data 5 status (ProcessStatus), 6 material hardness, 7 EU/t of the recipe, 8 stored energy ‰ (0-4 as before). */
    public int getStatus() { return data.get(5); }
    public int getMaterialHardness() { return data.get(6); }
    public int getPower() { return data.get(7); }
    public int getEnergyPermille() { return data.get(8); }

    // ---- 0.1.11.115: the instrument screen layout (248 x 204; docs/gui_processing_machines.md) ----

    public static final int GUI_WIDTH = 248, GUI_HEIGHT = 204, INVENTORY_X = 44, INVENTORY_Y = 124, HOTBAR_Y = 182;

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

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if (blockEntity instanceof mio_icif_block_cutter cutter) {
            this.setSyncData(0, cutter.getProgress());
            this.setSyncData(1, cutter.getMaxProgress());
            this.setSyncData(2, cutter.isWorking() ? 1 : 0);
            this.setSyncData(3, (int) cutter.getEnergyStorage().getAmount());
            this.setSyncData(4, (int) cutter.getEnergyStorage().getCapacity());
            this.setSyncData(5, cutter.processStatus());
            this.setSyncData(6, cutter.materialHardness());
            this.setSyncData(7, (int) Math.min(Short.MAX_VALUE, cutter.getEffectiveEnergyPerTick()));
            this.setSyncData(8, (int) Math.round(1000.0 * cutter.getEnergyStorage().getAmount() / Math.max(1, cutter.getEnergyStorage().getCapacity())));
        }
    }
}