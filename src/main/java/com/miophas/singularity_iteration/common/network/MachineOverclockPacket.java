// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.network;

import com.miophas.singularity_iteration.common.menu.base.mio_icif_base_menu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * C2S (0.1.10.6): the overclock slider of a machine GUI. Sets the share of the installed
 * overclockers that work (0..100 %) on the machine of the open menu. The server checks that the
 * menu is still open and valid; the value is clamped.
 */
public record MachineOverclockPacket(int containerId, int percent) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<MachineOverclockPacket> TYPE =
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("mio_icif", "machine_overclock"));

    public static final StreamCodec<FriendlyByteBuf, MachineOverclockPacket> CODEC = StreamCodec.of(
        (buf, packet) -> {
            buf.writeVarInt(packet.containerId);
            buf.writeByte(Math.max(0, Math.min(100, packet.percent)));
        },
        buf -> new MachineOverclockPacket(buf.readVarInt(), buf.readUnsignedByte()));

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(MachineOverclockPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player && player.containerMenu instanceof mio_icif_base_menu menu
                    && menu.containerId == packet.containerId) {
                menu.applyOverclockThrottle(player, packet.percent);
            }
        });
    }
}
