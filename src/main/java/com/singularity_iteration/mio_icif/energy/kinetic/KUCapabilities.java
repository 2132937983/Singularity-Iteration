package com.singularity_iteration.mio_icif.energy.kinetic;

import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.EntityCapability;
import net.neoforged.neoforge.capabilities.ItemCapability;
import org.jetbrains.annotations.Nullable;

/**
 * KU (Kinetic Units) 动能能力注册
 * 提供�?FE 能力系统）?HU 热能系统类似�?API，但完全独立
 * 
 * 动能系统的特点：
 * - 只能由特定动力源产生（如蒸汽轮机、水轮机、风力发电机等）
 * - 只能被特定机器消耗（如粉碎机、压缩机、离心机等）
 * - 不能像电力那样远距离传输，需要机械传�? * - 需要旋转部件来传�? */
@SuppressWarnings("null")
public final class KUCapabilities {
    
    /**
     * 动能存储能力
     */
    public static final class KineticStorage {
        /**
         * 方块动能存储能力（支持方向）
         * 用于传动轴、齿轮箱、动能机器等
         */
        public static final BlockCapability<IKineticStorage, @Nullable Direction> BLOCK = 
            BlockCapability.createSided(
                ResourceLocation.fromNamespaceAndPath("mio_icif", "kinetic_storage"), 
                IKineticStorage.class
            );
        
        /**
         * 实体动能存储能力
         * 用于可以携带动能的实�?         */
        public static final EntityCapability<IKineticStorage, @Nullable Direction> ENTITY = 
            EntityCapability.createSided(
                ResourceLocation.fromNamespaceAndPath("mio_icif", "kinetic_storage"), 
                IKineticStorage.class
            );
        
        /**
         * 物品动能存储能力
         * 用于动能电池、飞轮等可以存储动能的物�?         */
        public static final ItemCapability<IKineticStorage, @Nullable Void> ITEM = 
            ItemCapability.createVoid(
                ResourceLocation.fromNamespaceAndPath("mio_icif", "kinetic_storage"), 
                IKineticStorage.class
            );
        
        private KineticStorage() {}
    }
    
    /**
     * 动力源能量?     * 用于可以产生动能的方法?物品（如蒸汽轮机、水轮机等）
     */
    public static final class KineticSource {
        /**
         * 方块动力源能量?         */
        public static final BlockCapability<IKineticSource, @Nullable Direction> BLOCK = 
            BlockCapability.createSided(
                ResourceLocation.fromNamespaceAndPath("mio_icif", "kinetic_source"), 
                IKineticSource.class
            );
        
        /**
         * 物品动力源能量?         */
        public static final ItemCapability<IKineticSource, @Nullable Void> ITEM = 
            ItemCapability.createVoid(
                ResourceLocation.fromNamespaceAndPath("mio_icif", "kinetic_source"), 
                IKineticSource.class
            );
        
        private KineticSource() {}
    }
    
    /**
     * 动能传导器能量?     * 用于可以传导动能的方块（如传动轴、齿轮箱等）
     */
    public static final class KineticConductor {
        /**
         * 方块动能传导器能量?         */
        public static final BlockCapability<IKineticConductor, @Nullable Direction> BLOCK = 
            BlockCapability.createSided(
                ResourceLocation.fromNamespaceAndPath("mio_icif", "kinetic_conductor"), 
                IKineticConductor.class
            );
        
        private KineticConductor() {}
    }
    
    private KUCapabilities() {}
}

