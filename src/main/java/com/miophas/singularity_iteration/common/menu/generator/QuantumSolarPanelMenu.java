package com.miophas.singularity_iteration.common.menu.generator;

import com.miophas.singularity_iteration.common.blockentity.generator.mio_icif_solar_generator;
import com.miophas.singularity_iteration.common.registry.mio_icif_menus;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.Nullable;

/** 0.1.11.14: the shared solar panel menu under this panel's menu type (4 charge slots). */
public class QuantumSolarPanelMenu extends SolarPanelMenu {
    public static final int SLOT_COUNT = 4;

    public QuantumSolarPanelMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, null);
    }

    public QuantumSolarPanelMenu(int containerId, Inventory playerInventory, @Nullable mio_icif_solar_generator blockEntity) {
        super(mio_icif_menus.QUANTUM_SOLAR_PANEL_MENU_TYPE.get(), containerId, playerInventory, blockEntity, SLOT_COUNT);
    }

    @Override
    protected boolean isFourSlotMenu() { return true; }
}
