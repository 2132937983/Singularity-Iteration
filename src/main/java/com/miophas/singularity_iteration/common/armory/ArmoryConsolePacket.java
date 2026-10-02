package com.miophas.singularity_iteration.common.armory;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client -> server: Equipment Console Armory section (refresh, summon suit, summon showcase, return). */
public record ArmoryConsolePacket(int action, int index) implements CustomPacketPayload {
    public static final Type<ArmoryConsolePacket> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "armory_console"));

    public static final StreamCodec<FriendlyByteBuf, ArmoryConsolePacket> CODEC = StreamCodec.of(
        (buf, p) -> { buf.writeByte(p.action); buf.writeVarInt(p.index); },
        buf -> new ArmoryConsolePacket(buf.readUnsignedByte(), buf.readVarInt()));

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(ArmoryConsolePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player && player.isAlive()) {
                ArmoryRemoteService.handle(player, packet.action, packet.index);
            }
        });
    }
}
