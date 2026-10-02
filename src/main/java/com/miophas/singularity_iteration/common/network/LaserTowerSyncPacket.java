package com.miophas.singularity_iteration.common.network;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.common.blockentity.producer.tower.LaserTowerStats;
import com.miophas.singularity_iteration.common.blockentity.producer.tower.TargetFilter;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Server -> client: statistics and target list for an open tower GUI (sent only on change). */
@SuppressWarnings("null")
public record LaserTowerSyncPacket(int containerId, LaserTowerStats stats, TargetFilter filter) implements CustomPacketPayload {
    public static final Type<LaserTowerSyncPacket> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "laser_tower_sync"));

    public static final StreamCodec<FriendlyByteBuf, LaserTowerSyncPacket> CODEC = StreamCodec.of(
        (buf, p) -> { buf.writeVarInt(p.containerId); p.stats.write(buf); p.filter.write(buf); },
        buf -> new LaserTowerSyncPacket(buf.readVarInt(), LaserTowerStats.read(buf), TargetFilter.read(buf)));

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(LaserTowerSyncPacket packet, IPayloadContext context) {
        if (FMLEnvironment.dist != Dist.CLIENT) return;
        context.enqueueWork(() ->
            com.miophas.singularity_iteration.common.client.ClientPacketHandlers.handleLaserTowerSync(packet));
    }
}
