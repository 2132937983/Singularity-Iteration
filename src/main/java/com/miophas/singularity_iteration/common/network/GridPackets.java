package com.miophas.singularity_iteration.common.network;

import com.miophas.singularity_iteration.common.menu.wiring.EnergyTerminalMenu;
import com.miophas.singularity_iteration.core.platform.neoforge.energy.IndependentSpecialCableBlockEntity;
import com.miophas.singularity_iteration.common.util.PacketRateLimit;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * 0.1.10.22 (G4 / G5 / G7): grid packets.
 * <ul>
 *   <li>C2S {@link ProtectionSettings}: the settings screen of a breaker or fuse cable (rated flow, rated voltage,
 *       delay, automatic reclosing) or its reset. The player must be within 8 blocks of a loaded protective cable.</li>
 *   <li>C2S {@link TerminalAction}: from the monitoring panel of the energy management terminal: the load priority of
 *       a device, or the reset of a breaker. The device must be on the network of the open terminal.</li>
 * </ul>
 * Both go through the {@link PacketRateLimit#GRID} bucket; the server checks every value again.
 */
public final class GridPackets {
    private GridPackets() {}

    public record ProtectionSettings(BlockPos pos, long ratedFlow, long ratedVoltage, int delay, boolean autoReclose, boolean reset)
            implements CustomPacketPayload {
        public static final Type<ProtectionSettings> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("mio_icif", "protection_settings"));
        public static final StreamCodec<FriendlyByteBuf, ProtectionSettings> CODEC = StreamCodec.of((b, p) -> {
            b.writeBlockPos(p.pos); b.writeVarLong(p.ratedFlow); b.writeVarLong(p.ratedVoltage); b.writeVarInt(p.delay);
            b.writeBoolean(p.autoReclose); b.writeBoolean(p.reset);
        }, b -> new ProtectionSettings(b.readBlockPos(), b.readVarLong(), b.readVarLong(), b.readVarInt(), b.readBoolean(), b.readBoolean()));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public static final int SET_PRIORITY = 1, RESET_PROTECTION = 2;

    public record TerminalAction(int containerId, int kind, BlockPos device, int value) implements CustomPacketPayload {
        public static final Type<TerminalAction> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("mio_icif", "energy_terminal_action"));
        public static final StreamCodec<FriendlyByteBuf, TerminalAction> CODEC = StreamCodec.of((b, p) -> {
            b.writeVarInt(p.containerId); b.writeVarInt(p.kind); b.writeBlockPos(p.device); b.writeVarInt(p.value);
        }, b -> new TerminalAction(b.readVarInt(), b.readVarInt(), b.readBlockPos(), b.readVarInt()));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public static void register(PayloadRegistrar registrar) {
        registrar.playToServer(ProtectionSettings.TYPE, ProtectionSettings.CODEC, GridPackets::onSettings);
        registrar.playToServer(TerminalAction.TYPE, TerminalAction.CODEC, GridPackets::onTerminal);
    }

    private static void onSettings(ProtectionSettings p, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player) || !PacketRateLimit.allow(player, PacketRateLimit.GRID)) return;
            var level = player.serverLevel();
            if (!level.isLoaded(p.pos) || player.distanceToSqr(p.pos.getX() + 0.5, p.pos.getY() + 0.5, p.pos.getZ() + 0.5) > 64.0) return;
            if (!(level.getBlockEntity(p.pos) instanceof IndependentSpecialCableBlockEntity cable) || !cable.protective()) return;
            if (p.reset) cable.resetProtection();
            else cable.configure(p.ratedFlow, p.ratedVoltage, p.delay, p.autoReclose);
        });
    }

    private static void onTerminal(TerminalAction p, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player) || !PacketRateLimit.allow(player, PacketRateLimit.GRID)) return;
            if (player.containerMenu.containerId != p.containerId || !(player.containerMenu instanceof EnergyTerminalMenu menu)
                    || !menu.stillValid(player)) return;
            switch (p.kind) {
                case SET_PRIORITY -> menu.setPriority(p.device, p.value);
                case RESET_PROTECTION -> menu.resetProtection(p.device);
                default -> { }
            }
        });
    }
}
