package com.singularity_iteration.mio_icif.network;

import com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_scanner_elc;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * 扫描机按钮点击网络包
 * 用于处理删除和保存按钮的点击
 */
@SuppressWarnings("null")
public record ScannerButtonPacket(BlockPos pos, int buttonType) implements CustomPacketPayload {

    // 按钮类型常量
    public static final int BUTTON_DELETE = 0;  // 删除按钮
    public static final int BUTTON_SAVE = 1;    // 保存按钮

    public static final CustomPacketPayload.Type<ScannerButtonPacket> TYPE =
        new CustomPacketPayload.Type<>(ResourceLocation.parse("mio_icif:scanner_button"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ScannerButtonPacket> STREAM_CODEC =
        StreamCodec.composite(
            BlockPos.STREAM_CODEC, ScannerButtonPacket::pos,
            ByteBufCodecs.INT, ScannerButtonPacket::buttonType,
            ScannerButtonPacket::new
        );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /**
     * 服务器端处理�
 */
    public static void handleOnServer(ScannerButtonPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer serverPlayer) {
                ServerLevel level = serverPlayer.serverLevel();
                BlockPos pos = packet.pos;

                // 检查区块是否加载
            if (!level.isLoaded(pos)) {
                    return;
                }

                BlockEntity blockEntity = level.getBlockEntity(pos);
                if (!(blockEntity instanceof mio_icif_scanner_elc scanner)) {
                    return;
                }

                // 处理按钮点击
                if (packet.buttonType == BUTTON_DELETE) {
                    // 删除扫描结果
                    scanner.discardResult();
                } else if (packet.buttonType == BUTTON_SAVE) {
                    // 保存扫描结果到模式存储机
                    scanner.storeResult();
                }
            }
        });
    }
}

