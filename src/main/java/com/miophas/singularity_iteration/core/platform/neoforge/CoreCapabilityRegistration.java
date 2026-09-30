package com.miophas.singularity_iteration.core.platform.neoforge;

import com.miophas.singularity_iteration.core.api.energy.storage.EUApi;
import com.miophas.singularity_iteration.core.api.energy.storage.ILongEnergyStorage;
import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractEnergyBlockEntity;
import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractHeatBlockEntity;
import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractProcessingMachineBlockEntity;
import com.miophas.singularity_iteration.core.prefab.component.EnergyComponentHost;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

/** Call on the owning mod's RegisterCapabilitiesEvent for its own block entity types. */
public final class CoreCapabilityRegistration {
    private CoreCapabilityRegistration() {}

    public static <T extends AbstractEnergyBlockEntity> void registerEntity(RegisterCapabilitiesEvent event, BlockEntityType<T> type) {
        event.registerBlockEntity(EUApi.SIDED, type, AbstractEnergyBlockEntity::euPort);
        event.registerBlockEntity(ILongEnergyStorage.BLOCK, type, AbstractEnergyBlockEntity::euPort);
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, type, AbstractEnergyBlockEntity::scexFeCapability);
    }

    /**
     * 注册流体能力。
     *
     * <p>机器通过 {@code fluidHandler(side)} 暴露处理器；不支持流体时返回 null，
     * 能力查询即视为不存在，不会让机器被流体自动化顺带接管。
     */
    public static void registerFluidHandler(RegisterCapabilitiesEvent event, BlockEntityType<?> type) {
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, type, (entity, side) -> {
            if (entity instanceof AbstractProcessingMachineBlockEntity machine) {
                return machine.fluidHandler(side);
            }
            if (entity instanceof AbstractHeatBlockEntity heat) {
                return heat.fluidHandler(side);
            }
            return null;
        });
    }

    public static <T extends BlockEntity & EnergyComponentHost> void registerComponent(RegisterCapabilitiesEvent event, BlockEntityType<T> type) {
        event.registerBlockEntity(EUApi.SIDED, type, (entity, side) -> entity.energyComponent().euPort(side));
        event.registerBlockEntity(ILongEnergyStorage.BLOCK, type, (entity, side) -> entity.energyComponent().euPort(side));
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, type, (entity, side) -> entity.energyComponent().fePort(side));
    }
}
