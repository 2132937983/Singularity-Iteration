package com.miophas.singularity_iteration.common.network;

import com.miophas.singularity_iteration.common.Singularity_Iteration;

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

    private static final int MAX_RESULTS = 256;
    private static final int MAX_KEY_LENGTH = 256;

    public static final CustomPacketPayload.Type<ODScannerResultPacket> TYPE =
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "od_scanner_result"));

    public static final StreamCodec<FriendlyByteBuf, ODScannerResultPacket> CODEC = StreamCodec.of(
        (buf, packet) -> {
            if (packet.scanResults == null || packet.scanResults.size() > MAX_RESULTS) {
                throw new IllegalArgumentException("Invalid scanner result count");
            }
            // 写入扫描结果数量
            buf.writeVarInt(packet.scanResults.size());
            // 写入每个扫描结果
            packet.scanResults.forEach((key, value) -> {
                if (key == null || key.length() > MAX_KEY_LENGTH) {
                    throw new IllegalArgumentException("Invalid scanner result key");
                }
                buf.writeUtf(key, MAX_KEY_LENGTH);
                buf.writeVarInt(value);
            });
        },
        buf -> {
            // 读取扫描结果
            Map<String, Integer> results = new HashMap<>();
            int size = buf.readVarInt();
            if (size < 0 || size > MAX_RESULTS) {
                throw new IllegalArgumentException("Invalid scanner result count: " + size);
            }
            for (int i = 0; i < size; i++) {
                String key = buf.readUtf(MAX_KEY_LENGTH);
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
            com.miophas.singularity_iteration.common.client.ClientPacketHandlers.handleODScannerResult(packet);
        });
    }
}
