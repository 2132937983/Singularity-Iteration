package com.singularity_iteration.mio_icif.Blocks.entity.reactor;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings("null")
public class NukeExplosionScheduler {
    private static final Map<BlockPos, NukeExplosionTask> EXPLOSION_TASKS = new ConcurrentHashMap<>();

    public static void startExplosion(ServerLevel level, BlockPos pos, NukeExplosionTask task) {
        EXPLOSION_TASKS.put(pos, task);
    }

    public static void tick(Level level) {
        if (level.isClientSide || !(level instanceof ServerLevel)) {
            return;
        }

        Iterator<Map.Entry<BlockPos, NukeExplosionTask>> iterator = EXPLOSION_TASKS.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<BlockPos, NukeExplosionTask> entry = iterator.next();
            BlockPos pos = entry.getKey();
            NukeExplosionTask task = entry.getValue();

            task.tick();

            if (task.isComplete()) {
                level.gameEvent(null, net.minecraft.world.level.gameevent.GameEvent.EXPLODE, pos.getCenter());
                iterator.remove();
            }
        }
    }

    public static void clear() {
        EXPLOSION_TASKS.clear();
    }
}

