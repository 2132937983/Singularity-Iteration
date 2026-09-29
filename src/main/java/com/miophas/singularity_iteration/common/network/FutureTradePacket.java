// SCEX 2026-09-12: repaired malformed UTF-8 bytes in comments only.
package com.miophas.singularity_iteration.common.network;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.common.menu.producer.FutureElcMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * 期货交易数据??? * 处理客户端到服务端的交易请求
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
     * 处理数据???     */
    public static void handle(FutureTradePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)
                    || !player.serverLevel().getServer().isSameThread()
                    || !(player.containerMenu instanceof FutureElcMenu menu) || !menu.stillValid(player)) return;
            var future = menu.getBlockEntity();
            if (future == null || !future.getBlockPos().equals(packet.pos)) return;
            // Resolve only the open menu's live owner. A packet position never loads a chunk.
            switch (packet.action) {
                case ACTION_SELECT -> {
                    if (packet.commodityIndex >= 0 && packet.commodityIndex < future.getCurrentPageCommodities().size())
                        future.setSelectedCommodity(packet.commodityIndex);
                }
                case ACTION_INCREASE -> future.increaseTradeQuantity();
                case ACTION_DECREASE -> future.decreaseTradeQuantity();
                case ACTION_BUY -> future.executeBuy(player);
                case ACTION_SELL -> future.executeSell(player);
                case ACTION_SET_QUANTITY -> {
                    if (packet.quantity >= 1 && packet.quantity <= 64) future.setTradeQuantity(packet.quantity);
                }
                default -> { }
            }
        });
    }
}

