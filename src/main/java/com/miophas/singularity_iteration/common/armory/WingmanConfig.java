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
    private static ModConfigSpec.IntValue maxPerPlayer, upkeep, lowPowerSeconds, scanRange, leash, reserveMinutes, speedCostPercent;

    /** Defines the section entries (called once while the common spec is built). */
    public static void define(ModConfigSpec.Builder b) {
        maxPerPlayer = b.comment("Wingmen per player, counting the ones in unloaded chunks. 每名玩家的僚机数量上限（包括未加载区块中的僚机）。")
            .translation("mio_icif.configuration.wingmen.maxPerPlayer").defineInRange("maxPerPlayer", WingmanService.MAX_PER_PLAYER, 1, MAX_LIMIT);
        upkeep = b.comment("Upkeep of one Wingman in EU per second: the electric pieces of its suit pay first, then its reserve, then its Armory (when loaded).",
                "单架僚机每秒的维持费（EU）：先由套装的电动部件支付，再用出击储备，最后由其武装库支付（武装库须已加载）。")
            .translation("mio_icif.configuration.wingmen.upkeepEuPerSecond").defineInRange("upkeepEuPerSecond", 160, 0, 1_000_000);
        reserveMinutes = b.comment("0.1.11.41: minutes of upkeep the Armory charges into a Wingman at deployment (less when it has less). The Wingman",
                "pays from its suit first, then from this reserve, then from its Armory (when loaded); the rest of the reserve goes back to the",
                "Armory when the Wingman comes home. 0 = no reserve (as before 0.1.11.41).",
                "0.1.11.41：出击时武装库为僚机预充多少分钟的维持费（武装库电量不足时少充）。僚机先用套装电力，再用这笔储备，最后才由武装库支付（须已加载）；",
                "返航时剩余储备退回武装库。0 = 不预充（0.1.11.41 之前的行为）。")
            .translation("mio_icif.configuration.wingmen.reserveMinutes").defineInRange("reserveMinutes", 10, 0, 120);
        lowPowerSeconds = b.comment("Seconds a Wingman flies on without paid upkeep before it returns. 维持费付不起时，僚机继续飞行多少秒后返航。")
            .translation("mio_icif.configuration.wingmen.lowPowerSeconds").defineInRange("lowPowerSeconds", 5, 1, 600);
        scanRange = b.comment("Blocks around the owner in which an offensive Wingman looks for targets. 进攻模式的僚机在主人周围多少格内寻找目标。")
            .translation("mio_icif.configuration.wingmen.scanRange").defineInRange("scanRange", 24, 8, 64);
        leash = b.comment("Blocks from the owner beyond which a Wingman drops its target (without an order). 目标离主人超过多少格时僚机放弃它（无命令时）。")
            .translation("mio_icif.configuration.wingmen.leash").defineInRange("leash", 28, 8, 96);
        speedCostPercent = b.comment("0.1.11.50: percent of the maneuver flight's speed cost (30 EU/t x speed^2) a Wingman pays for its escort speed",
                "above cruise (0.9 blocks per tick), with its upkeep. When it cannot pay, it flies at cruise speed. 0 = escort speed is free.",
                "0.1.11.50：僚机伴飞速度超过巡航（0.9 格/tick）的部分，按机动模式速度耗电（30 EU/t × 速度²）的百分之几随维持费支付。",
                "付不起时只能以巡航速度飞。0 = 伴飞速度免费。")
            .translation("mio_icif.configuration.wingmen.speedCostPercent").defineInRange("speedCostPercent", 100, 0, 1000);
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
    public static int reserveMinutes() { return get(reserveMinutes, 10); }
    /** EU of a full upkeep reserve. */
    public static long reserveEu() { return upkeepEuPerSecond() * 60L * reserveMinutes(); }
    public static double scanRange() { return get(scanRange, 24); }
    /** Never below the scan range (a target found must not be dropped at once). */
    /** 0.1.11.50: the factor of the escort speed cost (1 = the maneuver flight's). */
    public static double speedCostFactor() { return get(speedCostPercent, 100) / 100.0; }
    public static double leash() { return Math.max(scanRange(), get(leash, 28)); }
}
