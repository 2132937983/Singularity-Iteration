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

/**
 * Payloads of the suit sensors (threat sensor, deflector, grid telemetry) and of the Special
 * Maneuver Mode. {@link SuitSensorData} stores the data that the client receives.
 *
 * <h2>Payloads</h2>
 * <table>
 *   <caption>Suit payloads</caption>
 *   <tr><th>Payload</th><th>Direction</th><th>Purpose</th></tr>
 *   <tr><td>{@link Threats}</td><td>server to client</td><td>entities that watch or target the wearer</td></tr>
 *   <tr><td>{@link ShieldHit}</td><td>server to client</td><td>one hit that the deflector stopped</td></tr>
 *   <tr><td>{@link ChunkGridRequest}</td><td>client to server</td><td>asks for the EU grid summary of the player's chunk</td></tr>
 *   <tr><td>{@link ChunkGrid}</td><td>server to client</td><td>EU grid summary of one chunk</td></tr>
 *   <tr><td>{@link ManeuverInput}</td><td>client to server</td><td>throttle and stick of the local flyer</td></tr>
 *   <tr><td>{@link ManeuverState}</td><td>server to client</td><td>throttle and stick of a different flyer</td></tr>
 * </table>
 *
 * <h2>Threading</h2>
 * {@link #register} does not call {@code executesOn}. Thus NeoForge 1.21 runs each handler on
 * the main thread (the server thread or the client main thread). On the main thread,
 * {@link IPayloadContext#enqueueWork} runs the task at once. The {@code enqueueWork} calls stay
 * so that the handlers are also correct if the registrar changes to the network thread.
 *
 * <h2>Missing channel</h2>
 * The channels are not optional. NeoForge refuses a vanilla client at login. A fake player or
 * a test player has no connection or a connection without the channel.
 * {@link #send(ServerPlayer, CustomPacketPayload)} skips such players. Use it for each
 * server-to-client payload of this class.
 */
