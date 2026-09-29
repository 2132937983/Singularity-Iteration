package com.miophas.singularity_iteration.common.event;

import com.miophas.singularity_iteration.common.registry.mio_icif_blocks;
import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.core.runtime.radiation.RadiationState;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/** Per-save radiation block timestamps, retained across JVM restarts. */
@EventBusSubscriber(modid = Singularity_Iteration.MOD_ID)
public class mio_icif_RadiationBlockManager {
    public static final long RADIATION_DURATION_MS = RadiationState.DURATION;

    public static void registerRadiationBlock(Level level, BlockPos pos) {
        if (level instanceof ServerLevel server) {
            var data = RadiationState.get(server);
            data.blocks.put(pos.asLong(), System.currentTimeMillis()); data.setDirty();
        }
    }
    public static void registerRadiationBlocks(Level level, Iterable<BlockPos> positions) {
        if (level instanceof ServerLevel server) {
            var data = RadiationState.get(server); long now = System.currentTimeMillis();
            for (var pos : positions) data.blocks.put(pos.asLong(), now);
            data.setDirty();
        }
    }
    public static boolean isRadiationBlockValid(Level level, BlockPos pos) { return getRemainingTime(level, pos) > 0; }
    public static void removeRadiationBlock(Level level, BlockPos pos) {
        if (level instanceof ServerLevel server) {
            var data = RadiationState.get(server);
            if (data.blocks.remove(pos.asLong()) != null) data.setDirty();
        }
    }
    @SubscribeEvent public static void onLevelTick(LevelTickEvent.Pre event) {
        if (event.getLevel() instanceof ServerLevel server && server.getGameTime() % 1200 == 0)
            RadiationState.get(server).clean(System.currentTimeMillis());
    }
    public static void cleanupInvalidBlocks(Level level) {
        if (!(level instanceof ServerLevel server)) return;
        var data = RadiationState.get(server);
        var iterator = data.blocks.keySet().iterator();
        while (iterator.hasNext()) {
            var pos = BlockPos.of(iterator.next());
            var chunk = server.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
            if (chunk == null) continue;
            var block = chunk.getBlockState(pos).getBlock();
            if (block != mio_icif_blocks.BLOCK_RADIATING_STONE.get() && block != mio_icif_blocks.BLOCK_RADIATING_DIRT.get()
                    && block != mio_icif_blocks.BLOCK_RADIATING_DEEPSLATE.get()) { iterator.remove(); data.setDirty(); }
        }
    }
    public static int getRadiationBlockCount(Level level) {
        return level instanceof ServerLevel server ? RadiationState.get(server).blocks.size() : 0;
    }
    public static long getRemainingTime(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel server)) return 0;
        var created = RadiationState.get(server).blocks.get(pos.asLong());
        if (created == null) return 0;
        return Math.clamp(RADIATION_DURATION_MS - (System.currentTimeMillis() - created), 0, RADIATION_DURATION_MS);
    }
}
