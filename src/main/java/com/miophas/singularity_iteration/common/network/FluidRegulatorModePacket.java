package com.miophas.singularity_iteration.common.network;

import com.miophas.singularity_iteration.common.blockentity.producer.mio_icif_fluid_regulator_elc;
import com.miophas.singularity_iteration.common.menu.producer.FluidRegulatorElcMenu;
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
public record FluidRegulatorModePacket(BlockPos pos, int event) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<FluidRegulatorModePacket> TYPE =
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("mio_icif", "fluid_regulator_mode"));

    public static final StreamCodec<FriendlyByteBuf, FluidRegulatorModePacket> CODEC =
        StreamCodec.of(
            (buf, packet) -> {
                buf.writeBlockPos(packet.pos);
                buf.writeInt(packet.event);
            },
            buf -> new FluidRegulatorModePacket(buf.readBlockPos(), buf.readInt())
        );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(FluidRegulatorModePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer serverPlayer) {
                Level level = serverPlayer.level();
                BlockPos pos = packet.pos;

                if (!level.isLoaded(pos)
                        || !(serverPlayer.containerMenu instanceof FluidRegulatorElcMenu menu)
                        || !pos.equals(menu.getBlockPos())
                        || !menu.stillValid(serverPlayer)
                        || serverPlayer.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > 64.0) return;

                BlockEntity blockEntity = level.getBlockEntity(pos);
                if (blockEntity instanceof mio_icif_fluid_regulator_elc regulator
                        && menu.getBlockEntity() == regulator) {
                    regulator.onNetworkEvent(packet.event());
                }
            }
        });
    }
}