public final class SuitPackets {
    private SuitPackets() {}

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, path);
    }

    /**
     * Server to client (the wearer only): the full threat list of the threat sensor.
     *
     * <p>When: {@code SuitServer} sends it every {@code SuitServer.THREAT_INTERVAL} (5) ticks
     * while the threat sensor module is active. Each packet replaces the previous list. When
     * the module stops, the server sends one empty list.
     *
     * <p>Fields:
     * <ul>
     *   <li>{@code ids}: network entity ids of living entities in {@code SuitServer.THREAT_RANGE}
     *       (32 blocks) of the wearer. The server sends at most 256 entries.</li>
     *   <li>{@code levels}: threat level per entry, same index as {@code ids}.
     *       1 = watches the wearer (yellow). 2 = targets the wearer (red).</li>
     * </ul>
     * The two arrays must have the same length. The encoder reads {@code levels[i]} for each id.
     *
     * <p>Receiver: the decoder reads at most 256 entries. The client keeps the list for
     * {@link SuitSensorData#THREAT_TTL_MS} only.
     */
    public record Threats(int[] ids, byte[] levels) implements CustomPacketPayload {
        /** Maximum number of entries in one packet. */
        public static final int MAX = 256;
        public static final Type<Threats> TYPE = new Type<>(id("suit_threats"));
        public static final StreamCodec<FriendlyByteBuf, Threats> CODEC = StreamCodec.of((b, p) -> {
            int count = com.miophas.singularity_iteration.common.util.PacketLimits.writeCount(b, Math.min(p.ids.length, p.levels.length), MAX);
            for (int i = 0; i < count; i++) { b.writeVarInt(p.ids[i]); b.writeByte(p.levels[i]); }
        }, b -> {
            int n = com.miophas.singularity_iteration.common.util.PacketLimits.readCount(b, MAX, "threat");
            int[] ids = new int[n];
            byte[] levels = new byte[n];
            for (int i = 0; i < n; i++) { ids[i] = b.readVarInt(); levels[i] = b.readByte(); }
            return new Threats(ids, levels);
        });
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /**
     * Server to client: the deflector of a wearer stopped damage from one direction.
     *
     * <p>When: {@code SuitServer.onDamage} sends it once per hit that the deflector reduced.
     * Receivers: the wearer and each player in the same level within 96 blocks of the wearer.
     *
     * <p>Fields:
     * <ul>
     *   <li>{@code entityId}: network entity id of the wearer.</li>
     *   <li>{@code dx}, {@code dy}, {@code dz}: unit vector from the wearer's centre to the
     *       source of the hit. Each component is in -1..1.</li>
     *   <li>{@code strength}: effect size, 0.25..1. The server calculates
     *       {@code min(1, 0.25 + stopped damage / 10)}.</li>
     * </ul>
     *
     * <p>Receiver: the client stores at most 32 hits. Each hit shows for
     * {@link SuitSensorData#SHIELD_HIT_MS}.
     */
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

    /**
     * Client to server: asks for the EU grid summary of the chunk that the player is in.
     * The payload has no fields.
     *
     * <p>When: {@code FcsClient} sends it once per 20 client ticks while the HUD shows the grid
     * telemetry module.
     *
     * <p>Trust boundary: the client chooses no chunk. The server uses the player's own chunk.
     * {@code SuitServer.answerChunkGrid} replies only when the grid telemetry module is
     * active. It replies at most once per 15 game ticks per player. It ignores all other
     * requests. The server answers with {@link ChunkGrid}.
     */
    public record ChunkGridRequest() implements CustomPacketPayload {
        public static final Type<ChunkGridRequest> TYPE = new Type<>(id("suit_chunk_grid_request"));
        public static final StreamCodec<ByteBuf, ChunkGridRequest> CODEC = StreamCodec.unit(new ChunkGridRequest());
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /**
     * Server to client: EU grid summary of one chunk, from the energy-net statistics of the
     * last tick. The server sends it only as the answer to {@link ChunkGridRequest}.
     *
     * <p>Fields:
     * <ul>
     *   <li>{@code chunkX}, {@code chunkZ}: chunk coordinates (block coordinate / 16).</li>
     *   <li>{@code generators}, {@code consumers}, {@code storages}, {@code cables}: counts of
     *       block entities per category, 0 or more. The server reads at most 2048 block entities
     *       per chunk.</li>
     *   <li>{@code generated}: EU/t that the generators sent out in the last tick.</li>
     *   <li>{@code consumed}: EU/t that the consumers received in the last tick.</li>
     *   <li>{@code maxVoltage}: highest packet voltage (EU per packet) in the chunk.</li>
     *   <li>{@code stored}, {@code capacity}: EU stored and EU capacity of the machines. The
     *       encoder writes values below 0 as 0.</li>
     *   <li>{@code overvolted}: number of consumers that receive a voltage above their tier.</li>
     * </ul>
     * The client shows the summary for 3 s ({@link SuitSensorData#chunkGrid()}).
     */
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

    /**
     * Client to server: the throttle and stick of the local flyer in Special Maneuver Mode.
     *
     * <p>When: {@code ManeuverModeClient.sendState} sends it when a value changes, at most
     * once per 2 client ticks. It also sends it once per 20 client ticks without a change, and
     * immediately when the flight stops. The client sends nothing when the server has no channel.
     *
     * <p>Fields (client values, not trusted):
     * <ul>
     *   <li>{@code throttle}: 0..1. The client sends 0 when it does not fly.</li>
     *   <li>{@code forward}, {@code sideways}: stick deflection, -1..1.</li>
     *   <li>{@code locked}: true when the player locked the throttle.</li>
     * </ul>
     *
     * <p>Trust boundary: the handler drops the packet when a float is NaN or infinite.
     * {@code ManeuverMode.relayState} clamps each value to its range. It sets the throttle to 0
     * when the player is not armed for the mode. The server uses these values only to relay a
     * {@link ManeuverState} for visuals. The flight physics do not read them. The handler drops
     * packets above the {@code PacketRateLimit.MANEUVER_INPUT} limit (10 in a burst, then 1 per
     * tick; 0.1.7.36).
     */
    public record ManeuverInput(float throttle, float forward, float sideways, boolean locked) implements CustomPacketPayload {
        public static final Type<ManeuverInput> TYPE = new Type<>(id("maneuver_input"));
        public static final StreamCodec<ByteBuf, ManeuverInput> CODEC = StreamCodec.of((b, p) -> {
            FriendlyByteBuf f = new FriendlyByteBuf(b);
            f.writeFloat(p.throttle); f.writeFloat(p.forward); f.writeFloat(p.sideways); f.writeBoolean(p.locked);
        }, b -> {
            FriendlyByteBuf f = new FriendlyByteBuf(b);
            return new ManeuverInput(f.readFloat(), f.readFloat(), f.readFloat(), f.readBoolean());
        });
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /**
     * Server to client: the throttle and stick of a different flyer. The client uses them for
     * the pose, the vapour cone and the wake trails of that flyer.
     *
     * <p>When: {@code ManeuverMode.relayState} sends one packet per received
     * {@link ManeuverInput}. Receivers: each other player in the same level within 160 blocks of
     * the flyer. The flyer does not receive its own state.
     *
     * <p>Fields:
     * <ul>
     *   <li>{@code entityId}: network entity id of the flyer.</li>
     *   <li>{@code throttle}: 0..1 (clamped by the server).</li>
     *   <li>{@code forward}, {@code sideways}: -1..1 (clamped by the server).</li>
     *   <li>{@code locked}: true when the flyer locked the throttle.</li>
     * </ul>
     * The client ignores a state that is older than 3 s ({@link SuitSensorData#maneuver(int)}).
     */
    public record ManeuverState(int entityId, float throttle, float forward, float sideways, boolean locked) implements CustomPacketPayload {
        public static final Type<ManeuverState> TYPE = new Type<>(id("maneuver_state"));
        public static final StreamCodec<ByteBuf, ManeuverState> CODEC = StreamCodec.of((b, p) -> {
            FriendlyByteBuf f = new FriendlyByteBuf(b);
            f.writeVarInt(p.entityId); f.writeFloat(p.throttle); f.writeFloat(p.forward); f.writeFloat(p.sideways); f.writeBoolean(p.locked);
        }, b -> {
            FriendlyByteBuf f = new FriendlyByteBuf(b);
            return new ManeuverState(f.readVarInt(), f.readFloat(), f.readFloat(), f.readFloat(), f.readBoolean());
        });
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /**
     * Registers the six suit payloads. {@code mio_icif_Network.register} calls this method.
     *
     * @param registrar the registrar of the mod channel version
     */
    public static void register(PayloadRegistrar registrar) {
        // Client values: reject NaN and infinity here. relayState clamps the ranges.
        registrar.playToServer(ManeuverInput.TYPE, ManeuverInput.CODEC, (p, c) -> c.enqueueWork(() -> {
            if (c.player() instanceof ServerPlayer player && com.miophas.singularity_iteration.common.util.PacketRateLimit.allow(player, com.miophas.singularity_iteration.common.util.PacketRateLimit.MANEUVER_INPUT) && Float.isFinite(p.throttle()) && Float.isFinite(p.forward()) && Float.isFinite(p.sideways()))
                ManeuverMode.relayState(player, p.throttle(), p.forward(), p.sideways(), p.locked());
        }));
        registrar.playToClient(ManeuverState.TYPE, ManeuverState.CODEC, (p, c) -> client(c, () -> SuitSensorData.acceptManeuver(p)));
        registrar.playToClient(Threats.TYPE, Threats.CODEC, (p, c) -> client(c, () -> SuitSensorData.acceptThreats(p.ids, p.levels)));
        registrar.playToClient(ShieldHit.TYPE, ShieldHit.CODEC, (p, c) -> client(c, () -> SuitSensorData.acceptShieldHit(p)));
        registrar.playToClient(ChunkGrid.TYPE, ChunkGrid.CODEC, (p, c) -> client(c, () -> SuitSensorData.acceptChunkGrid(p)));
        // No client data: the server picks the chunk. answerChunkGrid applies the module check and the rate limit.
        registrar.playToServer(ChunkGridRequest.TYPE, ChunkGridRequest.CODEC, (p, c) -> c.enqueueWork(() -> {
            if (c.player() instanceof ServerPlayer player) SuitServer.answerChunkGrid(player);
        }));
        // 0.1.8.0 tactical structure scanner: request (holomap unit check and rate limit on the server) and result
        registrar.playToServer(TacticalScan.Request.TYPE, TacticalScan.Request.CODEC, (p, c) -> c.enqueueWork(() -> {
            if (c.player() instanceof ServerPlayer player && com.miophas.singularity_iteration.common.util.PacketRateLimit.allow(player,
                    com.miophas.singularity_iteration.common.util.PacketRateLimit.TACTICAL_SCAN)) TacticalScan.answer(player);
        }));
        registrar.playToClient(TacticalScan.Result.TYPE, TacticalScan.Result.CODEC, (p, c) -> client(c, () -> {
            TacticalScan.LAST = p;
            Runnable hook = TacticalScan.CLIENT_HOOK;
            if (hook != null) hook.run();
        }));
        // 0.1.8.0 Smart Trajectory Guidance: locked target ids; the server checks each one when it uses it.
        registrar.playToServer(SmartGuidance.Locks.TYPE, SmartGuidance.Locks.CODEC, (p, c) -> c.enqueueWork(() -> {
            if (c.player() instanceof ServerPlayer player && com.miophas.singularity_iteration.common.util.PacketRateLimit.allow(player,
                    com.miophas.singularity_iteration.common.util.PacketRateLimit.SMART_GUIDANCE)) SmartGuidance.setLocks(player, p.ids());
        }));
        // 0.1.8.5 nano suit: burst keys, key assignment, core state and burst effects
        com.miophas.singularity_iteration.common.suit.nano.NanoPackets.register(registrar);
    }

    /**
     * Runs a client-side handler task. On a dedicated server the task does not run.
     * The guard keeps client-only work out of the server distribution.
     */
    private static void client(IPayloadContext context, Runnable work) {
        if (FMLEnvironment.dist != Dist.CLIENT) return;
        context.enqueueWork(work);
    }

    /**
     * Sends {@code payload} to {@code player} only when the connection negotiated the channel.
     * A fake player or a test player has no connection or no channel. This method skips such a
     * player. Without this check, NeoForge throws {@code UnsupportedOperationException} for a
     * connection without the channel.
     *
     * @param player  the receiver
     * @param payload a server-to-client payload of this class
     */
    public static void send(ServerPlayer player, CustomPacketPayload payload) {
        if (player.connection == null || !player.connection.hasChannel(payload.type())) return;
        PacketDistributor.sendToPlayer(player, payload);
    }
}
