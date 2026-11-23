// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.menu.generator;

import com.miophas.singularity_iteration.common.blockentity.generator.mio_icif_drop_generator;
import com.miophas.singularity_iteration.common.blockentity.generator.mio_icif_experience_generator;
import com.miophas.singularity_iteration.common.energy.IncineratorPhysics;
import com.miophas.singularity_iteration.common.energy.Recorder;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

/**
 * 0.1.11.19: the synced values of the item and experience generators' recorder screens.
 * <ul>
 *   <li>Item generators: 0 energy, 1 capacity, 2 fuel ticks, 3 working, 4 chamber °C, 5 efficiency x1000, 6 ash,
 *   7 EU/t, 8 range, 9 EU per fuel tick at the design point, 10.. the chart (chamber °C x4 + mark).</li>
 *   <li>Experience generators: 0 energy, 1 capacity, 2 XP stocked, 3 working, 4 EU/t, 5 range, 6 EU per XP point,
 *   7.. the chart (EU/t x4 + mark).</li>
 * </ul>
 */
public final class IncineratorValues {
    private IncineratorValues() {}

    public static final int CHART = 10, DATA_COUNT = CHART + Recorder.SAMPLES;
    public static final int XP_CHART = 7, XP_DATA_COUNT = XP_CHART + Recorder.SAMPLES;
    public static final int ASH_X = 26, ASH_Y = 56;   // 0.1.11.181: the ash drawer under the grate (IncineratorScreen)

    public static int[] of(mio_icif_drop_generator g) {
        int[] v = new int[DATA_COUNT];
        v[0] = (int) g.getEnergyStored();
        v[1] = (int) g.getEnergyCapacity();
        v[2] = g.getFuel();
        v[3] = g.isWorking() ? 1 : 0;
        v[4] = (int) Math.round(g.getChamber());
        v[5] = (int) Math.round(IncineratorPhysics.efficiency(g.getChamber()) * 1000);
        v[6] = g.getAsh();
        v[7] = (int) g.getLastEu();
        v[8] = g.getScanRange();
        v[9] = (int) g.getProduction();
        for (int i = 0; i < Recorder.SAMPLES; i++) v[CHART + i] = g.getChart().get(i);
        return v;
    }

    public static int[] of(mio_icif_experience_generator g) {
        int[] v = new int[XP_DATA_COUNT];
        v[0] = (int) g.getEnergyStored();
        v[1] = (int) g.getEnergyCapacity();
        v[2] = g.getFuel();
        v[3] = g.isWorking() ? 1 : 0;
        v[4] = (int) g.getLastEu();
        v[5] = g.getScanRange();
        v[6] = (int) g.getProduction();
        for (int i = 0; i < Recorder.SAMPLES; i++) v[XP_CHART + i] = g.getChart().get(i);
        return v;
    }

    /** The ash slot: taken out only. */
    public static SlotItemHandler ashSlot(IItemHandler items) {
        return new SlotItemHandler(items, 0, ASH_X, ASH_Y) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
        };
    }
}
