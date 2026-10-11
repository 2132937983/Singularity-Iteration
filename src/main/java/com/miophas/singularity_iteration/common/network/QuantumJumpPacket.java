package com.miophas.singularity_iteration.common.network;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Network packet for the quantum boots high jump. It stays for compatibility but is not used.
 *
 * The original IC2RIn120 logic moved to these locations:
 * - Client: mio_icif_ClientEvents.sendKeyStateUpdate() sends the key state.
 * - Server: mio_icif_boots_quantum.handleQuantumJumpServer() uses mio_icif_KeyboardManager to detect the key.
 *
 * This packet stays for backward compatibility. It does not handle the jump logic.
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

