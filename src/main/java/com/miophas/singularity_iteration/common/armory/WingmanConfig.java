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
    private static ModConfigSpec.BooleanValue emergencyRescue, groundForm;
    private static ModConfigSpec.IntValue maxPerPlayer, upkeep, lowPowerSeconds, scanRange, leash, reserveMinutes, speedCostPercent, pathNodes;

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
        emergencyRescue = b.comment("0.1.11.56: a Wingman catches its owner in a lethal fall and swaps suits with an owner near death",
                "who wears fewer than two armour pieces (the nearest Wingman, at most once a minute each).",
                "0.1.11.56：主人致命坠落时僚机接住他；主人濒死且身上盔甲不足两件时僚机飞来换装（由最近的僚机执行，每架每分钟最多一次）。")
            .translation("mio_icif.configuration.wingmen.emergencyRescue").define("emergencyRescue", true);
        groundForm = b.comment("0.1.11.228: a Wingman with the automatic stance sets down and goes on foot in low places (floor to ceiling at most",
                "6 blocks) and in rooms where its target stands on the floor. Off: only the wheel's \"on foot\" stance sets it down.",
                "0.1.11.228：自动立场的僚机在低矮处（地面到天花板不超过 6 格）以及目标站在地上的室内落地步行。关闭后只有轮盘的“地面”立场会让它落地。")
            .translation("mio_icif.configuration.wingmen.groundForm").define("groundForm", true);
        pathNodes = b.comment("0.1.11.228: grid cells one Wingman's path search may expand per tick (all Wingmen of a server together: 8 times as many).",
                "Fewer costs less server time; a long way round then takes more ticks to find.",
                "0.1.11.228：单架僚机寻路每 tick 最多展开多少格（全服所有僚机合计为它的 8 倍）。越少越省服务器时间，但绕远路时要多花几 tick 才找到路线。")
            .translation("mio_icif.configuration.wingmen.pathNodesPerTick").defineInRange("pathNodesPerTick", 256, 32, 2048);
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
    /** 0.1.11.56: true when Wingmen rescue their owner (a lethal fall, near death). */
    public static boolean emergencyRescue() { return get(emergencyRescue, true); }
    /** 0.1.11.50: the factor of the escort speed cost (1 = the maneuver flight's). */
    public static double speedCostFactor() { return get(speedCostPercent, 100) / 100.0; }
    public static double leash() { return Math.max(scanRange(), get(leash, 28)); }
    /** 0.1.11.228: true when the automatic stance may set a Wingman down (the ground form of 0.1.11.226). */
    public static boolean groundForm() { return get(groundForm, true); }
    /** 0.1.11.228: grid cells one Wingman's path search may expand per tick. */
    public static int pathNodesPerTick() { return get(pathNodes, 256); }
}
