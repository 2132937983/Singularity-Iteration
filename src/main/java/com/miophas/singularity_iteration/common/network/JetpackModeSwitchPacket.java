// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.network;

import com.miophas.singularity_iteration.core.api.item.IJetpackItem;
import com.miophas.singularity_iteration.common.Singularity_Iteration;
import java.lang.reflect.Method;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.fml.ModList;

/** Explicit client request to toggle the worn jetpack's hover mode. */
public record JetpackModeSwitchPacket() implements CustomPacketPayload {
    public static final Type<JetpackModeSwitchPacket> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath("mio_icif", "jetpack_mode_switch"));
    public static final StreamCodec<FriendlyByteBuf, JetpackModeSwitchPacket> CODEC =
        StreamCodec.unit(new JetpackModeSwitchPacket());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(JetpackModeSwitchPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            ItemStack stack = findModeTarget(player);
            if (!(stack.getItem() instanceof IJetpackItem jetpack)) return;
            IJetpackItem.JetpackMode next = jetpack.getMode(stack) == IJetpackItem.JetpackMode.HOVER
                ? IJetpackItem.JetpackMode.FLIGHT
                : IJetpackItem.JetpackMode.HOVER;
            jetpack.setMode(stack, next);
            if (next == IJetpackItem.JetpackMode.HOVER) {
                player.setDeltaMovement(player.getDeltaMovement().x, 0.0, player.getDeltaMovement().z);
                player.hurtMarked = true;
                player.sendSystemMessage(Component.translatable("hud.mio_icif.jetpack.hover_enabled"));
            } else {
                player.sendSystemMessage(Component.translatable("hud.mio_icif.jetpack.hover_disabled"));
            }
        });
    }

    /** The normal chest slot wins; otherwise use the first functional Curios back slot. */
    public static ItemStack findModeTarget(Player player) {
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        if (chest.getCount() == 1 && chest.getItem() instanceof IJetpackItem) return chest;
        Method lookup = OptionalBackLookup.FIND;
        if (lookup == null) return ItemStack.EMPTY;
        try {
            return (ItemStack) lookup.invoke(null, player);
        } catch (ReflectiveOperationException | LinkageError failure) {
            if (!OptionalBackLookup.failed) {
                OptionalBackLookup.failed = true;
                Singularity_Iteration.LOGGER.warn("Cannot resolve the optional Curios jetpack mode target", failure);
            }
            return ItemStack.EMPTY;
        }
    }

    private static final class OptionalBackLookup {
        private static boolean failed;
        private static final Method FIND = find();

        private static Method find() {
            if (!ModList.get().isLoaded("curios")) return null;
            try {
                return Class.forName("com.miophas.singularity_iteration.common.integration.curios.JetpackCuriosAdapter")
                        .getMethod("findModeTarget", Player.class);
            } catch (ReflectiveOperationException | LinkageError failure) {
                Singularity_Iteration.LOGGER.warn("Curios jetpack mode integration is unavailable", failure);
                return null;
            }
        }
    }
}