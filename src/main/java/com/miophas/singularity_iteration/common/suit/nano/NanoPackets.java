// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.suit.nano;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.common.suit.SuitPackets;
import com.miophas.singularity_iteration.common.util.PacketRateLimit;
import io.netty.buffer.ByteBuf;
import java.util.function.Consumer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Payloads of the nano suit (0.1.8.5).
 *
 * <table>
 *   <caption>Nano payloads</caption>
 *   <tr><th>Payload</th><th>Direction</th><th>Purpose</th></tr>
 *   <tr><td>{@link BurstRequest}</td><td>client to server</td><td>a burst key press with the movement input</td></tr>
 *   <tr><td>{@link BurstAssign}</td><td>client to server</td><td>panel: which unit a burst key fires</td></tr>
 *   <tr><td>{@link CoreSync}</td><td>server to client</td><td>reserve, heat, cooldowns and keys of the wearer (the wearer only)</td></tr>
 *   <tr><td>{@link Fx}</td><td>server to client</td><td>a burst effect for the wearer and the players near it</td></tr>
 * </table>
 *
 * Trust boundary: the server checks every burst request against the worn units, the reserve, the
 * heat and the cooldown. The movement input only chooses the dash direction (clamped, NaN dropped).
 */
public final class NanoPackets {
    private NanoPackets() {}

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, path);
    }

    /** Client hooks (set by the client set-up; null on a dedicated server). */
    public static volatile Consumer<CoreSync> SYNC_HOOK;
    public static volatile Consumer<Fx> FX_HOOK;

    /** Client to server: burst key {@code slot} (0 = primary V, 1 = secondary B) pressed; {@code forward}/{@code strafe} -1..1. */
    public record BurstRequest(byte slot, float forward, float strafe) implements CustomPacketPayload {
        public static final Type<BurstRequest> TYPE = new Type<>(id("nano_burst"));
        public static final StreamCodec<ByteBuf, BurstRequest> CODEC = StreamCodec.of((b, p) -> {
            b.writeByte(p.slot); b.writeFloat(p.forward); b.writeFloat(p.strafe);
        }, b -> new BurstRequest(b.readByte(), b.readFloat(), b.readFloat()));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** Client to server: burst key {@code slot} fires unit {@code key}; "" = automatic, "-" = nothing. */
    public record BurstAssign(byte slot, String key) implements CustomPacketPayload {
        public static final Type<BurstAssign> TYPE = new Type<>(id("nano_burst_assign"));
        public static final StreamCodec<FriendlyByteBuf, BurstAssign> CODEC = StreamCodec.of((b, p) -> {
            b.writeByte(p.slot); b.writeUtf(p.key, 128);
        }, b -> new BurstAssign(b.readByte(), b.readUtf(128)));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /**
     * Server to client (the wearer): state of the nano core. Sent on change, at most every 2 ticks,
     * and once a second without change while a nano piece is worn. 0.1.10.0: the keys also carry the
     * quantum actions; {@code quantum} is true when a key fires one (the HUD and the panel then show
     * the keys without a nano core).
     */
    public record CoreSync(float reserve, float heat, float heatCap, boolean overheated, int camoTicks, int camoMax,
                           String primaryChoice, String secondaryChoice, String primaryKey, String secondaryKey,
                           int primaryCooldown, int primaryCooldownMax, int secondaryCooldown, int secondaryCooldownMax,
                           boolean online, int meshTicks, int meshMax, int reconTicks, int reconMax, int hfLevel, int generated,
                           float resumeHeat, boolean quantum) implements CustomPacketPayload {
        public static final Type<CoreSync> TYPE = new Type<>(id("nano_core_sync"));
        public static final StreamCodec<FriendlyByteBuf, CoreSync> CODEC = StreamCodec.of((b, p) -> {
            b.writeFloat(p.reserve); b.writeFloat(p.heat); b.writeFloat(p.heatCap); b.writeBoolean(p.overheated);
            b.writeVarInt(p.camoTicks); b.writeVarInt(p.camoMax);
            b.writeUtf(p.primaryChoice, 128); b.writeUtf(p.secondaryChoice, 128); b.writeUtf(p.primaryKey, 128); b.writeUtf(p.secondaryKey, 128);
            b.writeVarInt(p.primaryCooldown); b.writeVarInt(p.primaryCooldownMax); b.writeVarInt(p.secondaryCooldown); b.writeVarInt(p.secondaryCooldownMax);
            b.writeBoolean(p.online);
            b.writeVarInt(p.meshTicks); b.writeVarInt(p.meshMax); b.writeVarInt(p.reconTicks); b.writeVarInt(p.reconMax);
            b.writeVarInt(p.hfLevel); b.writeVarInt(Math.max(0, p.generated)); b.writeFloat(p.resumeHeat);
            b.writeBoolean(p.quantum);
        }, b -> new CoreSync(b.readFloat(), b.readFloat(), b.readFloat(), b.readBoolean(), b.readVarInt(), b.readVarInt(),
            b.readUtf(128), b.readUtf(128), b.readUtf(128), b.readUtf(128), b.readVarInt(), b.readVarInt(), b.readVarInt(), b.readVarInt(),
            b.readBoolean(), b.readVarInt(), b.readVarInt(), b.readVarInt(), b.readVarInt(), b.readVarInt(), b.readVarInt(), b.readFloat(),
            b.readBoolean()));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** Effect kinds of {@link Fx}. */
    public static final byte FX_DASH = 0, FX_EMP = 1, FX_CAMO_ON = 2, FX_CAMO_OFF = 3, FX_CAMO_KEEP = 4, FX_DENIED = 5, FX_OVERHEAT = 6;
    /**
     * 0.1.8.6. MESH_ON: a = run ticks, b = tier. MESH_OFF. MESH_HIT: a,b,c = hit direction (unit vector).
     * RECON: the wearer only; targets = creatures that glow, a = glow ticks. RECON_CLOUD: everyone near;
     * x,y,z = eye, a,b,c = view direction, targets[0] = range, targets[1] = 1 for a full sphere (Mk.II).
     * REBOUND: a shock-gel rebound at x,y,z, a = tier.
     */
    public static final byte FX_MESH_ON = 7, FX_MESH_OFF = 8, FX_MESH_HIT = 9, FX_RECON = 10, FX_RECON_CLOUD = 11, FX_REBOUND = 12;
    /** 0.1.9.0 frame cores. DISCHARGE: arc discharge at x,y,z, a = radius, targets = creatures hit. CORE_DENIED: the wearer only, a = reason. */
    public static final byte FX_DISCHARGE = 13, FX_CORE_DENIED = 14;
    /**
     * 0.1.10.32 (S-6): electric armour of a quantum suit absorbed a hit: a,b,c = direction from the body to the hit (unit
     * vector), x = the absorbed share of the damage (0..1). Everyone near.
     */
    public static final byte FX_ARMOR_HIT = 15;

    /** Client to server (0.1.9.0): the core burst key (arc discharge of the advanced propulsion core). No fields. */
    public record CoreBurst() implements CustomPacketPayload {
        public static final Type<CoreBurst> TYPE = new Type<>(id("core_burst"));
        public static final StreamCodec<ByteBuf, CoreBurst> CODEC = StreamCodec.unit(new CoreBurst());
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /**
     * Server to client: one burst effect of entity {@code entityId} at {@code x,y,z}.
     * DASH: a,b = direction x,z; c = distance. EMP: a = radius; {@code targets} = stunned creatures,
     * {@code points} = erased projectiles (x,y,z triples). CAMO_ON/KEEP: a = 1 when hidden from
     * players. CAMO_OFF: a = reason (0 manual, 1 broken, 2 depleted). DENIED: a = reason.
     */
    public record Fx(int entityId, byte kind, float x, float y, float z, float a, float b, float c, int[] targets, float[] points)
        implements CustomPacketPayload {
        public static final int MAX_TARGETS = 48, MAX_POINTS = 32 * 3;
        public static final Type<Fx> TYPE = new Type<>(id("nano_fx"));
        public static final StreamCodec<FriendlyByteBuf, Fx> CODEC = StreamCodec.of((b, p) -> {
            b.writeVarInt(p.entityId); b.writeByte(p.kind);
            b.writeFloat(p.x); b.writeFloat(p.y); b.writeFloat(p.z); b.writeFloat(p.a); b.writeFloat(p.b); b.writeFloat(p.c);
            int nt = com.miophas.singularity_iteration.common.util.PacketLimits.writeCount(b, p.targets.length, MAX_TARGETS);
            for (int i = 0; i < nt; i++) b.writeVarInt(p.targets[i]);
            int np = com.miophas.singularity_iteration.common.util.PacketLimits.writeCount(b, p.points.length - p.points.length % 3, MAX_POINTS);
            for (int i = 0; i < np; i++) b.writeFloat(p.points[i]);
        }, b -> {
            int entity = b.readVarInt();
            byte kind = b.readByte();
            float x = b.readFloat(), y = b.readFloat(), z = b.readFloat(), a = b.readFloat(), bb = b.readFloat(), c = b.readFloat();
            int nt = com.miophas.singularity_iteration.common.util.PacketLimits.readCount(b, MAX_TARGETS, "nano fx target");
            int[] targets = new int[nt];
            for (int i = 0; i < nt; i++) targets[i] = b.readVarInt();
            int np = com.miophas.singularity_iteration.common.util.PacketLimits.readCount(b, MAX_POINTS, "nano fx point");
            float[] points = new float[np];
            for (int i = 0; i < np; i++) points[i] = b.readFloat();
            return new Fx(entity, kind, x, y, z, a, bb, c, targets, points);
        });
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

        public static Fx simple(Entity e, byte kind, float a, float b, float c) {
            return new Fx(e.getId(), kind, (float) e.getX(), (float) e.getY(), (float) e.getZ(), a, b, c, new int[0], new float[0]);
        }
    }

    public static void register(PayloadRegistrar registrar) {
        registrar.playToServer(BurstRequest.TYPE, BurstRequest.CODEC, (p, c) -> c.enqueueWork(() -> {
            if (c.player() instanceof ServerPlayer player && PacketRateLimit.allow(player, PacketRateLimit.NANO_BURST)
                    && Float.isFinite(p.forward()) && Float.isFinite(p.strafe()) && (p.slot() == 0 || p.slot() == 1))
                NanoBursts.request(player, p.slot(), Math.max(-1, Math.min(1, p.forward())), Math.max(-1, Math.min(1, p.strafe())));
        }));
        registrar.playToServer(CoreBurst.TYPE, CoreBurst.CODEC, (p, c) -> c.enqueueWork(() -> {
            if (c.player() instanceof ServerPlayer player && PacketRateLimit.allow(player, PacketRateLimit.NANO_BURST))
                com.miophas.singularity_iteration.common.suit.core.CoreEvents.discharge(player);
        }));
        registrar.playToServer(BurstAssign.TYPE, BurstAssign.CODEC, (p, c) -> c.enqueueWork(() -> {
            if (c.player() instanceof ServerPlayer player && PacketRateLimit.allow(player, PacketRateLimit.NANO_ASSIGN)
                    && (p.slot() == 0 || p.slot() == 1))
                NanoSuit.assign(player, p.slot(), p.key());
        }));
        registrar.playToClient(CoreSync.TYPE, CoreSync.CODEC, (p, c) -> {
            if (FMLEnvironment.dist != Dist.CLIENT) return;
            c.enqueueWork(() -> { Consumer<CoreSync> hook = SYNC_HOOK; if (hook != null) hook.accept(p); });
        });
        registrar.playToClient(Fx.TYPE, Fx.CODEC, (p, c) -> {
            if (FMLEnvironment.dist != Dist.CLIENT) return;
            c.enqueueWork(() -> { Consumer<Fx> hook = FX_HOOK; if (hook != null) hook.accept(p); });
        });
    }

    /** Sends {@code fx} to the players of the level within {@code range} blocks of {@code at} (channel-checked). */
    public static void broadcast(Entity at, Fx fx, double range) {
        if (!(at.level() instanceof ServerLevel level)) return;
        for (ServerPlayer viewer : level.players()) {
            if (viewer.distanceToSqr(at) <= range * range) SuitPackets.send(viewer, fx);
        }
    }
}
