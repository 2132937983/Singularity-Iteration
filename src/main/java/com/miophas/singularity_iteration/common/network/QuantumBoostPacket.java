// SCEX 2026-09-12: repaired malformed UTF-8 bytes in comments only.
package com.miophas.singularity_iteration.common.network;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * 量子靴突进速度同步???
 * 服务端计算速度后发送给客户端应???
 */
@SuppressWarnings("null") public record QuantumBoostPacket(double motionX, double motionY, double motionZ) implements CustomPacketPayload {

    public static final Type<QuantumBoostPacket> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "quantum_boost")
    );

    public static final StreamCodec<FriendlyByteBuf, QuantumBoostPacket> CODEC = StreamCodec.of(
        (buf, packet) -> {
            buf.writeDouble(packet.motionX);
            buf.writeDouble(packet.motionY);
            buf.writeDouble(packet.motionZ);
        },
        buf -> new QuantumBoostPacket(buf.readDouble(), buf.readDouble(), buf.readDouble())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /**
     * 客户端处理：应用突进速度
     */
    public static void handle(QuantumBoostPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            if (player == null) return;

            player.setDeltaMovement(packet.motionX, packet.motionY, packet.motionZ);
        });
    }
}


