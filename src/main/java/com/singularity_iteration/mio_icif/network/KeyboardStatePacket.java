package com.singularity_iteration.mio_icif.network;

import com.singularity_iteration.mio_icif.Singularity_Iteration;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * 按键状态同步包（IC2RIn120原版移植�?
 *
 * 客户端每tick检测按键状态，状态变化时发送此包到服务�?
 * 服务端存储玩家按键状态，供inventoryTick中使�?
 *
 * 对应IC2：KeyboardClient.sendKeyUpdate() �?IC2.network.initiateKeyUpdate()
 */
@SuppressWarnings("null")
public record KeyboardStatePacket(int keyState) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<KeyboardStatePacket> TYPE =
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "keyboard_state"));

    public static final StreamCodec<FriendlyByteBuf, KeyboardStatePacket> CODEC = StreamCodec.of(
        (buf, packet) -> buf.writeInt(packet.keyState),
        buf -> new KeyboardStatePacket(buf.readInt())
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /**
     * 处理按键状态更新?
     * 对应IC2：Keyboard.processKeyUpdate()
     */
    public static void handle(KeyboardStatePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() == null) return;
            mio_icif_KeyboardManager.processKeyUpdate(context.player(), packet.keyState());
        });
    }
}

