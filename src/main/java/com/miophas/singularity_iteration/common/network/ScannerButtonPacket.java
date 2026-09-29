package com.miophas.singularity_iteration.common.network;

import com.miophas.singularity_iteration.common.blockentity.producer.mio_icif_scanner_elc;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
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
                if (packet.buttonType < BUTTON_DELETE || packet.buttonType > BUTTON_SAVE
                        || !(serverPlayer.containerMenu instanceof com.miophas.singularity_iteration.common.menu.producer.ScannerElcMenu menu)
                        || !(menu.getBlockEntity() instanceof mio_icif_scanner_elc scanner)
                        || !scanner.getBlockPos().equals(packet.pos)
                        || !menu.stillValid(serverPlayer)
                        || serverPlayer.distanceToSqr(packet.pos.getX() + 0.5, packet.pos.getY() + 0.5, packet.pos.getZ() + 0.5) > 64.0) return;
                menu.clickMenuButton(serverPlayer,packet.buttonType);
            }
        });
    }
}

