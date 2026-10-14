// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.suit.quantum;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import java.util.function.Consumer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Payloads of the quantum core (0.1.10.0). The action keys travel in the nano payloads
 * ({@code NanoPackets.BurstRequest}, {@code BurstAssign}): one key system for both suit classes.
 * The action effects travel as {@code NanoPackets.Fx} with the kinds below (32 and up), so the
 * effect channel, its limits and its trust rules stay the same.
 *
 * <table>
 *   <caption>Quantum payloads</caption>
 *   <tr><th>Payload</th><th>Direction</th><th>Purpose</th></tr>
 *   <tr><td>{@link CoreSync}</td><td>server to client</td><td>coherence, decoherence and running actions of the wearer (the wearer only)</td></tr>
 * </table>
 */
public final class QuantumPackets {
    private QuantumPackets() {}

    /** Client hook (set by the client set-up; null on a dedicated server). */
    public static volatile Consumer<CoreSync> SYNC_HOOK;

    /**
     * Effect kinds in {@code NanoPackets.Fx}.
     * BLINK: x,y,z = start (feet), a,b,c = end (feet), targets[0] = tier.
     * PHASE_ON: a = ticks, b = tier. PHASE_OFF: a = tier, b = shock-wave radius (0 = none), targets = creatures hit.
     * FIELD_ON / FIELD_KEEP: a = radius, b = tier. FIELD_OFF: a = reason (0 key, 1 unit off, 2 coherence out).
     * FIELD_CATCH: a = radius, points = caught projectiles (x,y,z triples).
     * DILATION_ON / DILATION_KEEP: a = radius, b = ticks left, c = ticks in all. DILATION_OFF.
     * VOLLEY: x,y,z = the wearer, a = pairs, targets = (Wingman id, target id) pairs, points = (fire delay, impact delay, bolts) per pair.
     * DECOHERE: a = lock ticks. RECOHERE: the wearer only, the lock ended.
     */
    public static final byte FX_BLINK = 32, FX_PHASE_ON = 33, FX_PHASE_OFF = 34, FX_FIELD_ON = 35, FX_FIELD_KEEP = 36, FX_FIELD_OFF = 37,
        FX_FIELD_CATCH = 38, FX_DILATION_ON = 39, FX_DILATION_KEEP = 40, FX_DILATION_OFF = 41, FX_VOLLEY = 42, FX_DECOHERE = 43, FX_RECOHERE = 44;
    /**
     * 0.1.10.1. COMBO: a = combo (0 quantum tunnel, 1 frozen volley, 2 static barrage).
     * RESONANCE: Resonant Deflection burst at x,y,z, a = radius, b = damage, targets = creatures hit.
     * DECOY: Afterimage Decoy at x,y,z, a = ticks, targets = the drawn creatures. DECOY_END.
     * BARRAGE: the projectiles a Static Barrage threw back, points = their positions.
     */
    public static final byte FX_COMBO = 45, FX_RESONANCE = 46, FX_DECOY = 47, FX_DECOY_END = 48, FX_BARRAGE = 49;
    /**
     * 0.1.10.2 quantum command. ORDER (the owner only): x,y,z = order point (or the focus target or the
     * owner), a = command action, b = argument, c = focus entity id (-1), targets = the ordered Wingmen.
     * RECALL: a Wingman folds into its Armory at x,y,z, a = its height.
     */
    public static final byte FX_ORDER = 50, FX_RECALL = 51;

    /** Client hooks of the 0.1.10.1 payloads. */
    public static volatile Consumer<DataLink> DATALINK_HOOK;

