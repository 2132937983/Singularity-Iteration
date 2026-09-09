package com.singularity_iteration.mio_icif.Menu.Generator;

import com.singularity_iteration.mio_icif.Blocks.entity.KUEntity.KUGenerator.mio_icif_steam_kinetic_generator;
import com.singularity_iteration.mio_icif.Menu.mio_icif_menus;
import com.singularity_iteration.mio_icif.Menu.Base.mio_icif_generator_menu;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.Nullable;

/**
 * 蒸汽动能发生机的容器菜单独?
 */
@SuppressWarnings({"null"}) public class SteamKineticGeneratorMenu extends mio_icif_generator_menu {

    private static final int TURBINE_X = 80;
    private static final int TURBINE_Y = 26;
    private static final int UPGRADE_X = 152;
    private static final int UPGRADE_Y = 26;

    // 数据同步索引
    private static final int DATA_IS_HOT_STEAM = 0;
    private static final int DATA_HAS_TURBINE = 1;
    private static final int DATA_IS_TURBINE_FILLED = 2;
    private static final int DATA_IS_WORKING = 3;
    private static final int DATA_LAST_OUTPUT = 4;
    private static final int DATA_GAUGE_LIQUID = 5;
    private static final int DATA_WATER_AMOUNT = 6;
    private static final int DATA_COUNT = 7;
    private static final int SLOT_COUNT = 5; // extra(2) + turbine(1) + upgrade(2)

    public SteamKineticGeneratorMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, null, null);
    }

    public SteamKineticGeneratorMenu(int containerId, Inventory playerInventory, @Nullable IItemHandler itemHandler) {
        this(containerId, playerInventory, itemHandler, null);
    }

    public SteamKineticGeneratorMenu(int containerId, Inventory playerInventory, @Nullable IItemHandler itemHandler, @Nullable ContainerData data) {
        super(mio_icif_menus.STEAM_KINETIC_GENERATOR_MENU_TYPE.get(), containerId, SLOT_COUNT, playerInventory, itemHandler, data, DATA_COUNT);
    }

    public SteamKineticGeneratorMenu(int containerId, Inventory playerInventory, @Nullable mio_icif_steam_kinetic_generator blockEntity) {
        super(mio_icif_menus.STEAM_KINETIC_GENERATOR_MENU_TYPE.get(), containerId, SLOT_COUNT, playerInventory,
              blockEntity != null ? blockEntity.getItemHandler() : null,
              blockEntity != null ? blockEntity.getContainerData() : null, DATA_COUNT, blockEntity);
    }

    public void setSyncData(boolean isHotSteam, boolean hasTurbine, boolean isTurbineFilled, boolean isWorking, int lastOutput, int gaugeLiquid, int waterAmount) {
        this.data.set(DATA_IS_HOT_STEAM, isHotSteam ? 1 : 0);
        this.data.set(DATA_HAS_TURBINE, hasTurbine ? 1 : 0);
        this.data.set(DATA_IS_TURBINE_FILLED, isTurbineFilled ? 1 : 0);
        this.data.set(DATA_IS_WORKING, isWorking ? 1 : 0);
        this.data.set(DATA_LAST_OUTPUT, lastOutput);
        this.data.set(DATA_GAUGE_LIQUID, gaugeLiquid);
        this.data.set(DATA_WATER_AMOUNT, waterAmount);
    }

    public boolean isHotSteam() {
        return this.data.get(DATA_IS_HOT_STEAM) == 1;
    }

    public boolean hasTurbine() {
        return this.data.get(DATA_HAS_TURBINE) == 1;
    }

    public boolean isTurbineFilledWithWater() {
        return this.data.get(DATA_IS_TURBINE_FILLED) == 1;
    }

    public boolean isWorking() {
        return this.data.get(DATA_IS_WORKING) == 1;
    }

    public int getLastKineticOutput() {
        return this.data.get(DATA_LAST_OUTPUT);
    }

    public int getGaugeLiquidScaled26() {
        return this.data.get(DATA_GAUGE_LIQUID);
    }

    public int getWaterAmount() {
        return this.data.get(DATA_WATER_AMOUNT);
    }

    @Override
    protected void addMachineSlots() {
        // 额外槽位 0-1 (extra(2)) - 用于蒸汽单元输入和蒸馏水单元输出
        // 这些槽位由基类根据 SlotLayout 自动添加，这里不需要手动添加

        // 涡轮槽(80, 26) - 索引 2
        // mayPlace 由 MachineItemHandler.isItemValid 处理
        this.addSlot(new SlotItemHandler(itemHandler, mio_icif_steam_kinetic_generator.TURBINE_SLOT, TURBINE_X, TURBINE_Y));

        // 升级槽 3-4 (152, 26) 和 (152, 44)
        // mayPlace 由 MachineItemHandler.isItemValid 处理
        this.addUpgradeSlot(mio_icif_steam_kinetic_generator.UPGRADE_SLOT_START, UPGRADE_X, UPGRADE_Y);
        this.addUpgradeSlot(mio_icif_steam_kinetic_generator.UPGRADE_SLOT_START + 1, UPGRADE_X, UPGRADE_Y + 18);
    }



    @Override
    public void broadcastChanges() {
        if (blockEntity instanceof mio_icif_steam_kinetic_generator generator) {
            setSyncData(
                generator.isHotSteam(),
                generator.hasTurbine(),
                generator.isTurbineFilledWithWater(),
                generator.isWorking(),
                generator.getLastKineticOutput(),
                generator.gaugeLiquidScaled(26, 0),
                generator.getTank().getFluidAmount()
            );
        }
        super.broadcastChanges();
    }
}