// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.suit.nano;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.HashMap;
import java.util.Map;

/**
 * Player attachment (0.1.8.5): the nano core of one player. Saved: reserve, heat and the two burst
 * assignments (copied on death). Not saved: cooldowns, the running camouflage and the dash.
 *
 * <p>Reserve 0..100 feeds the bursts and refills from EU. Heat rises with each burst; at the cap
 * the suit overheats and the bursts lock until the heat falls below the resume level.
 */
public final class NanoCoreState {
    /** Burst reserve, 0..100. A new player starts full. */
    public float reserve = 100;
    /** Heat, 0..heat cap. */
    public float heat;
    public boolean overheated;
    /** Burst keys: unit key on the primary (V) and the secondary (B) key; "" = automatic. */
    public String primary = "", secondary = "";

    /** Remaining cooldown ticks by unit key. */
    public final Map<String, Integer> cooldown = new HashMap<>();
    /** Ticks the optical camouflage has run; 0 = off. */
    public int camoTicks;
    /** Game time until which guidance locks on this player are broken (own EMP). */
    public long lockJamUntil;
    /** Game time until which this player's own guidance is scrambled (another player's EMP). */
    public long guidanceJamUntil;
    /** Ticks left of a Kinetic Dash (no fall damage while it runs). */
    public int dashTicks;
    /** HF sync level last tick (0 = none). */
    int hfLevel;
    boolean modifiersOn;

    // ---------------------------------------------------------------- 0.1.8.6
    /** Ticks the kinetic armor mesh still runs; its length when it started. */
    public int meshTicks, meshMax;
    /** Ticks the recon glow still runs (HUD); its length. */
    public int reconTicks, reconMax;
    /** Game time of the last damage taken (self-repair waits for 5 s of quiet). */
    public long lastHit;
    /** Game time of the last landing from a fall of 2 blocks or more (shock gel rebound window). */
    public long landedAt = -100;
    /** Kinetic generator: horizontal distance not yet paid out, last position. */
    double walked;
    double lastX = Double.NaN, lastZ;
    /** EU the generators made in the last second (HUD), and the running sum of this second. */
    public int generatedLastSecond;
    int generatedThisSecond;
    /** Last values sent to the owner (sync only on change). */
    float sentReserve = -1, sentHeat = -1;
    int sentHash;
    int syncTimer;
    /** The last sync told the client that the core is offline (no nano piece worn). */
    boolean sentOffline;

    public static final Codec<NanoCoreState> CODEC = RecordCodecBuilder.create(i -> i.group(
        Codec.FLOAT.optionalFieldOf("reserve", 100F).forGetter(s -> s.reserve),
        Codec.FLOAT.optionalFieldOf("heat", 0F).forGetter(s -> s.heat),
        Codec.BOOL.optionalFieldOf("overheated", false).forGetter(s -> s.overheated),
        Codec.STRING.optionalFieldOf("primary", "").forGetter(s -> s.primary),
        Codec.STRING.optionalFieldOf("secondary", "").forGetter(s -> s.secondary)
    ).apply(i, (reserve, heat, overheated, primary, secondary) -> {
        NanoCoreState s = new NanoCoreState();
        s.reserve = Math.max(0, Math.min(100, reserve));
        s.heat = Math.max(0, heat);
        s.overheated = overheated;
        s.primary = primary;
        s.secondary = secondary;
        return s;
    }));

    /** 0.1.10.0: the next sync goes out at once (the quantum actions change the keys from another package). */
    public void dirty() {
        sentHash = 0;
    }

    public int cooldown(String key) {
        Integer v = cooldown.get(key);
        return v == null ? 0 : v;
    }

    public boolean camoActive() {
        return camoTicks > 0;
    }
}
