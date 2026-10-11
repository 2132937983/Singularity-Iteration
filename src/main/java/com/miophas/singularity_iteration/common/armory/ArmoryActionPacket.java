package com.miophas.singularity_iteration.common.armory;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client to server: an action in the open Armory block menu ({@code ArmoryScreen}).
 *
 * <p>When: the Armory screen sends one packet per button click or per rename.
 *
 * <p>Fields (client values, not trusted):
 * <ul>
 *   <li>{@code containerId}: id of the menu that the client has open.</li>
 *   <li>{@code action}: {@link #SUMMON}, {@link #RENAME}, {@link #UNBIND} or
 *       {@link #AUTOMATION}. The codec sends one unsigned byte, 0..255. The handler ignores
 *       other values.</li>
 *   <li>{@code index}: suit set index for {@code SUMMON} and {@code RENAME}. Other actions
 *       ignore it.</li>
 *   <li>{@code text}: new set name for {@code RENAME}, at most 64 characters on the wire. Other
 *       actions ignore it.</li>
 * </ul>
 *
 * <p>Trust boundary: the handler acts only when the sender's open menu has the same
 * {@code containerId}, is an {@code ArmoryMenu}, and {@code stillValid} accepts the sender.
 * {@code stillValid} checks that the Armory block entity is still in place, that the player is
 * alive and within 8 blocks, and that the player has access (owner, unowned Armory, or
 * operator). The Armory methods then check
 * the owner and the ranges:
 * <ul>
 *   <li>{@code summon}: owner only, set index in range, Armory not busy.</li>
 *   <li>{@code rename}: owner only, set index in range. It strips the name and cuts it to
 *       {@code NAME_LENGTH} (16) characters.</li>
 *   <li>{@code unbind} and {@code cycleAutomation}: owner or operator (permission level 2).</li>
 * </ul>
 *
 * <p>Threading: the handler runs on the server thread (default handler thread).
 * {@code enqueueWork} runs the task at once on that thread.
 *
 * <p>Missing channel: the client screen is part of this mod, so the server has the channel.
 */
public record ArmoryActionPacket(int containerId, int action, int index, String text) implements CustomPacketPayload {
    /** Action ids. Each id fits in one unsigned byte. */
    public static final int SUMMON = 0, RENAME = 1, UNBIND = 2, AUTOMATION = 3;

    public static final Type<ArmoryActionPacket> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "armory_action"));

    public static final StreamCodec<FriendlyByteBuf, ArmoryActionPacket> CODEC = StreamCodec.of(
        (buf, p) -> { buf.writeVarInt(p.containerId); buf.writeByte(p.action); buf.writeVarInt(p.index); buf.writeUtf(p.text, 64); },
        buf -> new ArmoryActionPacket(buf.readVarInt(), buf.readUnsignedByte(), buf.readVarInt(), buf.readUtf(64)));

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    /** Server handler. See the class Javadoc for the checks. */
    public static void handle(ArmoryActionPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            // The menu must be the one that the client names, and it must still be valid for the sender.
            var open = player.containerMenu;
            if (open == null || open.containerId != packet.containerId || !open.stillValid(player)) return;
            if (open instanceof ArmoryMenu menu && menu.getBlockEntity() instanceof mio_icif_armory armory) {
                switch (packet.action) {
                    case SUMMON -> player.displayClientMessage(armory.summon(player, packet.index).message(), true);
                    case RENAME -> armory.rename(player, packet.index, packet.text);
                    case UNBIND -> {
                        // Show the message and close the menu only when the unbind took place
                        // (0.1.7.36: before, a player without the right also saw "unbound").
                        if (armory.unbind(player)) {
                            player.displayClientMessage(Component.translatable("message.mio_icif.armory.unbound"), true);
                            player.closeContainer();
                        }
                    }
                    case AUTOMATION -> armory.cycleAutomation(player);
                    default -> { }
                }
            }
        });
    }
}
