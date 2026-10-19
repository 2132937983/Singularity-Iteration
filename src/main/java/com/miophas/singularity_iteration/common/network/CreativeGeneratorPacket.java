// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.network;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.common.menu.generator.CreativeGeneratorMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * 0.1.10.11, client to server: the output (EU/t) typed into the open Creative Generator screen. The
 * server checks the menu, the distance and the player (creative or operator) and clamps the value.
 */
@SuppressWarnings("null")
public record CreativeGeneratorPacket(int containerId, long rate) implements CustomPacketPayload {
    public static final Type<CreativeGeneratorPacket> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "creative_generator_rate"));

    public static final StreamCodec<FriendlyByteBuf, CreativeGeneratorPacket> CODEC = StreamCodec.of(
        (buf, p) -> { buf.writeVarInt(p.containerId); buf.writeVarLong(p.rate); },
        buf -> new CreativeGeneratorPacket(buf.readVarInt(), buf.readVarLong()));

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(CreativeGeneratorPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            if (player.containerMenu.containerId != packet.containerId
                    || !(player.containerMenu instanceof CreativeGeneratorMenu menu) || !menu.stillValid(player)) return;
            menu.applyRate(player, packet.rate);
        });
    }
}
