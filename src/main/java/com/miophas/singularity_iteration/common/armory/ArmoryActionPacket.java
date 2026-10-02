package com.miophas.singularity_iteration.common.armory;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client -> server: Armory console / remote actions. All validated against the open menu. */
public record ArmoryActionPacket(int containerId, int action, int index, String text) implements CustomPacketPayload {
    public static final int SUMMON = 0, RENAME = 1, UNBIND = 2, AUTOMATION = 3;

    public static final Type<ArmoryActionPacket> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "armory_action"));

    public static final StreamCodec<FriendlyByteBuf, ArmoryActionPacket> CODEC = StreamCodec.of(
        (buf, p) -> { buf.writeVarInt(p.containerId); buf.writeByte(p.action); buf.writeVarInt(p.index); buf.writeUtf(p.text, 64); },
        buf -> new ArmoryActionPacket(buf.readVarInt(), buf.readUnsignedByte(), buf.readVarInt(), buf.readUtf(64)));

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(ArmoryActionPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            var open = player.containerMenu;
            if (open == null || open.containerId != packet.containerId || !open.stillValid(player)) return;
            if (open instanceof ArmoryRemoteMenu remote) {
                if (packet.action == SUMMON) remote.summon(packet.index);
            } else if (open instanceof ArmoryMenu menu && menu.getBlockEntity() instanceof mio_icif_armory armory) {
                switch (packet.action) {
                    case SUMMON -> player.displayClientMessage(armory.summon(player, packet.index).message(), true);
                    case RENAME -> armory.rename(player, packet.index, packet.text);
                    case UNBIND -> {
                        armory.unbind(player);
                        player.displayClientMessage(Component.translatable("message.mio_icif.armory.unbound"), true);
                        player.closeContainer();
                    }
                    case AUTOMATION -> armory.cycleAutomation(player);
                    default -> { }
                }
            }
        });
    }
}
