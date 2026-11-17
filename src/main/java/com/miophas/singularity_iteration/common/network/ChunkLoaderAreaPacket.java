package com.miophas.singularity_iteration.common.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * 0.1.11.38 (chunk loader audit C2): selects ({@code select}) or clears a rectangle of chunk offsets of the chunk loader in
 * the open menu: a drag over the map, "select all" (the whole 9 x 9, nearest first up to the limit) and "clear" (all but
 * the loader's own chunk). The same checks as {@link ChunkLoaderTogglePacket}.
 */
public record ChunkLoaderAreaPacket(BlockPos pos, int dx0, int dz0, int dx1, int dz1, boolean select) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ChunkLoaderAreaPacket> TYPE =
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("mio_icif", "chunk_loader_area"));

    public static final StreamCodec<FriendlyByteBuf, ChunkLoaderAreaPacket> CODEC = StreamCodec.of(
        (buf, p) -> { buf.writeBlockPos(p.pos); buf.writeVarInt(p.dx0); buf.writeVarInt(p.dz0); buf.writeVarInt(p.dx1); buf.writeVarInt(p.dz1); buf.writeBoolean(p.select); },
        buf -> new ChunkLoaderAreaPacket(buf.readBlockPos(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readBoolean()));

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(ChunkLoaderAreaPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            var loader = ChunkLoaderTogglePacket.editable(player, packet.pos);
            if (loader != null) loader.selectArea(packet.dx0, packet.dz0, packet.dx1, packet.dz1, packet.select);
        });
    }
}
