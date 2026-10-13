// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.suit.nano;

import com.miophas.singularity_iteration.common.suit.NanoBurstDefaults;
import com.miophas.singularity_iteration.common.suit.SuitModuleType;
import com.miophas.singularity_iteration.core.api.suit.SuitBurstSpec;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Supplier;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Common config section {@code NanoSuit} (0.1.8.5): an on/off switch per nano unit, PvP switches
 * for the effects that act on other players (all on by default), and the numbers of the nano core
 * and of the bursts. Every getter returns the default before the config is loaded.
 */
public final class NanoConfig {
    private NanoConfig() {}

    private static ModConfigSpec spec;
    private static final Map<SuitModuleType, ModConfigSpec.BooleanValue> ENABLED = new EnumMap<>(SuitModuleType.class);
    private static final Map<SuitModuleType, ModConfigSpec.IntValue[]> BURST = new EnumMap<>(SuitModuleType.class);

    private static ModConfigSpec.BooleanValue empVsPlayers, camoVsPlayers, hfVsPlayers, jammerVsPlayers, reconVsPlayers;
    private static ModConfigSpec.DoubleValue mk2DrainFactor, meshReduction, swimBonus;
    private static ModConfigSpec.IntValue jammerRange, reconRange, repairEu, solarEuPerTick;
    private static ModConfigSpec.DoubleValue reserveRegen, heatDissipation, sprintBonus, hfAttackSpeed, hfArmorPen;
    private static ModConfigSpec.IntValue euPerReserve, overheatResume, capacitorBonus, empRadius, empStunTicks, camoMaxSeconds, dashDistance;

    /** Unit ids of the switches, in the config order. */
    private static final SuitModuleType[] UNITS = {SuitModuleType.MYOFIBER, SuitModuleType.THERMOREGULATOR,
        SuitModuleType.EMP_EMITTER, SuitModuleType.OPTICAL_CAMO, SuitModuleType.TOXIN_FILTER, SuitModuleType.MICRO_CAPACITOR,
        SuitModuleType.GECKO_SETAE, SuitModuleType.SHOCK_GEL, SuitModuleType.HEAT_SINK_FINS, SuitModuleType.THERMAL_CAMO,
        SuitModuleType.JAMMER, SuitModuleType.RECON_CLOUD, SuitModuleType.KINETIC_MESH, SuitModuleType.SELF_REPAIR,
        SuitModuleType.HYDROPHOBIC, SuitModuleType.KINETIC_GENERATOR, SuitModuleType.SOLAR_SKIN};

