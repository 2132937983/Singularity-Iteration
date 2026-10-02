package com.miophas.singularity_iteration.common.menu.tool;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.common.blockentity.wiring.terminal.NetworkWalker;
import com.miophas.singularity_iteration.core.runtime.energy.grid.EnergyNetGlobal;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Voltage-detector HUD. Client -> server: "what is on the network at {@code pos}" (sent at most
 * twice a second while a player holds the meter and aims at an energy block). Server -> client:
 * the answer (packet voltage, throughput, cables, endpoints, cable rating). Requests are rate
 * limited per player and limited to blocks within reach.
 */
public final class MeterHudPacket {
    private MeterHudPacket() {}

    public record Request(BlockPos pos) implements CustomPacketPayload {
        public static final Type<Request> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "meter_hud_request"));
        public static final StreamCodec<FriendlyByteBuf, Request> CODEC = StreamCodec.of((b, p) -> b.writeBlockPos(p.pos), b -> new Request(b.readBlockPos()));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record Reply(BlockPos pos, float voltage, float throughput, int conductors, int endpoints, long ratedPacket, boolean network)
        implements CustomPacketPayload {
        public static final Type<Reply> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "meter_hud_reply"));
        public static final StreamCodec<FriendlyByteBuf, Reply> CODEC = StreamCodec.of(
            (b, p) -> { b.writeBlockPos(p.pos); b.writeFloat(p.voltage); b.writeFloat(p.throughput); b.writeVarInt(p.conductors);
                b.writeVarInt(p.endpoints); b.writeVarLong(Math.max(0, p.ratedPacket)); b.writeBoolean(p.network); },
            b -> new Reply(b.readBlockPos(), b.readFloat(), b.readFloat(), b.readVarInt(), b.readVarInt(), b.readVarLong(), b.readBoolean()));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    private static final Map<UUID, Long> LAST = new HashMap<>();
    private static volatile Reply latest;

    /** Client: last reply (null before the first). */
    public static Reply latest() { return latest; }

    public static void handleRequest(Request request, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player) || !(player.level() instanceof ServerLevel level)) return;
            long now = level.getGameTime();
            Long last = LAST.get(player.getUUID());
            if (last != null && now - last < 8) return;
            LAST.put(player.getUUID(), now);
            BlockPos pos = request.pos();
            if (!level.isLoaded(pos) || player.distanceToSqr(pos.getCenter()) > 64 * 64) return;
            var be = level.getBlockEntity(pos);
            boolean network = NetworkWalker.isConductor(be) || NetworkWalker.isTransformer(be);
            if (be == null || (!network && NetworkWalker.categorize(be) == null)) return;
            NetworkWalker.Result r = NetworkWalker.walk(level, pos, false, 4096, 256);
            double in = 0, voltage = 0;
            for (BlockPos at : r.devices) {
                var st = EnergyNetGlobal.getCurrentTickNodeStats(level, at);
                if (st == null) continue;
                in += st.getEnergyIn();
                voltage = Math.max(voltage, st.getVoltage());
            }
            long rated = 0;
            for (var n : r.subnets) rated = Math.max(rated, n.ratedPacket);
            try {
                if (player.connection != null && player.connection.hasChannel(Reply.TYPE))
                    PacketDistributor.sendToPlayer(player, new Reply(pos, (float) voltage, (float) in, r.conductors, r.devices.size(), rated, network));
            } catch (RuntimeException ignored) { }
        });
    }

    public static void handleReply(Reply reply, IPayloadContext context) {
        if (FMLEnvironment.dist != Dist.CLIENT) return;
        context.enqueueWork(() -> latest = reply);
    }
}
