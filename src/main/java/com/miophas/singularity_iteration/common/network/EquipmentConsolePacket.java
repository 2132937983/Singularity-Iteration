package com.miophas.singularity_iteration.common.network;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.common.item.armor.ArmorFeatureSlots;
import com.miophas.singularity_iteration.core.api.item.ChargePriority;
import com.miophas.singularity_iteration.core.api.tool.IToolModeProvider;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client -> server: equipment console edits for one equipped item.
 * Slot ids are the armor feature slot ids ({@code head/chest/legs/feet/back}) plus
 * {@code mainhand} and {@code offhand}.
 */
@SuppressWarnings("null")
public record EquipmentConsolePacket(String slot, int action, int value) implements CustomPacketPayload {
    public static final int SET_PRIORITY = 0, SELECT_MODE = 1, SET_TUNING = 2;
    public static final String MAINHAND = "mainhand", OFFHAND = "offhand";

    public static final Type<EquipmentConsolePacket> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "equipment_console"));

    public static final StreamCodec<FriendlyByteBuf, EquipmentConsolePacket> CODEC = StreamCodec.of(
        (buf, p) -> { buf.writeUtf(p.slot, 48); buf.writeByte(p.action); buf.writeVarInt(p.value); },
        buf -> new EquipmentConsolePacket(buf.readUtf(48), buf.readUnsignedByte(), buf.readVarInt()));

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static ItemStack resolve(net.minecraft.world.entity.player.Player player, String slot) {
        if (MAINHAND.equals(slot)) return player.getMainHandItem();
        if (OFFHAND.equals(slot)) return player.getOffhandItem();
        return ArmorFeatureSlots.stackById(player, slot);
    }

    public static void handle(EquipmentConsolePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            ItemStack stack = resolve(player, packet.slot);
            if (stack.isEmpty()) return;
            switch (packet.action) {
                case SET_PRIORITY -> {
                    if (packet.value < 0 || packet.value >= ChargePriority.values().length) return;
                    ChargePriority.set(stack, ChargePriority.values()[packet.value]);
                    // back-slot items live in another container: write the edited copy back
                    if (!MAINHAND.equals(packet.slot) && !OFFHAND.equals(packet.slot)) ArmorFeatureSlots.write(player, packet.slot, stack);
                }
                case SELECT_MODE -> {
                    if (stack.getItem() instanceof IToolModeProvider tool) tool.selectToolMode(stack, player, packet.value);
                    if (!MAINHAND.equals(packet.slot) && !OFFHAND.equals(packet.slot)) ArmorFeatureSlots.write(player, packet.slot, stack);
                }
                case SET_TUNING -> {
                    // value = spec index << 16 | slider value; the spec clamps the value
                    if (!(stack.getItem() instanceof com.miophas.singularity_iteration.common.item.tuning.ITunableItem tunable)) return;
                    var specs = tunable.tuningSpecs(stack);
                    int index = packet.value >>> 16;
                    if (index >= specs.size()) return;
                    com.miophas.singularity_iteration.common.item.tuning.ITunableItem.set(stack, specs.get(index), (short) (packet.value & 0xFFFF));
                    if (!MAINHAND.equals(packet.slot) && !OFFHAND.equals(packet.slot)) ArmorFeatureSlots.write(player, packet.slot, stack);
                }
                default -> { }
            }
        });
    }
}
