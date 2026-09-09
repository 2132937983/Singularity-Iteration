package com.singularity_iteration.mio_icif.Menu.Producer;

import com.singularity_iteration.mio_icif.Menu.mio_icif_menus;
import com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_pump_elc;
import com.singularity_iteration.mio_icif.Menu.Base.mio_icif_machine_menu;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Inventory;
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

    // GUI位置
    private static final int BATTERY_X = 8;
    private static final int BATTERY_Y = 44;
    private static final int UPGRADE_START_X = 152;
    private static final int UPGRADE_START_Y = 8;
    private static final int CONTAINER_INPUT_X = 99;
    private static final int CONTAINER_INPUT_Y = 17;
    private static final int OUTPUT_X = 132;
    private static final int OUTPUT_Y = 34;

    private static final int DATA_PROGRESS = 0;
    private static final int DATA_MAX_PROGRESS = 1;
    private static final int DATA_ENERGY = 2;
    private static final int DATA_MAX_ENERGY = 3;
    private static final int DATA_FLUID_AMOUNT = 4;
    private static final int DATA_FLUID_CAPACITY = 5;
    private static final int DATA_FLUID_ID = 6;
    private static final int DATA_COUNT = 7;

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

    @Override
    protected void addMachineSlots() {
        // 电池槽 (152, 62)
        this.addSlot(new SlotItemHandler(itemHandler, SLOT_BATTERY, BATTERY_X, BATTERY_Y));
        
        // 4个升级槽 (152, 8) 开始，垂直排列
        for (int i = 0; i < SLOT_UPGRADE_COUNT; i++) {
            addUpgradeSlot(itemHandler, SLOT_UPGRADE_START + i, UPGRADE_START_X, UPGRADE_START_Y + i * 18);
        }
        
        // 空容器输入槽 (44, 35) - 顶部位置
        this.addSlot(new SlotItemHandler(itemHandler, SLOT_CONTAINER_INPUT, CONTAINER_INPUT_X, CONTAINER_INPUT_Y));
        
        // 填充容器输出槽 (116, 35) - 侧面位置
        this.addSlot(new SlotItemHandler(itemHandler, SLOT_OUTPUT, OUTPUT_X, OUTPUT_Y) {
            @Override
            public boolean mayPlace(net.minecraft.world.item.ItemStack stack) {
                return false;
            }
        });
    }
}