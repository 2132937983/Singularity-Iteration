package com.singularity_iteration.mio_icif.network;

import com.singularity_iteration.mio_icif.Singularity_Iteration;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * 量子靴大跳网络包（保留兼容，但不再使用）
 *
 * IC2RIn120原版逻辑已迁移到�?
 * - 客户端：mio_icif_ClientEvents.sendKeyStateUpdate() 发送按键状态?
 * - 服务端：mio_icif_boots_quantum.handleQuantumJumpServer() 使用mio_icif_KeyboardManager检测按�?
 *
 * 此包保留用于向后兼容，不再处理跳跃逻辑
 */
@SuppressWarnings("null")
public record QuantumJumpPacket(boolean dummy) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<QuantumJumpPacket> TYPE =
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "quantum_jump"));

    public static final StreamCodec<FriendlyByteBuf, QuantumJumpPacket> CODEC = StreamCodec.of(
        (buf, packet) -> buf.writeBoolean(packet.dummy),
        buf -> new QuantumJumpPacket(buf.readBoolean())
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(QuantumJumpPacket packet, IPayloadContext context) {
        // 跳跃逻辑已迁移到服务端inventoryTick + KeyboardManager
    }
}

