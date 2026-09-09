package com.singularity_iteration.mio_icif.network;

import com.singularity_iteration.mio_icif.api.item.IJetpackItem;
import com.singularity_iteration.mio_icif.Singularity_Iteration;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Jetpack mode switch packet
 * Client sends this to server when pressing the mode switch key
 * Server switches jetpack/quantum chestplate flight mode
 */
@SuppressWarnings("null")
public record JetpackModeSwitchPacket() implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<JetpackModeSwitchPacket> TYPE =
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "jetpack_mode_switch"));

    public static final StreamCodec<FriendlyByteBuf, JetpackModeSwitchPacket> CODEC = StreamCodec.of(
        (buf, packet) -> {},
        buf -> new JetpackModeSwitchPacket()
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(JetpackModeSwitchPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer serverPlayer) {
                ItemStack chestStack = serverPlayer.getItemBySlot(EquipmentSlot.CHEST);

                if (chestStack.getItem() instanceof IJetpackItem jetpack) {
                    IJetpackItem.JetpackMode currentMode = jetpack.getMode(chestStack);
                    IJetpackItem.JetpackMode newMode = (currentMode == IJetpackItem.JetpackMode.HOVER)
                        ? IJetpackItem.JetpackMode.FLIGHT
                        : IJetpackItem.JetpackMode.HOVER;
                    jetpack.setMode(chestStack, newMode);
                    Component modeName = newMode == IJetpackItem.JetpackMode.HOVER
                        ? Component.translatable("hud.mio_icif.jetpack.mode_hover")
                        : Component.translatable("hud.mio_icif.jetpack.mode_jetpack");
                    serverPlayer.sendSystemMessage(Component.translatable("message.mio_icif.jetpack.mode_switched", modeName));
                    serverPlayer.setItemSlot(EquipmentSlot.CHEST, chestStack);
                }
            }
        });
    }
}