package com.singularity_iteration.mio_icif.network;

import com.singularity_iteration.mio_icif.Menu.Producer.IndustrialWorkbenchMenu;
import com.singularity_iteration.mio_icif.Singularity_Iteration;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

@SuppressWarnings("null")
public record WorkbenchClearPacket() implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<WorkbenchClearPacket> TYPE =
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "workbench_clear"));

    public static final StreamCodec<FriendlyByteBuf, WorkbenchClearPacket> CODEC = StreamCodec.of(
        (buf, packet) -> {},
        buf -> new WorkbenchClearPacket()
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(WorkbenchClearPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer serverPlayer
                && serverPlayer.containerMenu instanceof IndustrialWorkbenchMenu menu) {
                menu.clearCraftingGrid(serverPlayer);
            }
        });
    }
}