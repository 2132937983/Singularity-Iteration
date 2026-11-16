package com.miophas.singularity_iteration.common.menu.producer;

import com.miophas.singularity_iteration.common.blockentity.producer.mio_icif_molecular_transformer;
import com.miophas.singularity_iteration.common.menu.base.mio_icif_machine_menu;
import com.miophas.singularity_iteration.common.registry.mio_icif_menus;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.Nullable;

public class MolecularTransformerMenu extends mio_icif_machine_menu {

    public static final int INPUT_SLOT = 0;
    public static final int OUTPUT_SLOT = 1;
    public static final int SLOT_COUNT = 2;

    /** 0.1.11.6: the screen is 248 x 204 (observation window); input and output stand in a column on the left. */
    public static final int GUI_WIDTH = 248, GUI_HEIGHT = 204;
    public static final int INPUT_X = 10;
    public static final int INPUT_Y = 29;
    public static final int OUTPUT_X = 10;
    public static final int OUTPUT_Y = 81;
    /** Player inventory: centred under the window. */
    public static final int INVENTORY_X = 44, INVENTORY_Y = 122, HOTBAR_Y = 180;

    private static final int DATA_PROGRESS = 0;
    private static final int DATA_MAX_PROGRESS = 1;
    private static final int DATA_IS_WORKING = 2;
    private static final int DATA_ENERGY = 3;
    private static final int DATA_MAX_ENERGY = 4;
    private static final int DATA_RECIPE_EU = 5;
    private static final int DATA_EU_PER_TICK = 6;
    private static final int DATA_COUNT = 7;

    public MolecularTransformerMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, null, null);
    }

    public MolecularTransformerMenu(int containerId, Inventory playerInventory, @Nullable IItemHandler itemHandler, @Nullable ContainerData data) {
        super(mio_icif_menus.MOLECULAR_TRANSFORMER_MENU_TYPE.get(), containerId, SLOT_COUNT, playerInventory, itemHandler, data, DATA_COUNT);
    }

    public MolecularTransformerMenu(int containerId, Inventory playerInventory, @Nullable mio_icif_molecular_transformer blockEntity) {
        super(mio_icif_menus.MOLECULAR_TRANSFORMER_MENU_TYPE.get(), containerId, SLOT_COUNT, playerInventory,
              blockEntity != null ? blockEntity.getItemHandler() : null,
              blockEntity != null ? blockEntity.getContainerData() : null, DATA_COUNT, blockEntity);
    }

    @Override
    protected int getPlayerInventoryY() { return INVENTORY_Y; }

    @Override
    protected int getPlayerHotbarY() { return HOTBAR_Y; }

    /** Same slots and order as the base, centred in the wider screen. */
    @Override
    protected void addPlayerInventory(Inventory playerInventory, int startY, int hotbarY) {
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.addSlot(new net.minecraft.world.inventory.Slot(playerInventory, j + i * 9 + 9, INVENTORY_X + j * 18, startY + i * 18));
            }
        }
        for (int i = 0; i < 9; ++i) {
            this.addSlot(new net.minecraft.world.inventory.Slot(playerInventory, i, INVENTORY_X + i * 18, hotbarY));
        }
    }

    @Override
    protected void addMachineSlots() {
        addSlot(new SlotItemHandler(itemHandler, INPUT_SLOT, INPUT_X, INPUT_Y));
        addSlot(new SlotItemHandler(itemHandler, OUTPUT_SLOT, OUTPUT_X, OUTPUT_Y));
    }



    public int getProgress() {
        return data.get(DATA_PROGRESS);
    }

    public int getMaxProgress() {
        return data.get(DATA_MAX_PROGRESS);
    }

    public boolean isWorking() {
        return data.get(DATA_IS_WORKING) == 1;
    }

    public int getEnergy() {
        return data.get(DATA_ENERGY);
    }

    public int getMaxEnergy() {
        return data.get(DATA_MAX_ENERGY);
    }

    public int getRecipeEU() {
        return data.get(DATA_RECIPE_EU);
    }

    public int getEUPerTick() {
        return data.get(DATA_EU_PER_TICK);
    }

    public float getProgressPercent() {
        if (getMaxProgress() <= 0) return 0.0f;
        return (float) getProgress() / (float) getMaxProgress();
    }
}