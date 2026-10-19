// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.network;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** 0.1.10.12, server to client: one reactor incident; the client opens the audit report. */
public record ReactorAuditPacket(CompoundTag incident) implements CustomPacketPayload {
    public static final Type<ReactorAuditPacket> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "reactor_audit"));

    public static final StreamCodec<FriendlyByteBuf, ReactorAuditPacket> CODEC =
        ByteBufCodecs.COMPOUND_TAG.map(ReactorAuditPacket::new, ReactorAuditPacket::incident).cast();

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(ReactorAuditPacket packet, IPayloadContext context) {
        if (FMLEnvironment.dist != Dist.CLIENT) return;
        context.enqueueWork(() -> com.miophas.singularity_iteration.common.client.ClientPacketHandlers.handleReactorAudit(packet.incident()));
    }
}
