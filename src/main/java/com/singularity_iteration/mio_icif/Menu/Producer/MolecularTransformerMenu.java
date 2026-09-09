package com.singularity_iteration.mio_icif.Menu.Producer;

import com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_molecular_transformer;
import com.singularity_iteration.mio_icif.Menu.Base.mio_icif_machine_menu;
import com.singularity_iteration.mio_icif.Menu.mio_icif_menus;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.Nullable;

public class MolecularTransformerMenu extends mio_icif_machine_menu {

    public static final int INPUT_SLOT = 0;
    public static final int OUTPUT_SLOT = 1;
    public static final int SLOT_COUNT = 2;

    private static final int INPUT_X = 55;
    private static final int INPUT_Y = 35;
    private static final int OUTPUT_X = 115;
    private static final int OUTPUT_Y = 35;

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