package com.singularity_iteration.mio_icif.event;

import com.singularity_iteration.mio_icif.Blocks.mio_icif_blocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import com.singularity_iteration.mio_icif.Singularity_Iteration;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 辐射方块管理�? * 追踪所有辐射方块的创建时间，在24小时后自动清�? */
@EventBusSubscriber(modid = Singularity_Iteration.MOD_ID)
@SuppressWarnings("null")
public class mio_icif_RadiationBlockManager {

    // 辐射持续时间隔?4小时 = 24 * 60 * 60 * 1000 毫秒�
public static final long RADIATION_DURATION_MS = 24L * 60L * 60L * 1000L;
    // 每多�?tick 清理一次过期辐射方法
private static final int CLEANUP_INTERVAL = 1200; // 每分钟清理一�?
    // 使用世界维度ID作为键，值是该世界中的辐射方块位置和创建时间
    private static final Map<String, Map<BlockPos, Long>> WORLD_RADIATION_BLOCKS = new ConcurrentHashMap<>();

    /**
     * 注册辐射方块
     * @param level 世界
     * @param pos 辐射方块位置
     */
    public static void registerRadiationBlock(Level level, BlockPos pos) {
        String worldKey = getWorldKey(level);
        WORLD_RADIATION_BLOCKS.computeIfAbsent(worldKey, k -> new ConcurrentHashMap<>())
                              .put(pos.immutable(), System.currentTimeMillis());
    }

    /**
     * 批量注册辐射方块（用于核爆炸�
 * @param level 世界
     * @param positions 辐射方块位置列表
     */
    public static void registerRadiationBlocks(Level level, Iterable<BlockPos> positions) {
        String worldKey = getWorldKey(level);
        long currentTime = System.currentTimeMillis();
        Map<BlockPos, Long> worldBlocks = WORLD_RADIATION_BLOCKS.computeIfAbsent(worldKey, k -> new ConcurrentHashMap<>());

        for (BlockPos pos : positions) {
            worldBlocks.put(pos.immutable(), currentTime);
        }
    }

    /**
     * 检查辐射方块是否还在有效期�
 * @param level 世界
     * @param pos 位置
     * @return 是否有效
     */
    public static boolean isRadiationBlockValid(Level level, BlockPos pos) {
        String worldKey = getWorldKey(level);
        Map<BlockPos, Long> worldBlocks = WORLD_RADIATION_BLOCKS.get(worldKey);
        if (worldBlocks == null) {
            return false;
        }

        Long creationTime = worldBlocks.get(pos.immutable());
        if (creationTime == null) {
            return false;
        }

        return System.currentTimeMillis() - creationTime < RADIATION_DURATION_MS;
    }

    /**
     * 移除辐射方块记录
     * @param level 世界
     * @param pos 位置
     */
    public static void removeRadiationBlock(Level level, BlockPos pos) {
        String worldKey = getWorldKey(level);
        Map<BlockPos, Long> worldBlocks = WORLD_RADIATION_BLOCKS.get(worldKey);
        if (worldBlocks != null) {
            worldBlocks.remove(pos.immutable());
        }
    }

    /**
     * 获取世界的唯一�
 */
    private static String getWorldKey(Level level) {
        return level.dimension().location().toString();
    }

    /**
     * 定期清理过期的辐射方法
 */
    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Pre event) {
        // 只在服务端执�
    if (event.getLevel().isClientSide) {
            return;
        }

        // 每分钟检查一�
    if (event.getLevel().getGameTime() % CLEANUP_INTERVAL != 0) {
            return;
        }

        long currentTime = System.currentTimeMillis();
        long cutoffTime = currentTime - RADIATION_DURATION_MS;

        // 清理过期方块
        for (Map.Entry<String, Map<BlockPos, Long>> worldEntry : WORLD_RADIATION_BLOCKS.entrySet()) {
            Map<BlockPos, Long> worldBlocks = worldEntry.getValue();
            Iterator<Map.Entry<BlockPos, Long>> iterator = worldBlocks.entrySet().iterator();

            while (iterator.hasNext()) {
                Map.Entry<BlockPos, Long> entry = iterator.next();
                if (entry.getValue() < cutoffTime) {
                    iterator.remove();
                }
            }
        }
    }

    /**
     * 清理世界中已过期或已被替换的辐射方块
     * 在世界加载时调用，确保数据一致�
 */
    public static void cleanupInvalidBlocks(Level level) {
        String worldKey = getWorldKey(level);
        Map<BlockPos, Long> worldBlocks = WORLD_RADIATION_BLOCKS.get(worldKey);
        if (worldBlocks == null) {
            return;
        }

        Iterator<Map.Entry<BlockPos, Long>> iterator = worldBlocks.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<BlockPos, Long> entry = iterator.next();
            BlockPos pos = entry.getKey();

            // 检查方块是否还是辐射方法
        if (!level.getBlockState(pos).getBlock().equals(mio_icif_blocks.BLOCK_RADIATING_STONE.get()) &&
                !level.getBlockState(pos).getBlock().equals(mio_icif_blocks.BLOCK_RADIATING_DIRT.get()) &&
                !level.getBlockState(pos).getBlock().equals(mio_icif_blocks.BLOCK_RADIATING_DEEPSLATE.get())) {
                // 方块已被替换或删除，移除记录
                iterator.remove();
            }
        }
    }

    /**
     * 获取指定世界中辐射方块的数量
     */
    public static int getRadiationBlockCount(Level level) {
        String worldKey = getWorldKey(level);
        Map<BlockPos, Long> worldBlocks = WORLD_RADIATION_BLOCKS.get(worldKey);
        return worldBlocks != null ? worldBlocks.size() : 0;
    }

    /**
     * 获取辐射方块的剩余时间（毫秒�
 * @return 剩余时间，如果方块不存在或已过期返回0
     */
    public static long getRemainingTime(Level level, BlockPos pos) {
        String worldKey = getWorldKey(level);
        Map<BlockPos, Long> worldBlocks = WORLD_RADIATION_BLOCKS.get(worldKey);
        if (worldBlocks == null) {
            return 0;
        }

        Long creationTime = worldBlocks.get(pos.immutable());
        if (creationTime == null) {
            return 0;
        }

        long remaining = RADIATION_DURATION_MS - (System.currentTimeMillis() - creationTime);
        return Math.max(0, remaining);
    }
}

