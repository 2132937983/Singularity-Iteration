package com.miophas.singularity_iteration.common.network;

import com.miophas.singularity_iteration.common.blockentity.producer.mio_icif_item_distributor_elc;
import com.miophas.singularity_iteration.common.blockentity.producer.mio_icif_weighted_fluid_distributor_elc;
import com.miophas.singularity_iteration.common.menu.producer.ItemDistributorElcMenu;
import com.miophas.singularity_iteration.common.menu.producer.WeightedFluidDistributorElcMenu;
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

    private static final int MAX_PRIORITY_COUNT = Direction.values().length;

    public static final CustomPacketPayload.Type<WeightedDistributorPriorityPacket> TYPE =
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("mio_icif", "weighted_distributor_priority"));

    public static final StreamCodec<FriendlyByteBuf, WeightedDistributorPriorityPacket> CODEC =
        StreamCodec.of(
            (buf, packet) -> {
                if (packet.priorityOrdinals == null || packet.priorityOrdinals.size() > MAX_PRIORITY_COUNT) {
                    throw new IllegalArgumentException("Invalid priority count");
                }
                buf.writeBlockPos(packet.pos);
                buf.writeInt(packet.priorityOrdinals.size());
                for (int ord : packet.priorityOrdinals) {
                    buf.writeInt(ord);
                }
            },
            buf -> {
                BlockPos pos = buf.readBlockPos();
                int size = buf.readInt();
                if (size < 0 || size > MAX_PRIORITY_COUNT) {
                    throw new IllegalArgumentException("Invalid priority count: " + size);
                }
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
                if (packet.priorityOrdinals == null || packet.priorityOrdinals.size() > MAX_PRIORITY_COUNT) return;
                Level level = serverPlayer.level();
                BlockPos pos = packet.pos;

                boolean menuMatches = serverPlayer.containerMenu instanceof ItemDistributorElcMenu itemMenu
                        ? pos.equals(itemMenu.getBlockPos()) && itemMenu.stillValid(serverPlayer)
                        : serverPlayer.containerMenu instanceof WeightedFluidDistributorElcMenu fluidMenu
                            && pos.equals(fluidMenu.getBlockPos()) && fluidMenu.stillValid(serverPlayer);
                if (!menuMatches || serverPlayer.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > 64.0
                        || !level.isLoaded(pos)) return;

                BlockEntity blockEntity = level.getBlockEntity(pos);
                List<Direction> priorities = new ArrayList<>(packet.priorityOrdinals.size());
                for (int ord : packet.priorityOrdinals) {
                    if (ord < 0 || ord >= MAX_PRIORITY_COUNT) return;
                    priorities.add(Direction.from3DDataValue(ord));
                }

                if (blockEntity instanceof mio_icif_item_distributor_elc dist
                        && serverPlayer.containerMenu instanceof ItemDistributorElcMenu menu
                        && menu.getBlockEntity() == dist) {
                    dist.setPriority(priorities);
                } else if (blockEntity instanceof mio_icif_weighted_fluid_distributor_elc dist
                        && serverPlayer.containerMenu instanceof WeightedFluidDistributorElcMenu menu
                        && menu.getBlockEntity() == dist) {
                    dist.setPriority(priorities);
                }
            }
        });
    }
}

