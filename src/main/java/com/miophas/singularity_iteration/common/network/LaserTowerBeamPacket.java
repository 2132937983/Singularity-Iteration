package com.miophas.singularity_iteration.common.network;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Laser tower combat event, sent to players tracking the tower's chunk.
 *
 * <ul>
 *   <li>{@link #KIND_LOCK}: targets acquired; the turret slews toward the first one
 *       while the tower charges (no beams yet).</li>
 *   <li>{@link #KIND_FIRE}: one volley; a beam per listed target.</li>
 *   <li>{@link #KIND_IDLE}: no targets left; the turret returns to its idle sweep.</li>
 * </ul>
 * Only state changes and volleys are sent (never per tick). The client animates the
 * turret and beams itself and follows targets with interpolated positions.
 */
@SuppressWarnings("null")
public record LaserTowerBeamPacket(BlockPos pos, int style, int kind, int[] targets) implements CustomPacketPayload {
    public static final int STYLE_GROUND = 0;
    public static final int STYLE_SKY = 1;
    public static final int KIND_FIRE = 0;
    public static final int KIND_LOCK = 1;
    public static final int KIND_IDLE = 2;
    /** Upper bound on decoded ids; towers fire at most 10 per volley. */
    private static final int MAX_TARGETS = 32;

    public static final Type<LaserTowerBeamPacket> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "laser_tower_beams"));

    public static final StreamCodec<FriendlyByteBuf, LaserTowerBeamPacket> CODEC = StreamCodec.of(
        (buf, packet) -> {
            buf.writeBlockPos(packet.pos);
            buf.writeByte(packet.style | (packet.kind << 4));
            int count = Math.min(packet.targets.length, MAX_TARGETS);
            buf.writeVarInt(count);
            for (int i = 0; i < count; i++) buf.writeVarInt(packet.targets[i]);
        },
        buf -> {
            BlockPos pos = buf.readBlockPos();
            int packed = buf.readUnsignedByte();
            int count = buf.readVarInt();
            if (count < 0 || count > MAX_TARGETS) throw new IllegalArgumentException("Bad laser target count " + count);
            int[] ids = new int[count];
            for (int i = 0; i < count; i++) ids[i] = buf.readVarInt();
            return new LaserTowerBeamPacket(pos, packed & 15, packed >>> 4, ids);
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
