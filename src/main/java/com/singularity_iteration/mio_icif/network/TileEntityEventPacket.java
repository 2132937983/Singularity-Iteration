package com.singularity_iteration.mio_icif.network;

import com.singularity_iteration.mio_icif.Singularity_Iteration;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * 方块实体网络事件同步包。
 * 从服务端发送到客户端，通知客户端方块实体发生了指定事件。
 * <p>
 * 替代旧版 NetworkHelper 中仅调用本地监听器的实现，
 * 实现真正的服务端→客户端网络同步。
 */
public record TileEntityEventPacket(BlockPos pos, int eventId, boolean limitRange)
        implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<TileEntityEventPacket> TYPE =
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "tile_entity_event"));

    public static final StreamCodec<FriendlyByteBuf, TileEntityEventPacket> CODEC =
        StreamCodec.composite(
            BlockPos.STREAM_CODEC, TileEntityEventPacket::pos,
            net.minecraft.network.codec.ByteBufCodecs.VAR_INT, TileEntityEventPacket::eventId,
            net.minecraft.network.codec.ByteBufCodecs.BOOL, TileEntityEventPacket::limitRange,
            TileEntityEventPacket::new
        );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /**
     * 处理数据包（在客户端执行）
     */
    public static void handle(TileEntityEventPacket packet, IPayloadContext context) {
        if (FMLEnvironment.dist != Dist.CLIENT) {
            return;
        }

        context.enqueueWork(() -> {
            com.singularity_iteration.mio_icif.client.ClientPacketHandlers.handleTileEntityEvent(packet);
        });
    }
}