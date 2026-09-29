package com.miophas.singularity_iteration.common.menu.generator;

import com.miophas.singularity_iteration.common.blockentity.generator.mio_icif_advanced_stirling_generator;
import com.miophas.singularity_iteration.common.registry.mio_icif_menus;
import com.miophas.singularity_iteration.common.menu.base.mio_icif_generator_menu;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("null")
public class AdvancedStirlingGeneratorMenu extends mio_icif_generator_menu {
    public static final int SLOT_COUNT = 0;
    public static final int DATA_COUNT = 6;

    public AdvancedStirlingGeneratorMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, null, null);
    }

    public AdvancedStirlingGeneratorMenu(int containerId, Inventory playerInventory,
                                  @Nullable IItemHandler itemHandler,
                                  @Nullable ContainerData data) {
        super(mio_icif_menus.ADVANCED_STIRLING_GENERATOR_MENU_TYPE.get(), containerId, SLOT_COUNT,
              playerInventory, itemHandler, data, DATA_COUNT);
    }

    public AdvancedStirlingGeneratorMenu(int containerId, Inventory playerInventory,
                                  @Nullable mio_icif_advanced_stirling_generator blockEntity,
                                  @Nullable IItemHandler itemHandler,
                                  @Nullable ContainerData data) {
        super(mio_icif_menus.ADVANCED_STIRLING_GENERATOR_MENU_TYPE.get(), containerId, SLOT_COUNT,
              playerInventory, itemHandler, data, DATA_COUNT, blockEntity);
    }

    public int getEnergy() { return data.get(0); }
    public int getMaxEnergy() { return data.get(1); }
    public int getHeat() { return data.get(2); }
    public int getMaxHeat() { return data.get(3); }
    public boolean isWorking() { return data.get(4) == 1; }
    public int getEnergyOutput() { return data.get(5); }

    @Override
    protected void addMachineSlots() {
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if (blockEntity instanceof mio_icif_advanced_stirling_generator gen) {
            setSyncData(0, (int) gen.getEnergyStorage().getAmount());
            setSyncData(1, (int) gen.getEnergyStorage().getCapacity());
            setSyncData(2, (int) gen.getHeatStorage().getHeatStored());
            setSyncData(3, (int) gen.getHeatStorage().getMaxHeatStored());
            setSyncData(4, gen.isWorking() ? 1 : 0);
            setSyncData(5, (int) gen.getEnergyOutput());
        }
    }
}