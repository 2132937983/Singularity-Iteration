package com.singularity_iteration.mio_icif.api.kinetic;

import net.minecraft.world.level.block.entity.BlockEntity;

import java.lang.reflect.Method;

final class KineticBridge {

    private KineticBridge() {}

    private static final Class<?> I_KINETIC_STORAGE;
    private static final Class<?> I_KINETIC_SOURCE;
    private static final Class<?> I_KINETIC_CONDUCTOR;

    private static final Method M_GET_KINETIC_STORED;
    private static final Method M_GET_MAX_KINETIC_STORED;
    private static final Method M_GET_RPM;
    private static final Method M_CAN_EXTRACT_KINETIC;
    private static final Method M_CAN_RECEIVE_KINETIC;
    private static final Method M_RECEIVE_KINETIC;
    private static final Method M_EXTRACT_KINETIC;
    private static final Method M_IS_OVERSPEED;
    private static final Method M_GET_KINETIC_LOSS_PER_TICK;
    private static final Method M_GET_MAX_RECEIVE;
    private static final Method M_GET_MAX_EXTRACT;

    static {
        ClassLoader cl = KineticBridge.class.getClassLoader();
        Class<?> storage = null;
        Class<?> source = null;
        Class<?> conductor = null;
        Method getKineticStored = null;
        Method getMaxKineticStored = null;
        Method getRPM = null;
        Method canExtractKinetic = null;
        Method canReceiveKinetic = null;
        Method receiveKinetic = null;
        Method extractKinetic = null;
        Method isOverspeed = null;
        Method getKineticLossPerTick = null;
        Method getMaxReceive = null;
        Method getMaxExtract = null;
        try {
            storage = cl.loadClass("com.singularity_iteration.mio_icif.energy.kinetic.IKineticStorage");
            source = cl.loadClass("com.singularity_iteration.mio_icif.energy.kinetic.IKineticSource");
            conductor = cl.loadClass("com.singularity_iteration.mio_icif.energy.kinetic.IKineticConductor");
            getKineticStored = storage.getMethod("getKineticStored");
            getMaxKineticStored = storage.getMethod("getMaxKineticStored");
            getRPM = storage.getMethod("getRPM");
            canExtractKinetic = storage.getMethod("canExtractKinetic");
            canReceiveKinetic = storage.getMethod("canReceiveKinetic");
            receiveKinetic = storage.getMethod("receiveKinetic", long.class, boolean.class);
            extractKinetic = storage.getMethod("extractKinetic", long.class, boolean.class);
            isOverspeed = storage.getMethod("isOverspeed");
            getKineticLossPerTick = storage.getMethod("getKineticLossPerTick");
            getMaxReceive = storage.getMethod("getMaxReceive");
            getMaxExtract = storage.getMethod("getMaxExtract");
        } catch (Throwable t) {
            storage = null;
            source = null;
            conductor = null;
        }
        I_KINETIC_STORAGE = storage;
        I_KINETIC_SOURCE = source;
        I_KINETIC_CONDUCTOR = conductor;
        M_GET_KINETIC_STORED = getKineticStored;
        M_GET_MAX_KINETIC_STORED = getMaxKineticStored;
        M_GET_RPM = getRPM;
        M_CAN_EXTRACT_KINETIC = canExtractKinetic;
        M_CAN_RECEIVE_KINETIC = canReceiveKinetic;
        M_RECEIVE_KINETIC = receiveKinetic;
        M_EXTRACT_KINETIC = extractKinetic;
        M_IS_OVERSPEED = isOverspeed;
        M_GET_KINETIC_LOSS_PER_TICK = getKineticLossPerTick;
        M_GET_MAX_RECEIVE = getMaxReceive;
        M_GET_MAX_EXTRACT = getMaxExtract;
    }

    static boolean isStorage(BlockEntity be) {
        return I_KINETIC_STORAGE != null && I_KINETIC_STORAGE.isInstance(be);
    }

