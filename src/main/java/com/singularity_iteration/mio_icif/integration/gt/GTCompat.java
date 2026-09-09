package com.singularity_iteration.mio_icif.integration.gt;

import com.singularity_iteration.mio_icif.Singularity_Iteration;
import com.singularity_iteration.mio_icif.energy.EnergyUnit.CableTier;
import com.singularity_iteration.mio_icif.energy.EnergyUnit.IEUEnergyStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.BlockCapability;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;

/**
 * GregTech Modern 兼容性层
 * 使用反射安全地访问?GTCEu �?API，避免在没有 GTCEu 时崩�?
 */
@SuppressWarnings("null")
public class GTCompat {

    private static final String GT_PACKAGE_PREFIX = "com.gregtechceu.gtceu";

    private static boolean isGTLoaded = false;
    private static Class<?> gtCapabilityClass;
    private static Class<?> ieEnergyContainerClass;
    private static Object energyContainerCapability;
    private static Method acceptEnergyFromNetworkMethod;
    private static Method inputsEnergyMethod;
    private static Method outputsEnergyMethod;
    private static Method changeEnergyMethod;
    private static Method getEnergyStoredMethod;
    private static Method getEnergyCapacityMethod;
    private static Method getInputAmperageMethod;
    private static Method getInputVoltageMethod;
    private static Method getOutputAmperageMethod;
    private static Method getOutputVoltageMethod;

    private static final long[] GT_VOLTAGES = { 8, 32, 128, 512, 2048, 8192, 32768, 131072, 524288, 2097152, 8388608,
            33554432, 134217728, 536870912, 2147483647L };

    static {
        try {
            gtCapabilityClass = Class.forName("com.gregtechceu.gtceu.api.capability.GTCapability");
            ieEnergyContainerClass = Class.forName("com.gregtechceu.gtceu.api.capability.IEnergyContainer");

            energyContainerCapability = gtCapabilityClass.getField("CAPABILITY_ENERGY_CONTAINER").get(null);
            Singularity_Iteration.LOGGER.info("[GTCompat] Successfully loaded GTCapability.CAPABILITY_ENERGY_CONTAINER: {}", energyContainerCapability);

            acceptEnergyFromNetworkMethod = ieEnergyContainerClass.getMethod("acceptEnergyFromNetwork", Direction.class, long.class, long.class);
            inputsEnergyMethod = ieEnergyContainerClass.getMethod("inputsEnergy", Direction.class);
            outputsEnergyMethod = ieEnergyContainerClass.getMethod("outputsEnergy", Direction.class);
            changeEnergyMethod = ieEnergyContainerClass.getMethod("changeEnergy", long.class);
            getEnergyStoredMethod = ieEnergyContainerClass.getMethod("getEnergyStored");
            getEnergyCapacityMethod = ieEnergyContainerClass.getMethod("getEnergyCapacity");
            getInputAmperageMethod = ieEnergyContainerClass.getMethod("getInputAmperage");
            getInputVoltageMethod = ieEnergyContainerClass.getMethod("getInputVoltage");
            getOutputAmperageMethod = ieEnergyContainerClass.getMethod("getOutputAmperage");
            getOutputVoltageMethod = ieEnergyContainerClass.getMethod("getOutputVoltage");

            isGTLoaded = true;
            Singularity_Iteration.LOGGER.info("[GTCompat] GregTech Modern compatibility layer initialized successfully");
        } catch (Exception e) {
            isGTLoaded = false;
            Singularity_Iteration.LOGGER.info("[GTCompat] GregTech Modern not loaded, compatibility layer disabled: {}", e.getMessage());
        }
    }

    public static boolean isGTLoaded() {
        return isGTLoaded;
    }
    
    /**
     * 检查指定位置的方块实体是否来自 GregTech CEu 模组�?
     * 用于防止�?GT 方块被误识别�?GT 能量容器�?
     */
    public static boolean isGTBlockEntity(Level level, BlockPos pos) {
        if (!isGTLoaded) return false;
        BlockEntity be = level.getBlockEntity(pos);
        if (be == null) return false;
        return be.getClass().getName().startsWith(GT_PACKAGE_PREFIX);
    }

    /**
     * 从指定位置获�?GT 能量容器�?
     * 仅当方块实体来自 GTCEu 模组时才返回结果�?
     */
    @SuppressWarnings("unchecked")
    @Nullable
    public static Object getGTEnergyContainer(Level level, BlockPos pos, Direction direction) {
        if (!isGTLoaded) {
            return null;
        }

        if (!isGTBlockEntity(level, pos)) {
            return null;
        }

        try {
            BlockCapability<Object, Direction> capability = (BlockCapability<Object, Direction>) energyContainerCapability;
            return level.getCapability(capability, pos, direction);
        } catch (Exception e) {
            Singularity_Iteration.LOGGER.error("[GTCompat] Failed to get GT energy container via Level.getCapability: {}", e.getMessage());
            return null;
        }
    }

    public static long acceptEnergyFromNetwork(Object gtContainer, Direction side, long voltage, long amperage) {
        if (!isGTLoaded || gtContainer == null) return 0;

        try {
            return (long) acceptEnergyFromNetworkMethod.invoke(gtContainer, side, voltage, amperage);
        } catch (Exception e) {
            Singularity_Iteration.LOGGER.error("[GTCompat] Failed to accept energy from network: {}", e.getMessage());
            return 0;
        }
    }

