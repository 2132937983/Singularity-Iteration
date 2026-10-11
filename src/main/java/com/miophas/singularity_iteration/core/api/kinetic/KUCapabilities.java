package com.miophas.singularity_iteration.core.api.kinetic;


import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.EntityCapability;
import net.neoforged.neoforge.capabilities.ItemCapability;
import org.jetbrains.annotations.Nullable;

/**
 * KU (Kinetic Units) kinetic energy capability registration.
 * It gives an API like the FE capability system and the HU heat system, but it is fully independent.
 * 
 * Properties of the kinetic energy system:
 * - Only specific power sources produce it (for example steam turbines, water turbines and wind generators).
 * - Only specific machines consume it (for example macerators, compressors and centrifuges).
 * - It cannot travel long distances like electricity. It needs mechanical transmission.
 * - It needs rotating parts to transmit it.
 */
@SuppressWarnings("null")
public final class KUCapabilities {
    
    /**
     * Kinetic energy storage capabilities.
     */
    public static final class KineticStorage {
        /**
         * Block kinetic energy storage capability. It supports directions.
         * For shafts, gearboxes, kinetic machines and similar blocks.
         */
        public static final BlockCapability<IKineticStorage, @Nullable Direction> BLOCK = 
            BlockCapability.createSided(
                ResourceLocation.fromNamespaceAndPath("mio_icif", "kinetic_storage"), 
                IKineticStorage.class
            );
        
        /**
         * Entity kinetic energy storage capability.
         * For entities that can carry kinetic energy.
         */
        public static final EntityCapability<IKineticStorage, @Nullable Direction> ENTITY = 
            EntityCapability.createSided(
                ResourceLocation.fromNamespaceAndPath("mio_icif", "kinetic_storage"), 
                IKineticStorage.class
            );
        
        /**
         * Item kinetic energy storage capability.
         * For items that can store kinetic energy, for example kinetic batteries and flywheels.
         */
        public static final ItemCapability<IKineticStorage, @Nullable Void> ITEM = 
            ItemCapability.createVoid(
                ResourceLocation.fromNamespaceAndPath("mio_icif", "kinetic_storage"), 
                IKineticStorage.class
            );
        
        private KineticStorage() {}
    }
    
    /**
     * Kinetic power source capabilities.
     * For blocks and items that can produce kinetic energy (for example steam turbines and water turbines).
     */
    public static final class KineticSource {
        /**
         * Block kinetic power source capability.
         */
        public static final BlockCapability<IKineticSource, @Nullable Direction> BLOCK = 
            BlockCapability.createSided(
                ResourceLocation.fromNamespaceAndPath("mio_icif", "kinetic_source"), 
                IKineticSource.class
            );
        
        /**
         * Item kinetic power source capability.
         */
        public static final ItemCapability<IKineticSource, @Nullable Void> ITEM = 
            ItemCapability.createVoid(
                ResourceLocation.fromNamespaceAndPath("mio_icif", "kinetic_source"), 
                IKineticSource.class
            );
        
        private KineticSource() {}
    }
    
    /**
     * Kinetic conductor capabilities.
     * For blocks that can conduct kinetic energy (for example shafts and gearboxes).
     */
    public static final class KineticConductor {
        /**
         * Block kinetic conductor capability.
         */
        public static final BlockCapability<IKineticConductor, @Nullable Direction> BLOCK = 
            BlockCapability.createSided(
                ResourceLocation.fromNamespaceAndPath("mio_icif", "kinetic_conductor"), 
                IKineticConductor.class
            );
        
        private KineticConductor() {}
    }
    
    private KUCapabilities() {}
}

