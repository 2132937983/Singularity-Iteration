package com.singularity_iteration.mio_icif.network;

import com.singularity_iteration.mio_icif.Blocks.Producer.mio_icif_block_metal_former;
import com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_metal_former;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * 金属成型机模式切换数据包
 */
@SuppressWarnings("null")
public record MetalFormerModePacket(BlockPos pos, int modeOrdinal) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<MetalFormerModePacket> TYPE =
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("mio_icif", "metal_former_mode"));

    public static final StreamCodec<FriendlyByteBuf, MetalFormerModePacket> CODEC =
        StreamCodec.of(
            (buf, packet) -> {
                buf.writeBlockPos(packet.pos);
                buf.writeInt(packet.modeOrdinal);
            },
            buf -> new MetalFormerModePacket(buf.readBlockPos(), buf.readInt())
        );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /**
     * 处理数据�
 */
    public static void handle(MetalFormerModePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            // 在服务端执行
            if (context.player() instanceof ServerPlayer serverPlayer) {
                Level level = serverPlayer.level();
                BlockPos pos = packet.pos;

                // 检查位置是否加载
            if (!level.isLoaded(pos)) {
                    return;
                }

                BlockState state = level.getBlockState(pos);

                // 检查是否是金属成型机方法
            if (state.getBlock() instanceof mio_icif_block_metal_former) {
                    // 获取新模式
                mio_icif_block_metal_former.MetalFormerMode newMode =
                        mio_icif_block_metal_former.MetalFormerMode.values()[packet.modeOrdinal % 3];

                    // 更新方块状态
                level.setBlockAndUpdate(pos, state.setValue(mio_icif_block_metal_former.MODE, newMode));

                    // 更新方块实体中的模式
                    BlockEntity blockEntity = level.getBlockEntity(pos);
                    if (blockEntity instanceof mio_icif_metal_former metalFormer) {
                        // 转换模式枚举类型
                        mio_icif_metal_former.MetalFormerMode entityMode =
                            mio_icif_metal_former.MetalFormerMode.valueOf(newMode.name());
                        metalFormer.setMode(entityMode);
                    }
                }
            }
        });
    }
}

