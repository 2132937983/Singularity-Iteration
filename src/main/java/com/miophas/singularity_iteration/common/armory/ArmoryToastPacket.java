package com.miophas.singularity_iteration.common.armory;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Server -> owner: every stored suit has been repaired and recharged. */
public record ArmoryToastPacket(GlobalPos armory, int repairedPoints, long chargedEu, int manaUsed) implements CustomPacketPayload {
    public static final Type<ArmoryToastPacket> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "armory_toast"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ArmoryToastPacket> CODEC = StreamCodec.of(
        (buf, p) -> {
            GlobalPos.STREAM_CODEC.encode(buf, p.armory);
            buf.writeVarInt(p.repairedPoints);
            buf.writeVarLong(p.chargedEu);
            buf.writeVarInt(p.manaUsed);
        },
        buf -> new ArmoryToastPacket(GlobalPos.STREAM_CODEC.decode(buf), buf.readVarInt(), buf.readVarLong(), buf.readVarInt()));

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(ArmoryToastPacket packet, IPayloadContext context) {
        if (FMLEnvironment.dist != Dist.CLIENT) return;
        context.enqueueWork(() -> com.miophas.singularity_iteration.common.client.armory.ArmoryToastOverlay.push(packet));
    }
}
