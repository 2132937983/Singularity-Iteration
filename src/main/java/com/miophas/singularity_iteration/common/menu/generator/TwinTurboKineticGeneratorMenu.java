// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.menu.generator;

import com.miophas.singularity_iteration.common.blockentity.generator.mio_icif_twin_turbo_kinetic_generator;
import com.miophas.singularity_iteration.common.registry.mio_icif_menus;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.Nullable;

/**
 * 0.1.11.17 (the shaft mimic screen): 0 energy, 1 capacity, 2 KU stored, 3 KU capacity, 4 working, 5 EU/t, 6 KU/t in,
 * 7 EU per KU x1000, 8 source (0 none, 1 wind/water, 2 other), 9 source rpm, 10 max KU/t in.
 */
public class TwinTurboKineticGeneratorMenu extends MimicGeneratorMenu {
    public static final int DATA_COUNT = 11;

    public TwinTurboKineticGeneratorMenu(int containerId, Inventory playerInventory) { this(containerId, playerInventory, null); }

    public TwinTurboKineticGeneratorMenu(int containerId, Inventory playerInventory, @Nullable mio_icif_twin_turbo_kinetic_generator be) {
        super(mio_icif_menus.TWIN_TURBO_KINETIC_GENERATOR_MENU_TYPE.get(), containerId, 0, playerInventory, null, DATA_COUNT, be);
    }

    @Override protected void addMachineSlots() {}

    @Override
    protected int[] values() {
        var be = (mio_icif_twin_turbo_kinetic_generator) blockEntity;
        int[] src = source(be.getLevel(), be.getBlockPos(), facing(be));
        return new int[]{(int) be.getEnergyStorage().getAmount(), (int) be.getEnergyStorage().getCapacity(), (int) be.getKineticStorage().getKineticStored(),
            (int) be.getKineticStorage().getMaxKineticStored(), be.isBurning() ? 1 : 0, (int) Math.round(be.getLastProduction()), be.getLastKuInput(),
            (int) Math.round(be.getEuPerKu() * 1000), src[0], src[1], (int) be.getKineticStorage().getMaxReceive()};
    }
}
