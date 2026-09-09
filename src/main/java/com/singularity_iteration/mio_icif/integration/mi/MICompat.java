package com.singularity_iteration.mio_icif.integration.mi;

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
 * Modern Industrialization 兼容性层
 * 使用反射安全地访问?MI �?API，避免在没有 MI 时崩�?
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
    
    // MI CableTier 枚举�?
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

            // 获取 CableTier 枚举�?
            miTierLV = miCableTierClass.getField("LV").get(null);
            miTierMV = miCableTierClass.getField("MV").get(null);
            miTierHV = miCableTierClass.getField("HV").get(null);
            miTierEV = miCableTierClass.getField("EV").get(null);
            miTierSuperconductor = miCableTierClass.getField("SUPERCONDUCTOR").get(null);

            isMILoaded = true;
            Singularity_Iteration.LOGGER.info("[MICompat] MI compatibility layer initialized successfully");
        } catch (Exception e) {
            // MI 未加载，这是正常�?
            isMILoaded = false;
            Singularity_Iteration.LOGGER.info("[MICompat] MI not loaded, compatibility layer disabled: {}", e.getMessage());
        }
    }
    
    /**
     * 检�?MI 是否已加载?
     */
    public static boolean isMILoaded() {
        return isMILoaded;
    }
    
    /**
     * 检查指定位置的方块实体是否来自 Modern Industrialization 模组�?
     * 用于防止 GrandPower 自动适配器将其他模组�?FE 存储误判�?MI 存存储?
     */
    public static boolean isMIBlockEntity(Level level, BlockPos pos) {
        if (!isMILoaded) return false;
        BlockEntity be = level.getBlockEntity(pos);
        if (be == null) return false;
        return be.getClass().getName().startsWith(MI_PACKAGE_PREFIX);
    }
    
    /**
     * 从指定位置获�?MI 能量存存储?
     * 仅当方块实体来自 MI 模组时才返回结果，防止?GrandPower 自动适配方?
     * 将其他模组（如沉浸工程）�?FE 存储误包装为 MI 存存储?
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
            // 使用 Level.getCapability 方法查询 MI 的能量?
            // 首先获取 BlockCapability 的类�?
            BlockCapability<Object, Direction> capability = (BlockCapability<Object, Direction>) sidedCapability;
            return level.getCapability(capability, pos, direction);
        } catch (Exception e) {
            Singularity_Iteration.LOGGER.error("[MICompat] Failed to get MI storage via Level.getCapability: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * 包装 MI 能量存存储?IEUEnergyStorage
     */
    public static IEUEnergyStorage wrapMIStorage(Object miStorage) {
        if (!isMILoaded || miStorage == null) return null;
        return new WrappedMIEnergyStorage(miStorage);
    }
    
    /**
     * 将我们的 CableTier 转换�?MI �?CableTier
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
     * 包装类：�?MI �?MIEnergyStorage 包装置?IEUEnergyStorage
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
            } catch (Exception e) {
                return 0;
            }
        }
        
        @Override
        public long extract(long maxExtract, boolean simulate) {
            try {
                return (long) extractMethod.invoke(wrapped, maxExtract, simulate);
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

