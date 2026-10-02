package com.miophas.singularity_iteration.common.armory;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Server -> client: remote controller console contents. */
public record ArmoryRemoteSyncPacket(int containerId, ArmorySnapshot snapshot, Component message) implements CustomPacketPayload {
    public static final Type<ArmoryRemoteSyncPacket> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "armory_remote_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ArmoryRemoteSyncPacket> CODEC = StreamCodec.of(
        (buf, p) -> { buf.writeVarInt(p.containerId); p.snapshot.write(buf); ComponentSerialization.TRUSTED_STREAM_CODEC.encode(buf, p.message); },
        buf -> new ArmoryRemoteSyncPacket(buf.readVarInt(), ArmorySnapshot.read(buf), ComponentSerialization.TRUSTED_STREAM_CODEC.decode(buf)));

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(ArmoryRemoteSyncPacket packet, IPayloadContext context) {
        if (FMLEnvironment.dist != Dist.CLIENT) return;
        context.enqueueWork(() -> {
            var player = context.player();
            if (player != null && player.containerMenu instanceof ArmoryRemoteMenu menu && menu.containerId == packet.containerId) {
                menu.acceptSnapshot(packet.snapshot, packet.message);
            }
        });
    }
}
