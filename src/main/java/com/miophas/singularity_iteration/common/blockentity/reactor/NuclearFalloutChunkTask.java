package com.miophas.singularity_iteration.common.blockentity.reactor;

import com.miophas.singularity_iteration.common.registry.mio_icif_blocks;
import com.miophas.singularity_iteration.core.runtime.radiation.NuclearFalloutState;
import java.util.function.BooleanSupplier;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
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
    private final boolean[] plants;
    private int sectionCursor,column,y,minY,maxY;
    private boolean scanning,groundDone,done;
    public NuclearFalloutChunkTask(ServerLevel level,NuclearFalloutState.Region region,LevelChunk chunk,NuclearFalloutState saved,BooleanSupplier valid){
        this.level=level;this.region=region;this.chunk=chunk;this.saved=saved;this.valid=valid;
        chunkKey=chunk.getPos().toLong();column=region.columns.getOrDefault(chunkKey,0);plants=new boolean[chunk.getSections().length];
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
        if(sectionCursor<plants.length){
            var section=chunk.getSections()[sectionCursor];plants[sectionCursor++]=!section.hasOnlyAir()&&section.maybeHas(NuclearFalloutChunkTask::vegetation);return false;
        }
        if(column>=256){done=true;return true;}
        int x=chunk.getPos().getMinBlockX()+(column&15),z=chunk.getPos().getMinBlockZ()+(column>>4);
        if(!scanning){
            if(!region.containsColumn(x,z)){completeColumn();return false;}
            y=Math.min(maxY,chunk.getHeight(Heightmap.Types.WORLD_SURFACE,x&15,z&15));groundDone=false;scanning=true;
            if(y<minY)completeColumn();return false;
        }
        if(y<minY){completeColumn();return false;}
        int sectionIndex=level.getSectionIndex(y);
        if(groundDone&&!plants[sectionIndex]){y=Math.max(minY-1,Math.floorDiv(y,16)*16-1);return false;}
        var at=new BlockPos(x,y--,z);var state=chunk.getBlockState(at);
        if(vegetation(state)){
            // Preserve water in waterlogged vegetation; do not create item entities across the fallout area.
            level.setBlock(at,state.getFluidState().createLegacyBlock(),REPLACE_FLAGS);
        }else if(!groundDone&&!state.isAir()&&state.getFluidState().isEmpty()&&!state.is(BlockTags.LOGS)){
            groundDone=true;
            Block replacement=radioactive(state.getBlock());
            double dx=x+.5-region.center.x,dz=z+.5-region.center.z;
            if(state.is(Blocks.SAND)&&dx*dx+dz*dz<region.radius*(double)region.radius*.64)replacement=Blocks.GLASS;
            if(replacement!=null)level.setBlock(at,replacement.defaultBlockState(),REPLACE_FLAGS);
        }
        return false;
    }
    private void completeColumn(){scanning=false;region.columns.put(chunkKey,++column);if(saved!=null)saved.setDirty();}
    private static Block radioactive(Block block){
        if(block==Blocks.DIRT||block==Blocks.GRASS_BLOCK||block==Blocks.COARSE_DIRT||block==Blocks.PODZOL||block==Blocks.MYCELIUM||block==Blocks.ROOTED_DIRT)return mio_icif_blocks.BLOCK_RADIATING_DIRT.get();
        if(block==Blocks.STONE||block==Blocks.COBBLESTONE)return mio_icif_blocks.BLOCK_RADIATING_STONE.get();
        if(block==Blocks.DEEPSLATE||block==Blocks.COBBLED_DEEPSLATE)return mio_icif_blocks.BLOCK_RADIATING_DEEPSLATE.get();
        return null;
    }
}
