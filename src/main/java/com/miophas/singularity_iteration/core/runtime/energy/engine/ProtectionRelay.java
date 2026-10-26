// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.runtime.energy.engine;

/**
 * <p><b>EN:</b> 0.1.10.22 (G5): the protection of one breaker or fuse cable. Pure: the cable feeds it the energy that
 * passed in each tick and the largest packet; the relay decides when the cable opens and when a breaker closes again.
 * <ul>
 *   <li><b>Overload (inverse time):</b> above the rated flow, the thermal state rises by
 *       {@code (r^2 - 1) / (3 * delay)} per tick, where {@code r} is flow / rated flow; the cable opens at 1. Thus it
 *       opens after {@code delay} ticks at twice the rated flow, after about 14 times {@code delay} at 1.1 times, and
 *       sooner for larger overloads. Below the rated flow the state cools by {@code 1 / (4 * delay)} per tick.
 *       Time grading: a breaker near the loads gets a shorter delay than the breaker of the feeder, so it opens first.</li>
 *   <li><b>Short circuit:</b> ten times the rated flow opens the cable in the same tick.</li>
 *   <li><b>Overvoltage:</b> a packet above the rated voltage (0 = off) opens the cable at once. The engine checks this
 *       before it applies the overvoltage effects, so the blocks behind the cable do not melt or explode.</li>
 *   <li><b>Reclosing (breaker):</b> with automatic reclosing the breaker closes again after {@link #RECLOSE_TICKS}.
 *       The fourth trip without a quiet period of {@link #SETTLE_TICKS} locks it out until a player resets it.
 *       Without automatic reclosing it stays open until a reset.</li>
 *   <li><b>Fuse:</b> it blows once; a new fuse item restores it.</li>
 * </ul>
 * <p><b>ZH:</b> 0.1.10.22（G5）：一个断路器或保险丝线缆的保护逻辑。纯计算：线缆把每个 tick 通过的能量与最大数据包交给它，由它决定线缆何时断开、
 * 断路器何时重新合上。过载（反时限）：超过额定流量时，热状态每 tick 增加 {@code (r^2 - 1) / (3 * 延时)}（{@code r} = 流量 / 额定流量），到 1 时断开；
 * 因此 2 倍额定时经过 {@code 延时} tick 断开，1.1 倍时约 14 倍延时，过载越大越快；低于额定时每 tick 冷却 {@code 1 / (4 * 延时)}。
 * 时间配合：靠近负荷的断路器延时较短，先于馈线断路器断开。短路：10 倍额定流量当 tick 断开。过电压：数据包超过额定电压（0 = 关闭）时立即断开；
 * 引擎在施加过电压效果之前检查，因此线缆后方的方块不会熔毁或爆炸。重合闸（断路器）：自动重合闸时经过 {@link #RECLOSE_TICKS} 后重新合上；
 * 在 {@link #SETTLE_TICKS} 的稳定期内第 4 次跳闸则闭锁，直到玩家复位；不自动重合闸时保持断开直到复位。保险丝：只熔断一次，用新的保险丝物品恢复。
 * <p><b>JA:</b> 0.1.10.22（G5）：ブレーカー線やヒューズ線 1 本分の保護。純粋計算：線は各 tick に通ったエネルギーと最大のパケットを渡し、
 * リレーが線をいつ開くか、ブレーカーをいつ再投入するかを決める。過負荷（反限時）：定格流量を超えると熱状態が tick ごとに
 * {@code (r^2 - 1) / (3 * 遅延)}（{@code r} = 流量 / 定格流量）増え、1 で開く。定格の 2 倍なら {@code 遅延} tick 後、1.1 倍なら遅延の約 14 倍後に開き、
 * 過負荷が大きいほど早い。定格以下では tick ごとに {@code 1 / (4 * 遅延)} 冷える。時限協調：負荷に近いブレーカーの遅延を幹線より短くすれば、先に開く。
 * 短絡：定格の 10 倍の流量で同じ tick に開く。過電圧：定格電圧（0 = 無効）を超えるパケットで即座に開く。エンジンは過電圧の効果を与える前に
 * これを調べるので、線の先のブロックは溶けず爆発もしない。再閉路（ブレーカー）：自動再閉路なら {@link #RECLOSE_TICKS} 後に再投入する。
 * {@link #SETTLE_TICKS} の安定期間なしに 4 回目のトリップでロックアウトし、プレイヤーがリセットするまで開いたまま。自動再閉路なしならリセットまで開いたまま。
 * ヒューズ：1 回だけ切れる。新しいヒューズのアイテムで元に戻す。
 */