    /** Defines the section entries (called once while the common spec is built). */
    public static void define(ModConfigSpec.Builder b) {
        b.comment("On/off switch of each nano unit. A unit that is off does nothing (it stays installed).").translation("mio_icif.configuration.nano.section.units").push("units");
        for (SuitModuleType type : UNITS) {
            ENABLED.put(type, b.translation("mio_icif.configuration.nano.unit." + type.id()).define(type.id(), true));
        }
        b.pop();

        b.comment("Effects of nano units on other players (PvP). All on by default.").translation("mio_icif.configuration.nano.section.pvp").push("pvp");
        empVsPlayers = b.comment("EMP: erases projectiles that players fired, cancels the optical camouflage of players in range",
                "and breaks the guidance locks that other players hold on the wearer.")
            .translation("mio_icif.configuration.nano.pvp.emp").define("empAffectsPlayers", true);
        camoVsPlayers = b.comment("Optical camouflage hides the wearer from other players (off: only creatures lose sight of the wearer).")
            .translation("mio_icif.configuration.nano.pvp.camo").define("camoHidesFromPlayers", true);
        hfVsPlayers = b.comment("HF sync (nano myofiber + active nanosaber) pierces the armour of players.")
            .translation("mio_icif.configuration.nano.pvp.hf").define("hfSyncPiercesPlayers", true);
        jammerVsPlayers = b.comment("Jammer: the guided weapons of other players cannot lock the wearer.")
            .translation("mio_icif.configuration.nano.pvp.jammer").define("jammerBlocksPlayerLocks", true);
        reconVsPlayers = b.comment("Nano recon cloud: other players in the cloud glow for the wearer too.")
            .translation("mio_icif.configuration.nano.pvp.recon").define("reconRevealsPlayers", true);
        b.pop();

        b.comment("Nano core: burst reserve (0..100) and heat (0..100).").translation("mio_icif.configuration.nano.section.core").push("core");
        reserveRegen = b.comment("Reserve points regained per second while a nano piece is worn.")
            .translation("mio_icif.configuration.nano.reserve_regen").defineInRange("reserveRegenPerSecond", 2.0, 0.0, 50.0);
        euPerReserve = b.comment("EU paid for one reserve point (from the suit energy pool).")
            .translation("mio_icif.configuration.nano.eu_per_reserve").defineInRange("euPerReservePoint", 400, 0, 1_000_000);
        heatDissipation = b.comment("Heat shed per second (doubled by the thermoregulating membrane).")
            .translation("mio_icif.configuration.nano.heat_dissipation").defineInRange("heatDissipationPerSecond", 4.0, 0.1, 100.0);
        overheatResume = b.comment("After an overheat, bursts unlock again below this heat.")
            .translation("mio_icif.configuration.nano.overheat_resume").defineInRange("overheatResumeBelow", 40, 0, 99);
        b.pop();

        b.comment("Unit numbers.").translation("mio_icif.configuration.nano.section.tuning").push("tuning");
        capacitorBonus = b.comment("EU storage that the micro-capacitor lattice adds to its piece.")
            .translation("mio_icif.configuration.nano.capacitor").defineInRange("capacitorBonusEU", 250_000, 0, 100_000_000);
        sprintBonus = b.comment("Sprint speed bonus of the nano myofiber (0.15 = +15 %).")
            .translation("mio_icif.configuration.nano.sprint").defineInRange("myofiberSprintBonus", 0.15, 0.0, 1.0);
        dashDistance = b.comment("Kinetic Dash distance in blocks.")
            .translation("mio_icif.configuration.nano.dash_distance").defineInRange("dashDistance", 8, 2, 16);
        hfAttackSpeed = b.comment("HF sync attack speed bonus (0.12 = +12 %).")
            .translation("mio_icif.configuration.nano.hf_speed").defineInRange("hfSyncAttackSpeed", 0.12, 0.0, 1.0);
        hfArmorPen = b.comment("HF sync armour penetration: share of the armour reduction that is ignored (0.15 = 15 %).")
            .translation("mio_icif.configuration.nano.hf_pen").defineInRange("hfSyncArmorPenetration", 0.15, 0.0, 1.0);
        empRadius = b.comment("EMP radius in blocks.")
            .translation("mio_icif.configuration.nano.emp_radius").defineInRange("empRadius", 6, 2, 16);
        empStunTicks = b.comment("EMP stun of mechanical creatures, in ticks.")
            .translation("mio_icif.configuration.nano.emp_stun").defineInRange("empStunTicks", 60, 0, 400);
        camoMaxSeconds = b.comment("Longest optical camouflage run in seconds.")
            .translation("mio_icif.configuration.nano.camo_max").defineInRange("camoMaxSeconds", 15, 1, 120);
        jammerRange = b.comment("Jammer: hostile creatures farther than this lose track of the wearer (Mk.II: two thirds).")
            .translation("mio_icif.configuration.nano.jammer_range").defineInRange("jammerRange", 12, 2, 64);
        reconRange = b.comment("Nano recon cloud range in blocks (Mk.II: +1/3).")
            .translation("mio_icif.configuration.nano.recon_range").defineInRange("reconRange", 24, 4, 96);
        meshReduction = b.comment("Kinetic armor mesh: share of the remaining damage it cuts (Mk.II: +0.1). Total absorption stays at most 95 %.")
            .translation("mio_icif.configuration.nano.mesh_reduction").defineInRange("meshReduction", 0.30, 0.0, 0.9);
        repairEu = b.comment("Self-repair coat: EU per repair step.")
            .translation("mio_icif.configuration.nano.repair_eu").defineInRange("selfRepairEU", 2000, 0, 1_000_000);
        swimBonus = b.comment("Hydrophobic coat: swim speed bonus (0.3 = +30 %, Mk.II +0.2).")
            .translation("mio_icif.configuration.nano.swim").defineInRange("hydrophobicSwimBonus", 0.3, 0.0, 2.0);
        solarEuPerTick = b.comment("Solar skin: EU/t under open daylight (Mk.II: double).")
            .translation("mio_icif.configuration.nano.solar").defineInRange("solarSkinEUPerTick", 2, 0, 512);
        mk2DrainFactor = b.comment("Mk.II units draw this share of the Mk.I drain.")
            .translation("mio_icif.configuration.nano.mk2_drain").defineInRange("mk2DrainFactor", 0.8, 0.0, 1.0);
        b.pop();

        b.comment("Burst costs: reserve points (per second for the camouflage), heat, cooldown ticks.").translation("mio_icif.configuration.nano.section.bursts").push("bursts");
        burst(b, SuitModuleType.MYOFIBER, "kineticDash", NanoBurstDefaults.KINETIC_DASH);
        burst(b, SuitModuleType.EMP_EMITTER, "emp", NanoBurstDefaults.EMP);
        burst(b, SuitModuleType.OPTICAL_CAMO, "opticalCamo", NanoBurstDefaults.OPTICAL_CAMO);
        burst(b, SuitModuleType.RECON_CLOUD, "reconCloud", NanoBurstDefaults.RECON_CLOUD);
        burst(b, SuitModuleType.KINETIC_MESH, "kineticMesh", NanoBurstDefaults.KINETIC_MESH);
        b.pop();
    }

