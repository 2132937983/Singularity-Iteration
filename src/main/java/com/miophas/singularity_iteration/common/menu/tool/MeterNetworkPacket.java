package com.miophas.singularity_iteration.common.menu.tool;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Server -> client: what the EU meter / voltage detector sees on the network around its target,
 * plus the last per-tick samples of throughput (EU/t delivered into endpoints) and packet voltage,
 * for the oscilloscope. Sent every few ticks while the meter is open.
 */
public record MeterNetworkPacket(int containerId, boolean conductor, long ratedPacket, int conductors, int generators,
                                 int consumers, int storages, int transformers, int subnets,
                                 float[] throughput, float[] voltage) implements CustomPacketPayload {
    public static final Type<MeterNetworkPacket> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "meter_network"));

    public static final StreamCodec<FriendlyByteBuf, MeterNetworkPacket> CODEC = StreamCodec.of(
        (buf, p) -> {
            buf.writeVarInt(p.containerId); buf.writeBoolean(p.conductor); buf.writeVarLong(Math.max(0, p.ratedPacket));
            buf.writeVarInt(p.conductors); buf.writeVarInt(p.generators); buf.writeVarInt(p.consumers);
            buf.writeVarInt(p.storages); buf.writeVarInt(p.transformers); buf.writeVarInt(p.subnets);
            buf.writeVarInt(p.throughput.length);
            for (int i = 0; i < p.throughput.length; i++) { buf.writeFloat(p.throughput[i]); buf.writeFloat(p.voltage[i]); }
        },
        buf -> {
            int id = buf.readVarInt(); boolean cond = buf.readBoolean(); long rated = buf.readVarLong();
            int c = buf.readVarInt(), g = buf.readVarInt(), u = buf.readVarInt(), s = buf.readVarInt(), t = buf.readVarInt(), n = buf.readVarInt();
            int len = Math.min(64, buf.readVarInt());
            float[] tp = new float[len], v = new float[len];
            for (int i = 0; i < len; i++) { tp[i] = buf.readFloat(); v[i] = buf.readFloat(); }
            return new MeterNetworkPacket(id, cond, rated, c, g, u, s, t, n, tp, v);
        });

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(MeterNetworkPacket packet, IPayloadContext context) {
        if (FMLEnvironment.dist != Dist.CLIENT) return;
        context.enqueueWork(() -> {
            var player = context.player();
            if (player != null && player.containerMenu instanceof mio_icif_meter_menu meter && meter.containerId == packet.containerId) {
                meter.acceptNetwork(packet);
            }
        });
    }
}
