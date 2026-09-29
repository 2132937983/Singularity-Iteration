package com.miophas.singularity_iteration.common.blockentity.reactor;

import com.miophas.singularity_iteration.common.reactor.ExplosionWorkScheduler;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

/** Compatibility facade; all work is budgeted once per server tick across dimensions and blast types. */
public class ReactorExplosionSchedulerWave {
    public static void startExplosion(ServerLevel level, BlockPos pos, ReactorExplosionTaskWave task) {
        ExplosionWorkScheduler.submit(level, pos, ReactorExplosionSchedulerWave.class, task, task::advance);
    }

    /** Admission-aware entry point for callers that have not applied world side effects yet. */
    public static boolean tryStartExplosion(ServerLevel level, BlockPos pos, ReactorExplosionTaskWave task) {
        return ExplosionWorkScheduler.trySubmit(level, pos, ReactorExplosionSchedulerWave.class, task, task::advance);
    }

    /** The central ServerTick.Post subscriber owns execution. Retained for existing callers. */
    @Deprecated
    public static void tick(Level level) { }

    public static void clear(ServerLevel level) {
        ExplosionWorkScheduler.clear(level, ReactorExplosionSchedulerWave.class);
    }

    public static void clear() {
        ExplosionWorkScheduler.clear(ReactorExplosionSchedulerWave.class);
    }

    public static int getActiveExplosionCount() {
        return ExplosionWorkScheduler.count(ReactorExplosionSchedulerWave.class);
    }
}
