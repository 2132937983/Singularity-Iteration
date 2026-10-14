// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.suit.quantum;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Common config section {@code QuantumSuit} (0.1.10.0): a switch per quantum action, PvP switches
 * for the actions that act on other players (all on by default), the coherence numbers, the action
 * numbers, and the single-player real slow motion of the time dilation (off by default). Every
 * getter returns the default before the config is loaded.
 */
public final class QuantumConfig {
    private QuantumConfig() {}

    private static ModConfigSpec spec;
    /** Action switches by action id (blink, phase_shift, force_field, time_dilation, volley) and the capacitor. */
    private static final Map<String, ModConfigSpec.BooleanValue> ENABLED = new LinkedHashMap<>();
    public static final String[] SWITCHES = {"blink", "phase_shift", "force_field", "time_dilation", "volley", "coherence_capacitor"};

    private static ModConfigSpec.BooleanValue phaseShockVsPlayers, fieldVsPlayers, dilationVsPlayers, volleyVsPlayers, realSlowMotion;
    private static ModConfigSpec.DoubleValue coherenceRegen, phaseShockDamage, dilationSlow, dilationProjectile, volleyDamage;
    private static ModConfigSpec.IntValue regenDelay, euPerCoherence, decoherenceTicks, capacitorBonus;
    private static ModConfigSpec.IntValue blinkRange, blinkRangeMk2, blinkCost, blinkCooldown;
    private static ModConfigSpec.IntValue phaseTicks, phaseTicksMk2, phaseCost, phaseCooldown, phaseShockRadius;
    private static ModConfigSpec.IntValue fieldRadius, fieldStartCost, fieldPerSecond, fieldCooldown;
    private static ModConfigSpec.IntValue dilationRadius, dilationTicks, dilationCost, dilationCooldown, realSlowTickRate;
    private static ModConfigSpec.IntValue volleyCost, volleyCooldown, volleyRange;
    // 0.1.10.1
    private static ModConfigSpec.BooleanValue resonanceVsPlayers;
    private static ModConfigSpec.DoubleValue resonanceShare, frozenVolleyDamage;
    private static ModConfigSpec.IntValue resonanceThreshold, resonanceRadius, decoyTicks, counterMax, tunnelWindow;
    // 0.1.10.2
    private static ModConfigSpec.IntValue recallCost, recallCooldown, holdRadius, suppressRadius, focusRange;
    private static ModConfigSpec.BooleanValue unloadedRecall;

