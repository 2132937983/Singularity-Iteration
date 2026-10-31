package com.miophas.singularity_iteration.common.integration.jade;

import com.miophas.singularity_iteration.common.blockentity.generator.mio_icif_nuclear_reactor_generator;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

/**
 * 0.1.10.39: why a nuclear reactor makes nothing (off without a redstone signal, stopped with the reason, on hold, after an
 * accident), and in fluid mode that its heat leaves as hot coolant instead of EU. A player saw 0 EU/t at 5,567 heat and could not
 * tell the reactor was simply off.
 */
public enum ReactorStatusProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    INSTANCE;

    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("mio_icif", "reactor_status");

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        CompoundTag data = accessor.getServerData();
        if (!data.contains("ReactorStatus")) return;
        int status = data.getInt("ReactorStatus");
        if (status == mio_icif_nuclear_reactor_generator.STATUS_OFF)
            tooltip.add(Component.translatable("jade.mio_icif.reactor.off").withStyle(ChatFormatting.RED));
        else if (status == mio_icif_nuclear_reactor_generator.STATUS_STOPPED)
            tooltip.add(Component.translatable("jade.mio_icif.reactor.stopped", data.getString("ReactorFailure")).withStyle(ChatFormatting.RED));
        else if (status != mio_icif_nuclear_reactor_generator.STATUS_OK)
            tooltip.add(Component.translatable(mio_icif_nuclear_reactor_generator.statusKey(status)).withStyle(ChatFormatting.RED));
        if (data.getBoolean("ReactorFluidMode"))
            tooltip.add(Component.translatable("jade.mio_icif.reactor.fluid_mode", String.format(java.util.Locale.ROOT, "%,d", data.getInt("ReactorHeatOutput"))));
    }

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        if (!(accessor.getBlockEntity() instanceof mio_icif_nuclear_reactor_generator reactor)) return;
        int status = reactor.statusCode();
        data.putInt("ReactorStatus", status);
        if (status == mio_icif_nuclear_reactor_generator.STATUS_STOPPED) data.putString("ReactorFailure", reactor.getReactorFailure());
        if (reactor.isFluidMode()) {
            data.putBoolean("ReactorFluidMode", true);
            data.putInt("ReactorHeatOutput", reactor.getFluidHeatOutput());
        }
    }

    @Override
    public ResourceLocation getUid() {
        return ID;
    }
}
