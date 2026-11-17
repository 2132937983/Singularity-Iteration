package com.miophas.singularity_iteration.common.menu.huentity;

import com.miophas.singularity_iteration.common.blockentity.huentity.hugenerator.mio_icif_rt_heat_generator;
import com.miophas.singularity_iteration.common.energy.Recorder;
import com.miophas.singularity_iteration.common.item.resource.mio_icif_resources;
import com.miophas.singularity_iteration.common.menu.generator.MimicGeneratorMenu;
import com.miophas.singularity_iteration.common.registry.mio_icif_menus;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.Nullable;

/**
 * RT heat generator menu. 0.1.11.20 (the recorder screen): six pellet slots on the left. Values: 0 heat, 1 heat capacity,
 * 2 HU/t, 3 pellets, 4.. the chart (HU/t x4).
 */
@SuppressWarnings("null")
public class RTHeatGeneratorMenu extends MimicGeneratorMenu {
    public static final int SLOT_COUNT = 6, CHART = 4, DATA_COUNT = CHART + Recorder.SAMPLES;
    public static final int FIRST_PELLET_SLOT = 0, LAST_PELLET_SLOT = 5;

    public RTHeatGeneratorMenu(int containerId, Inventory playerInventory) { this(containerId, playerInventory, null); }

    public RTHeatGeneratorMenu(int containerId, Inventory playerInventory, @Nullable mio_icif_rt_heat_generator be) {
        super(mio_icif_menus.RT_HEAT_GENERATOR_MENU_TYPE.get(), containerId, SLOT_COUNT, playerInventory, be != null ? be.getItemHandler() : null, DATA_COUNT, be);
        addCustomMoveRule(stack -> stack.is(mio_icif_resources.RTG_PELLET.get()), FIRST_PELLET_SLOT, LAST_PELLET_SLOT + 1);
    }

    @Override
    protected void addMachineSlots() {
        for (int i = 0; i < SLOT_COUNT; i++)
            this.addSlot(new SlotItemHandler(itemHandler, i, 10 + (i % 2) * 18, 20 + (i / 2) * 22) {
                @Override public boolean mayPlace(ItemStack stack) { return stack.is(mio_icif_resources.RTG_PELLET.get()); }
            });
    }

    public int getHeatStored() { return value(0); }
    public int getMaxHeatStored() { return value(1); }
    public int getHeatRate() { return value(2); }

    @Override
    protected int[] values() {
        var g = (mio_icif_rt_heat_generator) blockEntity;
        int[] v = new int[DATA_COUNT];
        v[0] = (int) g.getHeatStorage().getHeatStored();
        v[1] = (int) g.getHeatStorage().getMaxHeatStored();
        v[2] = g.getCurrentHeatRate();
        v[3] = g.getPellets();
        for (int i = 0; i < Recorder.SAMPLES; i++) v[CHART + i] = g.getChart().get(i);
        return v;
    }
}
