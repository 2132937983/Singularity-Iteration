package com.singularity_iteration.mio_icif.network;

import com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_future_elc;
import com.singularity_iteration.mio_icif.Singularity_Iteration;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * 期货交易数据�? * 处理客户端到服务端的交易请求
 */
@SuppressWarnings("null")
public record FutureTradePacket(BlockPos pos, int action, int commodityIndex, int quantity) implements CustomPacketPayload {
    
    // Action: 0 = select commodity, 1 = increase quantity, 2 = decrease quantity, 3 = buy, 4 = sell, 5 = set quantity
    public static final int ACTION_SELECT = 0;
    public static final int ACTION_INCREASE = 1;
    public static final int ACTION_DECREASE = 2;
    public static final int ACTION_BUY = 3;
    public static final int ACTION_SELL = 4;
    public static final int ACTION_SET_QUANTITY = 5;
    
    public static final CustomPacketPayload.Type<FutureTradePacket> TYPE = 
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "future_trade"));

    public static final StreamCodec<FriendlyByteBuf, FutureTradePacket> CODEC = StreamCodec.of(
        (buf, packet) -> {
            buf.writeBlockPos(packet.pos);
            buf.writeInt(packet.action);
            buf.writeInt(packet.commodityIndex);
            buf.writeInt(packet.quantity);
        },
        buf -> new FutureTradePacket(
            buf.readBlockPos(),
            buf.readInt(),
            buf.readInt(),
            buf.readInt()
        )
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /**
     * 处理数据�?     */
    public static void handle(FutureTradePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer serverPlayer) {
                Singularity_Iteration.LOGGER.info("FutureTradePacket received: action={}, pos={}, commodityIndex={}",
                    packet.action, packet.pos, packet.commodityIndex);
                BlockEntity be = serverPlayer.level().getBlockEntity(packet.pos);
                if (be instanceof mio_icif_future_elc futureElc) {
                    Singularity_Iteration.LOGGER.info("BlockEntity found, processing action...");
                    switch (packet.action) {
                        case ACTION_SELECT:
                            futureElc.setSelectedCommodity(packet.commodityIndex);
                            serverPlayer.sendSystemMessage(Component.literal("Selected commodity: " + packet.commodityIndex));
                            break;
                        case ACTION_INCREASE:
                            futureElc.increaseTradeQuantity();
                            serverPlayer.sendSystemMessage(Component.literal("Quantity increased to: " + futureElc.getTradeQuantity()));
                            break;
                        case ACTION_DECREASE:
                            futureElc.decreaseTradeQuantity();
                            serverPlayer.sendSystemMessage(Component.literal("Quantity decreased to: " + futureElc.getTradeQuantity()));
                            break;
                        case ACTION_BUY:
                            futureElc.executeBuy(serverPlayer);
                            break;
                        case ACTION_SELL:
                            futureElc.executeSell(serverPlayer);
                            break;
                        case ACTION_SET_QUANTITY:
                            futureElc.setTradeQuantity(packet.quantity);
                            break;
                    }
                } else {
                    Singularity_Iteration.LOGGER.warn("BlockEntity not found at pos: {}", packet.pos);
                }
            }
        });
    }
}

