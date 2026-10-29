package com.miophas.singularity_iteration.common.blockentity.reactor;

import com.miophas.singularity_iteration.common.registry.mio_icif_blocks;
import com.miophas.singularity_iteration.common.reactor.NuclearTerrainUpdates;
import com.miophas.singularity_iteration.common.reactor.NuclearThermalEffects;
import com.miophas.singularity_iteration.core.runtime.radiation.NuclearFalloutState;
import java.util.function.BooleanSupplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.common.IShearable;

/** One loaded chunk, one block/palette/cursor operation per shared scheduler step. */
public final class NuclearFalloutChunkTask implements BooleanSupplier {
    // Avoid indirect neighbour shape destruction outside the circular footprint and vegetation drops.
    private static final int REPLACE_FLAGS=Block.UPDATE_CLIENTS|Block.UPDATE_KNOWN_SHAPE|Block.UPDATE_SUPPRESS_DROPS;
    private final ServerLevel level;
    private final NuclearFalloutState.Region region;
    private final NuclearFalloutState saved;
    private final LevelChunk chunk;
    private final long chunkKey;
    private final BooleanSupplier valid;
    private final boolean[] cleanupSections;
    private int sectionCursor,column,y,minY,maxY;
    private boolean scanning,groundDone,done;
    public NuclearFalloutChunkTask(ServerLevel level,NuclearFalloutState.Region region,LevelChunk chunk,NuclearFalloutState saved,BooleanSupplier valid){
        this.level=level;this.region=region;this.chunk=chunk;this.saved=saved;this.valid=valid;
        chunkKey=chunk.getPos().toLong();column=region.columns.getOrDefault(chunkKey,0);cleanupSections=new boolean[chunk.getSections().length];
        minY=Math.max(level.getMinBuildHeight(),(int)Math.ceil(region.center.y-region.radiusY-.5));
        maxY=Math.min(level.getMaxBuildHeight()-1,(int)Math.floor(region.center.y+region.radiusY-.5));
    }
    /** Leaves and fragile vegetation, including compatible modded shearable blocks; no drops. */
    public static boolean vegetation(BlockState state){
        Block block=state.getBlock();
        return state.is(BlockTags.LEAVES)||state.is(BlockTags.FLOWERS)||block instanceof BushBlock
            ||block instanceof GrowingPlantBlock||block instanceof VineBlock||block instanceof MultifaceBlock
            ||block instanceof IShearable||state.is(Blocks.MOSS_CARPET)||block instanceof HangingRootsBlock||state.is(Blocks.COBWEB);
    }
    @Override public boolean getAsBoolean(){
        if(done)return true;
        if(!valid.getAsBoolean()||level.getChunkSource().getChunkNow(chunk.getPos().x,chunk.getPos().z)!=chunk){done=true;return true;}
        if(!com.miophas.singularity_iteration.common.Singularity_Iteration_Config.ENABLE_NUCLEAR_EXPLOSION.get())return false;
        if(sectionCursor<cleanupSections.length){
            var section=chunk.getSections()[sectionCursor];cleanupSections[sectionCursor++]=!section.hasOnlyAir()
                &&section.maybeHas(state->vegetation(state)||NuclearThermalEffects.frozen(state));return false;
        }
        if(column>=256)return transmuteStep();
        int x=chunk.getPos().getMinBlockX()+(column&15),z=chunk.getPos().getMinBlockZ()+(column>>4);
        if(!scanning){
            if(!region.containsColumn(x,z)){completeColumn();return false;}
            y=Math.min(maxY,chunk.getHeight(Heightmap.Types.WORLD_SURFACE,x&15,z&15));groundDone=false;scanning=true;
            if(y<minY)completeColumn();return false;
        }
        if(y<minY){completeColumn();return false;}
        int sectionIndex=level.getSectionIndex(y);
        if(groundDone&&!cleanupSections[sectionIndex]){y=Math.max(minY-1,Math.floorDiv(y,16)*16-1);return false;}
        var at=new BlockPos(x,y--,z);var state=chunk.getBlockState(at);
        if(NuclearThermalEffects.frozen(state)){
            NuclearTerrainUpdates.replace(level,at,NuclearThermalEffects.melted(level,state),REPLACE_FLAGS);
        }else if(vegetation(state)){
            // Preserve water in waterlogged vegetation; do not create item entities across the fallout area.
            NuclearTerrainUpdates.replace(level,at,state.getFluidState().createLegacyBlock(),REPLACE_FLAGS);
        }else if(!groundDone&&!state.isAir()&&state.getFluidState().isEmpty()&&!state.is(BlockTags.LOGS)){
            groundDone=true;
            Block replacement=radioactive(state.getBlock());
            double dx=x+.5-region.center.x,dz=z+.5-region.center.z;
            if(state.is(Blocks.SAND)&&dx*dx+dz*dz<region.radius*(double)region.radius*.64)replacement=Blocks.GLASS;
            if(replacement!=null)NuclearTerrainUpdates.replace(level,at,replacement.defaultBlockState(),REPLACE_FLAGS);
        }
        return false;
    }
    private void completeColumn(){scanning=false;region.columns.put(chunkKey,++column);if(saved!=null)saved.setDirty();}

