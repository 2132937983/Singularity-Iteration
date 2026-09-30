package com.miophas.singularity_iteration.common.menu.producer;

import com.miophas.singularity_iteration.common.registry.mio_icif_menus;
import com.miophas.singularity_iteration.common.blockentity.producer.mio_icif_matron_elc;
import com.miophas.singularity_iteration.common.menu.base.mio_icif_machine_menu;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.Nullable;

/**
 * 作物监管机的容器菜单独?
 */
@SuppressWarnings({"null"}) public class MatronElcMenu extends mio_icif_machine_menu {

    public static final int BATTERY_SLOT = 0;
    public static final int WATER_CELL_INPUT_SLOT = 1;
    public static final int WATER_CELL_OUTPUT_SLOT = 2;
    public static final int FERTILIZER_START_SLOT = 3;
    public static final int WEEDEX_START_SLOT = 10;
    public static final int UPGRADE_START_SLOT = 17;
    public static final int SLOT_COUNT = 21;

    private static final int BATTERY_X = 134;
    private static final int BATTERY_Y = 80;
    private static final int WATER_CELL_X = 26;
    private static final int WATER_CELL_Y = 71;
    private static final int WATER_CELL_OUTPUT_X = 26;
    private static final int WATER_CELL_OUTPUT_Y = 89;
    private static final int WEEDEX_START_X = 26;
    private static final int WEEDEX_Y = 27;
    private static final int FERTILIZER_START_X = 26;
    private static final int FERTILIZER_Y = 50;

    private static final int DATA_ENERGY = 0;
    private static final int DATA_MAX_ENERGY = 1;
    private static final int DATA_FLUID_AMOUNT = 2;
    private static final int DATA_MAX_FLUID = 3;
    private static final int DATA_COUNT = 5;

    public MatronElcMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, null, null);
    }

    public MatronElcMenu(int containerId, Inventory playerInventory, @Nullable IItemHandler itemHandler) {
        this(containerId, playerInventory, itemHandler, null);
    }

    public MatronElcMenu(int containerId, Inventory playerInventory, @Nullable IItemHandler itemHandler, @Nullable ContainerData data) {
        super(mio_icif_menus.MATRON_ELC_MENU_TYPE.get(), containerId, SLOT_COUNT, playerInventory, itemHandler, data, DATA_COUNT);
    }

    public MatronElcMenu(int containerId, Inventory playerInventory, @Nullable mio_icif_matron_elc blockEntity) {
        super(mio_icif_menus.MATRON_ELC_MENU_TYPE.get(), containerId, SLOT_COUNT, playerInventory,
              blockEntity != null ? blockEntity.getItemHandler() : null,
              blockEntity != null ? blockEntity.getContainerData() : null, DATA_COUNT, blockEntity);
    }

    public void setSyncData(int energy, int maxEnergy, int fluidAmount, int maxFluid) {
        this.data.set(DATA_ENERGY, energy);
        this.data.set(DATA_MAX_ENERGY, maxEnergy);
        this.data.set(DATA_FLUID_AMOUNT, fluidAmount);
        this.data.set(DATA_MAX_FLUID, maxFluid);
    }

    public int getEnergy() {
        return this.data.get(DATA_ENERGY);
    }

    public int getMaxEnergy() {
        return this.data.get(DATA_MAX_ENERGY);
    }

    public int getFluidAmount() {
        return this.data.get(DATA_FLUID_AMOUNT);
    }

    public int getWeedExAmount() { return this.data.get(4); }

    public int getMaxFluid() {
        return this.data.get(DATA_MAX_FLUID);
    }

    @Override
    protected int getPlayerInventoryY() { return 110; }

    @Override
    protected int getPlayerHotbarY() { return 168; }

    @Override
    protected void addMachineSlots() {
        this.addSlot(new SlotItemHandler(itemHandler, BATTERY_SLOT, BATTERY_X, BATTERY_Y));
        this.addSlot(new SlotItemHandler(itemHandler, WATER_CELL_INPUT_SLOT, WATER_CELL_X, WATER_CELL_Y));
        this.addSlot(new SlotItemHandler(itemHandler, WATER_CELL_OUTPUT_SLOT, WATER_CELL_OUTPUT_X, WATER_CELL_OUTPUT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
        for (int i = 0; i < 7; i++) {
            this.addSlot(new SlotItemHandler(itemHandler, WEEDEX_START_SLOT + i, WEEDEX_START_X + i * 18, WEEDEX_Y));
        }
        for (int i = 0; i < 7; i++) {
            this.addSlot(new SlotItemHandler(itemHandler, FERTILIZER_START_SLOT + i, FERTILIZER_START_X + i * 18, FERTILIZER_Y));
        }
        for (int i = 0; i < 4; i++) addUpgradeSlot(itemHandler, UPGRADE_START_SLOT + i, 180, 18 + i * 18);
    }
    @Override protected int getUpgradeSlotStart() { return UPGRADE_START_SLOT; }
    @Override protected int getUpgradeSlotCount() { return 4; }
}
