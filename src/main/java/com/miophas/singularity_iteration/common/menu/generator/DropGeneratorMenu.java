package com.miophas.singularity_iteration.common.menu.generator;

import com.miophas.singularity_iteration.common.blockentity.generator.mio_icif_drop_generator;
import com.miophas.singularity_iteration.common.registry.mio_icif_menus;
import com.miophas.singularity_iteration.common.menu.base.mio_icif_generator_menu;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("null")
public class DropGeneratorMenu extends mio_icif_generator_menu {
    public static final int SLOT_COUNT = 0;
    public static final int DATA_COUNT = 4;

    public DropGeneratorMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, null);
    }

    public DropGeneratorMenu(int containerId, Inventory playerInventory,
                              @Nullable mio_icif_drop_generator blockEntity) {
        super(mio_icif_menus.DROP_GENERATOR_MENU_TYPE.get(), containerId, SLOT_COUNT,
              playerInventory, null, null, DATA_COUNT, blockEntity);
    }

    public int getEnergy() { return data.get(0); }
    public int getMaxEnergy() { return data.get(1); }
    public int getFuel() { return data.get(2); }
    public boolean isWorking() { return data.get(3) == 1; }

    @Override
    protected void addMachineSlots() {
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if (blockEntity instanceof mio_icif_drop_generator gen) {
            setSyncData(0, (int) gen.getEnergyStored());
            setSyncData(1, (int) gen.getEnergyCapacity());
            setSyncData(2, gen.getFuel());
            setSyncData(3, gen.isWorking() ? 1 : 0);
        }
    }
}