    /** Defines the section entries (called once while the common spec is built). */
    public static void define(ModConfigSpec.Builder b) {
        b.comment("On/off switch of each quantum action and of the coherence capacitor. An action that is off does nothing (the unit stays installed;",
                "a Mk.I deflector and behaviour predictor keep their passive effect).").translation("mio_icif.configuration.quantum.section.actions").push("actions");
        for (String id : SWITCHES) ENABLED.put(id, b.translation("mio_icif.configuration.quantum.action." + id).define(id, true));
        b.pop();

        // 0.1.10.1 entanglement synergies
        b.comment("On/off switch of each entanglement synergy (0.1.10.1). A synergy that is off does nothing; the link stays stored.")
            .translation("mio_icif.configuration.quantum.section.synergies").push("synergies");
        for (QuantumSynergy s : QuantumSynergy.VALUES) {
            ENABLED.put("synergy." + s.id, b.translation("mio_icif.configuration.quantum.synergy." + s.id).define(s.id, true));
        }
        b.pop();

        b.comment("Effects of quantum actions on other players (PvP). All on by default; the server PvP setting applies too.")
            .translation("mio_icif.configuration.quantum.section.pvp").push("pvp");
        phaseShockVsPlayers = b.comment("The shock wave at the end of a Mk.II phase shift hits and pushes other players.")
            .translation("mio_icif.configuration.quantum.pvp.phase").define("phaseShockHitsPlayers", true);
        fieldVsPlayers = b.comment("The force field pushes other players out and stops the projectiles they fired.")
            .translation("mio_icif.configuration.quantum.pvp.field").define("fieldAffectsPlayers", true);
        dilationVsPlayers = b.comment("The time dilation field slows other players and the projectiles they fired.")
            .translation("mio_icif.configuration.quantum.pvp.dilation").define("dilationSlowsPlayers", true);
        volleyVsPlayers = b.comment("An entangled volley may target other players (locked or aimed at).")
            .translation("mio_icif.configuration.quantum.pvp.volley").define("volleyHitsPlayers", true);
        resonanceVsPlayers = b.comment("0.1.10.1: the burst of the Resonant Deflection synergy and the shock wave of the Phase Counter synergy hit other players.")
            .translation("mio_icif.configuration.quantum.pvp.synergy").define("synergyBurstsHitPlayers", true);
        b.pop();

        b.comment("Quantum coherence: the resource of the quantum actions.").translation("mio_icif.configuration.quantum.section.coherence").push("coherence");
        coherenceRegen = b.comment("Coherence regained per second (coherence capacitor +25 %).")
            .translation("mio_icif.configuration.quantum.regen").defineInRange("coherenceRegenPerSecond", 8.0, 0.0, 100.0);
        regenDelay = b.comment("Ticks after the last action before the coherence regains.")
            .translation("mio_icif.configuration.quantum.regen_delay").defineInRange("regenDelayTicks", 30, 0, 400);
        euPerCoherence = b.comment("EU paid for one coherence point (from the suit energy pool).")
            .translation("mio_icif.configuration.quantum.eu_per_point").defineInRange("euPerCoherencePoint", 2500, 0, 10_000_000);
        decoherenceTicks = b.comment("Lock after the coherence hits 0, in ticks (no actions, the HUD glitches, the sensors refresh at half rate).")
            .translation("mio_icif.configuration.quantum.decoherence").defineInRange("decoherenceTicks", 80, 0, 1200);
        capacitorBonus = b.comment("Coherence maximum that one coherence capacitor adds.")
            .translation("mio_icif.configuration.quantum.capacitor").defineInRange("capacitorBonus", 50, 0, 200);
        b.pop();

        b.comment("Action numbers: coherence costs, cooldowns in ticks, ranges in blocks.").translation("mio_icif.configuration.quantum.section.tuning").push("tuning");
        blinkRange = b.comment("Quantum blink: longest jump in blocks.")
            .translation("mio_icif.configuration.quantum.blink_range").defineInRange("blinkRange", 12, 2, 48);
        blinkRangeMk2 = b.comment("Quantum blink of a Mk.II phase jump drive (two charges).")
            .translation("mio_icif.configuration.quantum.blink_range_mk2").defineInRange("blinkRangeMk2", 20, 2, 64);
        blinkCost = b.comment("Quantum blink: coherence per jump.").translation("mio_icif.configuration.quantum.blink_cost").defineInRange("blinkCost", 25, 0, 100);
        blinkCooldown = b.comment("Quantum blink: ticks per charge.").translation("mio_icif.configuration.quantum.blink_cooldown").defineInRange("blinkCooldownTicks", 50, 0, 2400);
        phaseTicks = b.comment("Phase shift length in ticks.").translation("mio_icif.configuration.quantum.phase_ticks").defineInRange("phaseShiftTicks", 20, 2, 200);
        phaseTicksMk2 = b.comment("Phase shift length of a Mk.II phase modulator.")
            .translation("mio_icif.configuration.quantum.phase_ticks_mk2").defineInRange("phaseShiftTicksMk2", 30, 2, 200);
        phaseCost = b.comment("Phase shift: coherence per use.").translation("mio_icif.configuration.quantum.phase_cost").defineInRange("phaseShiftCost", 35, 0, 100);
        phaseCooldown = b.comment("Phase shift: cooldown ticks.").translation("mio_icif.configuration.quantum.phase_cooldown").defineInRange("phaseShiftCooldownTicks", 160, 0, 2400);
        phaseShockRadius = b.comment("Mk.II phase shift: radius of the shock wave when it ends.")
            .translation("mio_icif.configuration.quantum.phase_shock_radius").defineInRange("phaseShockRadius", 4, 1, 16);
        phaseShockDamage = b.comment("Mk.II phase shift: damage of the shock wave.")
            .translation("mio_icif.configuration.quantum.phase_shock_damage").defineInRange("phaseShockDamage", 4.0, 0.0, 100.0);
        fieldRadius = b.comment("Force field radius.").translation("mio_icif.configuration.quantum.field_radius").defineInRange("forceFieldRadius", 4, 2, 12);
        fieldStartCost = b.comment("Force field: coherence to raise it.").translation("mio_icif.configuration.quantum.field_cost").defineInRange("forceFieldStartCost", 15, 0, 100);
        fieldPerSecond = b.comment("Force field: coherence per second while it stands.")
            .translation("mio_icif.configuration.quantum.field_drain").defineInRange("forceFieldPerSecond", 10, 0, 100);
        fieldCooldown = b.comment("Force field: cooldown ticks after it falls.").translation("mio_icif.configuration.quantum.field_cooldown").defineInRange("forceFieldCooldownTicks", 120, 0, 2400);
        dilationRadius = b.comment("Time dilation field radius.").translation("mio_icif.configuration.quantum.dilation_radius").defineInRange("timeDilationRadius", 10, 3, 32);
        dilationTicks = b.comment("Time dilation length in ticks.").translation("mio_icif.configuration.quantum.dilation_ticks").defineInRange("timeDilationTicks", 80, 10, 600);
        dilationCost = b.comment("Time dilation: coherence per use.").translation("mio_icif.configuration.quantum.dilation_cost").defineInRange("timeDilationCost", 60, 0, 100);
        dilationCooldown = b.comment("Time dilation: cooldown ticks.").translation("mio_icif.configuration.quantum.dilation_cooldown").defineInRange("timeDilationCooldownTicks", 500, 0, 6000);
        dilationSlow = b.comment("Movement slow of creatures in the field (0.7 = -70 %; bosses get half).")
            .translation("mio_icif.configuration.quantum.dilation_slow").defineInRange("timeDilationSlow", 0.7, 0.0, 0.95);
        dilationProjectile = b.comment("Speed factor of hostile projectiles in the field (0.25 = a quarter).")
            .translation("mio_icif.configuration.quantum.dilation_projectile").defineInRange("timeDilationProjectileFactor", 0.25, 0.05, 1.0);
        volleyCost = b.comment("Entangled volley: coherence per order.").translation("mio_icif.configuration.quantum.volley_cost").defineInRange("volleyCost", 20, 0, 100);
        volleyCooldown = b.comment("Entangled volley: cooldown ticks.").translation("mio_icif.configuration.quantum.volley_cooldown").defineInRange("volleyCooldownTicks", 200, 0, 2400);
        volleyDamage = b.comment("Damage of the bolts of one Wingman.")
            .translation("mio_icif.configuration.quantum.volley_damage").defineInRange("volleyDamagePerWingman", 8.0, 0.0, 200.0);
        volleyRange = b.comment("Wingmen within this distance of the target join the volley; targets within this distance of you can be aimed at.")
            .translation("mio_icif.configuration.quantum.volley_range").defineInRange("volleyRange", 128, 16, 512);
        b.pop();

        b.comment("Synergy and combo numbers (0.1.10.1).").translation("mio_icif.configuration.quantum.section.synergy_tuning").push("synergyTuning");
        resonanceShare = b.comment("Resonant Deflection: share of the deflected damage that charges the resonance.")
            .translation("mio_icif.configuration.quantum.resonance_share").defineInRange("resonanceShare", 0.25, 0.0, 1.0);
        resonanceThreshold = b.comment("Resonant Deflection: stored charge that releases the burst.")
            .translation("mio_icif.configuration.quantum.resonance_threshold").defineInRange("resonanceThreshold", 8, 1, 100);
        resonanceRadius = b.comment("Resonant Deflection: burst radius.")
            .translation("mio_icif.configuration.quantum.resonance_radius").defineInRange("resonanceRadius", 4, 1, 16);
        decoyTicks = b.comment("Afterimage Decoy: ticks the decoy draws the hunters.")
            .translation("mio_icif.configuration.quantum.decoy_ticks").defineInRange("decoyTicks", 40, 5, 400);
        counterMax = b.comment("Phase Counter: highest shock-wave damage from the stored hits.")
            .translation("mio_icif.configuration.quantum.counter_max").defineInRange("phaseCounterMaxDamage", 20, 0, 200);
        tunnelWindow = b.comment("Quantum Tunnel combo: ticks after a blink in which a phase shift costs half.")
            .translation("mio_icif.configuration.quantum.tunnel_window").defineInRange("quantumTunnelWindowTicks", 20, 1, 200);
        frozenVolleyDamage = b.comment("Frozen Volley combo: damage factor of a volley during your time dilation.")
            .translation("mio_icif.configuration.quantum.frozen_volley").defineInRange("frozenVolleyDamageFactor", 1.5, 1.0, 5.0);
        b.pop();

        b.comment("Quantum command (0.1.10.2): Wingman orders from the command wheel and the holo table. The orders work at any",
                "distance in the owner's dimension; a Wingman in an unloaded chunk cannot move, only the quantum recall reaches it.")
            .translation("mio_icif.configuration.quantum.section.command").push("command");
        recallCost = b.comment("Quantum recall: coherence per use (every Wingman returns to its Armory at once).")
            .translation("mio_icif.configuration.quantum.recall_cost").defineInRange("quantumRecallCost", 30, 0, 100);
        recallCooldown = b.comment("Quantum recall: cooldown ticks.")
            .translation("mio_icif.configuration.quantum.recall_cooldown").defineInRange("quantumRecallCooldownTicks", 600, 0, 12000);
        unloadedRecall = b.comment("Quantum recall also reaches Wingmen in unloaded chunks and other dimensions (their chunk loads for a moment).")
            .translation("mio_icif.configuration.quantum.recall_unloaded").define("recallUnloadedWingmen", true);
        holdRadius = b.comment("Move / hold orders: radius around the point in which the Wingmen engage.")
            .translation("mio_icif.configuration.quantum.hold_radius").defineInRange("holdRadius", 10, 3, 32);
        suppressRadius = b.comment("Area suppression: radius of the area the Wingmen patrol and clear.")
            .translation("mio_icif.configuration.quantum.suppress_radius").defineInRange("suppressRadius", 14, 4, 48);
        focusRange = b.comment("Focus fire: the Wingmen follow the target up to this distance, then the order ends.")
            .translation("mio_icif.configuration.quantum.focus_range").defineInRange("focusRange", 128, 16, 512);
        b.pop();

        b.comment("Single-player real slow motion: while your time dilation runs, the whole game runs at this tick rate",
                "(you included). Only in single player with nobody else online. Off by default.")
            .translation("mio_icif.configuration.quantum.section.slowmo").push("slowMotion");
        realSlowMotion = b.comment("Turn the real slow motion on.").translation("mio_icif.configuration.quantum.slowmo").define("realSlowMotion", false);
        realSlowTickRate = b.comment("Tick rate during the real slow motion (20 = normal speed).")
            .translation("mio_icif.configuration.quantum.slowmo_rate").defineInRange("realSlowMotionTickRate", 8, 2, 19);
        b.pop();
    }

