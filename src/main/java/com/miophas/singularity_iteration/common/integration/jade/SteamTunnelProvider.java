package com.miophas.singularity_iteration.common.integration.jade;

import com.miophas.singularity_iteration.common.blockentity.generator.SteamTunnelBlockEntity;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.MutableComponent;
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
 * (EU per tick over the last second) with its maximum, and (0.1.10.41) the steam use, the rotors inside and what it waits for (steam,
 * or room in a full buffer). While the structure is not formed, one more
 * line says what is missing where. The keys are the tunnel's own: SI's EU display reads {@code Energy} / {@code MaxEnergy} from the
 * same data and would draw a second bar.
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
        tooltip.add(Component.translatable("jade.mio_icif.steam_tunnel.output", amount(data.getLong("TunnelOutput")), amount(data.getLong("TunnelMaxOutput"))));
        tooltip.add(Component.translatable("jade.mio_icif.steam_tunnel.steam_use", amount(data.getLong("TunnelSteamUse")),
            amount((long) SteamTunnelBlockEntity.MAX_STEPS * SteamTunnelBlockEntity.STEAM_PER_STEP)));
        if (data.contains("TunnelRotors")) {   // 0.1.10.41: the count and tiers only
            ListTag rotors = data.getList("TunnelRotors", Tag.TAG_STRING);
            MutableComponent names = Component.empty();
            for (int i = 0; i < rotors.size(); i++) {
                if (i > 0) names.append(Component.translatable("jade.mio_icif.steam_tunnel.rotor_separator"));
                names.append(Component.translatable("jade.mio_icif.steam_tunnel.rotor." + rotors.getString(i)));
            }
            tooltip.add(Component.translatable("jade.mio_icif.steam_tunnel.rotors", rotors.size(), names));
        }
        int wait = data.getInt("TunnelWait");
        if (wait == SteamTunnelBlockEntity.WAIT_BUFFER)
            tooltip.add(Component.translatable("jade.mio_icif.steam_tunnel.buffer_full").withStyle(ChatFormatting.GOLD));
        else if (wait == SteamTunnelBlockEntity.WAIT_STEAM)
            tooltip.add(Component.translatable("jade.mio_icif.steam_tunnel.waiting_steam").withStyle(ChatFormatting.GOLD));
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
        data.putLong("TunnelMaxOutput", tunnel.maxEuPerTick());
        data.putLong("TunnelSteamUse", tunnel.steamUsePerTick());
        data.putInt("TunnelWait", tunnel.waitReason());
        if (!tunnel.rotors().isEmpty()) {
            ListTag rotors = new ListTag();
            for (var tier : tunnel.rotors()) rotors.add(StringTag.valueOf(tier.id()));
            data.put("TunnelRotors", rotors);
        }
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
