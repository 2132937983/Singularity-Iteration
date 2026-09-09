package com.singularity_iteration.mio_icif.Blocks.entity.reactor;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 核弹爆炸调度器 - Octree版本
 * 用于管理多个同时进行的核弹爆炸任务 */
@SuppressWarnings("null")
public class NukeExplosionSchedulerOctree {
    private static final Map<BlockPos, NukeExplosionTaskOctree> EXPLOSION_TASKS = new ConcurrentHashMap<>();

    /**
     * 开始一个新的爆炸任务
 * @param level 服务器世界
 * @param pos 爆炸位置
     * @param task 爆炸任务
     */
    public static void startExplosion(ServerLevel level, BlockPos pos, NukeExplosionTaskOctree task) {
        EXPLOSION_TASKS.put(pos, task);
    }

    /**
     * 每tick更新所有爆炸任务
 * @param level 世界
     */
    public static void tick(Level level) {
        if (level.isClientSide || !(level instanceof ServerLevel)) {
            return;
        }

        Iterator<Map.Entry<BlockPos, NukeExplosionTaskOctree>> iterator = EXPLOSION_TASKS.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<BlockPos, NukeExplosionTaskOctree> entry = iterator.next();
            BlockPos pos = entry.getKey();
            NukeExplosionTaskOctree task = entry.getValue();

            task.tick();

            if (task.isComplete()) {
                level.gameEvent(null, net.minecraft.world.level.gameevent.GameEvent.EXPLODE, pos.getCenter());
                iterator.remove();
            }
        }
    }

    /**
     * 清除所有爆炸任务（用于重置或调试）
     */
    public static void clear() {
        EXPLOSION_TASKS.clear();
    }

    /**
     * 获取当前活跃的爆炸任务数量
 * @return 任务数量
     */
    public static int getActiveExplosionCount() {
        return EXPLOSION_TASKS.size();
    }
}