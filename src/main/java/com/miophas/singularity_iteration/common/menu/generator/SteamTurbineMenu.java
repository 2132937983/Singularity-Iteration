// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.menu.generator;

import com.miophas.singularity_iteration.common.blockentity.generator.mio_icif_steam_turbine;
import com.miophas.singularity_iteration.common.item.cell.mio_icif_cells;
import com.miophas.singularity_iteration.common.registry.mio_icif_menus;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/** 0.1.11.16: the steam turbine's menu: the engine menu's slots (steam cell, empty cell, battery) with the turbine's data. */
public class SteamTurbineMenu extends EngineMenu {
    public SteamTurbineMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, null);
    }

    public SteamTurbineMenu(int containerId, Inventory playerInventory, @Nullable mio_icif_steam_turbine blockEntity) {
        super(mio_icif_menus.STEAM_TURBINE_MENU_TYPE.get(), containerId, playerInventory, blockEntity,
            blockEntity != null ? blockEntity.getTurbineData() : null, mio_icif_steam_turbine.DATA_COUNT);
    }

    @Override
    protected boolean isValidFuel(ItemStack stack) {
        if (stack.isEmpty()) return false;
        var content = mio_icif_cells.getCellFluid(stack);
        return !content.isEmpty() && mio_icif_steam_turbine.stateOf(content.getFluid()) != null
            || holdsFluid(stack, f -> mio_icif_steam_turbine.stateOf(f) != null);
    }
}
