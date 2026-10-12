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
 * Client to server: a button of the open Blueprint Builder menu (0.1.8.0).
 *
 * <p>Fields (client values, not trusted): {@code containerId} of the open menu, {@code action}
 * (one of the {@code A_*} ids of {@link BlueprintBuilderBlockEntity}, one unsigned byte) and
 * {@code index} (the offset button for {@code A_OFFSET}, 0..5; other actions ignore it).
 *
 * <p>Trust boundary: the handler acts only when the sender's open menu has the same
 * {@code containerId}, is a {@link BlueprintBuilderMenu} and {@code stillValid} accepts the
 * sender. The block entity clamps the offset to the builder range and ignores unknown actions.
 * The file name never comes from the client: the client only moves the index in the list that
 * the server read.
 */
public record BuilderActionPacket(int containerId, int action, int index) implements CustomPacketPayload {
    public static final Type<BuilderActionPacket> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "builder_action"));

    public static final StreamCodec<FriendlyByteBuf, BuilderActionPacket> CODEC = StreamCodec.of(
        (buf, p) -> { buf.writeVarInt(p.containerId); buf.writeByte(p.action); buf.writeByte(p.index); },
        buf -> new BuilderActionPacket(buf.readVarInt(), buf.readUnsignedByte(), buf.readUnsignedByte()));

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(BuilderActionPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            var open = player.containerMenu;
            if (open == null || open.containerId != packet.containerId || !open.stillValid(player)) return;
            if (open instanceof BlueprintBuilderMenu menu && menu.getBlockEntity() instanceof BlueprintBuilderBlockEntity builder) {
                builder.action(player, packet.action, packet.index);
            }
        });
    }
}
