// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.suit.core;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Supplier;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Common config section {@code FrameCores} (0.1.9.0): a switch per core and the numbers of the
 * arc discharge burst of the advanced propulsion core. Every getter returns the default before
 * the config is loaded.
 */
public final class CoreConfig {
    private CoreConfig() {}

    private static ModConfigSpec spec;
    private static final Map<FrameCore, ModConfigSpec.BooleanValue> ENABLED = new EnumMap<>(FrameCore.class);
    private static ModConfigSpec.BooleanValue dischargeVsPlayers;
    private static ModConfigSpec.IntValue dischargeRadius, dischargeCooldown, dischargeEu;
    private static ModConfigSpec.DoubleValue dischargeDamage, dischargePush;

    public static void define(ModConfigSpec.Builder b) {
        b.comment("On/off switch of each frame core. A core that is off does nothing (it stays installed).")
            .translation("mio_icif.configuration.core.section.cores").push("cores");
        for (FrameCore core : FrameCore.VALUES) {
            ENABLED.put(core, b.translation("mio_icif.configuration.core.enabled." + core.id()).define(core.id(), true));
        }
        b.pop();
        b.comment("Arc discharge burst of the advanced propulsion core (core burst key).")
            .translation("mio_icif.configuration.core.section.discharge").push("discharge");
        dischargeVsPlayers = b.comment("The arc discharge also hits and pushes other players (PvP).")
            .translation("mio_icif.configuration.core.discharge_players").define("dischargeHitsPlayers", true);
        dischargeRadius = b.comment("Radius in blocks.").translation("mio_icif.configuration.core.discharge_radius")
            .defineInRange("dischargeRadius", 7, 2, 24);
        dischargeDamage = b.comment("Damage of each arc.").translation("mio_icif.configuration.core.discharge_damage")
            .defineInRange("dischargeDamage", 10.0, 0.0, 100.0);
        dischargePush = b.comment("Horizontal push of the shock wave (blocks per tick at the centre).")
            .translation("mio_icif.configuration.core.discharge_push").defineInRange("dischargePush", 1.6, 0.0, 6.0);
        dischargeEu = b.comment("EU per discharge, from the suit energy pool.").translation("mio_icif.configuration.core.discharge_eu")
            .defineInRange("dischargeEU", 1_500_000, 0, 100_000_000);
        dischargeCooldown = b.comment("Cooldown in ticks.").translation("mio_icif.configuration.core.discharge_cooldown")
            .defineInRange("dischargeCooldownTicks", 160, 0, 2400);
        b.pop();
    }

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

    public static boolean enabled(FrameCore core) {
        ModConfigSpec.BooleanValue v = ENABLED.get(core);
        return v == null || get(v, true);
    }

    public static boolean dischargeVsPlayers() { return get(dischargeVsPlayers, true); }
    public static int dischargeRadius() { return get(dischargeRadius, 7); }
    public static double dischargeDamage() { return get(dischargeDamage, 10.0); }
    public static double dischargePush() { return get(dischargePush, 1.6); }
    public static long dischargeEu() { return get(dischargeEu, 1_500_000); }
    public static int dischargeCooldown() { return get(dischargeCooldown, 160); }
}
