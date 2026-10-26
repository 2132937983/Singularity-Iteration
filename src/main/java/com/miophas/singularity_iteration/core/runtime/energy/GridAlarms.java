// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.runtime.energy;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/**
 * <p><b>EN:</b> 0.1.10.22 (G7): the alarm log of the grid of one dimension for the monitoring panel of the energy
 * management terminal: breaker trips, reclosings and lockouts, blown fuses, melted cables, burned insulation, exploded
 * machines and critical loads that did not get their energy. The last {@link #MAX} alarms per dimension, in memory
 * only (a restart clears them). Server thread only.
 * <p><b>ZH:</b> 0.1.10.22（G7）：一个维度电网的警报记录，供能源管理终端的监控面板使用：断路器跳闸、重合闸与闭锁、保险丝熔断、线缆熔毁、
 * 绝缘烧毁、机器爆炸，以及没有得到电力的重要负荷。每个维度保留最近 {@link #MAX} 条，只在内存中（重启后清空）。仅限服务器线程。
 * <p><b>JA:</b> 0.1.10.22（G7）：1 つのディメンションの電力網の警報記録。エネルギー管理端末の監視パネル用：ブレーカーのトリップ・再閉路・ロックアウト、
 * ヒューズ断、導線の溶断、絶縁の焼損、機械の爆発、電力を得られなかった重要負荷。ディメンションごとに直近 {@link #MAX} 件をメモリだけに持つ
 * （再起動で消える）。サーバースレッド専用。
 */
public final class GridAlarms {
    private GridAlarms() {}

    public enum Type { BREAKER_TRIP, BREAKER_RECLOSE, BREAKER_LOCKOUT, BREAKER_RESET, FUSE_BLOWN, CABLE_MELTED, INSULATION_BURNED,
        MACHINE_EXPLODED, CRITICAL_SHORTFALL }

    /** One alarm: game time, type, block position ({@link BlockPos#asLong()}), detail (a cause ordinal or a count) and a value. */
    public record Alarm(long time, Type type, long pos, int detail, double value) { }

    public static final int MAX = 128;
    /** A critical shortfall at one position is logged at most once in this many ticks. */
    public static final int SHORTFALL_INTERVAL = 100;

    private static final Map<ServerLevel, ArrayDeque<Alarm>> LOG = new WeakHashMap<>();
    private static final Map<ServerLevel, Map<Long, Long>> SHORTFALLS = new WeakHashMap<>();

    public static void record(ServerLevel level, Type type, BlockPos pos, int detail, double value) {
        ArrayDeque<Alarm> log = LOG.computeIfAbsent(level, l -> new ArrayDeque<>());
        log.addLast(new Alarm(level.getGameTime(), type, pos.asLong(), detail, value));
        while (log.size() > MAX) log.removeFirst();
    }

    /** A critical load got less than its rated demand: logged once per {@link #SHORTFALL_INTERVAL} ticks per load. */
    public static void shortfall(ServerLevel level, BlockPos pos, double missing) {
        Map<Long, Long> last = SHORTFALLS.computeIfAbsent(level, l -> new java.util.HashMap<>());
        long now = level.getGameTime();
        Long before = last.get(pos.asLong());
        if (before != null && now - before < SHORTFALL_INTERVAL && now >= before) return;
        if (last.size() > 4096) last.clear();
        last.put(pos.asLong(), now);
        record(level, Type.CRITICAL_SHORTFALL, pos, 0, missing);
    }

    /** The alarms of the dimension, oldest first (a copy). */
    public static List<Alarm> recent(ServerLevel level) {
        ArrayDeque<Alarm> log = LOG.get(level);
        return log == null ? List.of() : new ArrayList<>(log);
    }

    /** Tests and tools. */
    public static void clear(ServerLevel level) {
        LOG.remove(level);
        SHORTFALLS.remove(level);
    }
}
