package com.miophas.singularity_iteration.common.network;

import com.miophas.singularity_iteration.common.blockentity.producer.mio_icif_advanced_miner_elc;
import com.miophas.singularity_iteration.common.menu.producer.AdvancedMinerElcMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * 高级采矿机黑白名单过滤槽设置包。
 *
 * <p>过滤槽是幽灵槽（不占用 itemHandler）：通过点击槽位设置时会随容器点击包一起到达服务端，
 * 但从 JEI 面板拖拽（{@code IGhostIngredientHandler}）只发生在客户端，因此需要本包
 * 主动把过滤配置同步到服务端。
 */
@SuppressWarnings("null")
public record MinerFilterPacket(BlockPos pos, int filterIndex, ItemStack stack) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<MinerFilterPacket> TYPE =
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("mio_icif", "miner_filter"));

    public static final StreamCodec<RegistryFriendlyByteBuf, MinerFilterPacket> CODEC =
        StreamCodec.composite(
            BlockPos.STREAM_CODEC, MinerFilterPacket::pos,
            ByteBufCodecs.VAR_INT, MinerFilterPacket::filterIndex,
            ItemStack.OPTIONAL_STREAM_CODEC, MinerFilterPacket::stack,
            MinerFilterPacket::new
        );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /**
     * 服务端处理：校验玩家确实打开了对应采矿机后再写入过滤槽（客户端输入不可信）。
     */
    public static void handle(MinerFilterPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer serverPlayer)) {
                return;
            }
            if (!(serverPlayer.containerMenu instanceof AdvancedMinerElcMenu menu)
                    || !(menu.getMinerBlockEntity() instanceof mio_icif_advanced_miner_elc miner)
                    || !miner.getBlockPos().equals(packet.pos)
                    || !menu.stillValid(serverPlayer)
                    || serverPlayer.distanceToSqr(packet.pos.getX() + 0.5, packet.pos.getY() + 0.5, packet.pos.getZ() + 0.5) > 64.0) {
                return;
            }
            miner.setFilterStack(packet.filterIndex, packet.stack);
        });
    }
}
