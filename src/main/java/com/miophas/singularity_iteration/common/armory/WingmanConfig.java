// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.armory;

import java.util.function.Supplier;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Common config section {@code Wingmen} (0.1.11.39, Wingman audit W0, G-14): the numbers that were constants of the
 * Wingman. Every getter returns the default (the value before 0.1.11.39) before the config is loaded.
 */
public final class WingmanConfig {
    private WingmanConfig() {}

    /** Most Wingmen per player: the console and the squad packets carry at most 8. */
    public static final int MAX_LIMIT = 8;

    private static ModConfigSpec spec;
    private static ModConfigSpec.IntValue maxPerPlayer, upkeep, lowPowerSeconds, scanRange, leash;

    /** Defines the section entries (called once while the common spec is built). */
    public static void define(ModConfigSpec.Builder b) {
        maxPerPlayer = b.comment("Wingmen per player, counting the ones in unloaded chunks. 每名玩家的僚机数量上限（包括未加载区块中的僚机）。")
            .translation("mio_icif.configuration.wingmen.maxPerPlayer").defineInRange("maxPerPlayer", WingmanService.MAX_PER_PLAYER, 1, MAX_LIMIT);
        upkeep = b.comment("Upkeep of one Wingman in EU per second: the electric pieces of its suit pay first, then its Armory (when loaded).",
                "单架僚机每秒的维持费（EU）：先由套装的电动部件支付，再由其武装库支付（武装库须已加载）。")
            .translation("mio_icif.configuration.wingmen.upkeepEuPerSecond").defineInRange("upkeepEuPerSecond", 160, 0, 1_000_000);
        lowPowerSeconds = b.comment("Seconds a Wingman flies on without paid upkeep before it returns. 维持费付不起时，僚机继续飞行多少秒后返航。")
            .translation("mio_icif.configuration.wingmen.lowPowerSeconds").defineInRange("lowPowerSeconds", 5, 1, 600);
        scanRange = b.comment("Blocks around the owner in which an offensive Wingman looks for targets. 进攻模式的僚机在主人周围多少格内寻找目标。")
            .translation("mio_icif.configuration.wingmen.scanRange").defineInRange("scanRange", 24, 8, 64);
        leash = b.comment("Blocks from the owner beyond which a Wingman drops its target (without an order). 目标离主人超过多少格时僚机放弃它（无命令时）。")
            .translation("mio_icif.configuration.wingmen.leash").defineInRange("leash", 28, 8, 96);
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

    public static int maxPerPlayer() { return get(maxPerPlayer, WingmanService.MAX_PER_PLAYER); }
    public static long upkeepEuPerSecond() { return get(upkeep, 160); }
    public static int lowPowerSeconds() { return get(lowPowerSeconds, 5); }
    public static double scanRange() { return get(scanRange, 24); }
    /** Never below the scan range (a target found must not be dropped at once). */
    public static double leash() { return Math.max(scanRange(), get(leash, 28)); }
}
