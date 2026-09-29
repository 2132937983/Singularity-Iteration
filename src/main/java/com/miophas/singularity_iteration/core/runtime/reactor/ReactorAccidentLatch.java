// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.runtime.reactor;

/** Server-thread-confined one-owner lifecycle. World effects remain the caller's responsibility. */
public final class ReactorAccidentLatch {
    public enum State { OPEN, PENDING, DISPATCHING, CLOSED, UNCERTAIN }
    public enum Effect { LOCAL_MACHINE, NUCLEAR_TERRAIN }
    /** 千分之一精度存放 IC2 的 boomPower；IC2 基础威力为 10.0。 */
    public static final int DEFAULT_POWER_MILLIS = 10_000;
    public record Trigger(long finalHeat, long finalCapacity, long gameTime, Effect effect,
                          int powerMillis) {
        public Trigger {
            if (finalCapacity <= 0 || finalHeat < finalCapacity || effect == null)
                throw new IllegalArgumentException("Invalid committed accident trigger");
            if (powerMillis < 0 || powerMillis > 200_000_000)
                throw new IllegalArgumentException("Invalid committed accident profile");
        }
        public Trigger(long finalHeat, long finalCapacity, long gameTime, Effect effect) {
            this(finalHeat, finalCapacity, gameTime, effect, DEFAULT_POWER_MILLIS);
        }
        /**
         * 对齐 IC2 {@code explode()}：威力 = (10 + Σ加法项) × Π隔板减免 × HEM，再按
         * {@code protection/reactorExplosionPowerLimit}（默认 45）封顶。这里只负责读出已提交的数值。
         */
        public float terrainPower() {
            return Math.max(1, powerMillis / 1000f);
        }
    }
    public record Saved(State state, Trigger trigger) {
        public Saved {
            if (state == null || (state == State.OPEN) != (trigger == null))
                throw new IllegalArgumentException("Inconsistent accident state");
        }
    }
    private State state = State.OPEN;
    private Trigger trigger;

    /** Call while preparing the owned transaction, before any inventory/balance publication. */
    public static Trigger decide(long finalHeat, long finalCapacity, long gameTime, boolean nuclearTerrainEnabled) {
        return decide(finalHeat, finalCapacity, gameTime, nuclearTerrainEnabled, DEFAULT_POWER_MILLIS);
    }
    public static Trigger decide(long finalHeat, long finalCapacity, long gameTime, boolean nuclearTerrainEnabled,
                                 int powerMillis) {
        if (finalHeat < 0 || finalCapacity <= 0) throw new IllegalArgumentException("Invalid final thermal state");
        return finalHeat >= finalCapacity
            ? new Trigger(finalHeat, finalCapacity, gameTime,
                          nuclearTerrainEnabled ? Effect.NUCLEAR_TERRAIN : Effect.LOCAL_MACHINE,
                          powerMillis)
            : null;
    }
    public boolean mayOperateOrExport() { return state == State.OPEN; }
    public State state() { return state; }
    public Trigger trigger() { return trigger; }

    /** Publish only after the cycle's slots/thermal/fluid state commits. No world callbacks here. */
    public boolean armAfterCommit(Trigger prepared) {
        if (prepared == null || state != State.OPEN) return false;
        trigger = prepared;
        state = State.PENDING;
        return true;
    }
    /** Caller must first verify a supported effect executor and the same live loaded owner. */
    public boolean claimEffect() {
        if (state != State.PENDING) return false;
        state = State.DISPATCHING;
        return true;
    }
    public void closeEffect() {
        if (state != State.DISPATCHING) throw new IllegalStateException("No dispatched effect");
        state = State.CLOSED;
    }
    /** An exception after an external effect began must never silently schedule another explosion. */
    public void markUncertain() {
        if (state != State.DISPATCHING) throw new IllegalStateException("No dispatched effect");
        state = State.UNCERTAIN;
    }
    public Saved snapshot() { return new Saved(state, trigger); }
    public static ReactorAccidentLatch restore(Saved saved) {
        if (saved == null) throw new IllegalArgumentException("Missing accident state");
        var restored = new ReactorAccidentLatch();
        restored.state = saved.state() == State.DISPATCHING ? State.UNCERTAIN : saved.state();
        restored.trigger = saved.trigger();
        return restored;
    }
}
