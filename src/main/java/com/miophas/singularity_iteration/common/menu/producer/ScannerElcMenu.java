package com.miophas.singularity_iteration.common.menu.producer;

import com.miophas.singularity_iteration.common.registry.mio_icif_menus;
import com.miophas.singularity_iteration.common.blockentity.producer.mio_icif_scanner_elc;
import com.miophas.singularity_iteration.common.menu.base.mio_icif_machine_menu;
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
    private static final int DATA_MAX_ENERGY = 5;
    private static final int DATA_SCAN_COMPLETE = 7;
    private static final int DATA_STATE = 8;
    private static final int DATA_UU_COST = 9;
    private static final int DATA_EU_COST = 13;
    private static final int DATA_COUNT = 15;

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
        com.miophas.singularity_iteration.core.runtime.processing.PatternMenuData.write(data,DATA_ENERGY,2,energy);
        com.miophas.singularity_iteration.core.runtime.processing.PatternMenuData.write(data,DATA_MAX_ENERGY,2,maxEnergy);
        this.data.set(DATA_SCAN_COMPLETE, scanComplete ? 1 : 0);
        this.data.set(DATA_STATE, state);
        com.miophas.singularity_iteration.core.runtime.processing.PatternMenuData.write(data,DATA_UU_COST,4,((long)uuCostHi<<32)|(uuCostLo&0xFFFFFFFFL));
        com.miophas.singularity_iteration.core.runtime.processing.PatternMenuData.write(data,DATA_EU_COST,2,euCost);
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
        return (int)com.miophas.singularity_iteration.core.runtime.processing.PatternMenuData.read(data,DATA_ENERGY,2);
    }

    public int getMaxEnergy() {
        return (int)com.miophas.singularity_iteration.core.runtime.processing.PatternMenuData.read(data,DATA_MAX_ENERGY,2);
    }

    public boolean isScanComplete() {
        return this.data.get(DATA_SCAN_COMPLETE) == 1;
    }

    public int getState() {
        return this.data.get(DATA_STATE);
    }

    public double getUuCost() {
        long bits = com.miophas.singularity_iteration.core.runtime.processing.PatternMenuData.read(data,DATA_UU_COST,4);
        return Double.longBitsToDouble(bits);
    }

    public int getEuCost() {
        return (int)com.miophas.singularity_iteration.core.runtime.processing.PatternMenuData.read(data,DATA_EU_COST,2);
    }

    @Override
    public boolean clickMenuButton(Player player, int buttonId) {
        if (this.blockEntity instanceof mio_icif_scanner_elc scanner
                && com.miophas.singularity_iteration.core.runtime.processing.MachineMenuAccess.action(player,this,scanner)) {
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
        return false;
    }

    @Override public boolean stillValid(Player player) {
        return player.level().isClientSide() || this.blockEntity instanceof mio_icif_scanner_elc scanner
            && com.miophas.singularity_iteration.core.runtime.processing.MachineMenuAccess.valid(player,scanner);
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
