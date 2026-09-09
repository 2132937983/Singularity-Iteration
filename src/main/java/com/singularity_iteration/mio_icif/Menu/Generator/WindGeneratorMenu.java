package com.singularity_iteration.mio_icif.Menu.Generator;

import com.singularity_iteration.mio_icif.Blocks.entity.generator.mio_icif_wind_generator;
import com.singularity_iteration.mio_icif.Menu.mio_icif_menus;
import com.singularity_iteration.mio_icif.Menu.Base.mio_icif_generator_menu;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.Nullable;

/**
 * 风力发电机的容器菜单独?
 */
@SuppressWarnings({"null"}) public class WindGeneratorMenu extends mio_icif_generator_menu {

    // 槽位索引
    public static final int BATTERY_SLOT = 0;

    // 数据同步索引
    private static final int DATA_GENERATING = 0;
    private static final int DATA_OUTPUT = 1;
    private static final int DATA_COUNT = 2;
    private static final int SLOT_COUNT = 1;

    public WindGeneratorMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, null, null);
    }

    public WindGeneratorMenu(int containerId, Inventory playerInventory, @Nullable IItemHandler itemHandler) {
        this(containerId, playerInventory, itemHandler, null);
    }

    public WindGeneratorMenu(int containerId, Inventory playerInventory, @Nullable IItemHandler itemHandler, @Nullable ContainerData data) {
        super(mio_icif_menus.WIND_GENERATOR_MENU_TYPE.get(), containerId, SLOT_COUNT, playerInventory, itemHandler, data, DATA_COUNT);
    }

    public WindGeneratorMenu(int containerId, Inventory playerInventory, @Nullable mio_icif_wind_generator blockEntity) {
        super(mio_icif_menus.WIND_GENERATOR_MENU_TYPE.get(), containerId, SLOT_COUNT, playerInventory,
              blockEntity != null ? blockEntity.getItemHandler() : null, null, DATA_COUNT, blockEntity);
    }

    public void setSyncData(boolean generating, int output) {
        this.data.set(DATA_GENERATING, generating ? 1 : 0);
        this.data.set(DATA_OUTPUT, output);
    }

    public boolean isGenerating() {
        return this.data.get(DATA_GENERATING) == 1;
    }

    public int getCurrentOutput() {
        return this.data.get(DATA_OUTPUT);
    }

    @Override
    public void broadcastChanges() {
        if (blockEntity instanceof mio_icif_wind_generator generator) {
            setSyncData(DATA_GENERATING, generator.isGenerating() ? 1 : 0);
            setSyncData(DATA_OUTPUT, (int) generator.getCurrentEnergyOutput());
        }
        super.broadcastChanges();
    }

    @Override
    protected void addMachineSlots() {
        // 电池�?(80, 26)
        this.addSlot(new SlotItemHandler(itemHandler, BATTERY_SLOT, 80, 26));
    }
}


