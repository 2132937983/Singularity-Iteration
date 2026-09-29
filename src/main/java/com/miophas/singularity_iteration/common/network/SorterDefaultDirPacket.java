package com.miophas.singularity_iteration.common.network;

import com.miophas.singularity_iteration.common.blockentity.producer.mio_icif_sorter_elc;
import com.miophas.singularity_iteration.common.menu.producer.SorterElcMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

@SuppressWarnings("null")
public record SorterDefaultDirPacket(BlockPos pos, int directionOrdinal) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SorterDefaultDirPacket> TYPE =
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("mio_icif", "sorter_default_dir"));

    public static final StreamCodec<FriendlyByteBuf, SorterDefaultDirPacket> CODEC =
        StreamCodec.of(
            (buf, packet) -> {
                buf.writeBlockPos(packet.pos);
                buf.writeInt(packet.directionOrdinal);
            },
            buf -> new SorterDefaultDirPacket(buf.readBlockPos(), buf.readInt())
        );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SorterDefaultDirPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer serverPlayer) {
                Level level = serverPlayer.level();
                BlockPos pos = packet.pos;

                if (packet.directionOrdinal < 0 || packet.directionOrdinal >= Direction.values().length
                        || !level.isLoaded(pos)
                        || !(serverPlayer.containerMenu instanceof SorterElcMenu menu)
                        || !pos.equals(menu.getBlockPos())
                        || !menu.stillValid(serverPlayer)
                        || serverPlayer.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > 64.0) return;

                BlockEntity blockEntity = level.getBlockEntity(pos);
                if (blockEntity instanceof mio_icif_sorter_elc sorter
                        && menu.getBlockEntity() == sorter) {
                    Direction dir = Direction.from3DDataValue(packet.directionOrdinal);
                    sorter.setDefaultOutputDirection(dir);
                }
            }
        });
    }
}