public final class ProtectionRelay {
    public enum Kind { BREAKER, FUSE }
    public enum State { CLOSED, TRIPPED, LOCKED_OUT, BLOWN }
    public enum Cause { NONE, OVERLOAD, SHORT_CIRCUIT, OVERVOLTAGE }
    public enum Event { NONE, TRIP, RECLOSE, LOCKOUT, BLOWN, RESET }

    /** Ticks from a trip to the automatic reclosing of a breaker. */
    public static final int RECLOSE_TICKS = 60;
    /** Reclosings in a row before a lockout. */
    public static final int MAX_RECLOSES = 3;
    /** Ticks without a trip after a reclosing that clear the trip count. */
    public static final int SETTLE_TICKS = 600;
    /** Flow / rated flow from which the cable opens in the same tick. */
    public static final double INSTANT_RATIO = 10;
    /** Setting bounds. */
    public static final long MIN_FLOW = 1, MAX_FLOW = 1L << 31;
    public static final int MIN_DELAY = 1, MAX_DELAY = 1200;

    private final Kind kind;
    private long ratedFlow, ratedVoltage;
    private int delay;
    private boolean autoReclose;
    private State state = State.CLOSED;
    private Cause lastCause = Cause.NONE;
    private double heat, lastFlow, lastVoltage;
    private int recloseIn, recentTrips;
    private long closedAt = Long.MIN_VALUE / 2, lastTripAt = Long.MIN_VALUE / 2;
    private long flowTick = Long.MIN_VALUE;
    private double flow, voltage;

    public ProtectionRelay(Kind kind) {
        this.kind = kind;
        if (kind == Kind.FUSE) { ratedFlow = 128; delay = 5; autoReclose = false; }
        else { ratedFlow = 512; delay = 20; autoReclose = true; }
    }

    public Kind kind() { return kind; }
    public State state() { return state; }
    public Cause lastCause() { return lastCause; }
    public long ratedFlow() { return ratedFlow; }
    public long ratedVoltage() { return ratedVoltage; }
    public int delay() { return delay; }
    public boolean autoReclose() { return autoReclose; }
    public double heat() { return heat; }
    public double lastFlow() { return lastFlow; }
    public double lastVoltage() { return lastVoltage; }
    public int recloseIn() { return recloseIn; }
    public int recentTrips() { return recentTrips; }
    public long lastTripAt() { return lastTripAt; }

    /** True while the cable conducts. */
    public boolean conducts() { return state == State.CLOSED; }

    /** Settings within bounds; a fuse never recloses. */
    public void configure(long ratedFlow, long ratedVoltage, int delay, boolean autoReclose) {
        this.ratedFlow = Math.max(MIN_FLOW, Math.min(MAX_FLOW, ratedFlow));
        this.ratedVoltage = Math.max(0, Math.min(MAX_FLOW, ratedVoltage));
        this.delay = Math.max(MIN_DELAY, Math.min(MAX_DELAY, delay));
        this.autoReclose = kind == Kind.BREAKER && autoReclose;
    }

    /** Energy that passed in tick {@code tick} (several deliveries add up) and its packet. */
    public void carry(long tick, double energy, double packet) {
        if (tick != flowTick) { flowTick = tick; flow = 0; voltage = 0; }
        flow += Math.max(0, energy);
        voltage = Math.max(voltage, packet);
    }

