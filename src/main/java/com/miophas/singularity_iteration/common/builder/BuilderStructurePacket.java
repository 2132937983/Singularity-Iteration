// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.builder;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * 0.1.11.188, client to server: build one of the multiblock structures ({@link BuilderStructures}) with the open Blueprint
 * Builder. Trust boundary as {@link BuilderActionPacket}: the sender's open menu must be this builder's and still valid; the
 * id must name a registered structure (the builder checks), so a client cannot name anything else.
 */
public record BuilderStructurePacket(int containerId, ResourceLocation structure) implements CustomPacketPayload {
    public static final Type<BuilderStructurePacket> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "builder_structure"));

    public static final StreamCodec<FriendlyByteBuf, BuilderStructurePacket> CODEC = StreamCodec.of(
        (buf, p) -> { buf.writeVarInt(p.containerId); buf.writeResourceLocation(p.structure); },
        buf -> new BuilderStructurePacket(buf.readVarInt(), buf.readResourceLocation()));

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(BuilderStructurePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            var open = player.containerMenu;
            if (open == null || open.containerId != packet.containerId || !open.stillValid(player)) return;
            if (open instanceof BlueprintBuilderMenu menu && menu.getBlockEntity() instanceof BlueprintBuilderBlockEntity builder)
                builder.selectStructure(player, packet.structure);
        });
    }
}
