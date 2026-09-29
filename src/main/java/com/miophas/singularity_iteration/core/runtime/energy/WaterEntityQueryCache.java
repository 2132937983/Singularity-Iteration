package com.miophas.singularity_iteration.core.runtime.energy;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Shares server-side living-entity lookups made by waterlogged wires during one
 * game tick. The cache is spatially bucketed so overlapping wire ranges reuse
 * the same level query without changing which entities each caller damages.
 */
public final class WaterEntityQueryCache {
    private static final int BUCKET_SIZE = 64;
    private static final Map<Level, Frame> FRAMES = new WeakHashMap<>();

    private WaterEntityQueryCache() {
    }

    public static List<LivingEntity> query(Level level, AABB area) {
        if (level.isClientSide()) {
            return level.getEntitiesOfClass(LivingEntity.class, area);
        }

        Frame frame;
        long gameTime = level.getGameTime();
        synchronized (FRAMES) {
            frame = FRAMES.get(level);
            if (frame == null || frame.gameTime != gameTime) {
                frame = new Frame(gameTime);
                FRAMES.put(level, frame);
            }
        }
        return frame.query(level, area);
    }

    private static final class Frame {
        private final long gameTime;
        private final Map<Long, List<WeakReference<LivingEntity>>> buckets = new java.util.HashMap<>();

        private Frame(long gameTime) {
            this.gameTime = gameTime;
        }

        private synchronized List<LivingEntity> query(Level level, AABB area) {
            List<LivingEntity> result = new ArrayList<>();
            Set<LivingEntity> seen = Collections.newSetFromMap(new IdentityHashMap<>());
            int minX = bucket(area.minX);
            int minY = bucket(area.minY);
            int minZ = bucket(area.minZ);
            int maxX = bucket(area.maxX);
            int maxY = bucket(area.maxY);
            int maxZ = bucket(area.maxZ);
            for (int x = minX; x <= maxX; x++) {
                for (int y = minY; y <= maxY; y++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        for (WeakReference<LivingEntity> reference : bucket(level, x, y, z)) {
                            LivingEntity entity = reference.get();
                            if (entity != null && seen.add(entity) && area.intersects(entity.getBoundingBox())) {
                                result.add(entity);
                            }
                        }
                    }
                }
            }
            return result.isEmpty() ? List.of() : List.copyOf(result);
        }

        private List<WeakReference<LivingEntity>> bucket(Level level, int x, int y, int z) {
            long key = new net.minecraft.core.BlockPos(x, y, z).asLong();
            List<WeakReference<LivingEntity>> cached = buckets.get(key);
            if (cached != null) {
                return cached;
            }
            double minX = (double) x * BUCKET_SIZE;
            double minY = (double) y * BUCKET_SIZE;
            double minZ = (double) z * BUCKET_SIZE;
            AABB bucketArea = new AABB(minX, minY, minZ,
                    minX + BUCKET_SIZE, minY + BUCKET_SIZE, minZ + BUCKET_SIZE);
            List<WeakReference<LivingEntity>> snapshot = level
                    .getEntitiesOfClass(LivingEntity.class, bucketArea)
                    .stream()
                    .map(WeakReference::new)
                    .toList();
            buckets.put(key, snapshot);
            return snapshot;
        }

        private static int bucket(double coordinate) {
            return (int) Math.floor(coordinate / BUCKET_SIZE);
        }
    }
}
