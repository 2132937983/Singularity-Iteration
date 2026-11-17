// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.armory;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

/**
 * 0.1.11.60 (gaze and Iron Man phase P8, decisions A / A): remote piloting. The owner flies one Wingman directly, seeing
 * through its eyes, while the body stays where it is.
 *
 * <ul>
 *   <li>Start and stop: the pilot key (default U: the selected Wingman, else the nearest) or the command wheel; it needs
 *       the command link (the entanglement relay). The client sends {@link Input} every tick: the movement keys, sprint, the
 *       look, attack (held) and use (pressed).</li>
 *   <li>The piloted Wingman flies along the look (forward and back), sideways, up (jump) and down (sneak), faster with
 *       sprint; attack strikes, fires or cuts with its weapon (a SlashBlade blade chains as a player's would); use is a
 *       thruster boost along the look.</li>
 *   <li>The body: the first hurt ends the flight (the view snaps back). While piloting, the owner's other Wingmen escort
 *       the body (their modes come back afterwards).</li>
 *   <li>The link ends when the Wingman is more than {@link #RANGE} blocks from the body or in another dimension, when it
 *       stops being active (home, swap, rescue, Clean Slate), when the command link fails, or on logout.</li>
 * </ul>
 */
@EventBusSubscriber(modid = Singularity_Iteration.MOD_ID)
public final class WingmanPilot {
    private WingmanPilot() {}

    /** Farthest a piloted Wingman may be from the body, blocks. */
    public static final double RANGE = 128;

    /** Piloted Wingman entity id by owner. */
    private static final Map<UUID, Integer> PILOTS = new HashMap<>();
    /** Modes of the other Wingmen while they escort the body: Wingman UUID -> {mode ordinal, individual}. */
    private static final Map<UUID, int[]> SAVED = new HashMap<>();