    static boolean isSource(BlockEntity be) {
        return I_KINETIC_SOURCE != null && I_KINETIC_SOURCE.isInstance(be);
    }

    static boolean isConductor(BlockEntity be) {
        return I_KINETIC_CONDUCTOR != null && I_KINETIC_CONDUCTOR.isInstance(be);
    }

    static long getKineticStored(BlockEntity be) {
        if (M_GET_KINETIC_STORED == null) return 0;
        try {
            Object v = M_GET_KINETIC_STORED.invoke(be);
            return v instanceof Number n ? n.longValue() : 0;
        } catch (Throwable t) { return 0; }
    }

    static long getMaxKineticStored(BlockEntity be) {
        if (M_GET_MAX_KINETIC_STORED == null) return 0;
        try {
            Object v = M_GET_MAX_KINETIC_STORED.invoke(be);
            return v instanceof Number n ? n.longValue() : 0;
        } catch (Throwable t) { return 0; }
    }

    static int getRPM(BlockEntity be) {
        if (M_GET_RPM == null) return 0;
        try {
            Object v = M_GET_RPM.invoke(be);
            return v instanceof Number n ? n.intValue() : 0;
        } catch (Throwable t) { return 0; }
    }

    static boolean canExtractKinetic(BlockEntity be) {
        if (M_CAN_EXTRACT_KINETIC == null) return false;
        try {
            Object v = M_CAN_EXTRACT_KINETIC.invoke(be);
            return v instanceof Boolean b ? b : false;
        } catch (Throwable t) { return false; }
    }

    static boolean canReceiveKinetic(BlockEntity be) {
        if (M_CAN_RECEIVE_KINETIC == null) return false;
        try {
            Object v = M_CAN_RECEIVE_KINETIC.invoke(be);
            return v instanceof Boolean b ? b : false;
        } catch (Throwable t) { return false; }
    }

    static boolean isOverspeed(BlockEntity be) {
        if (M_IS_OVERSPEED == null) return false;
        try {
            Object v = M_IS_OVERSPEED.invoke(be);
            return v instanceof Boolean b ? b : false;
        } catch (Throwable t) { return false; }
    }

    static long getKineticLossPerTick(BlockEntity be) {
        if (M_GET_KINETIC_LOSS_PER_TICK == null) return 0;
        try {
            Object v = M_GET_KINETIC_LOSS_PER_TICK.invoke(be);
            return v instanceof Number n ? n.longValue() : 0;
        } catch (Throwable t) { return 0; }
    }

    static long getMaxReceive(BlockEntity be) {
        if (M_GET_MAX_RECEIVE == null) return 0;
        try {
            Object v = M_GET_MAX_RECEIVE.invoke(be);
            return v instanceof Number n ? n.longValue() : 0;
        } catch (Throwable t) { return 0; }
    }

    static long getMaxExtract(BlockEntity be) {
        if (M_GET_MAX_EXTRACT == null) return 0;
        try {
            Object v = M_GET_MAX_EXTRACT.invoke(be);
            return v instanceof Number n ? n.longValue() : 0;
        } catch (Throwable t) { return 0; }
    }

    static long insertKinetic(BlockEntity be, long amount, boolean simulate) {
        if (M_RECEIVE_KINETIC == null) return 0;
        try {
            Object v = M_RECEIVE_KINETIC.invoke(be, amount, simulate);
            return v instanceof Number n ? n.longValue() : 0;
        } catch (Throwable t) { return 0; }
    }

    static long extractKinetic(BlockEntity be, long amount, boolean simulate) {
        if (M_EXTRACT_KINETIC == null) return 0;
        try {
            Object v = M_EXTRACT_KINETIC.invoke(be, amount, simulate);
            return v instanceof Number n ? n.longValue() : 0;
        } catch (Throwable t) { return 0; }
    }

    static Object getCapabilityInstance(BlockEntity be) {
        if (I_KINETIC_STORAGE != null && I_KINETIC_STORAGE.isInstance(be)) {
            return be;
        }
        return null;
    }
}