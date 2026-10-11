package com.miophas.singularity_iteration.common.machine;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.core.api.machine.MachineStatus;
import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractEnergyBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Server to client: status lamp states of machines in one chunk.
 *
 * <p>When: {@link MachineStatusTracker} sends it. It sends the lamps that changed, at most once
 * per chunk per {@link MachineStatusTracker#INTERVAL} ticks, to each player that tracks the
 * chunk. It also sends each lamp that is not {@code OFF} to one player when that player starts
 * to watch the chunk.
 *
 * <p>Fields:
 * <ul>
 *   <li>{@code positions}: packed block positions ({@link BlockPos#asLong()}). All positions
 *       are in one chunk.</li>
 *   <li>{@code states}: {@link MachineStatus} ordinals, same index as {@code positions}.
 *       Valid range 0..3 (OFF, RUNNING, NO_POWER, BLOCKED).</li>
 * </ul>
 * The two arrays must have the same length.
 *
 * <p>Receiver validation: the decoder reads at most 4096 entries.
 * {@link MachineStatus#byId(int)} maps an unknown ordinal to {@code OFF}. The handler skips each
 * position that has no {@link AbstractEnergyBlockEntity} in the client level.
 *
 * <p>Missing channel: the channel is not optional, so a vanilla client cannot connect. The
 * tracker checks the channel for a single receiver. It ignores send errors.
 */
public record MachineStatusPacket(long[] positions, byte[] states) implements CustomPacketPayload {
    /** Maximum number of entries in one packet. The sender splits a larger set. */
    public static final int MAX_ENTRIES = 4096;

    public static final Type<MachineStatusPacket> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "machine_status"));

    public static final StreamCodec<FriendlyByteBuf, MachineStatusPacket> CODEC = StreamCodec.of(
        (buf, p) -> {
            int count = com.miophas.singularity_iteration.common.util.PacketLimits.writeCount(buf, Math.min(p.positions.length, p.states.length), MAX_ENTRIES);
            for (int i = 0; i < count; i++) { buf.writeLong(p.positions[i]); buf.writeByte(p.states[i]); }
        },
        buf -> {
            int n = com.miophas.singularity_iteration.common.util.PacketLimits.readCount(buf, MAX_ENTRIES, "machine status");
            long[] pos = new long[n]; byte[] st = new byte[n];
            for (int i = 0; i < n; i++) { pos[i] = buf.readLong(); st[i] = buf.readByte(); }
            return new MachineStatusPacket(pos, st);
        });

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    /**
     * Client handler. NeoForge runs it on the client main thread, because the registrar uses
     * the default handler thread. {@code enqueueWork} then runs the task at once. The task
     * reads the client level, so it must run on the main thread. A dedicated server ignores the
     * payload.
     */
    public static void handle(MachineStatusPacket packet, IPayloadContext context) {
        if (FMLEnvironment.dist != Dist.CLIENT) return;
        context.enqueueWork(() -> {
            var level = context.player() == null ? null : context.player().level();
            if (level == null) return;
            // acceptClientMachineStatus re-meshes the block only when the state changes.
            for (int i = 0; i < packet.positions.length; i++) {
                if (level.getBlockEntity(BlockPos.of(packet.positions[i])) instanceof AbstractEnergyBlockEntity machine) {
                    machine.acceptClientMachineStatus(MachineStatus.byId(packet.states[i]));
                }
            }
        });
    }
}
