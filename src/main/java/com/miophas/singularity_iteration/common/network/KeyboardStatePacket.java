package com.miophas.singularity_iteration.common.network;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Key state sync packet (ported from the original IC2RIn120).
 *
 * The client checks the key state each tick. When the state changes, the client sends this packet to the server.
 * The server stores the key state of the player. inventoryTick reads this state.
 *
 * IC2 equivalent: KeyboardClient.sendKeyUpdate() and IC2.network.initiateKeyUpdate().
 */
@SuppressWarnings("null")
public record KeyboardStatePacket(int keyState) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<KeyboardStatePacket> TYPE =
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "keyboard_state"));

    public static final StreamCodec<FriendlyByteBuf, KeyboardStatePacket> CODEC = StreamCodec.of(
        (buf, packet) -> buf.writeInt(packet.keyState),
        buf -> new KeyboardStatePacket(buf.readInt())
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /**
     * Processes the key state update.
     * IC2 equivalent: Keyboard.processKeyUpdate()
     */
    public static void handle(KeyboardStatePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() == null) return;
            mio_icif_KeyboardManager.processKeyUpdate(context.player(), packet.keyState());
        });
    }
}

