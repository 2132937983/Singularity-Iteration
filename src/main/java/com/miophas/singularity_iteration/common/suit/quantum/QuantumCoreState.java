// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.suit.quantum;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.ints.Int2BooleanOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;

/**
 * Player attachment (0.1.10.0): the quantum core of one player. Saved: the coherence (copied on
 * death). Not saved: the decoherence lock, the running actions and the blink charges; a running
 * action ends when the player leaves (QuantumEvents), so nothing stays changed in the world.
 *
 * <p>The action keys themselves (which unit V and B fire, and the cooldowns by unit key) live in
 * the nano core state, which since 0.1.10.0 holds the keys of both suit classes.
 */
public final class QuantumCoreState {
    /** Coherence 0..maximum. A new player starts full. */
    public float coherence = 100;
    /** Ticks left of the decoherence lock (0 = none) and its length. */
    public int decoherentTicks, decoherentMax;
    /** Game time of the last action (the coherence regains a little later). */
    public long lastAction = -1000;

    // ---------------------------------------------------------------- phase shift
    public int phaseTicks, phaseMax, phaseTier;

    // ---------------------------------------------------------------- quantum blink
    /** Charges ready (-1 = not set yet: full on the first tick with a drive), ticks to the next charge. */
    public int blinkCharges = -1, blinkRecharge;
    /** Ticks of fall protection after a blink. */
    public int blinkSafeTicks;

    // ---------------------------------------------------------------- force field
    public boolean fieldOn;
    public int fieldTicks;
    /** Projectiles held by the field: entity id -> their own no-gravity flag (given back when the field falls). */
    final Int2BooleanOpenHashMap frozen = new Int2BooleanOpenHashMap();

    // ---------------------------------------------------------------- time dilation
    public int dilationTicks, dilationMax;
    /** Creatures that carry the slow modifiers, and the entry speed of each slowed projectile. */
    final IntOpenHashSet slowed = new IntOpenHashSet();
    final Int2DoubleOpenHashMap projectileSpeed = new Int2DoubleOpenHashMap();
    /** Single-player real slow motion runs; the tick rate it replaced. */
    boolean realSlow;
    float savedTickRate = 20;

    // ---------------------------------------------------------------- addon actions
    /** Key of the running sustained addon action ("" = none). */
    public String addonKey = "";

    // ---------------------------------------------------------------- 0.1.10.1 synergies and combos
    /** Static Barrage combo: the velocity of each held projectile when the field caught it. */
    final it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap<net.minecraft.world.phys.Vec3> frozenVelocity = new it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap<>();
    /** Game time of the last blink (Quantum Tunnel combo). */
    public long lastBlink = -1000;
    /** Phase Counter synergy: damage that passed through during the running phase shift. */
    public float phaseAbsorbed;
    /** Resonant Deflection synergy: charge stored from deflected damage. */
    public float resonance;
    /** Afterimage Decoy synergy: the decoy position, the ticks left, and the creatures it draws. */
    public net.minecraft.world.phys.Vec3 decoy;
    public int decoyTicks;
    final IntOpenHashSet decoyed = new IntOpenHashSet();
    /** Fleet Datalink synergy: the creature in the wearer's sights (Wingmen take it first), refreshed every 5 ticks. */
    public int markedTarget = -1;

    // ---------------------------------------------------------------- sync
    float sentCoherence = -1;
    int sentHash, syncTimer;
    boolean sentOffline;

    public static final Codec<QuantumCoreState> CODEC = RecordCodecBuilder.create(i -> i.group(
        Codec.FLOAT.optionalFieldOf("coherence", 100F).forGetter(s -> s.coherence)
    ).apply(i, coherence -> {
        QuantumCoreState s = new QuantumCoreState();
        s.coherence = Math.max(0, Math.min(1000, coherence));
        return s;
    }));

    /** True while an action keeps running (no regeneration then). */
    public boolean sustaining() {
        return fieldOn || !addonKey.isEmpty();
    }

    public boolean phased() {
        return phaseTicks > 0;
    }

    /** Nothing runs and nothing waits: the core may sleep without units. */
    boolean idle() {
        return !fieldOn && phaseTicks <= 0 && dilationTicks <= 0 && decoherentTicks <= 0 && addonKey.isEmpty() && blinkSafeTicks <= 0
            && frozen.isEmpty() && slowed.isEmpty() && projectileSpeed.isEmpty() && !realSlow && decoyTicks <= 0 && decoyed.isEmpty();
    }
}
