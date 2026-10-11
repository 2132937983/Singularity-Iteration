package com.miophas.singularity_iteration.core.api.heat;


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
 * Registration event for HU (heat unit) capabilities.
 * This event wraps the NeoForge RegisterCapabilitiesEvent.
 * Use it only for the HU heat system.
 *
 * Example:
 * <pre>{@code
 * @SubscribeEvent
 * public static void registerHUCapabilities(RegisterCapabilitiesEvent event) {
 *     // Register the heat storage capability of the nuclear reactor.
 *     HURegisterCapabilitiesEvent.registerHeatStorage(
 *         event,
 *         ModBlockEntities.NUCLEAR_REACTOR.get(),
 *         (reactor, direction) -> reactor.getHeatStorage()
 *     );
 * }
 * }</pre>
 */
@SuppressWarnings("null")
public class HURegisterCapabilitiesEvent extends Event implements IModBusEvent {

    private final RegisterCapabilitiesEvent parentEvent;

    HURegisterCapabilitiesEvent(RegisterCapabilitiesEvent parentEvent) {
        this.parentEvent = parentEvent;
    }

    /**
     * Registers the heat storage capability for a block entity type.
     *
     * @param event the NeoForge capability registration event
     * @param blockEntityType the block entity type that gets the capability
     * @param provider the capability provider
     */
    public static <T extends BlockEntity> void registerHeatStorage(
            RegisterCapabilitiesEvent event,
            BlockEntityType<T> blockEntityType,
            ICapabilityProvider<? super T, @Nullable Direction, IHeatStorage> provider) {

        Objects.requireNonNull(provider);

        event.registerBlockEntity(
            HUCapabilities.HeatStorage.BLOCK,
            blockEntityType,
            provider
        );
    }

    /**
     * Registers the heat source capability for a block entity type.
     *
     * @param event the NeoForge capability registration event
     * @param blockEntityType the block entity type that gets the capability
     * @param provider the capability provider
     */
    public static <T extends BlockEntity> void registerHeatSource(
            RegisterCapabilitiesEvent event,
            BlockEntityType<T> blockEntityType,
            ICapabilityProvider<? super T, @Nullable Direction, IHeatSource> provider) {

        Objects.requireNonNull(provider);

        event.registerBlockEntity(
            HUCapabilities.HeatSource.BLOCK,
            blockEntityType,
            provider
        );
    }

    /**
     * Registers the heat conductor capability for a block entity type.
     *
     * @param event the NeoForge capability registration event
     * @param blockEntityType the block entity type that gets the capability
     * @param provider the capability provider
     */
    public static <T extends BlockEntity> void registerHeatConductor(
            RegisterCapabilitiesEvent event,
            BlockEntityType<T> blockEntityType,
            ICapabilityProvider<? super T, @Nullable Direction, IHeatConductor> provider) {

        Objects.requireNonNull(provider);

        event.registerBlockEntity(
            HUCapabilities.HeatConductor.BLOCK,
            blockEntityType,
            provider
        );
    }

    /**
     * Instance variant. Registers the heat storage capability on the wrapped event.
     */
    public <T extends BlockEntity> void registerHeatStorage(
            BlockEntityType<T> blockEntityType,
            ICapabilityProvider<? super T, @Nullable Direction, IHeatStorage> provider) {
        registerHeatStorage(this.parentEvent, blockEntityType, provider);
    }

    /**
     * Instance variant. Registers the heat source capability on the wrapped event.
     */
    public <T extends BlockEntity> void registerHeatSource(
            BlockEntityType<T> blockEntityType,
            ICapabilityProvider<? super T, @Nullable Direction, IHeatSource> provider) {
        registerHeatSource(this.parentEvent, blockEntityType, provider);
    }

    /**
     * Instance method: registers the heat conductor capability.
     */
    public <T extends BlockEntity> void registerHeatConductor(
            BlockEntityType<T> blockEntityType,
            ICapabilityProvider<? super T, @Nullable Direction, IHeatConductor> provider) {
        registerHeatConductor(this.parentEvent, blockEntityType, provider);
    }
}


