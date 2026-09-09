package com.singularity_iteration.mio_icif.Menu.Producer;

import com.singularity_iteration.mio_icif.Menu.mio_icif_menus;
import com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_scanner_elc;
import com.singularity_iteration.mio_icif.Menu.Base.mio_icif_machine_menu;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.Nullable;

/**
 * 模式扫描机的容器菜单独?
 */
@SuppressWarnings({"null"}) public class ScannerElcMenu extends mio_icif_machine_menu {

    // 槽位索引
    public static final int SCANNER_SLOT = 0;
    public static final int BATTERY_SLOT = 1;
    public static final int MEMORY_SLOT = 2;
    public static final int SLOT_COUNT = 3;

    // 槽位位置
    private static final int SCANNER_X = 55;
    private static final int SCANNER_Y = 35;
    private static final int BATTERY_X = 8;
    private static final int BATTERY_Y = 43;
    private static final int MEMORY_X = 152;
    private static final int MEMORY_Y = 65;

    // 数据同步索引
    private static final int DATA_PROGRESS = 0;
    private static final int DATA_MAX_PROGRESS = 1;
    private static final int DATA_IS_WORKING = 2;
    private static final int DATA_ENERGY = 3;
    private static final int DATA_MAX_ENERGY = 4;
    private static final int DATA_SCAN_COMPLETE = 5;
    private static final int DATA_STATE = 6;
    private static final int DATA_UU_COST_HI = 7;
    private static final int DATA_UU_COST_LO = 8;
    private static final int DATA_EU_COST = 9;
    private static final int DATA_COUNT = 10;

    public ScannerElcMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, null, null);
    }

    public ScannerElcMenu(int containerId, Inventory playerInventory, @Nullable IItemHandler itemHandler) {
        this(containerId, playerInventory, itemHandler, null);
    }

    public ScannerElcMenu(int containerId, Inventory playerInventory, @Nullable IItemHandler itemHandler, @Nullable ContainerData data) {
        super(mio_icif_menus.SCANNER_ELC_MENU_TYPE.get(), containerId, SLOT_COUNT, playerInventory, itemHandler, data, DATA_COUNT);
    }

    public ScannerElcMenu(int containerId, Inventory playerInventory, @Nullable mio_icif_scanner_elc blockEntity) {
        super(mio_icif_menus.SCANNER_ELC_MENU_TYPE.get(), containerId, SLOT_COUNT, playerInventory,
              blockEntity != null ? blockEntity.getItemHandler() : null,
              blockEntity != null ? blockEntity.getContainerData() : null, DATA_COUNT, blockEntity);
    }

    public void setSyncData(int progress, int maxProgress, boolean isWorking, int energy, int maxEnergy, boolean scanComplete, int state, int uuCostHi, int uuCostLo, int euCost) {
        this.data.set(DATA_PROGRESS, progress);
        this.data.set(DATA_MAX_PROGRESS, maxProgress);
        this.data.set(DATA_IS_WORKING, isWorking ? 1 : 0);
        this.data.set(DATA_ENERGY, energy);
        this.data.set(DATA_MAX_ENERGY, maxEnergy);
        this.data.set(DATA_SCAN_COMPLETE, scanComplete ? 1 : 0);
        this.data.set(DATA_STATE, state);
        this.data.set(DATA_UU_COST_HI, uuCostHi);
        this.data.set(DATA_UU_COST_LO, uuCostLo);
        this.data.set(DATA_EU_COST, euCost);
    }

    public int getProgress() {
        return this.data.get(DATA_PROGRESS);
    }

    public int getMaxProgress() {
        return this.data.get(DATA_MAX_PROGRESS);
    }

    public boolean isWorking() {
        return this.data.get(DATA_IS_WORKING) == 1;
    }

    public int getEnergy() {
        return this.data.get(DATA_ENERGY);
    }

    public int getMaxEnergy() {
        return this.data.get(DATA_MAX_ENERGY);
    }

    public boolean isScanComplete() {
        return this.data.get(DATA_SCAN_COMPLETE) == 1;
    }

    public int getState() {
        return this.data.get(DATA_STATE);
    }

    public double getUuCost() {
        long bits = ((long) this.data.get(DATA_UU_COST_HI) << 32) | (this.data.get(DATA_UU_COST_LO) & 0xFFFFFFFFL);
        return Double.longBitsToDouble(bits);
    }

    public int getEuCost() {
        return this.data.get(DATA_EU_COST);
    }

    @Override
    public boolean clickMenuButton(Player player, int buttonId) {
        if (this.blockEntity instanceof mio_icif_scanner_elc scanner) {
            switch (buttonId) {
                case 0: // 删除/取消按钮
                    scanner.discardResult();
                    return true;
                case 1: // 保存按钮
                    return scanner.storeResult();
                default:
                    return false;
            }
        }
        return super.clickMenuButton(player, buttonId);
    }

    @Override
    protected void addMachineSlots() {
        // 扫描述?(55, 35)
        this.addSlot(new SlotItemHandler(itemHandler, SCANNER_SLOT, SCANNER_X, SCANNER_Y));
        // 电池�?(8, 43)
        this.addSlot(new SlotItemHandler(itemHandler, BATTERY_SLOT, BATTERY_X, BATTERY_Y));
        // 记忆�?(152, 65)
        this.addSlot(new SlotItemHandler(itemHandler, MEMORY_SLOT, MEMORY_X, MEMORY_Y));
    }
}