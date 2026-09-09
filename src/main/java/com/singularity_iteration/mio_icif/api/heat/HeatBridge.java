package com.singularity_iteration.mio_icif.api.heat;

import net.minecraft.world.level.block.entity.BlockEntity;

import java.lang.reflect.Method;

final class HeatBridge {

    private HeatBridge() {}

    private static final Class<?> I_HEAT_STORAGE;
    private static final Class<?> I_HEAT_SOURCE;
    private static final Class<?> I_HEAT_CONDUCTOR;

    private static final Method M_GET_HEAT_STORED;
    private static final Method M_GET_MAX_HEAT_STORED;
    private static final Method M_GET_TEMPERATURE;
    private static final Method M_CAN_EXTRACT_HEAT;
    private static final Method M_CAN_RECEIVE_HEAT;
    private static final Method M_RECEIVE_HEAT;
    private static final Method M_EXTRACT_HEAT;
    private static final Method M_IS_OVERHEATED;
    private static final Method M_GET_HEAT_LOSS_PER_TICK;

    static {
        ClassLoader cl = HeatBridge.class.getClassLoader();
        Class<?> storage = null;
        Class<?> source = null;
        Class<?> conductor = null;
        Method getHeatStored = null;
        Method getMaxHeatStored = null;
        Method getTemperature = null;
        Method canExtractHeat = null;
        Method canReceiveHeat = null;
        Method receiveHeat = null;
        Method extractHeat = null;
        Method isOverheated = null;
        Method getHeatLossPerTick = null;
        try {
            storage = cl.loadClass("com.singularity_iteration.mio_icif.energy.heat.IHeatStorage");
            source = cl.loadClass("com.singularity_iteration.mio_icif.energy.heat.IHeatSource");
            conductor = cl.loadClass("com.singularity_iteration.mio_icif.energy.heat.IHeatConductor");
            getHeatStored = storage.getMethod("getHeatStored");
            getMaxHeatStored = storage.getMethod("getMaxHeatStored");
            getTemperature = storage.getMethod("getTemperature");
            canExtractHeat = storage.getMethod("canExtractHeat");
            canReceiveHeat = storage.getMethod("canReceiveHeat");
            receiveHeat = storage.getMethod("receiveHeat", long.class, boolean.class);
            extractHeat = storage.getMethod("extractHeat", long.class, boolean.class);
            isOverheated = storage.getMethod("isOverheated");
            getHeatLossPerTick = storage.getMethod("getHeatLossPerTick");
        } catch (Throwable t) {
            storage = null;
            source = null;
            conductor = null;
        }
        I_HEAT_STORAGE = storage;
        I_HEAT_SOURCE = source;
        I_HEAT_CONDUCTOR = conductor;
        M_GET_HEAT_STORED = getHeatStored;
        M_GET_MAX_HEAT_STORED = getMaxHeatStored;
        M_GET_TEMPERATURE = getTemperature;
        M_CAN_EXTRACT_HEAT = canExtractHeat;
        M_CAN_RECEIVE_HEAT = canReceiveHeat;
        M_RECEIVE_HEAT = receiveHeat;
        M_EXTRACT_HEAT = extractHeat;
        M_IS_OVERHEATED = isOverheated;
        M_GET_HEAT_LOSS_PER_TICK = getHeatLossPerTick;
    }

    static boolean isStorage(BlockEntity be) {
        return I_HEAT_STORAGE != null && I_HEAT_STORAGE.isInstance(be);
    }

    static boolean isSource(BlockEntity be) {
        return I_HEAT_SOURCE != null && I_HEAT_SOURCE.isInstance(be);
    }

    static boolean isConductor(BlockEntity be) {
        return I_HEAT_CONDUCTOR != null && I_HEAT_CONDUCTOR.isInstance(be);
    }

    static long getHeatStored(BlockEntity be) {
        if (M_GET_HEAT_STORED == null) return 0;
        try {
            Object v = M_GET_HEAT_STORED.invoke(be);
            return v instanceof Number n ? n.longValue() : 0;
        } catch (Throwable t) { return 0; }
    }

    static long getMaxHeatStored(BlockEntity be) {
        if (M_GET_MAX_HEAT_STORED == null) return 0;
        try {
            Object v = M_GET_MAX_HEAT_STORED.invoke(be);
            return v instanceof Number n ? n.longValue() : 0;
        } catch (Throwable t) { return 0; }
    }

    static int getTemperature(BlockEntity be) {
        if (M_GET_TEMPERATURE == null) return 0;
        try {
            Object v = M_GET_TEMPERATURE.invoke(be);
            return v instanceof Number n ? n.intValue() : 0;
        } catch (Throwable t) { return 0; }
    }

    static boolean canExtractHeat(BlockEntity be) {
        if (M_CAN_EXTRACT_HEAT == null) return false;
        try {
            Object v = M_CAN_EXTRACT_HEAT.invoke(be);
            return v instanceof Boolean b ? b : false;
        } catch (Throwable t) { return false; }
    }

    static boolean canReceiveHeat(BlockEntity be) {
        if (M_CAN_RECEIVE_HEAT == null) return false;
        try {
            Object v = M_CAN_RECEIVE_HEAT.invoke(be);
            return v instanceof Boolean b ? b : false;
        } catch (Throwable t) { return false; }
    }

    static boolean isOverheated(BlockEntity be) {
        if (M_IS_OVERHEATED == null) return false;
        try {
            Object v = M_IS_OVERHEATED.invoke(be);
            return v instanceof Boolean b ? b : false;
        } catch (Throwable t) { return false; }
    }

    static long getHeatLossPerTick(BlockEntity be) {
        if (M_GET_HEAT_LOSS_PER_TICK == null) return 0;
        try {
            Object v = M_GET_HEAT_LOSS_PER_TICK.invoke(be);
            return v instanceof Number n ? n.longValue() : 0;
        } catch (Throwable t) { return 0; }
    }

    static long insertHeat(BlockEntity be, long amount, boolean simulate) {
        if (M_RECEIVE_HEAT == null) return 0;
        try {
            Object v = M_RECEIVE_HEAT.invoke(be, amount, simulate);
            return v instanceof Number n ? n.longValue() : 0;
        } catch (Throwable t) { return 0; }
    }

    static long extractHeat(BlockEntity be, long amount, boolean simulate) {
        if (M_EXTRACT_HEAT == null) return 0;
        try {
            Object v = M_EXTRACT_HEAT.invoke(be, amount, simulate);
            return v instanceof Number n ? n.longValue() : 0;
        } catch (Throwable t) { return 0; }
    }

    static Object getCapabilityInstance(BlockEntity be) {
        if (I_HEAT_STORAGE != null && I_HEAT_STORAGE.isInstance(be)) {
            return be;
        }
        return null;
    }
}