package com.miophas.singularity_iteration.core.api.kinetic;


import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;
import net.neoforged.neoforge.capabilities.ICapabilityProvider;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Registration event for KU kinetic energy capabilities.
 * Wraps the NeoForge RegisterCapabilitiesEvent for the KU kinetic energy system.
 *
 * Example:
 * <pre>{@code
 * @SubscribeEvent
 * public static void registerKUCapabilities(RegisterCapabilitiesEvent event) {
 *     // Register the kinetic storage capability of the steam turbine.
 *     KURegisterCapabilitiesEvent.registerKineticStorage(
 *         event,
 *         ModBlockEntities.STEAM_TURBINE.get(),
 *         (turbine, direction) -> turbine.getKineticStorage()
 *     );
 * }
 * }</pre>
 */
@SuppressWarnings("null")
public class KURegisterCapabilitiesEvent extends Event implements IModBusEvent {
    
    @SuppressWarnings("unused")
    private final RegisterCapabilitiesEvent parentEvent;
    
    KURegisterCapabilitiesEvent(RegisterCapabilitiesEvent parentEvent) {
        this.parentEvent = parentEvent;
    }
    
    /**
     * Registers the kinetic storage capability for a block entity type.
     *
     * @param event the NeoForge capability registration event
     * @param blockEntityType the block entity type
     * @param provider the capability provider
     */
    public static <T extends BlockEntity> void registerKineticStorage(
            RegisterCapabilitiesEvent event,
            BlockEntityType<T> blockEntityType,
            ICapabilityProvider<? super T, @Nullable Direction, IKineticStorage> provider) {
        
        Objects.requireNonNull(provider);
        
        event.registerBlockEntity(
            KUCapabilities.KineticStorage.BLOCK,
            blockEntityType,
            provider
        );
    }
    
    /**
     * Registers the kinetic source capability for a block entity type.
     * 
     * @param event the NeoForge capability registration event
     * @param blockEntityType the block entity type
     * @param provider the capability provider
     */
    public static <T extends BlockEntity> void registerKineticSource(
            RegisterCapabilitiesEvent event,
            BlockEntityType<T> blockEntityType,
            ICapabilityProvider<? super T, @Nullable Direction, IKineticSource> provider) {
        
        Objects.requireNonNull(provider);
        
        event.registerBlockEntity(
            KUCapabilities.KineticSource.BLOCK,
            blockEntityType,
            provider
        );
    }
    
    /**
     * Registers the kinetic conductor capability for a block entity type.
     * 
     * @param event the NeoForge capability registration event
     * @param blockEntityType the block entity type
     * @param provider the capability provider
     */
    public static <T extends BlockEntity> void registerKineticConductor(
            RegisterCapabilitiesEvent event,
            BlockEntityType<T> blockEntityType,
            ICapabilityProvider<? super T, @Nullable Direction, IKineticConductor> provider) {
        
        Objects.requireNonNull(provider);
        
        event.registerBlockEntity(
            KUCapabilities.KineticConductor.BLOCK,
            blockEntityType,
            provider
        );
    }
}