    /**
     * Server to client (the wearer, 0.1.10.1 Fleet Datalink): the creature in the wearer's sights
     * (-1 none) and the hostile creatures the Wingmen see. Sent every 10 ticks while the synergy
     * works, and once (empty) when it stops.
     */
    public record DataLink(int marked, int[] seen) implements CustomPacketPayload {
        public static final int MAX = 64;
        public static final Type<DataLink> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "quantum_datalink"));
        public static final StreamCodec<FriendlyByteBuf, DataLink> CODEC = StreamCodec.of((b, p) -> {
            b.writeVarInt(p.marked + 1);
            int n = com.miophas.singularity_iteration.common.util.PacketLimits.writeCount(b, p.seen.length, MAX);
            for (int i = 0; i < n; i++) b.writeVarInt(p.seen[i]);
        }, b -> {
            int marked = b.readVarInt() - 1;
            int n = com.miophas.singularity_iteration.common.util.PacketLimits.readCount(b, MAX, "datalink entity");
            int[] seen = new int[n];
            for (int i = 0; i < n; i++) seen[i] = b.readVarInt();
            return new DataLink(marked, seen);
        });
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** Client to server (0.1.10.1): link or unlink the synergy {@code key} on the worn quantum chestplate. */
    public record LinkToggle(String key) implements CustomPacketPayload {
        public static final Type<LinkToggle> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "quantum_link_toggle"));
        public static final StreamCodec<FriendlyByteBuf, LinkToggle> CODEC = StreamCodec.of((b, p) -> b.writeUtf(p.key, 128),
            b -> new LinkToggle(b.readUtf(128)));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** Server to client (0.1.10.1): the result of a link toggle (for the panel message), see {@link QuantumLinks}. */
    public record LinkResult(String key, byte result) implements CustomPacketPayload {
        public static final Type<LinkResult> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "quantum_link_result"));
        public static final StreamCodec<FriendlyByteBuf, LinkResult> CODEC = StreamCodec.of((b, p) -> { b.writeUtf(p.key, 128); b.writeByte(p.result); },
            b -> new LinkResult(b.readUtf(128), b.readByte()));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public static volatile Consumer<LinkResult> LINK_HOOK;

    /**
     * Server to client (the wearer): state of the quantum core. Sent on change, at most every 2
     * ticks, and once a second without change while the core is online.
     */
    public record CoreSync(float coherence, float max, int decoherent, int decoherentMax, boolean online, boolean regenerating,
                           int phaseTicks, int phaseMax, boolean field, int fieldTicks, int dilationTicks, int dilationMax,
                           int blinkCharges, int blinkMax, int blinkRecharge, int blinkRechargeMax) implements CustomPacketPayload {
        public static final Type<CoreSync> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "quantum_core_sync"));
        public static final StreamCodec<FriendlyByteBuf, CoreSync> CODEC = StreamCodec.of((b, p) -> {
            b.writeFloat(p.coherence); b.writeFloat(p.max); b.writeVarInt(p.decoherent); b.writeVarInt(p.decoherentMax);
            b.writeBoolean(p.online); b.writeBoolean(p.regenerating);
            b.writeVarInt(p.phaseTicks); b.writeVarInt(p.phaseMax); b.writeBoolean(p.field); b.writeVarInt(p.fieldTicks);
            b.writeVarInt(p.dilationTicks); b.writeVarInt(p.dilationMax);
            b.writeVarInt(p.blinkCharges); b.writeVarInt(p.blinkMax); b.writeVarInt(p.blinkRecharge); b.writeVarInt(p.blinkRechargeMax);
        }, b -> new CoreSync(b.readFloat(), b.readFloat(), b.readVarInt(), b.readVarInt(), b.readBoolean(), b.readBoolean(),
            b.readVarInt(), b.readVarInt(), b.readBoolean(), b.readVarInt(), b.readVarInt(), b.readVarInt(),
            b.readVarInt(), b.readVarInt(), b.readVarInt(), b.readVarInt()));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public static void register(PayloadRegistrar registrar) {
        registrar.playToClient(CoreSync.TYPE, CoreSync.CODEC, (p, c) -> {
            if (FMLEnvironment.dist != Dist.CLIENT) return;
            c.enqueueWork(() -> { Consumer<CoreSync> hook = SYNC_HOOK; if (hook != null) hook.accept(p); });
        });
        // 0.1.10.1 entanglement links and the Fleet Datalink
        registrar.playToClient(DataLink.TYPE, DataLink.CODEC, (p, c) -> {
            if (FMLEnvironment.dist != Dist.CLIENT) return;
            c.enqueueWork(() -> { Consumer<DataLink> hook = DATALINK_HOOK; if (hook != null) hook.accept(p); });
        });
        registrar.playToClient(LinkResult.TYPE, LinkResult.CODEC, (p, c) -> {
            if (FMLEnvironment.dist != Dist.CLIENT) return;
            c.enqueueWork(() -> { Consumer<LinkResult> hook = LINK_HOOK; if (hook != null) hook.accept(p); });
        });
        // 0.1.10.2 quantum command
        com.miophas.singularity_iteration.common.armory.WingmanCommandPackets.register(registrar);
        registrar.playToServer(LinkToggle.TYPE, LinkToggle.CODEC, (p, c) -> c.enqueueWork(() -> {
            if (c.player() instanceof net.minecraft.server.level.ServerPlayer player
                    && com.miophas.singularity_iteration.common.util.PacketRateLimit.allow(player, com.miophas.singularity_iteration.common.util.PacketRateLimit.NANO_ASSIGN)) {
                int result = QuantumLinks.toggle(player, p.key());
                com.miophas.singularity_iteration.common.suit.SuitPackets.send(player, new LinkResult(p.key(), (byte) result));
            }
        }));
    }
}
