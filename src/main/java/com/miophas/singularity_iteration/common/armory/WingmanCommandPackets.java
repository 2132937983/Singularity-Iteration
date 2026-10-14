// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.armory;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.common.util.PacketLimits;
import com.miophas.singularity_iteration.common.util.PacketRateLimit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Payloads of the quantum command (0.1.10.2).
 *
 * <table>
 *   <caption>Command payloads</caption>
 *   <tr><th>Payload</th><th>Direction</th><th>Purpose</th></tr>
 *   <tr><td>{@link Command}</td><td>client to server</td><td>one order of the command wheel or the holo table (rate limited, every field checked)</td></tr>
 *   <tr><td>{@link Squad}</td><td>server to client</td><td>the owner's Wingmen, their orders and the command settings (the owner only, every 10 ticks while the command link works)</td></tr>
 * </table>
 */
public final class WingmanCommandPackets {
    private WingmanCommandPackets() {}

    /** Selection "all Wingmen". */
    public static final UUID ALL = new UUID(0L, 0L);

    /** Client hook (set by the client set-up; null on a dedicated server). */
    public static volatile Consumer<Squad> SQUAD_HOOK;

    /** Command rate: a wheel or table click, about 5 per second at most. */
    public static final PacketRateLimit.Bucket BUCKET = new PacketRateLimit.Bucket("wingman_command", 8, 4);

    /**
     * Client to server. {@code action}: one of the {@code WingmanCommands.A_*} codes; {@code unit}: a
     * Wingman UUID or {@link #ALL}; {@code arg}: the mode (MODE), on/off (DATALINK), 1 = "where each
     * Wingman is" (HOLD); {@code x, y, z}: the order point; {@code target}: the focus entity id.
     * Client values, not trusted: the server checks the owner, the dimension, the world bounds and the target.
     */
    public record Command(byte action, UUID unit, byte arg, double x, double y, double z, int target) implements CustomPacketPayload {
        public static final Type<Command> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "wingman_command"));
        public static final StreamCodec<FriendlyByteBuf, Command> CODEC = StreamCodec.of((b, p) -> {
            b.writeByte(p.action); b.writeUUID(p.unit); b.writeByte(p.arg);
            b.writeDouble(p.x); b.writeDouble(p.y); b.writeDouble(p.z); b.writeVarInt(p.target + 1);
        }, b -> new Command(b.readByte(), b.readUUID(), b.readByte(), b.readDouble(), b.readDouble(), b.readDouble(), b.readVarInt() - 1));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /**
     * One Wingman in the squad view. {@code entityId}: -1 when it is not in the owner's dimension or
     * not loaded; {@code where}: 0 with the owner, 1 in another dimension, 2 in an unloaded chunk
     * (roster); {@code order}: a {@link WingmanOrder} ordinal; {@code ox, oy, oz}: the order point;
     * {@code focus}: the focus entity id or -1; {@code health}, {@code energy}: 0..1 (energy -1
     * without electric pieces); {@code x, y, z}: the position (the last known one when unloaded).
     */
    public record Unit(UUID id, int entityId, byte set, byte mode, boolean individual, byte order, float ox, float oy, float oz, int focus,
                       float health, float energy, byte state, byte where, float x, float y, float z) {
        void write(FriendlyByteBuf b) {
            b.writeUUID(id); b.writeVarInt(entityId + 1); b.writeByte(set); b.writeByte(mode); b.writeBoolean(individual); b.writeByte(order);
            b.writeFloat(ox); b.writeFloat(oy); b.writeFloat(oz); b.writeVarInt(focus + 1);
            b.writeFloat(health); b.writeFloat(energy); b.writeByte(state); b.writeByte(where);
            b.writeFloat(x); b.writeFloat(y); b.writeFloat(z);
        }

        static Unit read(FriendlyByteBuf b) {
            return new Unit(b.readUUID(), b.readVarInt() - 1, b.readByte(), b.readByte(), b.readBoolean(), b.readByte(),
                b.readFloat(), b.readFloat(), b.readFloat(), b.readVarInt() - 1,
                b.readFloat(), b.readFloat(), b.readByte(), b.readByte(), b.readFloat(), b.readFloat(), b.readFloat());
        }
    }

    /**
     * Server to client (the owner). {@code commander}: the command link works (an active
     * entanglement relay on a worn quantum piece); {@code datalink}: the data link setting;
     * {@code squadMode}: the squad mode ordinal or -1; {@code recallCooldown}: ticks until the next
     * quantum recall; {@code holdRadius}, {@code suppressRadius}: the server's order radii (for the
     * markers); {@code units}: at most {@link #MAX_UNITS}.
     */
    public record Squad(boolean commander, boolean datalink, byte squadMode, int recallCooldown, int holdRadius, int suppressRadius,
                        List<Unit> units) implements CustomPacketPayload {
        public static final int MAX_UNITS = 12;
        public static final Type<Squad> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "wingman_squad"));
        public static final StreamCodec<FriendlyByteBuf, Squad> CODEC = StreamCodec.of((b, p) -> {
            b.writeBoolean(p.commander); b.writeBoolean(p.datalink); b.writeByte(p.squadMode); b.writeVarInt(Math.max(0, p.recallCooldown));
            b.writeVarInt(Math.max(0, p.holdRadius)); b.writeVarInt(Math.max(0, p.suppressRadius));
            int n = PacketLimits.writeCount(b, p.units.size(), MAX_UNITS);
            for (int i = 0; i < n; i++) p.units.get(i).write(b);
        }, b -> {
            boolean commander = b.readBoolean(), datalink = b.readBoolean();
            byte mode = b.readByte();
            int cooldown = b.readVarInt();
            int hold = Math.min(64, b.readVarInt()), suppress = Math.min(64, b.readVarInt());
            int n = PacketLimits.readCount(b, MAX_UNITS, "squad unit");
            List<Unit> units = new ArrayList<>(n);
            for (int i = 0; i < n; i++) units.add(Unit.read(b));
            return new Squad(commander, datalink, mode, cooldown, hold, suppress, units);
        });
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public static void register(PayloadRegistrar registrar) {
        registrar.playToServer(Command.TYPE, Command.CODEC, (p, c) -> c.enqueueWork(() -> {
            if (c.player() instanceof ServerPlayer player && PacketRateLimit.allow(player, BUCKET)) WingmanCommands.handle(player, p);
        }));
        registrar.playToClient(Squad.TYPE, Squad.CODEC, (p, c) -> {
            if (FMLEnvironment.dist != Dist.CLIENT) return;
            c.enqueueWork(() -> { Consumer<Squad> hook = SQUAD_HOOK; if (hook != null) hook.accept(p); });
        });
    }
}
