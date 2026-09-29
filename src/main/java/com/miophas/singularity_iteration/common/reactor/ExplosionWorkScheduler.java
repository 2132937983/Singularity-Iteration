// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.reactor;

import com.miophas.singularity_iteration.core.runtime.reactor.FairExplosionQueue;

import com.mojang.logging.LogUtils;
import com.miophas.singularity_iteration.common.Singularity_Iteration;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.BooleanSupplier;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.gameevent.GameEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** One server-thread queue for all blast types and dimensions. */
@EventBusSubscriber(modid = Singularity_Iteration.MOD_ID)
public final class ExplosionWorkScheduler {
    public static final int MAX_STEPS_PER_TICK = 4096;
    // Cooperative: a third-party block/entity callback cannot be preempted mid-operation.
    public static final long MAX_NANOS_PER_TICK = 2_000_000L;
    private static final Map<MinecraftServer, State> STATES = new IdentityHashMap<>();
    private static final class State {
        final FairExplosionQueue queue = new FairExplosionQueue();
        int lastSteps;
        long completed, failed;
    }
    public record Snapshot(int activeTasks, int lastTickSteps, long completedTasks, long failedTasks) { }
    private ExplosionWorkScheduler() { }

    /** Legacy fire-and-forget entry point retained for existing callers. */
    public static void submit(ServerLevel level, BlockPos pos, Object category, Object task,
                              BooleanSupplier step) {
        trySubmit(level, pos, category, task, step);
    }

    /**
     * Try to admit a blast before its caller performs irreversible world side
     * effects. The queue remains single-threaded, so admission and insertion are
     * atomic with respect to other server-thread submissions.
     */
    public static boolean trySubmit(ServerLevel level, BlockPos pos, Object category, Object task,
                                    BooleanSupplier step) {
        var server = level.getServer();
        if (!server.isSameThread()) throw new IllegalStateException("Explosion submission must use the server thread");
        var center = pos.immutable();
        var state = STATES.computeIfAbsent(server, ignored -> new State());
        return state.queue.trySubmit(level, category, task, step,
                () -> {
                    state.completed++;
                    level.gameEvent(null, GameEvent.EXPLODE, center.getCenter());
                },
                failure -> {
                    state.failed++;
                    LogUtils.getLogger().error("Explosion task failed at {} in {}; partial work will not replay",
                            center, level.dimension().location(), failure);
                });
    }

    /** Cheap server-thread probe; avoids taking an entity snapshot when admission is impossible. */
    public static boolean hasCapacity(ServerLevel level) {
        var server = level.getServer();
        if (!server.isSameThread()) throw new IllegalStateException("Explosion admission must use the server thread");
        var state = STATES.get(server);
        return state == null || state.queue.hasCapacity();
    }

    /** Cancel one task after a caller-side effect failed before the blast was committed. */
    public static void cancel(ServerLevel level, Object task) {
        var state = STATES.get(level.getServer());
        if (state != null) state.queue.cancel(task);
    }

    /** The only normal execution entry point; former per-level scheduler hooks no longer spend work. */
    @SubscribeEvent
    public static void tick(ServerTickEvent.Post event) {
        var server = event.getServer();
        var state = STATES.get(server);
        if (state != null) state.lastSteps = state.queue.tick(server.getTickCount(), MAX_STEPS_PER_TICK, MAX_NANOS_PER_TICK);
    }

    public static Snapshot snapshot(MinecraftServer server) {
        var state = STATES.get(server);
        return state == null ? new Snapshot(0, 0, 0, 0)
                : new Snapshot(state.queue.size(), state.lastSteps, state.completed, state.failed);
    }
    public static long taskSteps(MinecraftServer server, Object task) {
        var state = STATES.get(server);
        return state == null ? -1 : state.queue.steps(task);
    }
    public static int count(ServerLevel level) {
        var state = STATES.get(level.getServer());
        return state == null ? 0 : state.queue.sizeForOwner(level);
    }
    public static boolean hasServerState(MinecraftServer server) { return STATES.containsKey(server); }
    public static void clear(ServerLevel level, Object category) {
        var state = STATES.get(level.getServer());
        if (state != null) state.queue.remove(level, category);
    }
    public static void clear(Object category) {
        for (var state : STATES.values()) state.queue.removeCategory(category);
    }
    public static int count(Object category) {
        int count = 0;
        for (var state : STATES.values()) count += state.queue.size(category);
        return count;
    }

    /** In-flight blasts are transient: unloading cancels remaining work without a completion event. */
    @SubscribeEvent
    public static void unload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) {
            var state = STATES.get(level.getServer());
            if (state != null) state.queue.removeOwner(level);
        }
    }
    @SubscribeEvent
    public static void stopped(ServerStoppedEvent event) {
        var state = STATES.remove(event.getServer());
        if (state != null) state.queue.clear();
    }
}
