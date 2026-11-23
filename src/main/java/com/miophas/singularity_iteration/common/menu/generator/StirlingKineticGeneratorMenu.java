// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.menu.generator;

import com.miophas.singularity_iteration.common.blockentity.kuentity.kugenerator.mio_icif_Stirling_Kinetic_Generator;
import com.miophas.singularity_iteration.common.registry.mio_icif_menus;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.Nullable;

/**
 * Stirling kinetic generator menu. 0.1.11.17 (the mimic screen): water and hot-water container slots on the left, the
 * upgrades docked. Values: 0 water, 1 hot water, 2 tank capacity, 3 buffered heat, 4 HU/t, 5 KU/t made, 6 KU/t given,
 * 7 rpm, 8 flywheel KU, 9 flywheel capacity, 10 overspeed, 11..16 the heater block, 17 rated HU/t, 18 working.
 */
public class StirlingKineticGeneratorMenu extends MimicGeneratorMenu {
    public static final int DATA_COUNT = 19;
    private static final int SLOT_COUNT = 7;
    public static final int COL1 = 10, COL2 = 30, ROW_WATER = 17, ROW_HOT = 57;   // 0.1.11.181: over and under their fillers

    public StirlingKineticGeneratorMenu(int containerId, Inventory playerInventory) { this(containerId, playerInventory, (mio_icif_Stirling_Kinetic_Generator) null); }

    public StirlingKineticGeneratorMenu(int containerId, Inventory playerInventory, @Nullable mio_icif_Stirling_Kinetic_Generator be) {
        super(mio_icif_menus.STIRLING_KINETIC_GENERATOR_MENU_TYPE.get(), containerId, SLOT_COUNT, playerInventory,
            be != null ? be.getItemHandler() : null, DATA_COUNT, be);
    }

    @Override
    protected void addMachineSlots() {
        IItemHandler items = itemHandler;
        this.addSlot(new SlotItemHandler(items, mio_icif_Stirling_Kinetic_Generator.COOL_FLUID_INPUT_SLOT, COL1, ROW_WATER));
        this.addSlot(new SlotItemHandler(items, mio_icif_Stirling_Kinetic_Generator.COOL_OUTPUT_SLOT, COL2, ROW_WATER));
        this.addSlot(new SlotItemHandler(items, mio_icif_Stirling_Kinetic_Generator.HOT_FLUID_INPUT_SLOT, COL1, ROW_HOT));
        this.addSlot(new SlotItemHandler(items, mio_icif_Stirling_Kinetic_Generator.HOT_OUTPUT_SLOT, COL2, ROW_HOT));
        for (int i = 0; i < mio_icif_Stirling_Kinetic_Generator.UPGRADE_SLOT_COUNT; i++)
            this.addUpgradeSlot(mio_icif_Stirling_Kinetic_Generator.UPGRADE_SLOT_START + i, COL1, ROW_HOT + 20 + i * 18);
    }

    @Override
    protected int[] values() {
        var be = (mio_icif_Stirling_Kinetic_Generator) blockEntity;
        int[] h = head(be.head());
        return new int[]{be.getWaterAmount(), be.getHotWaterAmount(), be.getMaxFluidAmount(), be.getBufferedHeat(), (int) be.getLastHu(),
            (int) be.getLastKu(), (int) be.getLastDelivered(), be.getRPM(), (int) be.getKUBuffer(), mio_icif_Stirling_Kinetic_Generator.FLYWHEEL_CAPACITY,
            be.isOverspeed() ? 1 : 0, h[0], h[1], h[2], h[3], h[4], h[5], (int) mio_icif_Stirling_Kinetic_Generator.RATED_HU, be.isWorking() ? 1 : 0};
    }
}
