package com.singularity_iteration.mio_icif.Menu.Generator;

import com.singularity_iteration.mio_icif.Blocks.entity.generator.mio_icif_drop_generator;
import com.singularity_iteration.mio_icif.Menu.mio_icif_menus;
import com.singularity_iteration.mio_icif.Menu.Base.mio_icif_generator_menu;
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