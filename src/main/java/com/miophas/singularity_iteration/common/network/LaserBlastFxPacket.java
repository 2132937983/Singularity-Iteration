// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.network;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * 0.1.10.11, server to client: an Ultra-High Energy Blast of the mining laser went off at the
 * position with the given explosion strength; the client plays the plasma blast effect. Sent to
 * the players within {@link #RANGE} blocks.
 */
public record LaserBlastFxPacket(double x, double y, double z, float strength) implements CustomPacketPayload {
    public static final double RANGE = 192.0;

    public static final Type<LaserBlastFxPacket> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "laser_blast_fx"));

    public static final StreamCodec<FriendlyByteBuf, LaserBlastFxPacket> CODEC = StreamCodec.composite(
        ByteBufCodecs.DOUBLE, LaserBlastFxPacket::x,
        ByteBufCodecs.DOUBLE, LaserBlastFxPacket::y,
        ByteBufCodecs.DOUBLE, LaserBlastFxPacket::z,
        ByteBufCodecs.FLOAT, LaserBlastFxPacket::strength,
        LaserBlastFxPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(LaserBlastFxPacket packet, IPayloadContext context) {
        if (FMLEnvironment.dist != Dist.CLIENT) return;
        context.enqueueWork(() -> com.miophas.singularity_iteration.common.client.ClientPacketHandlers.handleLaserBlast(
            packet.x(), packet.y(), packet.z(), packet.strength()));
    }
}
