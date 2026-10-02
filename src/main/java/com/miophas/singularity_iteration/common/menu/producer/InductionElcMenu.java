package com.miophas.singularity_iteration.common.menu.producer;

import com.miophas.singularity_iteration.common.registry.mio_icif_menus;
import com.miophas.singularity_iteration.common.blockentity.producer.mio_icif_induction_elc;
import com.miophas.singularity_iteration.core.api.MioIcifAPI;
import com.miophas.singularity_iteration.common.menu.base.mio_icif_machine_menu;
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
        super(mio_icif_menus.INDUCTION_ELC_MENU_TYPE.get(), containerId, playerInventory, blockEntity, SLOT_COUNT, 9);
    }

    @Override
    protected void addMachineSlots() {
        this.addSlot(new SlotItemHandler(itemHandler, INPUT_SLOT_1, 37, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return itemHandler.isItemValid(INPUT_SLOT_1, stack);
            }
        });
        this.addSlot(new SlotItemHandler(itemHandler, INPUT_SLOT_2, 55, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return itemHandler.isItemValid(INPUT_SLOT_2, stack);
            }
        });
        this.addSlot(new SlotItemHandler(itemHandler, BATTERY_SLOT, 17, 58) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return MioIcifAPI.instance().getItemAPI().isBattery(stack);
            }
        });
        this.addSlot(new SlotItemHandler(itemHandler, OUTPUT_SLOT_1, 115, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }

            @Override
            public void onTake(net.minecraft.world.entity.player.Player player, ItemStack stack) {
                super.onTake(player, stack);
                collectXp(player);   // taking smelted items pours the banked XP straight into the XP bar
            }
        });
        this.addSlot(new SlotItemHandler(itemHandler, OUTPUT_SLOT_2, 133, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }

            @Override
            public void onTake(net.minecraft.world.entity.player.Player player, ItemStack stack) {
                super.onTake(player, stack);
                collectXp(player);   // taking smelted items pours the banked XP straight into the XP bar
            }
        });
        this.addUpgradeSlot(UPGRADE_SLOT_1, 152, 26);
        this.addUpgradeSlot(UPGRADE_SLOT_2, 152, 44);
    }

    @Override
    protected int getBatterySlotIndex() { return BATTERY_SLOT; }

    @Override
    protected int getDataSlotCount() { return 9; }

    public int getProgress1() { return data.get(0); }
    public int getMaxProgress1() { return data.get(1); }
    public int getProgress2() { return data.get(2); }
    public int getMaxProgress2() { return data.get(3); }
    public boolean isWorking() { return data.get(4) == 1; }
    public int getHeatPercent() { return data.get(7); }
    /** Banked XP x10. */
    public int getStoredXpTenths() { return data.get(8); }

    public static final int BUTTON_COLLECT_XP = 0;

    void collectXp(net.minecraft.world.entity.player.Player player) {
        if (blockEntity instanceof mio_icif_induction_elc induction && player instanceof net.minecraft.server.level.ServerPlayer sp) induction.collectExperience(sp);
    }

    @Override
    public boolean clickMenuButton(net.minecraft.world.entity.player.Player player, int id) {
        if (id != BUTTON_COLLECT_XP) return false;
        collectXp(player);
        return true;
    }

    @Override
    public void removed(net.minecraft.world.entity.player.Player player) {
        super.removed(player);
        collectXp(player);
    }

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
            this.setSyncData(8, (int) Math.min(Integer.MAX_VALUE, induction.getStoredExperience() * 10));
        }
    }
}