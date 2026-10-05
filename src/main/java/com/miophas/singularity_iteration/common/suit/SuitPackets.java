// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.suit;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/** Network messages of the suit sensors. Client data lands in {@link SuitSensorData}. */
public final class SuitPackets {
    private SuitPackets() {}

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, path);
    }

    /** Server -> wearer: entities that watch (1) or target (2) the wearer. */
    public record Threats(int[] ids, byte[] levels) implements CustomPacketPayload {
        public static final Type<Threats> TYPE = new Type<>(id("suit_threats"));
        public static final StreamCodec<FriendlyByteBuf, Threats> CODEC = StreamCodec.of((b, p) -> {
            b.writeVarInt(p.ids.length);
            for (int i = 0; i < p.ids.length; i++) { b.writeVarInt(p.ids[i]); b.writeByte(p.levels[i]); }
        }, b -> {
            int n = Math.min(256, b.readVarInt());
            int[] ids = new int[n];
            byte[] levels = new byte[n];
            for (int i = 0; i < n; i++) { ids[i] = b.readVarInt(); levels[i] = b.readByte(); }
            return new Threats(ids, levels);
        });
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** Server -> tracking clients: the deflector stopped a hit from this direction. */
    public record ShieldHit(int entityId, float dx, float dy, float dz, float strength) implements CustomPacketPayload {
        public static final Type<ShieldHit> TYPE = new Type<>(id("suit_shield_hit"));
        public static final StreamCodec<ByteBuf, ShieldHit> CODEC = StreamCodec.of((b, p) -> {
            FriendlyByteBuf f = new FriendlyByteBuf(b);
            f.writeVarInt(p.entityId); f.writeFloat(p.dx); f.writeFloat(p.dy); f.writeFloat(p.dz); f.writeFloat(p.strength);
        }, b -> {
            FriendlyByteBuf f = new FriendlyByteBuf(b);
            return new ShieldHit(f.readVarInt(), f.readFloat(), f.readFloat(), f.readFloat(), f.readFloat());
        });
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** Client -> server: summary of the EU grid in the player's chunk, please. */
    public record ChunkGridRequest() implements CustomPacketPayload {
        public static final Type<ChunkGridRequest> TYPE = new Type<>(id("suit_chunk_grid_request"));
        public static final StreamCodec<ByteBuf, ChunkGridRequest> CODEC = StreamCodec.unit(new ChunkGridRequest());
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** Server -> client: EU grid summary of one chunk (last tick). */
    public record ChunkGrid(int chunkX, int chunkZ, int generators, int consumers, int storages, int cables,
                            float generated, float consumed, float maxVoltage, long stored, long capacity, int overvolted)
        implements CustomPacketPayload {
        public static final Type<ChunkGrid> TYPE = new Type<>(id("suit_chunk_grid"));
        public static final StreamCodec<FriendlyByteBuf, ChunkGrid> CODEC = StreamCodec.of((b, p) -> {
            b.writeInt(p.chunkX); b.writeInt(p.chunkZ); b.writeVarInt(p.generators); b.writeVarInt(p.consumers);
            b.writeVarInt(p.storages); b.writeVarInt(p.cables); b.writeFloat(p.generated); b.writeFloat(p.consumed);
            b.writeFloat(p.maxVoltage); b.writeVarLong(Math.max(0, p.stored)); b.writeVarLong(Math.max(0, p.capacity)); b.writeVarInt(p.overvolted);
        }, b -> new ChunkGrid(b.readInt(), b.readInt(), b.readVarInt(), b.readVarInt(), b.readVarInt(), b.readVarInt(),
            b.readFloat(), b.readFloat(), b.readFloat(), b.readVarLong(), b.readVarLong(), b.readVarInt()));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public static void register(PayloadRegistrar registrar) {
        registrar.playToClient(Threats.TYPE, Threats.CODEC, (p, c) -> client(c, () -> SuitSensorData.acceptThreats(p.ids, p.levels)));
        registrar.playToClient(ShieldHit.TYPE, ShieldHit.CODEC, (p, c) -> client(c, () -> SuitSensorData.acceptShieldHit(p)));
        registrar.playToClient(ChunkGrid.TYPE, ChunkGrid.CODEC, (p, c) -> client(c, () -> SuitSensorData.acceptChunkGrid(p)));
        registrar.playToServer(ChunkGridRequest.TYPE, ChunkGridRequest.CODEC, (p, c) -> c.enqueueWork(() -> {
            if (c.player() instanceof ServerPlayer player) SuitServer.answerChunkGrid(player);
        }));
    }

    private static void client(IPayloadContext context, Runnable work) {
        if (FMLEnvironment.dist != Dist.CLIENT) return;
        context.enqueueWork(work);
    }

    /** Only to connections that negotiated the channel (fake and test players are skipped). */
    public static void send(ServerPlayer player, CustomPacketPayload payload) {
        if (player.connection == null || !player.connection.hasChannel(payload.type())) return;
        PacketDistributor.sendToPlayer(player, payload);
    }
}
