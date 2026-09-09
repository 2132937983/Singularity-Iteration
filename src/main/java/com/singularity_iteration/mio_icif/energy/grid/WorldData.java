package com.singularity_iteration.mio_icif.energy.grid;

import net.minecraft.world.level.Level;

import java.util.WeakHashMap;
import java.util.Map;

@SuppressWarnings("null")
public class WorldData {
    private static final Map<Level, WorldData> instances = new WeakHashMap<>();

    public final EnergyNetLocal energyNet;

    private WorldData(Level world) {
        this.energyNet = EnergyNetLocal.create(world);
    }

    public static WorldData get(Level world) {
        return instances.computeIfAbsent(world, WorldData::new);
    }

    public static void remove(Level world) {
        WorldData data = instances.remove(world);
        if (data != null) {
            data.energyNet.clear();
        }
    }

    public static boolean has(Level world) {
        return instances.containsKey(world);
    }
}

