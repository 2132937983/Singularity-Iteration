package com.miophas.singularity_iteration.common.menu.generator;

import com.miophas.singularity_iteration.common.blockentity.generator.mio_icif_rt_generator;
import com.miophas.singularity_iteration.common.energy.Recorder;
import com.miophas.singularity_iteration.common.item.resource.mio_icif_resources;
import com.miophas.singularity_iteration.common.registry.mio_icif_menus;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.Nullable;

/**
 * RTG menu. 0.1.11.20 (the recorder screen): six pellet slots in two columns on the left. Values: 0 energy, 1 capacity,
 * 2 the pellet table's EU/t, 3 pellets, 4 cold-side factor x1000, 5 cold side °C x10, 6 its kind (RtgPhysics),
 * 7 lava touching, 8 EU/t x100, 9.. the chart (EU/t x10, x4).
 */
@SuppressWarnings("null")
public class RTGeneratorMenu extends MimicGeneratorMenu {
    public static final int SLOT_COUNT = 6, CHART = 9, DATA_COUNT = CHART + Recorder.SAMPLES;
    /** 0.1.11.181: the six pellets in their lead container, three by two (also the RT heat generator's). */
    public static final int PELLET_X = 14, PELLET_Y = 22, PELLET_DX = 19, PELLET_DY = 20;

    public RTGeneratorMenu(int containerId, Inventory playerInventory) { this(containerId, playerInventory, null); }

    public RTGeneratorMenu(int containerId, Inventory playerInventory, @Nullable mio_icif_rt_generator be) {
        super(mio_icif_menus.RT_GENERATOR_MENU_TYPE.get(), containerId, SLOT_COUNT, playerInventory, be != null ? be.getItemHandler() : null, DATA_COUNT, be);
        addCustomMoveRule(stack -> stack.is(mio_icif_resources.RTG_PELLET.get()), 0, SLOT_COUNT);
    }

    /** Two columns of three at x 10 and 28 (the window starts at x 54). */
    static void pelletSlots(MimicGeneratorMenu menu, net.neoforged.neoforge.items.IItemHandler items, java.util.function.Consumer<SlotItemHandler> add) {
        for (int i = 0; i < SLOT_COUNT; i++)
            add.accept(new SlotItemHandler(items, i, PELLET_X + (i % 3) * PELLET_DX, PELLET_Y + (i / 3) * PELLET_DY) {
                @Override public boolean mayPlace(ItemStack stack) { return stack.is(mio_icif_resources.RTG_PELLET.get()); }
            });
    }

    @Override
    protected void addMachineSlots() { pelletSlots(this, itemHandler, this::addSlot); }

    @Override
    protected int[] values() {
        var g = (mio_icif_rt_generator) blockEntity;
        var cold = g.coldSide();
        int[] v = new int[DATA_COUNT];
        v[0] = (int) g.getEnergyStorage().getAmount();
        v[1] = (int) g.getEnergyStorage().getCapacity();
        v[2] = g.getBaseRate();
        v[3] = g.getPelletCount();
        v[4] = (int) Math.round(cold.factor() * 1000);
        v[5] = (int) Math.round(cold.celsius() * 10);
        v[6] = cold.kind();
        v[7] = cold.lava() ? 1 : 0;
        v[8] = (int) Math.round(g.getLastEu() * 100);
        for (int i = 0; i < Recorder.SAMPLES; i++) v[CHART + i] = g.getChart().get(i);
        return v;
    }
}
