package com.singularity_iteration.mio_icif.Menu.Generator;

import com.singularity_iteration.mio_icif.Blocks.entity.generator.mio_icif_stirling_generator;
import com.singularity_iteration.mio_icif.Menu.mio_icif_menus;
import com.singularity_iteration.mio_icif.Menu.Base.mio_icif_generator_menu;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

public class StirlingGeneratorMenu extends mio_icif_generator_menu {
    public static final int SLOT_COUNT = 0;
    public static final int DATA_COUNT = 6;

    @Nullable
    private final mio_icif_stirling_generator stirlingBlockEntity;

    public StirlingGeneratorMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, null, null);
    }

    public StirlingGeneratorMenu(int containerId, Inventory playerInventory,
                                  @Nullable IItemHandler itemHandler,
                                  @Nullable ContainerData data) {
        super(mio_icif_menus.STIRLING_GENERATOR_MENU_TYPE.get(), containerId, SLOT_COUNT,
              playerInventory, itemHandler, data, DATA_COUNT);
        this.stirlingBlockEntity = null;
    }

    public StirlingGeneratorMenu(int containerId, Inventory playerInventory,
                                  @Nullable mio_icif_stirling_generator blockEntity,
                                  @Nullable IItemHandler itemHandler,
                                  @Nullable ContainerData data) {
        super(mio_icif_menus.STIRLING_GENERATOR_MENU_TYPE.get(), containerId, SLOT_COUNT,
              playerInventory, itemHandler, data, DATA_COUNT, blockEntity);
        this.stirlingBlockEntity = blockEntity;
    }

    public int getEnergy() { return data.get(0); }
    public int getMaxEnergy() { return data.get(1); }
    public int getHeat() { return data.get(2); }
    public int getMaxHeat() { return data.get(3); }
    public boolean isWorking() { return data.get(4) == 1; }
    public int getEnergyOutput() { return data.get(5); }

    @Override
    protected void addMachineSlots() {
        // 斯特林发电机没有机器槽位
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        mio_icif_stirling_generator be = this.stirlingBlockEntity != null ? this.stirlingBlockEntity
            : (this.blockEntity instanceof mio_icif_stirling_generator ? (mio_icif_stirling_generator) this.blockEntity : null);
        if (be != null) {
            setSyncData(0, (int) be.getEnergyStorage().getAmount());
            setSyncData(1, (int) be.getEnergyStorage().getCapacity());
            setSyncData(2, (int) be.getHeatStorage().getHeatStored());
            setSyncData(3, (int) be.getHeatStorage().getMaxHeatStored());
            setSyncData(4, be.isWorking() ? 1 : 0);
            setSyncData(5, (int) be.getEnergyOutput());
        }
    }
}