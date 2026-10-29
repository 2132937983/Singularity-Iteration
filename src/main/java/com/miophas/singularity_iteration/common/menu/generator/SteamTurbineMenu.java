// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.menu.generator;

import com.miophas.singularity_iteration.common.blockentity.generator.mio_icif_steam_turbine;
import com.miophas.singularity_iteration.common.item.cell.mio_icif_cells;
import com.miophas.singularity_iteration.common.registry.mio_icif_menus;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

/** 0.1.10.36: the steam turbine's menu: the semifluid generator's, taking steam cells. */
public class SteamTurbineMenu extends SemifluidGeneratorMenu {
    public SteamTurbineMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, null, null, null);
    }

    public SteamTurbineMenu(int containerId, Inventory playerInventory, @Nullable mio_icif_steam_turbine blockEntity,
                            @Nullable IItemHandler itemHandler, @Nullable ContainerData data) {
        super(mio_icif_menus.STEAM_TURBINE_MENU_TYPE.get(), containerId, playerInventory, blockEntity, itemHandler, data);
    }

    @Override
    protected boolean isValidFuel(ItemStack stack) {
        if (stack.isEmpty()) return false;
        var content = mio_icif_cells.getCellFluid(stack);
        return !content.isEmpty() && mio_icif_steam_turbine.steam(content.getFluid());
    }
}
