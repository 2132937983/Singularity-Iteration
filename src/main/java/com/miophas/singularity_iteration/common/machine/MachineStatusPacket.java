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

/** Server -> client: status-lamp states of machines in one chunk (only the ones that changed, or all on chunk watch). */
public record MachineStatusPacket(long[] positions, byte[] states) implements CustomPacketPayload {
    public static final Type<MachineStatusPacket> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "machine_status"));

    public static final StreamCodec<FriendlyByteBuf, MachineStatusPacket> CODEC = StreamCodec.of(
        (buf, p) -> {
            buf.writeVarInt(p.positions.length);
            for (int i = 0; i < p.positions.length; i++) { buf.writeLong(p.positions[i]); buf.writeByte(p.states[i]); }
        },
        buf -> {
            int n = Math.min(4096, buf.readVarInt());
            long[] pos = new long[n]; byte[] st = new byte[n];
            for (int i = 0; i < n; i++) { pos[i] = buf.readLong(); st[i] = buf.readByte(); }
            return new MachineStatusPacket(pos, st);
        });

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(MachineStatusPacket packet, IPayloadContext context) {
        if (FMLEnvironment.dist != Dist.CLIENT) return;
        context.enqueueWork(() -> {
            var level = context.player() == null ? null : context.player().level();
            if (level == null) return;
            for (int i = 0; i < packet.positions.length; i++) {
                if (level.getBlockEntity(BlockPos.of(packet.positions[i])) instanceof AbstractEnergyBlockEntity machine) {
                    machine.acceptClientMachineStatus(MachineStatus.byId(packet.states[i]));
                }
            }
        });
    }
}
