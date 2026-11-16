package com.miophas.singularity_iteration.common.menu.generator;

import com.miophas.singularity_iteration.common.blockentity.generator.mio_icif_geomagnetic_generator;
import com.miophas.singularity_iteration.common.energy.GeomagPhysics;
import com.miophas.singularity_iteration.common.registry.mio_icif_menus;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.Nullable;

/**
 * Geomagnetic generator menu. 0.1.11.20 (the magnetogram): the battery slot on the left. Values: 0 energy, 1 capacity,
 * 2 structure complete, 3 EU/t, 4 disturbance x1000, 5 Kp x10, 6 site ratio (depth, solid ground, biome) x1000,
 * 7 time of day, 8.. the disturbance over the last three days ({@link #TRACE} points, x1000), then Kp every three hours
 * ({@link #KP} bars, x10). The history is worked out on the server from the world's storms.
 */
@SuppressWarnings("null")
public class GeomagneticGeneratorMenu extends MimicGeneratorMenu {
    public static final int SLOT_COUNT = 1, BATTERY_SLOT = 0;
    public static final int TRACE = 72, KP = 24, TRACE_AT = 8, KP_AT = TRACE_AT + TRACE, DATA_COUNT = KP_AT + KP;
    public static final int SPAN = 3 * GeomagPhysics.DAY;

    public GeomagneticGeneratorMenu(int containerId, Inventory playerInventory) { this(containerId, playerInventory, null); }

    public GeomagneticGeneratorMenu(int containerId, Inventory playerInventory, @Nullable mio_icif_geomagnetic_generator be) {
        super(mio_icif_menus.GEOMAGNETIC_GENERATOR_MENU_TYPE.get(), containerId, SLOT_COUNT, playerInventory, be != null ? be.getItemHandler() : null, DATA_COUNT, be);
    }

    @Override
    protected void addMachineSlots() { addBatterySlot(BATTERY_SLOT, 10, 30); }

    public int getEnergy() { return value(0); }
    public int getMaxEnergy() { return value(1); }
    public boolean isGenerating() { return value(2) == 1; }

    @Override
    protected int[] values() {
        var g = (mio_icif_geomagnetic_generator) blockEntity;
        var level = g.getLevel();
        long seed = level == null ? 0 : GeomagPhysics.seed(level), now = level == null ? 0 : level.getDayTime();
        int[] v = new int[DATA_COUNT];
        v[0] = (int) Math.min(Integer.MAX_VALUE, g.getEnergyStorage().getAmount());
        v[1] = (int) Math.min(Integer.MAX_VALUE, g.getEnergyStorage().getCapacity());
        v[2] = g.isStructureComplete() ? 1 : 0;
        v[3] = (int) g.getPowerOutput();
        v[4] = (int) Math.round(g.getDisturbance() * 1000);
        v[5] = (int) Math.round(g.getKp() * 10);
        v[6] = (int) Math.round(g.getSiteRatio() * 1000);
        v[7] = (int) Math.floorMod(now, (long) GeomagPhysics.DAY);
        for (int i = 0; i < TRACE; i++) v[TRACE_AT + i] = (int) Math.round(GeomagPhysics.disturbance(seed, now - SPAN + (long) SPAN * (i + 1) / TRACE) * 1000);
        for (int i = 0; i < KP; i++) v[KP_AT + i] = (int) Math.round(GeomagPhysics.kp(seed, now - SPAN + 3000L * i + 1500) * 10);
        return v;
    }
}
