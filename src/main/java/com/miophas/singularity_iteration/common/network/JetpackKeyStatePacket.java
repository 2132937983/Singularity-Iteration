// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.network;

import com.miophas.singularity_iteration.core.prefab.flight.JetpackKeyHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client-to-server jetpack input snapshot. */
public record JetpackKeyStatePacket(int keyState) implements CustomPacketPayload {
    public static final Type<JetpackKeyStatePacket> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath("mio_icif", "jetpack_key_state"));
    public static final StreamCodec<FriendlyByteBuf, JetpackKeyStatePacket> CODEC = StreamCodec.of(
        (buffer, packet) -> buffer.writeVarInt(packet.keyState()),
        buffer -> new JetpackKeyStatePacket(buffer.readVarInt()));

    @Override
    public Type<JetpackKeyStatePacket> type() {
        return TYPE;
    }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> JetpackKeyHandler.processKeyUpdate(context.player(), keyState));
    }
}
