package com.singularity_iteration.mio_icif.network;

import com.singularity_iteration.mio_icif.util.JetpackKeyHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * 喷气背包按键状态数据包
 * 客户端发送按键状态到服务端
 */
public record JetpackKeyStatePacket(int keyState) implements CustomPacketPayload {
    
    public static final CustomPacketPayload.Type<JetpackKeyStatePacket> TYPE = 
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("mio_icif", "jetpack_key_state"));
    
    public static final StreamCodec<FriendlyByteBuf, JetpackKeyStatePacket> CODEC = 
        StreamCodec.composite(
            ByteBufCodecs.INT,
            JetpackKeyStatePacket::keyState,
            JetpackKeyStatePacket::new
        );
    
    @Override
    public CustomPacketPayload.Type<JetpackKeyStatePacket> type() {
        return TYPE;
    }
    
    /**
     * 服务端处理
     */
    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer serverPlayer) {
                JetpackKeyHandler.processKeyUpdate(serverPlayer, keyState);
            }
        });
    }
}