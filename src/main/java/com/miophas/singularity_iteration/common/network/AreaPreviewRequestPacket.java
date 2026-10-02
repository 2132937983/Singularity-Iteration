package com.miophas.singularity_iteration.common.network;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.common.area.AreaPreviewService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client -> server: "show the work area of the machine whose GUI I have open". */
@SuppressWarnings("null")
public record AreaPreviewRequestPacket(int containerId) implements CustomPacketPayload {
    public static final Type<AreaPreviewRequestPacket> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "area_preview_request"));

    public static final StreamCodec<FriendlyByteBuf, AreaPreviewRequestPacket> CODEC = StreamCodec.of(
        (buf, p) -> buf.writeVarInt(p.containerId),
        buf -> new AreaPreviewRequestPacket(buf.readVarInt()));

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(AreaPreviewRequestPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) AreaPreviewService.requestFromMenu(player, packet.containerId);
        });
    }
}
