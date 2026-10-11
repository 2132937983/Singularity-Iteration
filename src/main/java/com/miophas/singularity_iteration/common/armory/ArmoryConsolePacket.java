package com.miophas.singularity_iteration.common.armory;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client to server: an action in the Armory section of the Equipment Console. The player does
 * not need an open menu. The Armory Remote that the player wears or carries selects the Armory.
 *
 * <p>When: the console screen sends {@code REQUEST} once per 20 client ticks while the Armory
 * tab is open. It sends {@code SUMMON}, {@code SHOWCASE} or {@code RETURN} once per button
 * click.
 *
 * <p>Fields (client values, not trusted):
 * <ul>
 *   <li>{@code action}: {@link ArmoryRemoteService#REQUEST} (0, refresh),
 *       {@link ArmoryRemoteService#SUMMON} (1), {@link ArmoryRemoteService#SHOWCASE} (2) or
 *       {@link ArmoryRemoteService#RETURN} (3). The codec sends one unsigned byte, 0..255.
 *       {@code ArmoryRemoteService.handle} treats an unknown action as {@code REQUEST}.</li>
 *   <li>{@code index}: for {@code SUMMON}, the suit set index of the Armory. For
 *       {@code SHOWCASE}, the index in the server's showcase list. Other actions ignore it.</li>
 * </ul>
 *
 * <p>Trust boundary: the client sends no position and no player. The server finds the Armory
 * from the sender's own paired remote. {@code mio_icif_armory.summon} checks the owner and the
 * set index range. The showcase action checks the index against a list that the server builds
 * again. A remote Armory in an unloaded chunk causes at most one forced chunk load per player
 * per {@code ArmoryRemoteService.LOAD_COOLDOWN} ticks. The server answers each packet with one
 * {@link ArmoryConsoleSyncPacket}. {@code PacketRateLimit.ARMORY_CONSOLE} drops packets above
 * 10 in a burst or 5 per second (0.1.7.36).
 *
 * <p>Threading: the handler runs on the server thread (default handler thread).
 * {@code enqueueWork} runs the task at once on that thread. The task reads levels and moves
 * items, so it must run on the server thread.
 *
 * <p>Missing channel: the client screen is part of this mod, so the server has the channel.
 */
public record ArmoryConsolePacket(int action, int index) implements CustomPacketPayload {
    public static final Type<ArmoryConsolePacket> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "armory_console"));

    public static final StreamCodec<FriendlyByteBuf, ArmoryConsolePacket> CODEC = StreamCodec.of(
        (buf, p) -> { buf.writeByte(p.action); buf.writeVarInt(p.index); },
        buf -> new ArmoryConsolePacket(buf.readUnsignedByte(), buf.readVarInt()));

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    /** Server handler. A dead player cannot use the console. */
    public static void handle(ArmoryConsolePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player && player.isAlive()
                && com.miophas.singularity_iteration.common.util.PacketRateLimit.allow(player, com.miophas.singularity_iteration.common.util.PacketRateLimit.ARMORY_CONSOLE)) {
                ArmoryRemoteService.handle(player, packet.action, packet.index);
            }
        });
    }
}
