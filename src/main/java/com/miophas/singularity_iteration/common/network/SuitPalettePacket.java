// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.network;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.common.item.armor.ArmorFeatureSlots;
import com.miophas.singularity_iteration.common.suit.SuitPalette;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client to server: set or clear the colour palette of one worn suit piece (0.1.8.0).
 *
 * <p>The Singularity Management Panel (appearance section, palette page) sends one packet for
 * each change that the player confirms: a preset click, the end of a slider drag, or the reset
 * button.
 *
 * <p>Fields (client values, not trusted):
 * <ul>
 *   <li>{@code slot}: an armor slot id ({@code head}, {@code chest}, {@code legs}, {@code feet}),
 *       at most 16 characters.</li>
 *   <li>{@code primary}, {@code accent}: RGB colours. The handler masks them to 24 bits.</li>
 *   <li>{@code clear}: true removes the palette (the original texture comes back).</li>
 * </ul>
 *
 * <p>Trust boundary: the server reads the stack from the sender's own armor slot. It changes
 * only a piece that {@link SuitPalette#supports} accepts. {@code PacketRateLimit.EQUIPMENT_CONSOLE}
 * limits the rate. The handler runs on the server thread.
 */
public record SuitPalettePacket(String slot, int primary, int accent, boolean clear) implements CustomPacketPayload {
    public static final Type<SuitPalettePacket> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "suit_palette"));

    public static final StreamCodec<FriendlyByteBuf, SuitPalettePacket> CODEC = StreamCodec.of(
        (buf, p) -> { buf.writeUtf(p.slot, 16); buf.writeInt(p.primary); buf.writeInt(p.accent); buf.writeBoolean(p.clear); },
        buf -> new SuitPalettePacket(buf.readUtf(16), buf.readInt(), buf.readInt(), buf.readBoolean()));

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(SuitPalettePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            if (!com.miophas.singularity_iteration.common.util.PacketRateLimit.allow(
                    player, com.miophas.singularity_iteration.common.util.PacketRateLimit.EQUIPMENT_CONSOLE)) return;
            if (!java.util.Set.of("head", "chest", "legs", "feet").contains(packet.slot)) return;
            ItemStack stack = ArmorFeatureSlots.stackById(player, packet.slot);
            if (!SuitPalette.supports(stack)) return;
            SuitPalette.set(stack, packet.clear ? null : new SuitPalette(packet.primary, packet.accent));
        });
    }
}
