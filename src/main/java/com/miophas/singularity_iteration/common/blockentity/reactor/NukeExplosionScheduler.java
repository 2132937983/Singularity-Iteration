package com.miophas.singularity_iteration.common.blockentity.reactor;

import com.miophas.singularity_iteration.common.reactor.ExplosionWorkScheduler;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

/** Compatibility facade; all work is budgeted once per server tick across dimensions and blast types. */
public class NukeExplosionScheduler {
    public static void startExplosion(ServerLevel level, BlockPos pos, NukeExplosionTask task) {
        ExplosionWorkScheduler.submit(level, pos, NukeExplosionScheduler.class, task, task::advance);
    }

    /** Admission-aware entry point for callers that have not applied world side effects yet. */
    public static boolean tryStartExplosion(ServerLevel level, BlockPos pos, NukeExplosionTask task) {
        return ExplosionWorkScheduler.trySubmit(level, pos, NukeExplosionScheduler.class, task, task::advance);
    }

    public static void cancelExplosion(ServerLevel level, NukeExplosionTask task) {
        ExplosionWorkScheduler.cancel(level, task);
    }

    /** The central ServerTick.Post subscriber owns execution. Retained for existing callers. */
    @Deprecated
    public static void tick(Level level) { }

    public static void clear(ServerLevel level) {
        ExplosionWorkScheduler.clear(level, NukeExplosionScheduler.class);
    }

    public static void clear() {
        ExplosionWorkScheduler.clear(NukeExplosionScheduler.class);
    }

    public static int getActiveExplosionCount() {
        return ExplosionWorkScheduler.count(NukeExplosionScheduler.class);
    }
}

