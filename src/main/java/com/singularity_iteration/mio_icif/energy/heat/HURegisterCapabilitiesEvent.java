package com.singularity_iteration.mio_icif.energy.heat;

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
 * HU 热能能力注册事件
 * 包装 NeoForge �?RegisterCapabilitiesEvent，专门用�?HU 热能系统
 * 
 * 使用示例�? * <pre>{@code
 * @SubscribeEvent
 * public static void registerHUCapabilities(RegisterCapabilitiesEvent event) {
 *     // 注册核反应堆的热能存储能量? *     HURegisterCapabilitiesEvent.registerHeatStorage(
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
     * 为方块实体注册热能存储能量?     * 
     * @param event NeoForge 的注册事�?     * @param blockEntityType 方块实体类型
     * @param provider 能力提供�?     */
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
     * 为方块实体注册热源能量?     * 
     * @param event NeoForge 的注册事�?     * @param blockEntityType 方块实体类型
     * @param provider 能力提供�?     */
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
     * 为方块实体注册热传导能力
     * 
     * @param event NeoForge 的注册事�?     * @param blockEntityType 方块实体类型
     * @param provider 能力提供�?     */
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
     * 实例方法：注册热能存储能量?     */
    public <T extends BlockEntity> void registerHeatStorage(
            BlockEntityType<T> blockEntityType,
            ICapabilityProvider<? super T, @Nullable Direction, IHeatStorage> provider) {
        registerHeatStorage(this.parentEvent, blockEntityType, provider);
    }
    
    /**
     * 实例方法：注册热源能量?     */
    public <T extends BlockEntity> void registerHeatSource(
            BlockEntityType<T> blockEntityType,
            ICapabilityProvider<? super T, @Nullable Direction, IHeatSource> provider) {
        registerHeatSource(this.parentEvent, blockEntityType, provider);
    }
    
    /**
     * 实例方法：注册热传导能力
     */
    public <T extends BlockEntity> void registerHeatConductor(
            BlockEntityType<T> blockEntityType,
            ICapabilityProvider<? super T, @Nullable Direction, IHeatConductor> provider) {
        registerHeatConductor(this.parentEvent, blockEntityType, provider);
    }
}


