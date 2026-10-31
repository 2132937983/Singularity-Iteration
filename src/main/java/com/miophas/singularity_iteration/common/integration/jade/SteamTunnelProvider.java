package com.miophas.singularity_iteration.common.integration.jade;

import com.miophas.singularity_iteration.common.blockentity.generator.SteamTunnelBlockEntity;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.ui.BoxStyle;
import snownee.jade.api.ui.IElementHelper;
import snownee.jade.api.ui.ProgressStyle;

/**
 * 0.1.10.39: the Steam Tunnel has no menu; Jade shows its live data. Steam and water are Jade's own fluid bars (the controller is a
 * fluid handler); this adds the EU buffer as a bar in the style of SI's EU display ({@link EUEnergyStorageProvider}) and the output
 * (EU per tick over the last second). While the structure is not formed, one more line says what is missing where. The keys are
 * the tunnel's own: SI's EU display reads {@code Energy} / {@code MaxEnergy} from the same data and would draw a second bar.
 */
public enum SteamTunnelProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    INSTANCE;

    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("mio_icif", "steam_tunnel");

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        CompoundTag data = accessor.getServerData();
        if (!data.contains("TunnelEnergy")) return;
        if (data.contains("TunnelProblem"))
            ComponentSerialization.CODEC.parse(accessor.getLevel().registryAccess().createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE),
                data.get("TunnelProblem")).result().ifPresent(tooltip::add);
        IElementHelper helper = IElementHelper.get();
        long energy = data.getLong("TunnelEnergy"), capacity = SteamTunnelBlockEntity.ENERGY_CAPACITY;
        Component text = Component.translatable("jade.mio_icif.energy", ChatFormatting.WHITE + amount(energy) + " EU", amount(capacity) + " EU");
        ProgressStyle style = helper.progressStyle().color(0xFF1FA2FF, 0xFF11598C);
        tooltip.add(helper.progress(Math.min(1f, (float) energy / capacity), text, style, BoxStyle.getNestedBox(), true));
        tooltip.add(Component.translatable("jade.mio_icif.steam_tunnel.output", amount(data.getLong("TunnelOutput"))));
    }

    private static String amount(long value) {
        return String.format(Locale.ROOT, "%,d", value);
    }

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        if (!(accessor.getBlockEntity() instanceof SteamTunnelBlockEntity tunnel)) return;
        if (!tunnel.isFormed())
            ComponentSerialization.CODEC.encodeStart(accessor.getLevel().registryAccess().createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE),
                tunnel.status()).result().ifPresent(tag -> data.put("TunnelProblem", tag));
        data.putLong("TunnelEnergy", tunnel.energy());
        data.putLong("TunnelOutput", tunnel.euPerTick());
    }

    @Override
    public ResourceLocation getUid() {
        return ID;
    }

    /** After Jade's fluid bars (priority 1000): steam, water, then EU and output. */
    @Override
    public int getDefaultPriority() {
        return 1100;
    }
}
