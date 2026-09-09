package com.singularity_iteration.mio_icif.energy.kinetic;

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
 * KU 动能能力注册事件
 * 包装 NeoForge �?RegisterCapabilitiesEvent，专门用�?KU 动能系统
 * 
 * 使用示例�? * <pre>{@code
 * @SubscribeEvent
 * public static void registerKUCapabilities(RegisterCapabilitiesEvent event) {
 *     // 注册蒸汽轮机的动能存储能量? *     KURegisterCapabilitiesEvent.registerKineticStorage(
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
     * 为方块实体注册动能存储能量?     * 
     * @param event NeoForge 的注册事�?     * @param blockEntityType 方块实体类型
     * @param provider 能力提供�?     */
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
     * 为方块实体注册动力源能力
     * 
     * @param event NeoForge 的注册事�?     * @param blockEntityType 方块实体类型
     * @param provider 能力提供�?     */
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
     * 为方块实体注册动能传导器能力
     * 
     * @param event NeoForge 的注册事�?     * @param blockEntityType 方块实体类型
     * @param provider 能力提供�?     */
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

