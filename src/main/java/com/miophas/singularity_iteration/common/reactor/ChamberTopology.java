// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.reactor;
import com.miophas.singularity_iteration.common.block.generator.mio_icif_Block_Nuclear_Reactor_Generator;
import com.miophas.singularity_iteration.common.block.reactor.mio_icif_Block_Reactor_Chamber;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;

/** Normal R125 placement/removal observations: one adjacent core, no dismantling explosion. */
public final class ChamberTopology {
 private ChamberTopology(){}
 /** -1 is an incomplete loaded neighborhood; it never authorizes orphan destruction. */
 public static int nearbyCores(LevelReader world,BlockPos at){
  int count=0;boolean missing=false;
  for(var side:Direction.values()){
   var p=at.relative(side);if(!world.hasChunk(p.getX()>>4,p.getZ()>>4)){missing=true;continue;}
   if(world.getBlockState(p).getBlock() instanceof mio_icif_Block_Nuclear_Reactor_Generator)count++;
  }return missing&&count<2?-1:count;
 }
 public static void checkAndDrop(Level world,BlockPos at){
  if(!(world instanceof ServerLevel server)||!server.getServer().isSameThread()||!world.hasChunk(at.getX()>>4,at.getZ()>>4))return;
  if(!(world.getBlockState(at).getBlock() instanceof mio_icif_Block_Reactor_Chamber))return;
  int count=nearbyCores(world,at);
  if(count==0||count>1)world.destroyBlock(at,true);
 }
}
