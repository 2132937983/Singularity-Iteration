package com.miophas.singularity_iteration.common.network;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.common.menu.wiring.EnergyTerminalMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client -> server: switch one device of the terminal's network on/off (validated server side). */
@SuppressWarnings("null")
public record EnergyTerminalTogglePacket(int containerId, BlockPos device) implements CustomPacketPayload {
    public static final Type<EnergyTerminalTogglePacket> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "energy_terminal_toggle"));

    public static final StreamCodec<FriendlyByteBuf, EnergyTerminalTogglePacket> CODEC = StreamCodec.of(
        (buf, p) -> { buf.writeVarInt(p.containerId); buf.writeBlockPos(p.device); },
        buf -> new EnergyTerminalTogglePacket(buf.readVarInt(), buf.readBlockPos()));

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(EnergyTerminalTogglePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            if (player.containerMenu.containerId != packet.containerId
                    || !(player.containerMenu instanceof EnergyTerminalMenu menu) || !menu.stillValid(player)) return;
            menu.toggle(packet.device);
        });
    }
}
