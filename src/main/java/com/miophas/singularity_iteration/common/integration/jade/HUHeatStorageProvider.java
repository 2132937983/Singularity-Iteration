package com.miophas.singularity_iteration.common.integration.jade;

import com.miophas.singularity_iteration.core.api.MioIcifAPI;
import com.miophas.singularity_iteration.core.api.capability.IMioIcifCapabilities;
import com.miophas.singularity_iteration.core.api.machine.IHeatGeneratorBlock;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

/**
 * Jade HU 热能存储显示提供器，参照 EUEnergyStorageProvider / KUKineticStorageProvider 实现。
 * <p>
 * 注意：HU 必须从实际注册的 {@link IMioIcifCapabilities#HEAT_STORAGE_BLOCK} 读取，
 * 而不是 {@code HUCapabilities.HeatStorage.BLOCK}（后者在本模组中从未注册，会导致读不到数据）。
 */
@SuppressWarnings("null")
public enum HUHeatStorageProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {

    INSTANCE;

    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("mio_icif", "hu_heat_storage");

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        CompoundTag data = accessor.getServerData();

        if (data.contains("Heat") && data.contains("MaxHeat")) {
            long heat = data.getLong("Heat");
            long maxHeat = data.getLong("MaxHeat");

            if (maxHeat > 0) {
                tooltip.add(Component.translatable("jade.mio_icif.heat", heat, maxHeat));
            }
        }

        // 热能发生器额外显示当前发热速率（与 KU 的“产生”显示保持一致）
        if (data.contains("HeatOutput")) {
            int output = data.getInt("HeatOutput");
            if (output > 0) {
                tooltip.add(Component.translatable("jade.mio_icif.heat.generating", output));
            }
        }

        // 热交换机：对齐 IC2 TileEntityHeatSourceInventory#getOutput()，显示"最近一次实际传输 / 上限"。
        // 上面的 "HU: x / y" 是实时缓冲（被抽干再补满会 0↔上限跳），别把它当成输出不稳。
        if (data.contains("HeatTransmit")) {
            tooltip.add(Component.translatable("jade.mio_icif.heat.output",
                data.getInt("HeatTransmit"), data.getInt("HeatEmitLimit")));
        }
    }

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        BlockEntity blockEntity = accessor.getBlockEntity();
        if (blockEntity == null) {
            return;
        }

        IMioIcifCapabilities.IHeatStorage heatStorage = resolveHeatStorage(accessor, blockEntity);
        if (heatStorage != null) {
            long heat = heatStorage.getHeatStored();
            long maxHeat = heatStorage.getMaxHeatStored();

            data.putLong("Heat", heat);
            data.putLong("MaxHeat", maxHeat);
        }

        if (blockEntity instanceof IHeatGeneratorBlock heatGenerator) {
            data.putInt("HeatOutput", heatGenerator.getHeatOutput());
        }

        // IC2 TileEntityHeatSourceInventory#getOutput()：给出 "最近一次实际传输 / 上限"
        if (blockEntity instanceof com.miophas.singularity_iteration.common.blockentity.huentity.hugenerator.mio_icif_heat_source_fluid heatSource) {
            data.putInt("HeatTransmit", heatSource.getTransmitHeat());
            data.putInt("HeatEmitLimit", heatSource.getCurrentHeatOutput());
        } else if (blockEntity instanceof com.miophas.singularity_iteration.common.blockentity.huentity.hugenerator.mio_icif_heat_generator_elc heatGenerator) {
            data.putInt("HeatTransmit", heatGenerator.getTransmitHeat());
            data.putInt("HeatEmitLimit", heatGenerator.getMaxHeatEmittedPerTick());
        }
    }

    /**
     * 按优先级解析热能存储：
     * 方块实体自身 -> 已注册的 API 热能能力（无方向 / 当前朝向面）-> 能力适配器。
     * 这样即使某个方块只在特定面暴露热能能力，Jade 依然可以显示其热能数值。
     */
    private static IMioIcifCapabilities.IHeatStorage resolveHeatStorage(BlockAccessor accessor, BlockEntity blockEntity) {
        if (blockEntity instanceof IMioIcifCapabilities.IHeatStorage direct) {
            return direct;
        }

        IMioIcifCapabilities.IHeatStorage heatStorage = accessor.getLevel().getCapability(
                IMioIcifCapabilities.HEAT_STORAGE_BLOCK, blockEntity.getBlockPos(), null);
        if (heatStorage != null) {
            return heatStorage;
        }

        heatStorage = accessor.getLevel().getCapability(
                IMioIcifCapabilities.HEAT_STORAGE_BLOCK, blockEntity.getBlockPos(), accessor.getSide());
        if (heatStorage != null) {
            return heatStorage;
        }

        return MioIcifAPI.instance().getCapabilities().adaptHeatStorage(blockEntity);
    }

    @Override
    public ResourceLocation getUid() {
        return ID;
    }
}
