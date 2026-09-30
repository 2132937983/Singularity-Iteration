package com.miophas.singularity_iteration.common.network;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * One laser-tower volley: muzzle position, beam style and the ids of the entities hit.
 *
 * <p>Sent once per volley (not per tick) to players tracking the tower's chunk. The
 * client keeps each beam alive for a few frames and follows the target's interpolated
 * position itself, so no per-tick position sync is needed.
 */
@SuppressWarnings("null")
public record LaserTowerBeamPacket(double x, double y, double z, int style, int[] targets) implements CustomPacketPayload {
    public static final int STYLE_GROUND = 0;
    public static final int STYLE_SKY = 1;
    /** Upper bound on decoded ids; towers fire at most 10 per volley. */
    private static final int MAX_TARGETS = 32;

    public static final Type<LaserTowerBeamPacket> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "laser_tower_beams"));

    public static final StreamCodec<FriendlyByteBuf, LaserTowerBeamPacket> CODEC = StreamCodec.of(
        (buf, packet) -> {
            buf.writeDouble(packet.x);
            buf.writeDouble(packet.y);
            buf.writeDouble(packet.z);
            buf.writeByte(packet.style);
            int count = Math.min(packet.targets.length, MAX_TARGETS);
            buf.writeVarInt(count);
            for (int i = 0; i < count; i++) buf.writeVarInt(packet.targets[i]);
        },
        buf -> {
            double x = buf.readDouble(), y = buf.readDouble(), z = buf.readDouble();
            int style = buf.readByte();
            int count = buf.readVarInt();
            if (count < 0 || count > MAX_TARGETS) throw new IllegalArgumentException("Bad laser target count " + count);
            int[] ids = new int[count];
            for (int i = 0; i < count; i++) ids[i] = buf.readVarInt();
            return new LaserTowerBeamPacket(x, y, z, style, ids);
        });

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(LaserTowerBeamPacket packet, IPayloadContext context) {
        if (FMLEnvironment.dist != Dist.CLIENT) return;
        context.enqueueWork(() ->
            com.miophas.singularity_iteration.common.client.ClientPacketHandlers.handleLaserTowerBeams(packet));
    }
}
