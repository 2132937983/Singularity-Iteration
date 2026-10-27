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
    /** Smart Trajectory Guidance locks (0.1.8.0): the client sends on change, at most 5 per second. */
    public static final Bucket SMART_GUIDANCE = new Bucket("smart_guidance", 10, 4);
    /** Tactical structure scan (0.1.8.0): a scan reads up to 289 chunks, so at most one per 2 s. */
    public static final Bucket TACTICAL_SCAN = new Bucket("tactical_scan", 2, 40);
    /** Nano burst keys (0.1.8.5): key presses, about 5 per second. */
    public static final Bucket NANO_BURST = new Bucket("nano_burst", 6, 4);
    /** Nano burst key assignment from the panel (0.1.8.5). */
    public static final Bucket NANO_ASSIGN = new Bucket("nano_assign", 10, 4);
    /** Exchange panel (0.1.10.21): a view refresh per second plus clicks; actions also cost machine energy. */
    public static final Bucket EXCHANGE = new Bucket("exchange", 12, 4);
    /**
     * 0.1.10.29: the view refreshes of the exchange panel and the Trade Terminal (one a second plus one per page, market or box
     * change). Before they took tokens of {@link #EXCHANGE}, so quick page changes could leave a click without a token.
     */
    public static final Bucket EXCHANGE_VIEW = new Bucket("exchange_view", 10, 4);
    /** 0.1.10.22: protection settings and terminal actions (priority, reset). */
    public static final Bucket GRID = new Bucket("grid", 10, 4);

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
