package com.miophas.singularity_iteration.common.integration.jade;

import com.miophas.singularity_iteration.common.blockentity.generator.SteamTunnelBlockEntity;
import java.util.Locale;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.JadeIds;
import snownee.jade.api.config.IPluginConfig;

/**
 * 0.1.10.39: the Steam Tunnel has no menu; Jade shows its live data: steam, water, EU buffer and output (EU per tick over the last
 * second). Jade's own fluid bars of the controller would repeat the steam and water, so they are removed; while the structure is not
 * formed, one more line says what is missing where.
 */
public enum SteamTunnelProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    INSTANCE;

    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("mio_icif", "steam_tunnel");

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        CompoundTag data = accessor.getServerData();
        if (!data.contains("Steam")) return;
        tooltip.remove(JadeIds.UNIVERSAL_FLUID_STORAGE);
        if (data.contains("Problem"))
            ComponentSerialization.CODEC.parse(accessor.getLevel().registryAccess().createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE),
                data.get("Problem")).result().ifPresent(tooltip::add);
        tooltip.add(Component.translatable("jade.mio_icif.steam_tunnel.steam", amount(data.getInt("Steam")), amount(SteamTunnelBlockEntity.STEAM_CAPACITY)));
        tooltip.add(Component.translatable("jade.mio_icif.steam_tunnel.water", amount(data.getInt("Water")), amount(SteamTunnelBlockEntity.WATER_CAPACITY)));
        tooltip.add(Component.translatable("jade.mio_icif.steam_tunnel.buffer", amount(data.getLong("Energy")), amount(SteamTunnelBlockEntity.ENERGY_CAPACITY)));
        tooltip.add(Component.translatable("jade.mio_icif.steam_tunnel.output", amount(data.getLong("Output"))));
    }

    private static String amount(long value) {
        return String.format(Locale.ROOT, "%,d", value);
    }

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        if (!(accessor.getBlockEntity() instanceof SteamTunnelBlockEntity tunnel)) return;
        if (!tunnel.isFormed())
            ComponentSerialization.CODEC.encodeStart(accessor.getLevel().registryAccess().createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE),
                tunnel.status()).result().ifPresent(tag -> data.put("Problem", tag));
        data.putInt("Steam", tunnel.steam().getAmount());
        data.putInt("Water", tunnel.water().getAmount());
        data.putLong("Energy", tunnel.energy());
        data.putLong("Output", tunnel.euPerTick());
    }

    @Override
    public ResourceLocation getUid() {
        return ID;
    }

    /** After Jade's fluid bars (priority 1000), so that it can remove them. */
    @Override
    public int getDefaultPriority() {
        return 1100;
    }
}
