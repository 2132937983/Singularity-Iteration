package com.miophas.singularity_iteration.common.integration.jade;

import com.miophas.singularity_iteration.common.blockentity.generator.SteamTunnelBlockEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

/** 0.1.10.39: the Steam Tunnel has no menu; Jade shows its state, steam, water, buffer and output. */
public enum SteamTunnelProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    INSTANCE;

    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("mio_icif", "steam_tunnel");

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        CompoundTag data = accessor.getServerData();
        if (!data.contains("TunnelStatus")) return;
        ComponentSerialization.CODEC.parse(accessor.getLevel().registryAccess().createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE),
            data.get("TunnelStatus")).result().ifPresent(tooltip::add);
        tooltip.add(Component.translatable("jade.mio_icif.steam_tunnel.fluids", data.getInt("Steam"), SteamTunnelBlockEntity.STEAM_CAPACITY,
            data.getInt("Water"), SteamTunnelBlockEntity.WATER_CAPACITY));
        tooltip.add(Component.translatable("jade.mio_icif.steam_tunnel.energy", data.getLong("Energy"), SteamTunnelBlockEntity.ENERGY_CAPACITY,
            data.getLong("Output")));
    }

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        if (!(accessor.getBlockEntity() instanceof SteamTunnelBlockEntity tunnel)) return;
        ComponentSerialization.CODEC.encodeStart(accessor.getLevel().registryAccess().createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE),
            tunnel.status()).result().ifPresent(tag -> data.put("TunnelStatus", tag));
        data.putInt("Steam", tunnel.steam().getAmount());
        data.putInt("Water", tunnel.water().getAmount());
        data.putLong("Energy", tunnel.energy());
        data.putLong("Output", tunnel.euPerTick());
    }

    @Override
    public ResourceLocation getUid() {
        return ID;
    }
}
