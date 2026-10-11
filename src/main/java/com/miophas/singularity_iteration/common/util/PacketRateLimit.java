// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.util;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Token-bucket rate limit for client-to-server payloads (0.1.7.36).
 *
 * <p>Why: before 0.1.7.36, {@code ManeuverInput}, {@code ArmoryConsolePacket} and
 * {@code EquipmentConsolePacket} had no limit. A modified client could send thousands per
 * second. Each {@code ManeuverInput} is relayed to every player within 160 blocks, so one client
 * could flood many others.
 *
 * <p>Contract: each (player, bucket) pair holds at most {@code burst} tokens. One token comes
 * back every {@code refillTicks} server ticks. {@link #allow} takes one token, or returns false
 * when none is left. The normal client stays far below each limit, so it never sees a drop.
 *
 * <p>Invariants: the state is touched only on the server thread (payload handlers run there).
 * A player's entries are removed at logout, so the map size is bounded by the online players
 * times the bucket count.
 */
@EventBusSubscriber(modid = Singularity_Iteration.MOD_ID)
public final class PacketRateLimit {
    private PacketRateLimit() {}

    /** A named limit: {@code burst} tokens, one token back per {@code refillTicks}. */
    public record Bucket(String name, int burst, int refillTicks) {}

    /** Throttle and stick: the client sends at most once per tick on change. */
    public static final Bucket MANEUVER_INPUT = new Bucket("maneuver_input", 10, 1);
    /** Armory console: a refresh per 20 ticks plus button clicks. */
    public static final Bucket ARMORY_CONSOLE = new Bucket("armory_console", 10, 4);
    /** Equipment console buttons. Slider updates bypass this bucket so the last value is not lost. */
    public static final Bucket EQUIPMENT_CONSOLE = new Bucket("equipment_console", 20, 2);

    private static final class State {
        double tokens;
        long lastTick;
    }

    private static final Map<UUID, Map<String, State>> STATES = new HashMap<>();

    /** Takes one token of {@code bucket} for {@code player}. Returns false when the bucket is empty. */
    public static boolean allow(ServerPlayer player, Bucket bucket) {
        long now = player.server == null ? player.tickCount : player.server.getTickCount();
        State s = STATES.computeIfAbsent(player.getUUID(), k -> new HashMap<>(4))
            .computeIfAbsent(bucket.name(), k -> {
                State n = new State();
                n.tokens = bucket.burst();
                n.lastTick = now;
                return n;
            });
        long elapsed = Math.max(0, now - s.lastTick);
        s.lastTick = now;
        s.tokens = Math.min(bucket.burst(), s.tokens + elapsed / (double) Math.max(1, bucket.refillTicks()));
        if (s.tokens < 1) return false;
        s.tokens -= 1;
        return true;
    }

    @SubscribeEvent
    static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        STATES.remove(event.getEntity().getUUID());
    }
}
