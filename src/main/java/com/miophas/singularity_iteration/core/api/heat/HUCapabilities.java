package com.miophas.singularity_iteration.core.api.heat;


import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.EntityCapability;
import net.neoforged.neoforge.capabilities.ItemCapability;
import org.jetbrains.annotations.Nullable;

/**
 * Registers the HU (heat units) capabilities.
 * The API is similar to the FE capability system, but it is fully independent.
 * 
 * Properties of the heat system:
 * - Only specific heat sources produce heat.
 * - Only specific machines consume heat.
 * - Heat cannot travel long distances like electricity.
 * - Heat needs a transfer medium, such as heat conductors or steam.
 */
@SuppressWarnings("null")
public final class HUCapabilities {
    
    /**
     * Heat storage capabilities.
     */
    public static final class HeatStorage {
        /**
         * Block heat storage capability (sided).
         * For heat exchangers, nuclear reactors, geothermal generators and similar blocks.
         */
        public static final BlockCapability<IHeatStorage, @Nullable Direction> BLOCK = 
            BlockCapability.createSided(
                ResourceLocation.fromNamespaceAndPath("mio_icif", "heat_storage"), 
                IHeatStorage.class
            );
        
        /**
         * Entity heat storage capability.
         * For entities that can carry heat.
         */
        public static final EntityCapability<IHeatStorage, @Nullable Direction> ENTITY = 
            EntityCapability.createSided(
                ResourceLocation.fromNamespaceAndPath("mio_icif", "heat_storage"), 
                IHeatStorage.class
            );
        
        /**
         * Item heat storage capability.
         * For heat batteries, fuel rods and other items that can store heat.
         */
        public static final ItemCapability<IHeatStorage, @Nullable Void> ITEM = 
            ItemCapability.createVoid(
                ResourceLocation.fromNamespaceAndPath("mio_icif", "heat_storage"), 
                IHeatStorage.class
            );
        
        private HeatStorage() {}
    }
    
    /**
     * Heat source capabilities.
     * For blocks and items that can make heat (for example, nuclear fuel and combustion chambers).
     */
    public static final class HeatSource {
        /**
         * Block heat source capability.
         */
        public static final BlockCapability<IHeatSource, @Nullable Direction> BLOCK = 
            BlockCapability.createSided(
                ResourceLocation.fromNamespaceAndPath("mio_icif", "heat_source"), 
                IHeatSource.class
            );
        
        /**
         * Item heat source capability.
         * For fuel rods and similar items.
         */
        public static final ItemCapability<IHeatSource, @Nullable Void> ITEM = 
            ItemCapability.createVoid(
                ResourceLocation.fromNamespaceAndPath("mio_icif", "heat_source"), 
                IHeatSource.class
            );
        
        private HeatSource() {}
    }
    
    /**
     * Heat conductor capabilities.
     * For blocks that can conduct heat (for example, heat pipes).
     */
    public static final class HeatConductor {
        /**
         * Block heat conductor capability.
         */
        public static final BlockCapability<IHeatConductor, @Nullable Direction> BLOCK = 
            BlockCapability.createSided(
                ResourceLocation.fromNamespaceAndPath("mio_icif", "heat_conductor"), 
                IHeatConductor.class
            );
        
        private HeatConductor() {}
    }
    
    private HUCapabilities() {}
}

