package com.miophas.singularity_iteration.common.network;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.common.blockentity.wiring.terminal.EnergyNetworkSnapshot;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Server -> client: one second of terminal telemetry for an open terminal GUI. */
@SuppressWarnings("null")
public record EnergyTerminalSyncPacket(int containerId, EnergyNetworkSnapshot snapshot) implements CustomPacketPayload {
    public static final Type<EnergyTerminalSyncPacket> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "energy_terminal_sync"));

    public static final StreamCodec<FriendlyByteBuf, EnergyTerminalSyncPacket> CODEC = StreamCodec.of(
        (buf, p) -> { buf.writeVarInt(p.containerId); p.snapshot.write(buf); },
        buf -> new EnergyTerminalSyncPacket(buf.readVarInt(), EnergyNetworkSnapshot.read(buf)));

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(EnergyTerminalSyncPacket packet, IPayloadContext context) {
        if (FMLEnvironment.dist != Dist.CLIENT) return;
        context.enqueueWork(() ->
            com.miophas.singularity_iteration.common.client.ClientPacketHandlers.handleEnergyTerminalSync(packet));
    }
}
