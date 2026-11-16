// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.menu.generator;

import com.miophas.singularity_iteration.common.blockentity.generator.mio_icif_advanced_stirling_generator;
import com.miophas.singularity_iteration.common.registry.mio_icif_menus;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.Nullable;

/** Advanced Stirling generator menu. 0.1.11.17: the same values as {@link StirlingGeneratorMenu}. */
public class AdvancedStirlingGeneratorMenu extends MimicGeneratorMenu {
    public AdvancedStirlingGeneratorMenu(int containerId, Inventory playerInventory) { this(containerId, playerInventory, null); }

    public AdvancedStirlingGeneratorMenu(int containerId, Inventory playerInventory, @Nullable mio_icif_advanced_stirling_generator be) {
        super(mio_icif_menus.ADVANCED_STIRLING_GENERATOR_MENU_TYPE.get(), containerId, 0, playerInventory, null, StirlingGeneratorMenu.DATA_COUNT, be);
    }

    @Override protected void addMachineSlots() {}

    @Override
    protected int[] values() {
        var be = (mio_icif_advanced_stirling_generator) blockEntity;
        int[] h = head(be.head());
        return new int[]{(int) be.getEnergyStorage().getAmount(), (int) be.getEnergyStorage().getCapacity(), (int) be.getHeatStorage().getHeatStored(),
            (int) be.getHeatStorage().getMaxHeatStored(), be.getEnergyOutput() > 0 ? 1 : 0, (int) be.getEnergyOutput(), h[0], h[1], h[2], h[3], h[4], h[5],
            (int) mio_icif_advanced_stirling_generator.RATED_HU, (int) Math.round(be.ratio() * 1000)};
    }
}
