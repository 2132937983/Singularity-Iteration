package com.miophas.singularity_iteration.common.blockentity.reactor;

import com.miophas.singularity_iteration.common.reactor.ExplosionWorkScheduler;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

/** Compatibility facade; all work is budgeted once per server tick across dimensions and blast types. */
public class NukeExplosionSchedulerOctree {
    public static void startExplosion(ServerLevel level, BlockPos pos, NukeExplosionTaskOctree task) {
        ExplosionWorkScheduler.submit(level, pos, NukeExplosionSchedulerOctree.class, task, task::advance);
    }

    /** Admission-aware entry point for callers that have not applied world side effects yet. */
    public static boolean tryStartExplosion(ServerLevel level, BlockPos pos, NukeExplosionTaskOctree task) {
        return ExplosionWorkScheduler.trySubmit(level, pos, NukeExplosionSchedulerOctree.class, task, task::advance);
    }

    /** The central ServerTick.Post subscriber owns execution. Retained for existing callers. */
    @Deprecated
    public static void tick(Level level) { }

    public static void clear(ServerLevel level) {
        ExplosionWorkScheduler.clear(level, NukeExplosionSchedulerOctree.class);
    }

    public static void clear() {
        ExplosionWorkScheduler.clear(NukeExplosionSchedulerOctree.class);
    }

    public static int getActiveExplosionCount() {
        return ExplosionWorkScheduler.count(NukeExplosionSchedulerOctree.class);
    }
}