    public static boolean inputsEnergy(Object gtContainer, Direction side) {
        if (!isGTLoaded || gtContainer == null) return false;

        try {
            return (boolean) inputsEnergyMethod.invoke(gtContainer, side);
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean outputsEnergy(Object gtContainer, Direction side) {
        if (!isGTLoaded || gtContainer == null) return false;

        try {
            return (boolean) outputsEnergyMethod.invoke(gtContainer, side);
        } catch (Exception e) {
            return false;
        }
    }

    public static long getEnergyStored(Object gtContainer) {
        if (!isGTLoaded || gtContainer == null) return 0;

        try {
            return (long) getEnergyStoredMethod.invoke(gtContainer);
        } catch (Exception e) {
            return 0;
        }
    }

    public static long getEnergyCapacity(Object gtContainer) {
        if (!isGTLoaded || gtContainer == null) return 0;

        try {
            return (long) getEnergyCapacityMethod.invoke(gtContainer);
        } catch (Exception e) {
            return 0;
        }
    }

    public static long getInputVoltage(Object gtContainer) {
        if (!isGTLoaded || gtContainer == null) return 0;

        try {
            return (long) getInputVoltageMethod.invoke(gtContainer);
        } catch (Exception e) {
            return 0;
        }
    }

    public static long getInputAmperage(Object gtContainer) {
        if (!isGTLoaded || gtContainer == null) return 0;

        try {
            return (long) getInputAmperageMethod.invoke(gtContainer);
        } catch (Exception e) {
            return 0;
        }
    }

    public static long getOutputVoltage(Object gtContainer) {
        if (!isGTLoaded || gtContainer == null) return 0;

        try {
            return (long) getOutputVoltageMethod.invoke(gtContainer);
        } catch (Exception e) {
            return 0;
        }
    }

    public static long getOutputAmperage(Object gtContainer) {
        if (!isGTLoaded || gtContainer == null) return 0;

        try {
            return (long) getOutputAmperageMethod.invoke(gtContainer);
        } catch (Exception e) {
            return 0;
        }
    }

    public static long getGTVoltageFromTier(CableTier tier) {
        return switch (tier.name) {
            case "lv" -> GT_VOLTAGES[1];
            case "mv" -> GT_VOLTAGES[2];
            case "hv" -> GT_VOLTAGES[3];
            case "ev" -> GT_VOLTAGES[4];
            case "iv" -> GT_VOLTAGES[5];
            case "luv" -> GT_VOLTAGES[6];
            case "zpmv" -> GT_VOLTAGES[7];
            case "uv" -> GT_VOLTAGES[8];
            case "uhv" -> GT_VOLTAGES[9];
            case "uev" -> GT_VOLTAGES[10];
            case "uiv" -> GT_VOLTAGES[11];
            case "uxv" -> GT_VOLTAGES[12];
            case "opv" -> GT_VOLTAGES[13];
            case "max" -> GT_VOLTAGES[14];
            default -> GT_VOLTAGES[1];
        };
    }

    public static long[] calculateGTVoltageAndAmperage(long eu, CableTier tier) {
        long maxVoltage = getGTVoltageFromTier(tier);

        long amperage = (eu + maxVoltage - 1) / maxVoltage;
        if (amperage < 1) amperage = 1;

        long voltage = Math.min(eu, maxVoltage);
        if (voltage < 1) voltage = 1;

        return new long[]{voltage, amperage};
    }

    public static IEUEnergyStorage wrapGTContainerAsSource(Object gtContainer) {
        if (!isGTLoaded || gtContainer == null) return null;
        return new WrappedGTEnergyContainerAsSource(gtContainer);
    }

    private static class WrappedGTEnergyContainerAsSource implements IEUEnergyStorage {
        private final Object wrapped;

        public WrappedGTEnergyContainerAsSource(Object wrapped) {
            this.wrapped = wrapped;
        }

        @Override
        public boolean canConnect(CableTier cableTier) {
            return true;
        }

        @Override
        public long receive(long maxReceive, boolean simulate) {
            return 0;
        }

        @Override
        public long extract(long maxExtract, boolean simulate) {
            try {
                long outputVoltage = getOutputVoltage(wrapped);
                long outputAmperage = getOutputAmperage(wrapped);

                if (outputVoltage <= 0 || outputAmperage <= 0) {
                    return 0;
                }

                long maxOutput = outputVoltage * outputAmperage;
                long toExtract = Math.min(maxExtract, maxOutput);

                if (!simulate) {
                    return -changeEnergy(wrapped, -toExtract);
                }

                long stored = GTCompat.getEnergyStored(wrapped);
                return Math.min(toExtract, stored);
            } catch (Exception e) {
                return 0;
            }
        }

        private long changeEnergy(Object container, long amount) {
            try {
                return (long) changeEnergyMethod.invoke(container, amount);
            } catch (Exception e) {
                return 0;
            }
        }

        @Override
        public long getAmount() {
            return GTCompat.getEnergyStored(wrapped);
        }

        @Override
        public long getCapacity() {
            return GTCompat.getEnergyCapacity(wrapped);
        }

        @Override
        public boolean canExtract() {
            long outputVoltage = GTCompat.getOutputVoltage(wrapped);
            return outputVoltage > 0;
        }

        @Override
        public boolean canReceive() {
            return false;
        }
    }
}