    // ------------------------------------------------------------------ 0.1.10.36: deepslate ores -> deepslate iridium ore

    /** The deepslate ores a blast transmutes ({@code #c:ores_in_ground/deepslate} by default; the iridium ore itself never). */
    public static final TagKey<Block> TRANSMUTABLE=TagKey.create(Registries.BLOCK,ResourceLocation.fromNamespaceAndPath("mio_icif","nuke_transmutable"));
    /** Blocks of one section looked at per scheduler step. */
    private static final int TRANSMUTE_BATCH=256;
    private int oreIndex;
    public static boolean transmutable(BlockState state){
        return state.is(TRANSMUTABLE)&&!state.is(mio_icif_blocks.BLOCK_ORE_IRIDIUM_IN_DEEP.get());
    }
    /**
     * After the columns of the chunk: every transmutable ore inside the blast ellipsoid (radius, radiusY; underground too)
     * becomes deepslate iridium ore. A section without such an ore, or outside the blast's height, is skipped in one step
     * (palette check); the others are read {@link #TRANSMUTE_BATCH} blocks per step. The section cursor is saved, so a chunk
     * unloaded halfway resumes on its next load (a converted ore is never converted again).
     */
    private boolean transmuteStep(){
        int cursor=region.ores.getOrDefault(chunkKey,0);
        var sections=chunk.getSections();
        if(!region.transmute||cursor>=NuclearFalloutState.Region.ORES_DONE||cursor>=sections.length
                ||!region.chunkInBlast(chunk.getPos().x,chunk.getPos().z)){finishOres();done=true;return true;}
        var section=sections[cursor];
        int baseY=SectionPos.sectionToBlockCoord(level.getSectionYFromSectionIndex(cursor));
        if(oreIndex==0&&(baseY+15<region.center.y-region.radiusY-1||baseY>region.center.y+region.radiusY+1
                ||section.hasOnlyAir()||!section.maybeHas(NuclearFalloutChunkTask::transmutable))){nextOreSection(cursor);return false;}
        int minX=chunk.getPos().getMinBlockX(),minZ=chunk.getPos().getMinBlockZ();
        for(int n=0;n<TRANSMUTE_BATCH&&oreIndex<4096;n++,oreIndex++){
            int lx=oreIndex&15,lz=(oreIndex>>4)&15,ly=oreIndex>>8;
            var state=section.getBlockState(lx,ly,lz);
            if(!transmutable(state)||!region.inBlast(minX+lx,baseY+ly,minZ+lz))continue;
            NuclearTerrainUpdates.replace(level,new BlockPos(minX+lx,baseY+ly,minZ+lz),
                mio_icif_blocks.BLOCK_ORE_IRIDIUM_IN_DEEP.get().defaultBlockState(),REPLACE_FLAGS);
        }
        if(oreIndex>=4096)nextOreSection(cursor);
        return false;
    }
    private void nextOreSection(int cursor){oreIndex=0;region.ores.put(chunkKey,cursor+1);if(saved!=null)saved.setDirty();}
    private void finishOres(){region.ores.put(chunkKey,NuclearFalloutState.Region.ORES_DONE);if(saved!=null)saved.setDirty();}
    private static Block radioactive(Block block){
        if(block==Blocks.DIRT||block==Blocks.GRASS_BLOCK||block==Blocks.COARSE_DIRT||block==Blocks.PODZOL||block==Blocks.MYCELIUM||block==Blocks.ROOTED_DIRT)return mio_icif_blocks.BLOCK_RADIATING_DIRT.get();
        if(block==Blocks.STONE||block==Blocks.COBBLESTONE)return mio_icif_blocks.BLOCK_RADIATING_STONE.get();
        if(block==Blocks.DEEPSLATE||block==Blocks.COBBLED_DEEPSLATE)return mio_icif_blocks.BLOCK_RADIATING_DEEPSLATE.get();
        return null;
    }
}