    private static void burst(ModConfigSpec.Builder b, SuitModuleType type, String name, SuitBurstSpec def) {
        BURST.put(type, new ModConfigSpec.IntValue[]{
            b.translation("mio_icif.configuration.nano.burst." + name + ".reserve").defineInRange(name + "Reserve", def.reserveCost(), 0, 100),
            b.translation("mio_icif.configuration.nano.burst." + name + ".heat").defineInRange(name + "Heat", def.heat(), 0, 200),
            b.translation("mio_icif.configuration.nano.burst." + name + ".cooldown").defineInRange(name + "CooldownTicks", def.cooldownTicks(), 0, 2400)});
    }

    /** Called after the spec is built. */
    public static void bind(ModConfigSpec built) {
        spec = built;
    }

    private static boolean loaded() {
        return spec != null && spec.isLoaded();
    }

    private static <T> T get(Supplier<T> value, T def) {
        if (!loaded() || value == null) return def;
        try {
            return value.get();
        } catch (RuntimeException e) {
            return def;
        }
    }

    /** True when the unit may work (built-in nano units have a switch; any other unit is always allowed). */
    public static boolean enabled(SuitModuleType type) {
        ModConfigSpec.BooleanValue v = ENABLED.get(type);
        return v == null || get(v, true);
    }

    /** Burst costs of a built-in unit after the config, or the unit's own spec. */
    public static SuitBurstSpec burst(SuitModuleType type) {
        SuitBurstSpec def = type.burst();
        ModConfigSpec.IntValue[] v = BURST.get(type);
        if (def == null || v == null || !loaded()) return def;
        return new SuitBurstSpec(get(v[0], def.reserveCost()), get(v[1], def.heat()), get(v[2], def.cooldownTicks()), def.sustained());
    }

    public static boolean empVsPlayers() { return get(empVsPlayers, true); }
    public static boolean camoVsPlayers() { return get(camoVsPlayers, true); }
    public static boolean hfVsPlayers() { return get(hfVsPlayers, true); }
    public static double reserveRegen() { return get(reserveRegen, 2.0); }
    public static long euPerReserve() { return get(euPerReserve, 400); }
    public static double heatDissipation() { return get(heatDissipation, 4.0); }
    public static int overheatResume() { return get(overheatResume, 40); }
    public static long capacitorBonus() { return get(capacitorBonus, 250_000); }
    public static double sprintBonus() { return get(sprintBonus, 0.15); }
    public static int dashDistance() { return get(dashDistance, 8); }
    public static double hfAttackSpeed() { return get(hfAttackSpeed, 0.12); }
    public static double hfArmorPen() { return get(hfArmorPen, 0.15); }
    public static int empRadius() { return get(empRadius, 6); }
    public static int empStunTicks() { return get(empStunTicks, 60); }
    public static int camoMaxTicks() { return get(camoMaxSeconds, 15) * 20; }
    public static boolean jammerVsPlayers() { return get(jammerVsPlayers, true); }
    public static boolean reconVsPlayers() { return get(reconVsPlayers, true); }
    public static int jammerRange() { return get(jammerRange, 12); }
    public static int reconRange() { return get(reconRange, 24); }
    public static double meshReduction() { return get(meshReduction, 0.30); }
    public static long repairEu() { return get(repairEu, 2000); }
    public static double swimBonus() { return get(swimBonus, 0.3); }
    public static int solarEuPerTick() { return get(solarEuPerTick, 2); }
    public static double mk2DrainFactor() { return get(mk2DrainFactor, 0.8); }
}
