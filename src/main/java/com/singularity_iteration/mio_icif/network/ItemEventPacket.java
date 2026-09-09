package com.singularity_iteration.mio_icif.network;

import com.singularity_iteration.mio_icif.Singularity_Iteration;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * 物品网络事件同步包。
 * 从服务端发送到客户端，通知客户端持有物品的玩家发生了指定事件。
 * <p>
 * 替代旧版 NetworkHelper 中仅调用本地监听器的实现，
 * 实现真正的服务端→客户端网络同步。
 */
public record ItemEventPacket(int slotIndex, int eventId, boolean limitRange)
        implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ItemEventPacket> TYPE =
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "item_event"));

    public static final StreamCodec<FriendlyByteBuf, ItemEventPacket> CODEC =
        StreamCodec.composite(
            net.minecraft.network.codec.ByteBufCodecs.VAR_INT, ItemEventPacket::slotIndex,
            net.minecraft.network.codec.ByteBufCodecs.VAR_INT, ItemEventPacket::eventId,
            net.minecraft.network.codec.ByteBufCodecs.BOOL, ItemEventPacket::limitRange,
            ItemEventPacket::new
        );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /**
     * 处理数据包（在客户端执行）
     */
    public static void handle(ItemEventPacket packet, IPayloadContext context) {
        if (FMLEnvironment.dist != Dist.CLIENT) {
            return;
        }

        context.enqueueWork(() -> {
            com.singularity_iteration.mio_icif.client.ClientPacketHandlers.handleItemEvent(packet);
        });
    }
}