    /** Called after the spec is built. */
    public static void bind(ModConfigSpec built) {
        spec = built;
    }

    private static <T> T get(Supplier<T> value, T def) {
        if (spec == null || !spec.isLoaded() || value == null) return def;
        try {
            return value.get();
        } catch (RuntimeException e) {
            return def;
        }
    }

    /** True when the action (or the capacitor) may work. */
    public static boolean enabled(String id) {
        ModConfigSpec.BooleanValue v = ENABLED.get(id);
        return v == null || get(v, true);
    }

    public static boolean phaseShockVsPlayers() { return get(phaseShockVsPlayers, true); }
    public static boolean fieldVsPlayers() { return get(fieldVsPlayers, true); }
    public static boolean dilationVsPlayers() { return get(dilationVsPlayers, true); }
    public static boolean volleyVsPlayers() { return get(volleyVsPlayers, true); }
    public static boolean realSlowMotion() { return get(realSlowMotion, false); }
    public static int realSlowTickRate() { return get(realSlowTickRate, 8); }

    public static double coherenceRegen() { return get(coherenceRegen, 8.0); }
    public static int regenDelay() { return get(regenDelay, 30); }
    public static long euPerCoherence() { return get(euPerCoherence, 2500); }
    public static int decoherenceTicks() { return get(decoherenceTicks, 80); }
    public static int capacitorBonus() { return get(capacitorBonus, 50); }

