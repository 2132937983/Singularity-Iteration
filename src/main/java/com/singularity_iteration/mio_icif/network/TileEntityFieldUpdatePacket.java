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
 * 方块实体字段更新同步包。
 * 从服务端发送到客户端，通知客户端方块实体的指定字段已更新，并携带字段的最新值。
 * <p>
 * 替代旧版 NetworkHelper.updateTileEntityField 中仅调用本地监听器的实现，
 * 实现真正的服务端→客户端字段同步。
 * <p>
 * 字段值使用 {@code double} 类型传输，足以覆盖 IC2 风格的所有字段类型
 * （整数、浮点数、布尔值等），客户端收到后通过
 * {@link INetworkUpdateListener#onNetworkUpdate(String, double)} 获取新值。
 */
public record TileEntityFieldUpdatePacket(BlockPos pos, String fieldName, double fieldValue)
        implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<TileEntityFieldUpdatePacket> TYPE =
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "tile_entity_field_update"));

    public static final StreamCodec<FriendlyByteBuf, TileEntityFieldUpdatePacket> CODEC =
        StreamCodec.composite(
            BlockPos.STREAM_CODEC, TileEntityFieldUpdatePacket::pos,
            net.minecraft.network.codec.ByteBufCodecs.STRING_UTF8, TileEntityFieldUpdatePacket::fieldName,
            net.minecraft.network.codec.ByteBufCodecs.DOUBLE, TileEntityFieldUpdatePacket::fieldValue,
            TileEntityFieldUpdatePacket::new
        );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /**
     * 处理数据包（在客户端执行）
     */
    public static void handle(TileEntityFieldUpdatePacket packet, IPayloadContext context) {
        if (FMLEnvironment.dist != Dist.CLIENT) {
            return;
        }

        context.enqueueWork(() -> {
            com.singularity_iteration.mio_icif.client.ClientPacketHandlers.handleTileEntityFieldUpdate(packet);
        });
    }
}