package com.singularity_iteration.mio_icif.Menu.Producer;

import com.singularity_iteration.mio_icif.Menu.mio_icif_menus;
import com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_induction_elc;
import com.singularity_iteration.mio_icif.api.MioIcifAPI;
import com.singularity_iteration.mio_icif.Menu.Base.mio_icif_machine_menu;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings({"null"}) public class InductionElcMenu extends mio_icif_machine_menu {
    public static final int INPUT_SLOT_1 = 0;
    public static final int INPUT_SLOT_2 = 1;
    public static final int BATTERY_SLOT = 2;
    public static final int OUTPUT_SLOT_1 = 3;
    public static final int OUTPUT_SLOT_2 = 4;
    public static final int UPGRADE_SLOT_1 = 5;
    public static final int UPGRADE_SLOT_2 = 6;
    public static final int SLOT_COUNT = 7;

    public InductionElcMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, null);
    }

    public InductionElcMenu(int containerId, Inventory playerInventory, @Nullable mio_icif_induction_elc blockEntity) {
        super(mio_icif_menus.INDUCTION_ELC_MENU_TYPE.get(), containerId, playerInventory, blockEntity, SLOT_COUNT, 8);
    }

    @Override
    protected void addMachineSlots() {
        this.addSlot(new SlotItemHandler(itemHandler, INPUT_SLOT_1, 47, 17) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                if (blockEntity instanceof mio_icif_induction_elc induction) {
                    return induction.isItemValidForSlot(INPUT_SLOT_1, stack);
                }
                return true;
            }
        });
        this.addSlot(new SlotItemHandler(itemHandler, INPUT_SLOT_2, 64, 17) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                if (blockEntity instanceof mio_icif_induction_elc induction) {
                    return induction.isItemValidForSlot(INPUT_SLOT_2, stack);
                }
                return true;
            }
        });
        this.addSlot(new SlotItemHandler(itemHandler, BATTERY_SLOT, 56, 53) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return MioIcifAPI.instance().getItemAPI().isBattery(stack);
            }
        });
        this.addSlot(new SlotItemHandler(itemHandler, OUTPUT_SLOT_1, 113, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
        this.addSlot(new SlotItemHandler(itemHandler, OUTPUT_SLOT_2, 131, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
        this.addUpgradeSlot(UPGRADE_SLOT_1, 153, 26);
        this.addUpgradeSlot(UPGRADE_SLOT_2, 153, 44);
    }

    @Override
    protected int getBatterySlotIndex() { return BATTERY_SLOT; }

    @Override
    protected int getDataSlotCount() { return 8; }

    public int getProgress1() { return data.get(0); }
    public int getMaxProgress1() { return data.get(1); }
    public int getProgress2() { return data.get(2); }
    public int getMaxProgress2() { return data.get(3); }
    public boolean isWorking() { return data.get(4) == 1; }
    public int getHeatPercent() { return data.get(7); }

    @Override
    public int getEnergy() { return data.get(5); }

    @Override
    public int getMaxEnergy() { return data.get(6); }

    public int getProgressPixels() {
        int progress = getProgress1();
        int maxProgress = getMaxProgress1();
        if (maxProgress == 0) return 0;
        return (progress * 24) / maxProgress;
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if (blockEntity instanceof mio_icif_induction_elc induction) {
            this.setSyncData(0, induction.getProgress1());
            this.setSyncData(1, induction.getMaxProgress1());
            this.setSyncData(2, induction.getProgress2());
            this.setSyncData(3, induction.getMaxProgress2());
            this.setSyncData(4, induction.isWorking() ? 1 : 0);
            this.setSyncData(5, (int) induction.getEnergyStorage().getAmount());
            this.setSyncData(6, (int) induction.getEnergyStorage().getCapacity());
            this.setSyncData(7, induction.getHeatPercent());
        }
    }
}