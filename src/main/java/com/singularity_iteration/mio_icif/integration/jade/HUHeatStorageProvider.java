package com.singularity_iteration.mio_icif.integration.jade;

import com.singularity_iteration.mio_icif.energy.heat.HUCapabilities;
import com.singularity_iteration.mio_icif.energy.heat.IHeatStorage;
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
 * Jade HU 热能存储显示提供�? * 参数?EUEnergyStorageProvider 实现
 */
@SuppressWarnings("null")
public enum HUHeatStorageProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {

    INSTANCE;

    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("mio_icif", "hu_heat_storage");

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        CompoundTag data = accessor.getServerData();
        if (data.contains("Heat") && data.contains("MaxHeat")) {
            int heat = data.getInt("Heat");
            int maxHeat = data.getInt("MaxHeat");

            if (maxHeat > 0) {
                tooltip.add(Component.translatable("jade.mio_icif.heat", heat, maxHeat));
            }
        }
    }

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        BlockEntity blockEntity = accessor.getBlockEntity();
        if (blockEntity == null) {
            return;
        }

        // 先尝试用 null 方向查询方块本身的热能能力，避免�?specific 的方块在 Jade 中不显示
        IHeatStorage heatStorage = accessor.getLevel().getCapability(HUCapabilities.HeatStorage.BLOCK, blockEntity.getBlockPos(), null);
        if (heatStorage == null) {
            heatStorage = accessor.getLevel().getCapability(HUCapabilities.HeatStorage.BLOCK, blockEntity.getBlockPos(), accessor.getSide());
        }

        if (heatStorage != null) {
            long heat = heatStorage.getHeatStored();
            long maxHeat = heatStorage.getMaxHeatStored();

            data.putLong("Heat", heat);
            data.putLong("MaxHeat", maxHeat);
        }
    }

    @Override
    public ResourceLocation getUid() {
        return ID;
    }
}