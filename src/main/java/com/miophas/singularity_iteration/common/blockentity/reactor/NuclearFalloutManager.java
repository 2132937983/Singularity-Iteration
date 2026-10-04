package com.miophas.singularity_iteration.common.blockentity.reactor;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.common.reactor.ExplosionWorkScheduler;
import com.miophas.singularity_iteration.core.runtime.radiation.NuclearFalloutState;
import com.miophas.singularity_iteration.core.runtime.radiation.RadiationState;
import java.util.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Deferred fallout resumes on chunk load without force-loading terrain or replaying pressure damage. */
@EventBusSubscriber(modid=Singularity_Iteration.MOD_ID)
public final class NuclearFalloutManager {
    private record Key(ServerLevel level,UUID region,long chunk){}
    private static final Map<ServerLevel,Set<Long>> LOADED=new IdentityHashMap<>();
    private static final Set<Key> PENDING=new LinkedHashSet<>(),ACTIVE=new HashSet<>();
    private NuclearFalloutManager(){}
    public static void register(ServerLevel level,UUID id,Vec3 center,int radius,int radiusY){
        var data=NuclearFalloutState.get(level);data.regions.put(id,new NuclearFalloutState.Region(center,radius,radiusY,System.currentTimeMillis()));data.setDirty();
    }
    public static void ready(ServerLevel level,UUID id){
        var data=NuclearFalloutState.get(level);var region=data.regions.get(id);if(region==null)return;
        region.ready=true;data.setDirty();
        for(long chunk:LOADED.getOrDefault(level,Set.of()))queue(level,id,region,chunk);
    }
    public static void remove(ServerLevel level,UUID id){
        var data=NuclearFalloutState.get(level);if(data.regions.remove(id)!=null)data.setDirty();PENDING.removeIf(key->key.level==level&&key.region.equals(id));
    }
    /** Explicit clearing cancels pending physical cleanup as well as the lingering radiation zone. */
    public static void clear(ServerLevel level){
        var data=NuclearFalloutState.get(level);data.regions.clear();data.setDirty();PENDING.removeIf(key->key.level==level);
    }
    private static void queue(ServerLevel level,UUID id,NuclearFalloutState.Region region,long chunk){
        var key=new Key(level,id,chunk);
        if(region.ready&&region.intersectsChunk(ChunkPos.getX(chunk),ChunkPos.getZ(chunk))&&region.columns.getOrDefault(chunk,0)<256&&!ACTIVE.contains(key))PENDING.add(key);
    }
    /** Only records an address here; FULL-chunk access is delayed to the server tick. */
    public static void loaded(ServerLevel level,long chunk){
        LOADED.computeIfAbsent(level,ignored->new LinkedHashSet<>()).add(chunk);
        var data=NuclearFalloutState.get(level);data.regions.forEach((id,region)->queue(level,id,region,chunk));
    }
    @SubscribeEvent public static void load(ChunkEvent.Load event){
        if(event.getLevel() instanceof ServerLevel level&&event.getChunk() instanceof LevelChunk){
            long chunk=event.getChunk().getPos().toLong();
            if(level.getServer().isSameThread())loaded(level,chunk);else level.getServer().execute(()->loaded(level,chunk));
        }
    }
    @SubscribeEvent public static void unloadChunk(ChunkEvent.Unload event){
        if(event.getLevel() instanceof ServerLevel level){
            long chunk=event.getChunk().getPos().toLong();Runnable remove=()->{
                var loaded=LOADED.get(level);if(loaded!=null)loaded.remove(chunk);
                PENDING.removeIf(key->key.level==level&&key.chunk==chunk);
            };
            if(level.getServer().isSameThread())remove.run();else level.getServer().execute(remove);
        }
    }
    @SubscribeEvent public static void tick(ServerTickEvent.Pre event){
        if(!com.miophas.singularity_iteration.common.Singularity_Iteration_Config.ENABLE_NUCLEAR_EXPLOSION.get())return;
        // A load event does not guarantee immediate FULL availability, and an address can
        // remain tracked while its chunk waits for deferred unloading. Rotate retries so
        // those addresses cannot monopolize the bounded probe budget as players travel.
        int probes=0,limit=Math.min(32,PENDING.size());
        var retry=new ArrayList<Key>();var it=PENDING.iterator();
        while(it.hasNext()&&probes++<limit&&ACTIVE.size()<16){
            var key=it.next();it.remove();
            if(key.level.getServer()!=event.getServer()){retry.add(key);continue;}
            var data=NuclearFalloutState.get(key.level);var region=data.regions.get(key.region);
            if(region==null||region.columns.getOrDefault(key.chunk,0)>=256)continue;
            var chunk=key.level.getChunkSource().getChunkNow(ChunkPos.getX(key.chunk),ChunkPos.getZ(key.chunk));
            if(chunk==null){retry.add(key);continue;}
            if(!ExplosionWorkScheduler.hasCapacity(key.level)){retry.add(key);break;}
            var task=new NuclearFalloutChunkTask(key.level,region,chunk,data,()->data.regions.get(key.region)==region);
            if(ExplosionWorkScheduler.trySubmitCleanup(key.level,net.minecraft.core.BlockPos.containing(region.center),NuclearFalloutManager.class,task,()->{
                try{boolean done=task.getAsBoolean();if(done){ACTIVE.remove(key);if(region.columns.getOrDefault(key.chunk,0)<256&&LOADED.getOrDefault(key.level,Set.of()).contains(key.chunk))PENDING.add(key);}return done;}
                catch(RuntimeException failure){ACTIVE.remove(key);throw failure;}
            })){ACTIVE.add(key);}else retry.add(key);
        }
        PENDING.addAll(retry);
        if(event.getServer().getTickCount()%1200==0)for(var level:event.getServer().getAllLevels()){
            var data=NuclearFalloutState.get(level);boolean changed=data.regions.entrySet().removeIf(entry->entry.getValue().complete()&&(!RadiationState.get(level).zones.containsKey(entry.getKey())||RadiationState.expired(entry.getValue().created,System.currentTimeMillis())));if(changed)data.setDirty();
        }
    }
    @SubscribeEvent public static void unload(LevelEvent.Unload event){
        if(event.getLevel() instanceof ServerLevel level){LOADED.remove(level);PENDING.removeIf(key->key.level==level);ACTIVE.removeIf(key->key.level==level);}
    }
    @SubscribeEvent public static void stopped(ServerStoppedEvent event){
        LOADED.keySet().removeIf(level->level.getServer()==event.getServer());PENDING.removeIf(key->key.level.getServer()==event.getServer());ACTIVE.removeIf(key->key.level.getServer()==event.getServer());
    }
}
