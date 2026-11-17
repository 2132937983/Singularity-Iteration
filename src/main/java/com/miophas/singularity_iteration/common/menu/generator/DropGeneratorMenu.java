package com.miophas.singularity_iteration.common.menu.generator;

import com.miophas.singularity_iteration.common.blockentity.generator.mio_icif_drop_generator;
import com.miophas.singularity_iteration.common.registry.mio_icif_menus;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.Nullable;

/** 0.1.11.19 (the incinerator recorder): the values of {@link IncineratorValues}; the ash slot on the left (taken out only). */
@SuppressWarnings("null")
public class DropGeneratorMenu extends MimicGeneratorMenu {
    public static final int SLOT_COUNT = 1;

    public DropGeneratorMenu(int containerId, Inventory playerInventory) { this(containerId, playerInventory, null); }

    public DropGeneratorMenu(int containerId, Inventory playerInventory, @Nullable mio_icif_drop_generator be) {
        super(mio_icif_menus.DROP_GENERATOR_MENU_TYPE.get(), containerId, SLOT_COUNT, playerInventory, be != null ? be.getAshHandler() : null,
            IncineratorValues.DATA_COUNT, be);
    }

    public int getEnergy() { return value(0); }
    public int getMaxEnergy() { return value(1); }
    public int getFuel() { return value(2); }
    public boolean isWorking() { return value(3) == 1; }

    @Override
    protected void addMachineSlots() {
        this.addSlot(IncineratorValues.ashSlot(itemHandler));
    }

    @Override
    protected int[] values() { return IncineratorValues.of((mio_icif_drop_generator) blockEntity); }
}
