package com.miophas.singularity_iteration.common.network;

import com.miophas.singularity_iteration.common.blockentity.producer.mio_icif_chunk_loader;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

@SuppressWarnings("null")
public record ChunkLoaderTogglePacket(BlockPos pos, int dx, int dz) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ChunkLoaderTogglePacket> TYPE =
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("mio_icif", "chunk_loader_toggle"));

    public static final StreamCodec<FriendlyByteBuf, ChunkLoaderTogglePacket> CODEC =
        StreamCodec.of(
            (buf, packet) -> {
                buf.writeBlockPos(packet.pos);
                buf.writeInt(packet.dx);
                buf.writeInt(packet.dz);
            },
            buf -> new ChunkLoaderTogglePacket(buf.readBlockPos(), buf.readInt(), buf.readInt())
        );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ChunkLoaderTogglePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer serverPlayer) {
                Level level = serverPlayer.level();
                BlockPos pos = packet.pos;

                if (!level.isLoaded(pos) || serverPlayer.isSpectator()
                    || !(serverPlayer.containerMenu instanceof com.miophas.singularity_iteration.common.menu.producer.ChunkLoaderMenu menu)
                    || !pos.equals(menu.getBlockPos()) || !menu.stillValid(serverPlayer)) return;

                BlockEntity blockEntity = level.getBlockEntity(pos);
                if (blockEntity instanceof mio_icif_chunk_loader loader) {
                    if (menu.getBlockEntity() != loader || serverPlayer.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > 64.0) return;
                    if (!loader.isChunkInRange(packet.dx, packet.dz)) return;

                    ChunkPos selfChunk = loader.getSelfChunkPos();
                    ChunkPos targetChunk = new ChunkPos(selfChunk.x + packet.dx, selfChunk.z + packet.dz);
                    loader.toggleChunk(targetChunk);
                }
            }
        });
    }
}