    /** Client -> server: the pilot's controls of one tick. */
    public record Input(float forward, float strafe, float vertical, boolean sprint, float yaw, float pitch, boolean attack, boolean use)
        implements CustomPacketPayload {
        public static final Type<Input> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "wingman_pilot_input"));
        public static final StreamCodec<FriendlyByteBuf, Input> CODEC = StreamCodec.of((b, i) -> {
            b.writeFloat(i.forward); b.writeFloat(i.strafe); b.writeFloat(i.vertical); b.writeBoolean(i.sprint);
            b.writeFloat(i.yaw); b.writeFloat(i.pitch); b.writeBoolean(i.attack); b.writeBoolean(i.use);
        }, b -> new Input(b.readFloat(), b.readFloat(), b.readFloat(), b.readBoolean(), b.readFloat(), b.readFloat(), b.readBoolean(), b.readBoolean()));

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

        /** The same controls with the movement clamped to -1..1 and the angles made finite (untrusted input). */
        public Input clean() {
            return new Input(clamp(forward), clamp(strafe), clamp(vertical), sprint, finite(yaw), Math.max(-90, Math.min(90, finite(pitch))), attack, use);
        }

        private static float clamp(float v) { return Float.isFinite(v) ? Math.max(-1, Math.min(1, v)) : 0; }

        private static float finite(float v) { return Float.isFinite(v) ? v : 0; }
    }

    /** Server -> client: the Wingman now piloted (entity id), or -1. */
    public record State(int entityId) implements CustomPacketPayload {
        public static final Type<State> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "wingman_pilot_state"));
        public static final StreamCodec<FriendlyByteBuf, State> CODEC = StreamCodec.of((b, s) -> b.writeVarInt(s.entityId), b -> new State(b.readVarInt()));

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** Client hook (set by the client setup). */
    public static volatile java.util.function.Consumer<State> HOOK;

    /** The Wingman {@code player} pilots, or null. */
    @Nullable
    public static WingmanEntity piloted(ServerPlayer player) {
        Integer id = PILOTS.get(player.getUUID());
        if (id == null) return null;
        Entity e = player.serverLevel().getEntity(id);
        return e instanceof WingmanEntity w ? w : null;
    }

    /** Starts piloting {@code w}; null when it started, else the reason (a message). */
    @Nullable
    public static Component start(ServerPlayer player, WingmanEntity w) {
        if (PILOTS.containsKey(player.getUUID())) stop(player, null);
        if (w.state() != WingmanEntity.ACTIVE || w.swapPhase() != WingmanEntity.SWAP_NONE || w.rescuing() || w.cleaningSlate())
            return Component.translatable("message.mio_icif.wingman.pilot.busy");
        if (w.level() != player.level() || w.distanceTo(player) > RANGE) return Component.translatable("message.mio_icif.wingman.pilot.range", (int) RANGE);
        PILOTS.put(player.getUUID(), w.getId());
        w.setPilot(player.getUUID());
        for (WingmanEntity other : WingmanService.of(player)) {
            if (other == w || other.state() != WingmanEntity.ACTIVE) continue;
            SAVED.put(other.getUUID(), new int[]{other.mode().ordinal(), other.individual() ? 1 : 0});
            other.setMode(WingmanMode.ESCORT, true);   // guard the body
        }
        send(player, w.getId());
        return null;
    }

    /** Ends the flight of {@code player} (with a message, or none). */
    public static void stop(ServerPlayer player, @Nullable Component why) {
        Integer id = PILOTS.remove(player.getUUID());
        if (id == null) return;
        if (player.serverLevel().getEntity(id) instanceof WingmanEntity w) w.setPilot(null);
        for (WingmanEntity other : WingmanService.of(player)) {
            int[] saved = SAVED.remove(other.getUUID());
            if (saved != null && other.state() == WingmanEntity.ACTIVE) other.setMode(WingmanMode.byId(saved[0]), saved[1] != 0);
        }
        send(player, -1);
        if (why != null) player.displayClientMessage(why, true);
    }

    private static void send(ServerPlayer player, int id) {
        if (player.connection != null && player.connection.hasChannel(State.TYPE)) PacketDistributor.sendToPlayer(player, new State(id));
    }

    /** The controls of one tick from the pilot's client. */
    public static void input(ServerPlayer player, Input input) {
        WingmanEntity w = piloted(player);
        if (w != null) w.pilotInput(input.clean());
    }

    /** Ends the flight when the link breaks; null when it holds. */
    @Nullable
    static Component broken(ServerPlayer player, @Nullable WingmanEntity w) {
        if (w == null || w.isRemoved() || w.state() != WingmanEntity.ACTIVE || w.swapPhase() != WingmanEntity.SWAP_NONE || w.rescuing()
            || w.cleaningSlate()) return Component.translatable("message.mio_icif.wingman.pilot.lost");
        if (!player.isAlive() || w.level() != player.level() || w.distanceTo(player) > RANGE)
            return Component.translatable("message.mio_icif.wingman.pilot.range", (int) RANGE);
        if (!WingmanCommands.commander(player)) return Component.translatable("message.mio_icif.command.no_relay");
        return null;
    }

    @SubscribeEvent
    static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || !PILOTS.containsKey(p.getUUID())) return;
        Component why = broken(p, piloted(p));
        if (why != null) stop(p, why);
    }

    /** The body is hurt: back into it at once. */
    @SubscribeEvent
    static void onBodyHurt(LivingDamageEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || !PILOTS.containsKey(p.getUUID())) return;
        WingmanEntity w = piloted(p);
        stop(p, Component.translatable("message.mio_icif.wingman.pilot.hurt"));
        if (w != null) WingmanCallouts.say(w, WingmanCallouts.Kind.OWNER_HURT);
    }

    @SubscribeEvent
    static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) stop(p, null);
    }

    /** True while {@code player} pilots a Wingman (server). */
    public static boolean piloting(ServerPlayer player) { return PILOTS.containsKey(player.getUUID()); }
}
