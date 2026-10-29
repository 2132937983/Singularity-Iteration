// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.runtime.radiation;

import com.miophas.singularity_iteration.core.runtime.reactor.BlastInput;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;

/** Per-world circular fallout footprints. Only visited chunks allocate progress records. */
public final class NuclearFalloutState extends SavedData {
    public static final class Region {
        public final Vec3 center;
        public final int radius, radiusY;
        private final int expectedChunks;
        public final long created;
        public boolean ready;
        // Sequential completed columns (x low four bits, z next four bits); 256 means done.
        public final Map<Long,Integer> columns = new LinkedHashMap<>();
        /**
         * 0.1.10.36: the blast turns deepslate ores into iridium ore (set for new blasts only: a region saved by an older
         * version never transmutes). {@link #ores} holds the sections done per chunk; {@link #ORES_DONE} when the chunk is done.
         */
        public boolean transmute;
        public final Map<Long,Integer> ores = new LinkedHashMap<>();
        public static final int ORES_DONE = 1 << 16;
        public Region(Vec3 center,int radius,int radiusY,long created){
            BlastInput.nuke(center.x,center.y,center.z,1,radius);
            if(radius<=0||radiusY<=0||radiusY>radius)throw new IllegalArgumentException("Invalid fallout shape");
            this.center=center;this.radius=radius;this.radiusY=radiusY;this.created=created;
            double r=radius*1.5;int count=0;
            int firstZ=((int)Math.ceil(center.z-r-.5))>>4,lastZ=((int)Math.floor(center.z+r-.5))>>4;
            for(int z=firstZ;z<=lastZ;z++){
                double dz=Math.max(z*16.0+.5-center.z,Math.max(0,center.z-(z*16.0+15.5)));
                double span=Math.sqrt(Math.max(0,r*r-dz*dz));
                int firstX=((int)Math.ceil(center.x-span-.5))>>4,lastX=((int)Math.floor(center.x+span-.5))>>4;
                count+=Math.max(0,lastX-firstX+1);
            }expectedChunks=count;
        }
        public boolean containsColumn(int x,int z){
            double dx=x+.5-center.x,dz=z+.5-center.z,r=radius*1.5;
            return dx*dx+dz*dz<=r*r;
        }
        public boolean intersectsChunk(int x,int z){
            double dx=Math.max(x*16.0+.5-center.x,Math.max(0,center.x-(x*16.0+15.5)));
            double dz=Math.max(z*16.0+.5-center.z,Math.max(0,center.z-(z*16.0+15.5)));
            double r=radius*1.5;return dx*dx+dz*dz<=r*r;
        }
        public boolean complete(){
            if(columns.size()<expectedChunks)return false;
            for(int progress:columns.values())if(progress<256)return false;
            if(transmute)for(long chunk:columns.keySet())if(ores.getOrDefault(chunk,0)<ORES_DONE)return false;
            return true;
        }
        /** 0.1.10.36: the chunk has no fallout work left (columns, then the ore transmutation of a new blast). */
        public boolean chunkDone(long chunk){
            return columns.getOrDefault(chunk,0)>=256&&(!transmute||ores.getOrDefault(chunk,0)>=ORES_DONE);
        }
        /** 0.1.10.36: true when (x,y,z) is inside the blast (the ellipsoid of radius and radiusY, not the 1.5x fallout footprint). */
        public boolean inBlast(int x,int y,int z){
            double dx=(x+.5-center.x)/radius,dy=(y+.5-center.y)/radiusY,dz=(z+.5-center.z)/radius;
            return dx*dx+dy*dy+dz*dz<=1;
        }
        /** 0.1.10.36: the chunk column meets the blast ellipsoid's horizontal circle. */
        public boolean chunkInBlast(int chunkX,int chunkZ){
            double dx=Math.max(chunkX*16.0+.5-center.x,Math.max(0,center.x-(chunkX*16.0+15.5)));
            double dz=Math.max(chunkZ*16.0+.5-center.z,Math.max(0,center.z-(chunkZ*16.0+15.5)));
            return dx*dx+dz*dz<=(double)radius*radius;
        }
        public boolean processedColumn(int x,int z){
            return containsColumn(x,z)&&columns.getOrDefault(ChunkPos.asLong(x>>4,z>>4),0)>((z&15)*16+(x&15));
        }
    }
    public final Map<UUID,Region> regions=new LinkedHashMap<>();
    public static NuclearFalloutState get(ServerLevel level){
        return level.getDataStorage().computeIfAbsent(new SavedData.Factory<>(NuclearFalloutState::new,NuclearFalloutState::load),"si_nuclear_fallout");
    }
    public static NuclearFalloutState load(CompoundTag tag,HolderLookup.Provider registries){
        var data=new NuclearFalloutState();
        for(Tag value:tag.getList("regions",Tag.TAG_COMPOUND)){
            var row=(CompoundTag)value;if(!row.hasUUID("id"))continue;
            try{
                var region=new Region(new Vec3(row.getDouble("x"),row.getDouble("y"),row.getDouble("z")),row.getInt("radius"),row.getInt("radius_y"),row.getLong("created"));
                // Pressure tasks are transient. Reload resumes fallout, never another blast.
                region.ready=true;
                region.transmute=row.getBoolean("transmute");   // 0.1.10.36; older saves: false
                for(Tag entry:row.getList("chunks",Tag.TAG_COMPOUND)){
                    var chunk=(CompoundTag)entry;long key=chunk.getLong("pos");int cursor=chunk.getInt("columns");
                    if(cursor>0&&cursor<=256&&region.intersectsChunk(ChunkPos.getX(key),ChunkPos.getZ(key)))region.columns.put(key,cursor);
                    int ores=chunk.getInt("ores");
                    if(ores>0&&ores<=Region.ORES_DONE&&region.columns.containsKey(key))region.ores.put(key,ores);
                }
                data.regions.put(row.getUUID("id"),region);
            }catch(IllegalArgumentException ignored){/* Reject malformed footprints. */}
        }
        return data;
    }
    @Override public CompoundTag save(CompoundTag tag,HolderLookup.Provider registries){
        var list=new ListTag();regions.forEach((id,region)->{
            var row=new CompoundTag();row.putUUID("id",id);row.putDouble("x",region.center.x);row.putDouble("y",region.center.y);row.putDouble("z",region.center.z);
            row.putInt("radius",region.radius);row.putInt("radius_y",region.radiusY);row.putLong("created",region.created);
            if(region.transmute)row.putBoolean("transmute",true);
            var chunks=new ListTag();region.columns.forEach((key,cursor)->{var chunk=new CompoundTag();chunk.putLong("pos",key);chunk.putInt("columns",cursor);
                int ores=region.ores.getOrDefault(key,0);if(ores>0)chunk.putInt("ores",ores);chunks.add(chunk);});
            row.put("chunks",chunks);list.add(row);
        });tag.put("regions",list);return tag;
    }
}
