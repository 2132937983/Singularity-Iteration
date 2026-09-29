package com.miophas.singularity_iteration.common.menu.generator;

import com.miophas.singularity_iteration.common.blockentity.kuentity.kugenerator.mio_icif_steam_kinetic_generator;
import com.miophas.singularity_iteration.common.registry.mio_icif_menus;
import com.miophas.singularity_iteration.common.menu.base.mio_icif_generator_menu;
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
    private static final int DATA_IS_THROTTLED = 7;
    private static final int DATA_IS_VENTING = 8;
    private static final int DATA_COUNT = 9;
    private static final int SLOT_COUNT = 5; // extra(2,已雪藏) + turbine(1) + upgrade(2,原版只用 1)

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

    public void setSyncData(boolean isHotSteam, boolean hasTurbine, boolean isTurbineFilled, boolean isWorking,
                            int lastOutput, int gaugeLiquid, int waterAmount, boolean throttled, boolean venting) {
        this.data.set(DATA_IS_HOT_STEAM, isHotSteam ? 1 : 0);
        this.data.set(DATA_HAS_TURBINE, hasTurbine ? 1 : 0);
        this.data.set(DATA_IS_TURBINE_FILLED, isTurbineFilled ? 1 : 0);
        this.data.set(DATA_IS_WORKING, isWorking ? 1 : 0);
        this.data.set(DATA_LAST_OUTPUT, lastOutput);
        this.data.set(DATA_GAUGE_LIQUID, gaugeLiquid);
        this.data.set(DATA_WATER_AMOUNT, waterAmount);
        this.data.set(DATA_IS_THROTTLED, throttled ? 1 : 0);
        this.data.set(DATA_IS_VENTING, venting ? 1 : 0);
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

    /** 原版 IC2 {@code throttled}：蒸馏水槽非空（KU 输出按水量衰减）。 */
    public boolean isThrottled() {
        return this.data.get(DATA_IS_THROTTLED) == 1;
    }

    /** 原版 IC2 {@code ventingSteam}：蒸汽排不出去（有爆炸风险）。 */
    public boolean isVentingSteam() {
        return this.data.get(DATA_IS_VENTING) == 1;
    }

    @Override
    protected void addMachineSlots() {
        // 雪藏：槽位 0-1（蒸汽单元输入 / 蒸馏水单元输出）不是原版 IC2 机制，
        // 槽位定义保留在 SlotLayout/NBT 里，但不再加入 GUI，也不再接受物品。

        // 涡轮槽(80, 26) - 索引 2（原版位置一致）
        // mayPlace 由 MachineItemHandler.isItemValid 处理
        this.addSlot(new SlotItemHandler(itemHandler, mio_icif_steam_kinetic_generator.TURBINE_SLOT, TURBINE_X, TURBINE_Y));

        // 升级槽 (152, 26) - 原版 IC2 只有 1 个升级槽
        this.addUpgradeSlot(mio_icif_steam_kinetic_generator.UPGRADE_SLOT_START, UPGRADE_X, UPGRADE_Y);
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
                generator.getTank().getFluidAmount(),
                generator.isThrottled(),
                generator.isVentingSteam()
            );
        }
        super.broadcastChanges();
    }
}