package com.miophas.singularity_iteration.common.integration.mi;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.core.api.energy.storage.CableTier;
import com.miophas.singularity_iteration.core.api.energy.storage.IEUEnergyStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.BlockCapability;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Compatibility layer for Modern Industrialization (MI).
 * It uses reflection to access the MI API safely.
 * The game does not crash when MI is not installed.
 */
@SuppressWarnings("null")
public class MICompat {
    
    private static final String MI_PACKAGE_PREFIX = "aztech.modern_industrialization";
    
    private static boolean isMILoaded = false;
    private static Class<?> energyApiClass;
    private static Class<?> miEnergyStorageClass;
    private static Class<?> miCableTierClass;
    private static Object sidedCapability;
    private static Method receiveMethod;
    private static Method extractMethod;
    private static Method getAmountMethod;
    private static Method getCapacityMethod;
    private static Method canExtractMethod;
    private static Method canReceiveMethod;
    private static Method canConnectMethod;
    
    // MI CableTier enum constants.
    private static Object miTierLV;
    private static Object miTierMV;
    private static Object miTierHV;
    private static Object miTierEV;
    private static Object miTierSuperconductor;
    
    static {
        try {
            // 尝试加载 MI 的类
            energyApiClass = Class.forName("aztech.modern_industrialization.api.energy.EnergyApi");
            miEnergyStorageClass = Class.forName("aztech.modern_industrialization.api.energy.MIEnergyStorage");
            miCableTierClass = Class.forName("aztech.modern_industrialization.api.energy.CableTier");

            // 获取 SIDED 能力字段
            sidedCapability = energyApiClass.getField("SIDED").get(null);
            Singularity_Iteration.LOGGER.info("[MICompat] Successfully loaded MI EnergyApi.SIDED: {}", sidedCapability);

            // 获取 MIEnergyStorage 方法
            receiveMethod = miEnergyStorageClass.getMethod("receive", long.class, boolean.class);
            extractMethod = miEnergyStorageClass.getMethod("extract", long.class, boolean.class);
            getAmountMethod = miEnergyStorageClass.getMethod("getAmount");
            getCapacityMethod = miEnergyStorageClass.getMethod("getCapacity");
            canExtractMethod = miEnergyStorageClass.getMethod("canExtract");
            canReceiveMethod = miEnergyStorageClass.getMethod("canReceive");
            canConnectMethod = miEnergyStorageClass.getMethod("canConnect", miCableTierClass);

            // Get the CableTier enum constants.
            miTierLV = miCableTierClass.getField("LV").get(null);
            miTierMV = miCableTierClass.getField("MV").get(null);
            miTierHV = miCableTierClass.getField("HV").get(null);
            miTierEV = miCableTierClass.getField("EV").get(null);
            miTierSuperconductor = miCableTierClass.getField("SUPERCONDUCTOR").get(null);

            isMILoaded = true;
            Singularity_Iteration.LOGGER.info("[MICompat] MI compatibility layer initialized successfully");
        } catch (Exception e) {
            // MI is not loaded. This is a normal condition.
            isMILoaded = false;
            Singularity_Iteration.LOGGER.info("[MICompat] MI not loaded, compatibility layer disabled: {}", e.getMessage());
        }
    }
    
    /**
     * Checks if MI is loaded.
     */
    public static boolean isMILoaded() {
        return isMILoaded;
    }
    
    /**
     * Checks if the block entity at the position comes from the Modern Industrialization mod.
     * This prevents the GrandPower auto-adapter from treating FE storage from other mods as MI storage.
     */
    public static boolean isMIBlockEntity(Level level, BlockPos pos) {
        if (!isMILoaded) return false;
        BlockEntity be = level.getBlockEntity(pos);
        if (be == null) return false;
        return be.getClass().getName().startsWith(MI_PACKAGE_PREFIX);
    }
    
    /**
     * Gets the MI energy storage at the position.
     * It returns a result only when the block entity comes from the MI mod.
     * This prevents the GrandPower auto-adapter from wrapping FE storage from other mods (for example Immersive Engineering) as MI storage.
     */
    @SuppressWarnings("unchecked")
    @Nullable
    public static Object getMIStorage(Level level, BlockPos pos, Direction direction) {
        if (!isMILoaded) {
            return null;
        }

        if (!isMIBlockEntity(level, pos)) {
            return null;
        }

        try {
            // Use Level.getCapability to query the MI energy capability.
            // First cast the capability to its BlockCapability type.
            BlockCapability<Object, Direction> capability = (BlockCapability<Object, Direction>) sidedCapability;
            return level.getCapability(capability, pos, direction);
        } catch (Exception e) {
            Singularity_Iteration.LOGGER.error("[MICompat] Failed to get MI storage via Level.getCapability: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Wraps an MI energy storage as an IEUEnergyStorage.
     */
    public static IEUEnergyStorage wrapMIStorage(Object miStorage) {
        if (!isMILoaded || miStorage == null) return null;
        return new WrappedMIEnergyStorage(miStorage);
    }
    
    /**
     * Converts our CableTier to the MI CableTier.
     */
    private static Object convertCableTier(CableTier tier) {
        if (!isMILoaded) return miTierLV;
        
        return switch (tier.name) {
            case "mv" -> miTierMV;
            case "hv" -> miTierHV;
            case "ev" -> miTierEV;
            case "iv" -> miTierSuperconductor;
            default -> miTierLV;
        };
    }
    
    /**
     * Wrapper that adapts the MI MIEnergyStorage to IEUEnergyStorage.
     */
    private static class WrappedMIEnergyStorage implements IEUEnergyStorage {
        private final Object wrapped;
        
        public WrappedMIEnergyStorage(Object wrapped) {
            this.wrapped = wrapped;
        }
        
        @Override
        public boolean canConnect(CableTier cableTier) {
            try {
                Object miTier = convertCableTier(cableTier);
                return (boolean) canConnectMethod.invoke(wrapped, miTier);
            } catch (Exception e) {
                return true; // 默认允许连接
            }
        }
        
        @Override
        public long receive(long maxReceive, boolean simulate) {
            try {
                return (long) receiveMethod.invoke(wrapped, maxReceive, simulate);
            } catch (InvocationTargetException e) {
                throw new IllegalStateException("MI energy receive failed; transfer outcome is unknown", e.getCause());
            } catch (Exception e) {
                return 0;
            }
        }
        
        @Override
        public long extract(long maxExtract, boolean simulate) {
            try {
                return (long) extractMethod.invoke(wrapped, maxExtract, simulate);
            } catch (InvocationTargetException e) {
                throw new IllegalStateException("MI energy extract failed; transfer outcome is unknown", e.getCause());
            } catch (Exception e) {
                return 0;
            }
        }
        
        @Override
        public long getAmount() {
            try {
                return (long) getAmountMethod.invoke(wrapped);
            } catch (Exception e) {
                return 0;
            }
        }
        
        @Override
        public long getCapacity() {
            try {
                return (long) getCapacityMethod.invoke(wrapped);
            } catch (Exception e) {
                return 0;
            }
        }
        
        @Override
        public boolean canExtract() {
            try {
                return (boolean) canExtractMethod.invoke(wrapped);
            } catch (Exception e) {
                return false;
            }
        }
        
        @Override
        public boolean canReceive() {
            try {
                return (boolean) canReceiveMethod.invoke(wrapped);
            } catch (Exception e) {
                return false;
            }
        }
    }
}

