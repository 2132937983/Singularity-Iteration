package com.miophas.singularity_iteration.common.menu.generator;

import com.miophas.singularity_iteration.common.blockentity.generator.mio_icif_wind_generator;
import com.miophas.singularity_iteration.common.registry.mio_icif_menus;
import com.miophas.singularity_iteration.common.menu.base.mio_icif_generator_menu;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.Nullable;

/**
 * Container menu for the wind generator.
 */
@SuppressWarnings({"null"}) public class WindGeneratorMenu extends mio_icif_generator_menu {

    // Slot indexes.
    public static final int BATTERY_SLOT = 0;

    // Data synchronization indexes.
    private static final int DATA_GENERATING = 0;
    private static final int DATA_OUTPUT = 1;
    private static final int DATA_COUNT = mio_icif_wind_generator.DATA_COUNT;
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
              blockEntity != null ? blockEntity.getItemHandler() : null, blockEntity != null ? blockEntity.getContainerData() : null, DATA_COUNT, blockEntity);
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

    /** 0.1.11.9: 248 x 204 with the wind rose and the height curve; the battery slot on the left. */
    public static final int GUI_WIDTH = 248, GUI_HEIGHT = 244;
    public static final int SLOT_X = 10, SLOT_Y = 46;
    public static final int INVENTORY_X = 44, INVENTORY_Y = 162, HOTBAR_Y = 220;

    /** Synced value {@code i} (see the block entity's getContainerData). */
    public int value(int i) { return data.get(i); }

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
        // Battery slot (80, 26).
        this.addSlot(new SlotItemHandler(itemHandler, BATTERY_SLOT, SLOT_X, SLOT_Y));
    }
}


