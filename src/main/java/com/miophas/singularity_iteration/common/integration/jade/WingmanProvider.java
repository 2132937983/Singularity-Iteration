package com.miophas.singularity_iteration.common.integration.jade;

import com.miophas.singularity_iteration.common.armory.WingmanEntity;
import com.miophas.singularity_iteration.common.armory.WingmanService;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

/**
 * 0.1.11.41 (Wingman audit W2): Jade on a Wingman: its suit number and owner, its mode or state, and the flight time its
 * suit charge and its upkeep reserve pay for (the reserve in EU too). The server knows the reserve; the client does not.
 */
public enum WingmanProvider implements IEntityComponentProvider, IServerDataProvider<EntityAccessor> {
    INSTANCE;

    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("mio_icif", "wingman");

    @Override
    public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config) {
        CompoundTag data = accessor.getServerData();
        if (!data.contains("WingSet")) return;
        String owner = data.getString("WingOwner");
        tooltip.add(Component.translatable("jade.mio_icif.wingman.suit", data.getInt("WingSet") + 1,
            owner.isEmpty() ? Component.translatable("jade.mio_icif.wingman.owner_away") : Component.literal(owner)));
        int state = data.getInt("WingState");
        tooltip.add(state == WingmanEntity.ACTIVE ? Component.translatable(data.getString("WingMode"))
            : Component.translatable("gui.mio_icif.wingman.state." + state).withStyle(ChatFormatting.GOLD));
        int endurance = data.getInt("WingEndurance");
        Component left = endurance == WingmanEntity.ENDURANCE_FREE ? Component.literal("∞")
            : endurance == WingmanEntity.ENDURANCE_ARMORY ? Component.translatable("gui.mio_icif.wingman.endurance.armory")
            : Component.literal(WingmanService.formatSeconds(endurance));
        tooltip.add(Component.translatable("jade.mio_icif.wingman.endurance", left,
            String.format(Locale.ROOT, "%,d", data.getLong("WingReserve"))).withStyle(endurance >= 0 && endurance < 60 ? ChatFormatting.RED : ChatFormatting.WHITE));
    }

    @Override
    public void appendServerData(CompoundTag data, EntityAccessor accessor) {
        if (!(accessor.getEntity() instanceof WingmanEntity w)) return;
        data.putInt("WingSet", w.set());
        ServerPlayer owner = w.owner() == null || w.level().getServer() == null ? null : w.level().getServer().getPlayerList().getPlayer(w.owner());
        data.putString("WingOwner", owner == null ? "" : owner.getGameProfile().getName());
        data.putInt("WingState", w.state());
        data.putString("WingMode", w.mode().key());
        data.putInt("WingEndurance", w.enduranceSeconds());
        data.putLong("WingReserve", w.reserve());
    }

    @Override
    public ResourceLocation getUid() {
        return ID;
    }
}