    public static int blinkRange(int tier) { return tier >= 2 ? get(blinkRangeMk2, 20) : get(blinkRange, 12); }
    public static int blinkCost() { return get(blinkCost, 25); }
    public static int blinkCooldown() { return get(blinkCooldown, 50); }
    public static int phaseTicks(int tier) { return tier >= 2 ? get(phaseTicksMk2, 30) : get(phaseTicks, 20); }
    public static int phaseCost() { return get(phaseCost, 35); }
    public static int phaseCooldown() { return get(phaseCooldown, 160); }
    public static int phaseShockRadius() { return get(phaseShockRadius, 4); }
    public static double phaseShockDamage() { return get(phaseShockDamage, 4.0); }
    public static int fieldRadius() { return get(fieldRadius, 4); }
    public static int fieldStartCost() { return get(fieldStartCost, 15); }
    public static int fieldPerSecond() { return get(fieldPerSecond, 10); }
    public static int fieldCooldown() { return get(fieldCooldown, 120); }
    public static int dilationRadius() { return get(dilationRadius, 10); }
    public static int dilationTicks() { return get(dilationTicks, 80); }
    public static int dilationCost() { return get(dilationCost, 60); }
    public static int dilationCooldown() { return get(dilationCooldown, 500); }
    public static double dilationSlow() { return get(dilationSlow, 0.7); }
    public static double dilationProjectile() { return get(dilationProjectile, 0.25); }
    public static int volleyCost() { return get(volleyCost, 20); }
    public static int volleyCooldown() { return get(volleyCooldown, 200); }
    public static double volleyDamage() { return get(volleyDamage, 8.0); }
    public static int volleyRange() { return get(volleyRange, 128); }
    public static boolean synergyVsPlayers() { return get(resonanceVsPlayers, true); }
    public static double resonanceShare() { return get(resonanceShare, 0.25); }
    public static int resonanceThreshold() { return get(resonanceThreshold, 8); }
    public static int resonanceRadius() { return get(resonanceRadius, 4); }
    public static int decoyTicks() { return get(decoyTicks, 40); }
    public static int phaseCounterMax() { return get(counterMax, 20); }
    public static int tunnelWindow() { return get(tunnelWindow, 20); }
    public static double frozenVolleyDamage() { return get(frozenVolleyDamage, 1.5); }
    public static int recallCost() { return get(recallCost, 30); }
    public static int recallCooldown() { return get(recallCooldown, 600); }
    public static boolean unloadedRecall() { return get(unloadedRecall, true); }
    public static int holdRadius() { return get(holdRadius, 10); }
    public static int suppressRadius() { return get(suppressRadius, 14); }
    public static int focusRange() { return get(focusRange, 128); }
}
