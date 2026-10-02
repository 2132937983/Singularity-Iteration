package com.miophas.singularity_iteration.common.network;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.common.blockentity.producer.tower.TargetFilter;
import com.miophas.singularity_iteration.common.menu.producer.LaserTowerMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client -> server: edit the tower target list of the open menu. Validated server side. */
@SuppressWarnings("null")
public record LaserTowerConfigPacket(int containerId, int action, int index, String text) implements CustomPacketPayload {
    public static final int ADD = 0, REMOVE = 1, TOGGLE_MODE = 2, RESET_STATS = 3;

    public static final Type<LaserTowerConfigPacket> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "laser_tower_config"));

    public static final StreamCodec<FriendlyByteBuf, LaserTowerConfigPacket> CODEC = StreamCodec.of(
        (buf, p) -> { buf.writeVarInt(p.containerId); buf.writeByte(p.action); buf.writeVarInt(p.index); buf.writeUtf(p.text, TargetFilter.MAX_LENGTH); },
        buf -> new LaserTowerConfigPacket(buf.readVarInt(), buf.readUnsignedByte(), buf.readVarInt(), buf.readUtf(TargetFilter.MAX_LENGTH)));

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(LaserTowerConfigPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            if (player.containerMenu.containerId != packet.containerId
                    || !(player.containerMenu instanceof LaserTowerMenu menu) || !menu.stillValid(player)) return;
            menu.applyConfig(packet.action, packet.index, packet.text);
        });
    }
}
