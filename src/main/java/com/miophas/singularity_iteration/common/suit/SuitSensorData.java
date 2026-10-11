// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.suit;

import it.unimi.dsi.fastutil.ints.Int2ByteOpenHashMap;
import java.util.ArrayList;
import java.util.List;

/**
 * Client-side store of the suit data that the server sends with {@link SuitPackets}: the threat
 * list, the deflector hits, the chunk grid summary and the maneuver states of other flyers.
 *
 * <p>This class uses no client-only classes. Thus the packet handlers in common code can
 * reference it, and a dedicated server can load it.
 *
 * <p>Threading: the payload handlers write on the client main thread. The HUD and the renderers
 * read on the client main thread. Most methods are {@code synchronized}, so a read from a
 * different thread also gets a consistent copy.
 *
 * <p>Expiry: all times are wall-clock milliseconds ({@link System#currentTimeMillis()}). Each
 * kind of data has a time limit. A read after the limit returns "no data". Thus the HUD stops
 * showing old data when the server stops sending.
 *
 * <p>Memory cleanup: {@code FcsClient} calls {@link #clear()} at client logout.
 */
public final class SuitSensorData {
    private SuitSensorData() {}

    /**
     * Time limit of the threat list, in ms. The server sends a new list every 5 ticks (250 ms).
     * After 1500 ms without a list, the client shows no threats.
     */
    public static final long THREAT_TTL_MS = 1500;
    /** Display time of one deflector hit, in ms. */
    public static final long SHIELD_HIT_MS = 700;

    /** Entity id to threat level (1 or 2) of the last received threat list. */
    private static final Int2ByteOpenHashMap threats = new Int2ByteOpenHashMap();
    /** Wall-clock time (ms) of the last received threat list. */
    private static long threatsAt;
    /** Active deflector hits, oldest first. At most 32 entries. */
    private static final List<Hit> hits = new ArrayList<>();
    /** Last received chunk grid summary, or null. */
    private static volatile SuitPackets.ChunkGrid chunkGrid;
    /** Wall-clock time (ms) of the last received chunk grid summary. */
    private static long chunkGridAt;

    /**
     * A deflector hit as the client stores it.
     *
     * @param entityId network entity id of the wearer
     * @param dx       x of the unit vector from the wearer's centre to the hit source
     * @param dy       y of the unit vector
     * @param dz       z of the unit vector
     * @param strength effect size, 0.25..1
     * @param startMs  wall-clock time (ms) at which the client received the hit
     */
    public record Hit(int entityId, float dx, float dy, float dz, float strength, long startMs) {
        /** Age as a fraction of {@link #SHIELD_HIT_MS}: 0 at the start, 1 or more when the hit expired. */
        public float age(long now) { return (now - startMs) / (float) SHIELD_HIT_MS; }
    }

    /**
     * Replaces the threat list with a new list from {@link SuitPackets.Threats}.
     * The arrays must have the same length.
     */
    public static synchronized void acceptThreats(int[] ids, byte[] levels) {
        threats.clear();
        for (int i = 0; i < ids.length; i++) threats.put(ids[i], levels[i]);
        threatsAt = System.currentTimeMillis();
    }

    /**
     * Threat level of one entity: 0 = no threat, 1 = watches the wearer (yellow),
     * 2 = targets the wearer (red). Returns 0 when the list is older than {@link #THREAT_TTL_MS}.
     */
    public static synchronized int threatLevel(int entityId) {
        if (System.currentTimeMillis() - threatsAt > THREAT_TTL_MS) return 0;
        return threats.getOrDefault(entityId, (byte) 0);
    }

    /** A copy of the threat list. Empty when the list is older than {@link #THREAT_TTL_MS}. */
    public static synchronized Int2ByteOpenHashMap threats() {
        if (System.currentTimeMillis() - threatsAt > THREAT_TTL_MS) return new Int2ByteOpenHashMap();
        return new Int2ByteOpenHashMap(threats);
    }

    /**
     * Adds one deflector hit from {@link SuitPackets.ShieldHit}.
     * Removes the expired hits first. When 32 hits are active, removes the oldest hit.
     * Thus the list never holds more than 32 entries, also when the server sends many hits.
     */
    public static synchronized void acceptShieldHit(SuitPackets.ShieldHit hit) {
        long now = System.currentTimeMillis();
        hits.removeIf(h -> h.age(now) >= 1);
        if (hits.size() >= 32) hits.remove(0);
        hits.add(new Hit(hit.entityId(), hit.dx(), hit.dy(), hit.dz(), hit.strength(), now));
    }

    /** A copy of the hits that did not expire. Removes the expired hits. */
    public static synchronized List<Hit> shieldHits() {
        long now = System.currentTimeMillis();
        hits.removeIf(h -> h.age(now) >= 1);
        return List.copyOf(hits);
    }

    /** Stores the chunk grid summary from {@link SuitPackets.ChunkGrid}. */
    public static void acceptChunkGrid(SuitPackets.ChunkGrid grid) {
        chunkGrid = grid;
        chunkGridAt = System.currentTimeMillis();
    }

    /** Last chunk grid summary, or null when it is older than 3000 ms. */
    public static SuitPackets.ChunkGrid chunkGrid() {
        return System.currentTimeMillis() - chunkGridAt > 3000 ? null : chunkGrid;
    }

    /** Entity id of a different flyer to its last received maneuver state. */
    private static final it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap<SuitPackets.ManeuverState> maneuver = new it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap<>();
    /**
     * Entity id to wall-clock time (ms) of the last received state. Entries stay until
     * {@link #clear()}. Reads ignore entries that are older than 3000 ms.
     */
    private static final it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap maneuverAt = new it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap();

    /** Stores the maneuver state of a different flyer from {@link SuitPackets.ManeuverState}. */
    public static synchronized void acceptManeuver(SuitPackets.ManeuverState state) {
        maneuver.put(state.entityId(), state);
        maneuverAt.put(state.entityId(), System.currentTimeMillis());
    }

    /** Last relayed maneuver state of a different player. Null when unknown or older than 3000 ms. */
    public static synchronized SuitPackets.ManeuverState maneuver(int entityId) {
        long at = maneuverAt.getOrDefault(entityId, 0L);
        return System.currentTimeMillis() - at > 3000 ? null : maneuver.get(entityId);
    }

    /**
     * Removes all stored data. {@code FcsClient} calls this method at client logout, so no data
     * of one server stays for the next server. Entity ids are not valid across connections.
     */
    public static synchronized void clear() {
        maneuver.clear();
        maneuverAt.clear();
        threats.clear();
        hits.clear();
        chunkGrid = null;
    }
}
