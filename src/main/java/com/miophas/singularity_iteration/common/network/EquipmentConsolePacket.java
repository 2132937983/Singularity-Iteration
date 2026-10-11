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
 * Client to server: one edit in the Equipment Console for one item that the player wears or
 * holds.
 *
 * <p>When: the equipment console screen ({@code mio_icif_gui_armor_features}) sends one packet
 * per user action. The user action is a priority click, a mode selection or a slider change.
 *
 * <p>Fields (client values, not trusted):
 * <ul>
 *   <li>{@code slot}: slot id, at most 48 characters. Valid ids are the armor feature slot ids
 *       ({@code head}, {@code chest}, {@code legs}, {@code feet}, {@code back}, and the Curios
 *       accessory ids that start with {@code curio:}), plus {@link #MAINHAND} and
 *       {@link #OFFHAND}.</li>
 *   <li>{@code action}: {@link #SET_PRIORITY}, {@link #SELECT_MODE} or {@link #SET_TUNING}.
 *       The codec sends one unsigned byte, 0..255. The handler ignores other values.</li>
 *   <li>{@code value}: meaning depends on {@code action}.
 *     <ul>
 *       <li>{@code SET_PRIORITY}: {@link ChargePriority} ordinal.</li>
 *       <li>{@code SELECT_MODE}: tool mode index of {@link IToolModeProvider}.</li>
 *       <li>{@code SET_TUNING}: {@code (spec index << 16) | (slider value & 0xFFFF)}.</li>
 *     </ul>
 *   </li>
 * </ul>
 *
 * <p>Trust boundary: the server reads the item from the sender's own slots only. The client
 * cannot name a different player or a block. The handler checks each value before it writes:
 * <ul>
 *   <li>{@code SET_PRIORITY}: the ordinal must be in the range of {@link ChargePriority}.</li>
 *   <li>{@code SELECT_MODE}: {@code IToolModeProvider.selectToolMode} ignores an index out of
 *       range.</li>
 *   <li>{@code SET_TUNING}: the spec index must be below the number of specs. The spec clamps
 *       the slider value.</li>
 * </ul>
 * The handler limits button actions with {@code PacketRateLimit.EQUIPMENT_CONSOLE} (20 in a
 * burst, then 10 per second). Slider changes keep the value checks above but bypass this bucket:
 * the screen sends each mouse movement and has no final-value retry. Dropping a valid change
 * can leave the server at an earlier value. Each accepted edit updates the item component and
 * causes a slot sync to the client.
 *
 * <p>Threading: the handler runs on the server thread (default handler thread). It changes item
 * stacks, so it must run there. {@code enqueueWork} runs the task at once on that thread.
 *
 * <p>Missing channel: the client screen is part of this mod, so the server always has the
 * channel. A fake player never sends this payload.
 */
@SuppressWarnings("null")
public record EquipmentConsolePacket(String slot, int action, int value) implements CustomPacketPayload {
    /** Action ids. Each id fits in one unsigned byte. */
    public static final int SET_PRIORITY = 0, SELECT_MODE = 1, SET_TUNING = 2;
    /** Slot ids for the two hands. All other ids go to {@link ArmorFeatureSlots}. */
    public static final String MAINHAND = "mainhand", OFFHAND = "offhand";

    public static final Type<EquipmentConsolePacket> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "equipment_console"));

    public static final StreamCodec<FriendlyByteBuf, EquipmentConsolePacket> CODEC = StreamCodec.of(
        (buf, p) -> { buf.writeUtf(p.slot, 48); buf.writeByte(p.action); buf.writeVarInt(p.value); },
        buf -> new EquipmentConsolePacket(buf.readUtf(48), buf.readUnsignedByte(), buf.readVarInt()));

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    /**
     * Finds the item in a slot of {@code player}. Returns {@link ItemStack#EMPTY} for an unknown
     * slot id. For the hands and the vanilla armor slots, the result is the stack in the slot.
     * The back slot and the accessory slots are in the Curios container. The handler writes an
     * edited stack back to such a slot, so that Curios stores and syncs the change.
     */
    public static ItemStack resolve(net.minecraft.world.entity.player.Player player, String slot) {
        if (MAINHAND.equals(slot)) return player.getMainHandItem();
        if (OFFHAND.equals(slot)) return player.getOffhandItem();
        return ArmorFeatureSlots.stackById(player, slot);
    }

    /** Server handler. See the class Javadoc for the checks and the thread. */
    public static void handle(EquipmentConsolePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            if (packet.action != SET_TUNING && !com.miophas.singularity_iteration.common.util.PacketRateLimit.allow(
                    player, com.miophas.singularity_iteration.common.util.PacketRateLimit.EQUIPMENT_CONSOLE)) return;
            // Only the sender's own slots. An unknown slot id gives an empty stack.
            ItemStack stack = resolve(player, packet.slot);
            if (stack.isEmpty()) return;
            switch (packet.action) {
                case SET_PRIORITY -> {
                    if (packet.value < 0 || packet.value >= ChargePriority.values().length) return;
                    ChargePriority.set(stack, ChargePriority.values()[packet.value]);
                    // Items in the back slot and the accessory slots are in a different
                    // container. Write the edited stack back to that slot.
                    if (!MAINHAND.equals(packet.slot) && !OFFHAND.equals(packet.slot)) ArmorFeatureSlots.write(player, packet.slot, stack);
                }
                case SELECT_MODE -> {
                    if (stack.getItem() instanceof IToolModeProvider tool) tool.selectToolMode(stack, player, packet.value);
                    if (!MAINHAND.equals(packet.slot) && !OFFHAND.equals(packet.slot)) ArmorFeatureSlots.write(player, packet.slot, stack);
                }
                case SET_TUNING -> {
                    // value = (spec index << 16) | slider value. The unsigned shift gives an index of
                    // 0 or more. The low 16 bits become a signed short. The spec clamps that value.
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
