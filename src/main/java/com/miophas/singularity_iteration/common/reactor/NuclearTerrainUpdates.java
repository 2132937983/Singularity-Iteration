package com.miophas.singularity_iteration.common.reactor;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import it.unimi.dsi.fastutil.shorts.ShortOpenHashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.FullChunkStatus;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

/** Vanilla queues block changes only for ticking chunks, even when FULL chunks are visible. */
@EventBusSubscriber(modid=Singularity_Iteration.MOD_ID)
public final class NuclearTerrainUpdates {
    private record Key(ServerLevel level, SectionPos section) { }
    private static final Map<MinecraftServer,LinkedHashMap<Key,ShortOpenHashSet>> DIRTY=new IdentityHashMap<>();
    public static final int MAX_SECTIONS_PER_TICK=16;
    private NuclearTerrainUpdates() { }

    public static boolean replace(ServerLevel level,BlockPos pos,BlockState state,int flags) {
        var chunk=level.getChunkSource().getChunkNow(pos.getX()>>4,pos.getZ()>>4);
        if(chunk==null)return false;
        var before=chunk.getBlockState(pos);
        boolean changed=level.setBlock(pos,state,flags);
        var actual=chunk.getBlockState(pos);
        if(changed&&actual!=before){
            var status=chunk.getFullStatus();
            if(status==null||!status.isOrAfter(FullChunkStatus.BLOCK_TICKING)){
                var key=new Key(level,SectionPos.of(pos));
                DIRTY.computeIfAbsent(level.getServer(),ignored->new LinkedHashMap<>())
                    .computeIfAbsent(key,ignored->new ShortOpenHashSet()).add(SectionPos.sectionRelativePos(pos));
            }
        }
        return changed;
    }

    /** Snapshot final states, batch positions per section, and send only to actual chunk watchers. */
    public static int flush(MinecraftServer server,long maxNanos) {
        var dirty=DIRTY.get(server);
        if(dirty==null||maxNanos<=0)return 0;
        int processed=0;long start=System.nanoTime();var it=dirty.entrySet().iterator();
        while(it.hasNext()&&processed<MAX_SECTIONS_PER_TICK&&(processed==0||System.nanoTime()-start<maxNanos)){
            var entry=it.next();var key=entry.getKey();it.remove();processed++;
            int x=key.section.x(),z=key.section.z();
            var chunk=key.level.getChunkSource().getChunkNow(x,z);
            if(chunk==null)continue; // A subsequent full chunk packet carries the saved state.
            var players=key.level.getChunkSource().chunkMap.getPlayers(new ChunkPos(x,z),false);
            if(players.isEmpty())continue;
            var packet=new ClientboundSectionBlocksUpdatePacket(key.section,entry.getValue(),
                chunk.getSection(key.level.getSectionIndexFromSectionY(key.section.y())));
            for(var player:players)player.connection.send(packet);
        }
        if(dirty.isEmpty())DIRTY.remove(server);
        return processed;
    }

    public static int pendingSections(MinecraftServer server) {
        var dirty=DIRTY.get(server);return dirty==null?0:dirty.size();
    }
    @SubscribeEvent public static void unload(LevelEvent.Unload event) {
        if(event.getLevel() instanceof ServerLevel level){
            var dirty=DIRTY.get(level.getServer());
            if(dirty!=null){dirty.keySet().removeIf(key->key.level==level);if(dirty.isEmpty())DIRTY.remove(level.getServer());}
        }
    }
    @SubscribeEvent public static void stopped(ServerStoppedEvent event) { DIRTY.remove(event.getServer()); }
}
