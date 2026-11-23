package com.miophas.singularity_iteration.common.menu.producer;

import com.miophas.singularity_iteration.common.registry.mio_icif_menus;
import com.miophas.singularity_iteration.common.blockentity.producer.mio_icif_pump_elc;
import com.miophas.singularity_iteration.common.menu.base.mio_icif_machine_menu;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.Nullable;

/**
 * 电力泵的容器菜单 - 对齐原版IC2
 * 槽位布局:
 * 0: 电池槽
 * 1-4: 4个升级槽
 * 5: 空容器输入槽(顶部)
 * 6: 填充容器输出槽(侧面)
 */
@SuppressWarnings({"null"}) public class PumpElcMenu extends mio_icif_machine_menu {

    // 槽位索引 - 对齐原版IC2
    public static final int SLOT_BATTERY = 0;
    public static final int SLOT_UPGRADE_START = 1;
    public static final int SLOT_UPGRADE_COUNT = 4;
    public static final int SLOT_UPGRADE_END = SLOT_UPGRADE_START + SLOT_UPGRADE_COUNT; // 5
    public static final int SLOT_CONTAINER_INPUT = 5;  // 空容器输入
    public static final int SLOT_OUTPUT = 6;           // 填充容器输出
    public static final int SLOT_COUNT = 7;

    /** 0.1.11.7: 248 x 222 screen with the cross-section view; machine slots in one row under the controls. */
    public static final int GUI_WIDTH = 176, GUI_HEIGHT = 240;   // 0.1.11.181: the vanilla width (was 248 x 222)
    /** 0.1.11.181: the battery under its bolt; the empty container and the filled one either side of the filler fed by the tank. */
    public static final int BATTERY_X = 10, BATTERY_Y = 92;
    public static final int CONTAINER_INPUT_X = 56, OUTPUT_X = 92, CONTAINER_Y = 86;
    public static final int UPGRADE_START_X = 167;
    public static final int INVENTORY_X = 8, INVENTORY_Y = 160, HOTBAR_Y = 218;

    private static final int DATA_PROGRESS = 0;
    private static final int DATA_MAX_PROGRESS = 1;
    private static final int DATA_ENERGY = 2;
    private static final int DATA_MAX_ENERGY = 3;
    private static final int DATA_FLUID_AMOUNT = 4;
    private static final int DATA_FLUID_CAPACITY = 5;
    private static final int DATA_FLUID_ID = 6;
    private static final int DATA_COUNT = com.miophas.singularity_iteration.common.blockentity.producer.mio_icif_pump_elc.DATA_COUNT;

    public PumpElcMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, null, null);
    }

    public PumpElcMenu(int containerId, Inventory playerInventory, @Nullable IItemHandler itemHandler) {
        this(containerId, playerInventory, itemHandler, null);
    }

    public PumpElcMenu(int containerId, Inventory playerInventory, @Nullable IItemHandler itemHandler, @Nullable ContainerData data) {
        super(mio_icif_menus.PUMP_ELC_MENU_TYPE.get(), containerId, SLOT_COUNT, playerInventory, itemHandler, data, DATA_COUNT);
    }

    public PumpElcMenu(int containerId, Inventory playerInventory, @Nullable mio_icif_pump_elc blockEntity) {
        super(mio_icif_menus.PUMP_ELC_MENU_TYPE.get(), containerId, SLOT_COUNT, playerInventory,
              blockEntity != null ? blockEntity.getItemHandler() : null,
              blockEntity != null ? blockEntity.getContainerData() : null, DATA_COUNT, blockEntity);
    }

    public int getFluidAmount() {
        return this.data.get(DATA_FLUID_AMOUNT);
    }

    public int getFluidCapacity() {
        return this.data.get(DATA_FLUID_CAPACITY);
    }

    public int getFluidId() {
        return this.data.get(DATA_FLUID_ID);
    }

    public FluidStack getFluidStack() {
        int fluidId = getFluidId();
        if (fluidId == -1) return FluidStack.EMPTY;
        var fluid = BuiltInRegistries.FLUID.byId(fluidId);
        return fluid != null ? new FluidStack(fluid, getFluidAmount()) : FluidStack.EMPTY;
    }

    public void setSyncData(int progress, int maxProgress, int energy, int maxEnergy) {
        this.data.set(DATA_PROGRESS, progress);
        this.data.set(DATA_MAX_PROGRESS, maxProgress);
        this.data.set(DATA_ENERGY, energy);
        this.data.set(DATA_MAX_ENERGY, maxEnergy);
    }

    public int getProgress() {
        return this.data.get(DATA_PROGRESS);
    }

    public int getMaxProgress() {
        return this.data.get(DATA_MAX_PROGRESS);
    }

    public int getEnergy() {
        return this.data.get(DATA_ENERGY);
    }

    public int getMaxEnergy() {
        return this.data.get(DATA_MAX_ENERGY);
    }

    /** Synced value {@code i} (see {@code mio_icif_pump_elc.getContainerData}). */
    public int value(int i) {
        return this.data.get(i);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (!(getBlockEntity() instanceof mio_icif_pump_elc pump) || !stillValid(player)) return false;
        return pump.handleButton(id);
    }

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

    @Override
    protected void addMachineSlots() {
        // 电池槽 (152, 62)
        this.addSlot(new SlotItemHandler(itemHandler, SLOT_BATTERY, BATTERY_X, BATTERY_Y));
        
        // 4个升级槽 (152, 8) 开始，垂直排列
        for (int i = 0; i < SLOT_UPGRADE_COUNT; i++) {
            addUpgradeSlot(itemHandler, SLOT_UPGRADE_START + i, UPGRADE_START_X + i * 18, CONTAINER_Y);   // docked
        }
        
        // 空容器输入槽 (44, 35) - 顶部位置
        this.addSlot(new SlotItemHandler(itemHandler, SLOT_CONTAINER_INPUT, CONTAINER_INPUT_X, CONTAINER_Y));
        
        // 填充容器输出槽 (116, 35) - 侧面位置
        this.addSlot(new SlotItemHandler(itemHandler, SLOT_OUTPUT, OUTPUT_X, CONTAINER_Y) {
            @Override
            public boolean mayPlace(net.minecraft.world.item.ItemStack stack) {
                return false;
            }
        });
    }
}