package com.singularity_iteration.mio_icif.energy.heat;

import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.EntityCapability;
import net.neoforged.neoforge.capabilities.ItemCapability;
import org.jetbrains.annotations.Nullable;

/**
 * HU (Heat Units) 热能能力注册
 * 提供�?FE 能力系统类似�?API，但完全独立
 * 
 * 热能系统的特点：
 * - 只能由特定热源产�? * - 只能被特定机器消耗? * - 不能像电力那样远距离传输
 * - 需要热传导介质（如热导管、蒸汽等�? */
@SuppressWarnings("null")
public final class HUCapabilities {
    
    /**
     * 热能存储能力
     */
    public static final class HeatStorage {
        /**
         * 方块热能存储能力（支持方向）
         * 用于热交换器、核反应堆、地热发电机器?         */
        public static final BlockCapability<IHeatStorage, @Nullable Direction> BLOCK = 
            BlockCapability.createSided(
                ResourceLocation.fromNamespaceAndPath("mio_icif", "heat_storage"), 
                IHeatStorage.class
            );
        
        /**
         * 实体热能存储能力
         * 用于可以携带热能的实�?         */
        public static final EntityCapability<IHeatStorage, @Nullable Direction> ENTITY = 
            EntityCapability.createSided(
                ResourceLocation.fromNamespaceAndPath("mio_icif", "heat_storage"), 
                IHeatStorage.class
            );
        
        /**
         * 物品热能存储能力
         * 用于热电池、燃料棒等可以存储热能的物品
         */
        public static final ItemCapability<IHeatStorage, @Nullable Void> ITEM = 
            ItemCapability.createVoid(
                ResourceLocation.fromNamespaceAndPath("mio_icif", "heat_storage"), 
                IHeatStorage.class
            );
        
        private HeatStorage() {}
    }
    
    /**
     * 热源能力
     * 用于可以产生热能的方法?物品（如核燃料、燃烧室等）
     */
    public static final class HeatSource {
        /**
         * 方块热源能力
         */
        public static final BlockCapability<IHeatSource, @Nullable Direction> BLOCK = 
            BlockCapability.createSided(
                ResourceLocation.fromNamespaceAndPath("mio_icif", "heat_source"), 
                IHeatSource.class
            );
        
        /**
         * 物品热源能力
         * 用于燃料棒等
         */
        public static final ItemCapability<IHeatSource, @Nullable Void> ITEM = 
            ItemCapability.createVoid(
                ResourceLocation.fromNamespaceAndPath("mio_icif", "heat_source"), 
                IHeatSource.class
            );
        
        private HeatSource() {}
    }
    
    /**
     * 热传导介质能量?     * 用于可以传导热能的方块（如热导管理?     */
    public static final class HeatConductor {
        /**
         * 方块热传导能量?         */
        public static final BlockCapability<IHeatConductor, @Nullable Direction> BLOCK = 
            BlockCapability.createSided(
                ResourceLocation.fromNamespaceAndPath("mio_icif", "heat_conductor"), 
                IHeatConductor.class
            );
        
        private HeatConductor() {}
    }
    
    private HUCapabilities() {}
}

