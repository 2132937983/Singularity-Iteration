// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.suit;

import it.unimi.dsi.fastutil.ints.Int2ByteOpenHashMap;
import java.util.ArrayList;
import java.util.List;

/**
 * Client-side store of what the server told the suit (threat list, deflector hits, chunk grid).
 * Plain data: no client classes, so the packet handlers can reference it from common code.
 */
public final class SuitSensorData {
    private SuitSensorData() {}

    /** Threat entries older than this are stale (server sends every 5 ticks). */
    public static final long THREAT_TTL_MS = 1500;
    public static final long SHIELD_HIT_MS = 700;

    private static final Int2ByteOpenHashMap threats = new Int2ByteOpenHashMap();
    private static long threatsAt;
    private static final List<Hit> hits = new ArrayList<>();
    private static volatile SuitPackets.ChunkGrid chunkGrid;
    private static long chunkGridAt;

    /** A deflector hit: unit direction from the wearer's centre, strength 0..1, start time. */
    public record Hit(int entityId, float dx, float dy, float dz, float strength, long startMs) {
        public float age(long now) { return (now - startMs) / (float) SHIELD_HIT_MS; }
    }

    public static synchronized void acceptThreats(int[] ids, byte[] levels) {
        threats.clear();
        for (int i = 0; i < ids.length; i++) threats.put(ids[i], levels[i]);
        threatsAt = System.currentTimeMillis();
    }

    /** 0 none, 1 watching (yellow), 2 targeting (red). */
    public static synchronized int threatLevel(int entityId) {
        if (System.currentTimeMillis() - threatsAt > THREAT_TTL_MS) return 0;
        return threats.getOrDefault(entityId, (byte) 0);
    }

    public static synchronized Int2ByteOpenHashMap threats() {
        if (System.currentTimeMillis() - threatsAt > THREAT_TTL_MS) return new Int2ByteOpenHashMap();
        return new Int2ByteOpenHashMap(threats);
    }

    public static synchronized void acceptShieldHit(SuitPackets.ShieldHit hit) {
        long now = System.currentTimeMillis();
        hits.removeIf(h -> h.age(now) >= 1);
        if (hits.size() >= 32) hits.remove(0);
        hits.add(new Hit(hit.entityId(), hit.dx(), hit.dy(), hit.dz(), hit.strength(), now));
    }

    public static synchronized List<Hit> shieldHits() {
        long now = System.currentTimeMillis();
        hits.removeIf(h -> h.age(now) >= 1);
        return List.copyOf(hits);
    }

    public static void acceptChunkGrid(SuitPackets.ChunkGrid grid) {
        chunkGrid = grid;
        chunkGridAt = System.currentTimeMillis();
    }

    /** Last chunk summary, or null when older than 3 s. */
    public static SuitPackets.ChunkGrid chunkGrid() {
        return System.currentTimeMillis() - chunkGridAt > 3000 ? null : chunkGrid;
    }

    public static synchronized void clear() {
        threats.clear();
        hits.clear();
        chunkGrid = null;
    }
}
