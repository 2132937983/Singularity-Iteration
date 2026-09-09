package com.singularity_iteration.mio_icif.network;

import com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_item_distributor_elc;
import com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_weighted_fluid_distributor_elc;
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

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("null")
public record WeightedDistributorPriorityPacket(BlockPos pos, List<Integer> priorityOrdinals) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<WeightedDistributorPriorityPacket> TYPE =
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("mio_icif", "weighted_distributor_priority"));

    public static final StreamCodec<FriendlyByteBuf, WeightedDistributorPriorityPacket> CODEC =
        StreamCodec.of(
            (buf, packet) -> {
                buf.writeBlockPos(packet.pos);
                buf.writeInt(packet.priorityOrdinals.size());
                for (int ord : packet.priorityOrdinals) {
                    buf.writeInt(ord);
                }
            },
            buf -> {
                BlockPos pos = buf.readBlockPos();
                int size = buf.readInt();
                List<Integer> ordinals = new ArrayList<>(size);
                for (int i = 0; i < size; i++) {
                    ordinals.add(buf.readInt());
                }
                return new WeightedDistributorPriorityPacket(pos, ordinals);
            }
        );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(WeightedDistributorPriorityPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer serverPlayer) {
                Level level = serverPlayer.level();
                BlockPos pos = packet.pos;

                if (!level.isLoaded(pos)) return;

                BlockEntity blockEntity = level.getBlockEntity(pos);
                List<Direction> priorities = new ArrayList<>(packet.priorityOrdinals.size());
                for (int ord : packet.priorityOrdinals) {
                    priorities.add(Direction.from3DDataValue(ord));
                }

                if (blockEntity instanceof mio_icif_item_distributor_elc dist) {
                    dist.setPriority(priorities);
                } else if (blockEntity instanceof mio_icif_weighted_fluid_distributor_elc dist) {
                    dist.setPriority(priorities);
                }
            }
        });
    }
}

