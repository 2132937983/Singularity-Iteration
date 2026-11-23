// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.network;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.common.menu.wiring.TesuOutputMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * 0.1.11.191, client to server: the output (EU/t) typed into the open TESU output port screen. The server checks the menu and
 * the distance; the port clamps the value to its range.
 */
@SuppressWarnings("null")
public record TesuOutputPacket(int containerId, long rate) implements CustomPacketPayload {
    public static final Type<TesuOutputPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "tesu_output_rate"));

    public static final StreamCodec<FriendlyByteBuf, TesuOutputPacket> CODEC = StreamCodec.of(
        (buf, p) -> { buf.writeVarInt(p.containerId); buf.writeVarLong(p.rate); },
        buf -> new TesuOutputPacket(buf.readVarInt(), buf.readVarLong()));

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(TesuOutputPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            if (player.containerMenu.containerId != packet.containerId
                    || !(player.containerMenu instanceof TesuOutputMenu menu) || !menu.stillValid(player)) return;
            menu.applyRate(packet.rate);
        });
    }
}
