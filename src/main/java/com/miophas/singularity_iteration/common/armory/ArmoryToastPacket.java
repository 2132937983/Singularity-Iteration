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

/**
 * Server to client (the Armory owner only): the Armory repaired and recharged all stored suits.
 * The client shows a toast.
 *
 * <p>When: {@code mio_icif_armory.notifyOwner} sends one packet when a maintenance cycle ends
 * and the owner is online. The owner can be in any dimension.
 *
 * <p>Fields:
 * <ul>
 *   <li>{@code armory}: dimension and block position of the Armory.</li>
 *   <li>{@code repairedPoints}: durability points repaired in this cycle, 0 or more.</li>
 *   <li>{@code chargedEu}: EU charged into the suits in this cycle, 0 or more.</li>
 *   <li>{@code manaUsed}: mana used in this cycle, 0 or more.</li>
 * </ul>
 *
 * <p>Threading: the client handler runs on the client main thread (default handler thread).
 * {@code enqueueWork} runs the task at once on that thread. A dedicated server ignores the
 * payload.
 *
 * <p>Missing channel: the sender does not check the channel. The owner is a real player from
 * the player list, and the channel is not optional, so the owner has the channel.
 */
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
