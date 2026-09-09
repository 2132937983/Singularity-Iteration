package com.singularity_iteration.mio_icif.network;

import com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_fluid_distributor_elc;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

@SuppressWarnings("null")
public record FluidDistributorModePacket(BlockPos pos) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<FluidDistributorModePacket> TYPE =
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("mio_icif", "fluid_distributor_mode"));

    public static final StreamCodec<FriendlyByteBuf, FluidDistributorModePacket> CODEC =
        StreamCodec.of(
            (buf, packet) -> buf.writeBlockPos(packet.pos),
            buf -> new FluidDistributorModePacket(buf.readBlockPos())
        );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(FluidDistributorModePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer serverPlayer) {
                Level level = serverPlayer.level();
                BlockPos pos = packet.pos;

                if (!level.isLoaded(pos)) return;

                BlockEntity blockEntity = level.getBlockEntity(pos);
                if (blockEntity instanceof mio_icif_fluid_distributor_elc dist) {
                    dist.toggleMode();
                }
            }
        });
    }
}

