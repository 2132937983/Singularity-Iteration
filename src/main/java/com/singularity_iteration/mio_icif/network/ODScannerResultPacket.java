package com.singularity_iteration.mio_icif.network;

import com.singularity_iteration.mio_icif.Singularity_Iteration;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.HashMap;
import java.util.Map;

/**
 * OD扫描器扫描结果数据包
 * 从服务端发送扫描结果到客户端
 */
public record ODScannerResultPacket(Map<String, Integer> scanResults) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ODScannerResultPacket> TYPE =
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "od_scanner_result"));

    public static final StreamCodec<FriendlyByteBuf, ODScannerResultPacket> CODEC = StreamCodec.of(
        (buf, packet) -> {
            // 写入扫描结果数量
            buf.writeVarInt(packet.scanResults.size());
            // 写入每个扫描结果
            packet.scanResults.forEach((key, value) -> {
                buf.writeUtf(key);
                buf.writeVarInt(value);
            });
        },
        buf -> {
            // 读取扫描结果
            Map<String, Integer> results = new HashMap<>();
            int size = buf.readVarInt();
            for (int i = 0; i < size; i++) {
                String key = buf.readUtf();
                int value = buf.readVarInt();
                results.put(key, value);
            }
            return new ODScannerResultPacket(results);
        }
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /**
     * 处理数据包（在客户端执行）
     */
    public static void handle(ODScannerResultPacket packet, IPayloadContext context) {
        if (FMLEnvironment.dist != Dist.CLIENT) {
            return;
        }

        context.enqueueWork(() -> {
            com.singularity_iteration.mio_icif.client.ClientPacketHandlers.handleODScannerResult(packet);
        });
    }
}