    /** True when a packet of {@code packet} EU opens the closed cable at once (voltage element). */
    public boolean voltageExceeded(double packet) {
        return state == State.CLOSED && ratedVoltage > 0 && packet > ratedVoltage;
    }

    /** Opens the closed cable now (the voltage element, checked by the engine). */
    public Event tripNow(long tick, Cause cause) {
        return state == State.CLOSED ? trip(tick, cause) : Event.NONE;
    }

    /** The end of tick {@code tick}: overload, reclosing and cooling. */
    public Event evaluate(long tick) {
        double f = flowTick == tick ? flow : 0;
        lastFlow = f;
        lastVoltage = flowTick == tick ? voltage : 0;
        switch (state) {
            case CLOSED -> {
                double ratio = f / ratedFlow;
                if (ratio >= INSTANT_RATIO) return trip(tick, Cause.SHORT_CIRCUIT);
                if (ratio > 1) {
                    heat += (ratio * ratio - 1) / (3.0 * delay);
                    if (heat >= 1) return trip(tick, Cause.OVERLOAD);
                } else heat = Math.max(0, heat - 1.0 / (4.0 * delay));
                if (recentTrips > 0 && tick - closedAt >= SETTLE_TICKS) recentTrips = 0;
            }
            case TRIPPED -> {
                heat = Math.max(0, heat - 1.0 / (4.0 * delay));
                if (autoReclose && recloseIn > 0 && --recloseIn == 0) {
                    state = State.CLOSED;
                    closedAt = tick;
                    return Event.RECLOSE;
                }
            }
            default -> { }
        }
        return Event.NONE;
    }

    private Event trip(long tick, Cause cause) {
        heat = 0;
        lastCause = cause;
        lastTripAt = tick;
        if (kind == Kind.FUSE) { state = State.BLOWN; return Event.BLOWN; }
        recentTrips++;
        if (autoReclose && recentTrips > MAX_RECLOSES) { state = State.LOCKED_OUT; recloseIn = 0; return Event.LOCKOUT; }
        state = State.TRIPPED;
        recloseIn = autoReclose ? RECLOSE_TICKS : 0;
        return Event.TRIP;
    }

    /** A player closes an open breaker or restores a blown fuse. */
    public Event reset(long tick) {
        if (state == State.CLOSED) return Event.NONE;
        state = State.CLOSED;
        recentTrips = 0;
        recloseIn = 0;
        heat = 0;
        closedAt = tick;
        return Event.RESET;
    }

    /** The saved form: settings, state, cause, thermal state, counters. */
    public long[] save() {
        return new long[] {ratedFlow, ratedVoltage, delay, autoReclose ? 1 : 0, state.ordinal(), lastCause.ordinal(),
            Double.doubleToLongBits(heat), recloseIn, recentTrips};
    }

    public void load(long[] saved) {
        if (saved == null || saved.length < 9) return;
        configure(saved[0], saved[1], (int) saved[2], saved[3] != 0);
        State[] states = State.values();
        Cause[] causes = Cause.values();
        state = states[(int) Math.max(0, Math.min(states.length - 1, saved[4]))];
        if (kind == Kind.FUSE && (state == State.TRIPPED || state == State.LOCKED_OUT)) state = State.BLOWN;
        if (kind == Kind.BREAKER && state == State.BLOWN) state = State.LOCKED_OUT;
        lastCause = causes[(int) Math.max(0, Math.min(causes.length - 1, saved[5]))];
        double h = Double.longBitsToDouble(saved[6]);
        heat = Double.isFinite(h) ? Math.max(0, Math.min(1, h)) : 0;
        recloseIn = (int) Math.max(0, Math.min(RECLOSE_TICKS, saved[7]));
        recentTrips = (int) Math.max(0, Math.min(MAX_RECLOSES + 1, saved[8]));
        if (state == State.TRIPPED && autoReclose && recloseIn == 0) recloseIn = RECLOSE_TICKS;
    }
}
