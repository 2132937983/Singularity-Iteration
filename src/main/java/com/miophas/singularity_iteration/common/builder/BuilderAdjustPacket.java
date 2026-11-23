// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.builder;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * 0.1.11.185: client to server, placing a builder's blueprint in the world (the screen is closed): move it a block
 * ({@code A_OFFSET} with the axis and direction in {@code index}, as the old buttons), turn it, mirror it, or show the area.
 * The server acts only for a Blueprint Builder within {@link #REACH} blocks of the player, and only on these four actions;
 * the builder applies its own rules (not while it runs, not for a Create schematic, the offset within the range).
 */
public record BuilderAdjustPacket(BlockPos builder, int action, int index) implements CustomPacketPayload {
    public static final double REACH = 256;   // 0.1.11.186: was 96 (large blueprints)
    public static final Type<BuilderAdjustPacket> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "builder_adjust"));

    public static final StreamCodec<FriendlyByteBuf, BuilderAdjustPacket> CODEC = StreamCodec.of(
        (buf, p) -> { buf.writeBlockPos(p.builder); buf.writeByte(p.action); buf.writeByte(p.index); },
        buf -> new BuilderAdjustPacket(buf.readBlockPos(), buf.readUnsignedByte(), buf.readUnsignedByte()));

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(BuilderAdjustPacket p, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            if (p.action != BlueprintBuilderBlockEntity.A_OFFSET && p.action != BlueprintBuilderBlockEntity.A_ROTATE
                && p.action != BlueprintBuilderBlockEntity.A_MIRROR && p.action != BlueprintBuilderBlockEntity.A_PREVIEW) return;
            var level = player.serverLevel();
            if (!level.isLoaded(p.builder) || player.distanceToSqr(p.builder.getCenter()) > REACH * REACH) return;
            if (level.getBlockEntity(p.builder) instanceof BlueprintBuilderBlockEntity builder) builder.action(player, p.action, p.index);
        });
    }
}
