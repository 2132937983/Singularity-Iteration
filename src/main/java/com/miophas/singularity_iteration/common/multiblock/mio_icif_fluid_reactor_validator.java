// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.multiblock;

import com.miophas.singularity_iteration.common.blockentity.generator.mio_icif_nuclear_reactor_generator;
import com.miophas.singularity_iteration.common.block.generator.mio_icif_Block_Nuclear_Reactor_Generator;
import com.miophas.singularity_iteration.common.block.reactor.mio_icif_Block_Reactor_Access_Hatch;
import com.miophas.singularity_iteration.common.block.reactor.mio_icif_Block_Reactor_Fluid_Port;
import com.miophas.singularity_iteration.common.block.reactor.mio_icif_Block_Reactor_Chamber;
import com.miophas.singularity_iteration.common.block.reactor.mio_icif_Block_reactorvessel;
import com.miophas.singularity_iteration.common.block.reactor.mio_icif_block_reactor_redstone_port;
import java.util.HashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/** Loaded-only shell validation. R119 observes six chambers, a closed shell and a working redstone port. */
public class mio_icif_fluid_reactor_validator implements mio_icif_multiblock_validator {
    public static final int STRUCTURE_RADIUS=2;
    @Override public mio_icif_multiblock_validation_result validate(Level level,BlockPos center){
        for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++)
            if(!level.getChunkSource().hasChunk((center.getX()+x)>>4,(center.getZ()+z)>>4))return mio_icif_multiblock_validation_result.failure("Structure chunk unavailable");
        if(!(level.getBlockState(center).getBlock() instanceof mio_icif_Block_Nuclear_Reactor_Generator))return mio_icif_multiblock_validation_result.failure("Missing reactor");
        var members=new HashSet<BlockPos>();members.add(center);
        for(var side:Direction.values()){
            var at=center.relative(side);if(!(level.getBlockState(at).getBlock() instanceof mio_icif_Block_Reactor_Chamber))return mio_icif_multiblock_validation_result.failure("Missing chamber");members.add(at);
        }
        var redstone=new HashSet<BlockPos>();int fluids=0,hatches=0;
        for(int x=-2;x<=2;x++)for(int y=-2;y<=2;y++)for(int z=-2;z<=2;z++){
            if(Math.max(Math.max(Math.abs(x),Math.abs(y)),Math.abs(z))!=2)continue;
            var at=center.offset(x,y,z);Block block=level.getBlockState(at).getBlock();
            if(!(block instanceof mio_icif_Block_reactorvessel)&&!functional(block))return mio_icif_multiblock_validation_result.failure("Incomplete vessel shell");
            members.add(at);
            if(block instanceof mio_icif_Block_Reactor_Fluid_Port)fluids++;
            if(block instanceof mio_icif_Block_Reactor_Access_Hatch)hatches++;
            if(block instanceof mio_icif_block_reactor_redstone_port)redstone.add(at);
        }
        if(fluids+hatches+redstone.size()==0)return mio_icif_multiblock_validation_result.failure("Missing functional port");
        return mio_icif_multiblock_validation_result.builder().setValid(true).addBlocks(members)
            .setStructureData("fluidPortCount",fluids).setStructureData("accessHatchCount",hatches)
            .setStructureData("redstonePortCount",redstone.size()).setStructureData("redstonePortPositions",redstone).build();
    }
    private static boolean functional(Block block){return block instanceof mio_icif_Block_Reactor_Fluid_Port||block instanceof mio_icif_Block_Reactor_Access_Hatch||block instanceof mio_icif_block_reactor_redstone_port;}
    @Override @SuppressWarnings("unchecked") public void onStructureFormed(Level level,BlockPos center,mio_icif_multiblock_manager<?> manager){
        if(level.getBlockEntity(center) instanceof mio_icif_nuclear_reactor_generator reactor)reactor.setFluidReactorMultiblock((mio_icif_multiblock_manager<mio_icif_fluid_reactor_validator>)manager);
    }
    @Override public void onStructureBroken(Level level,BlockPos center,mio_icif_multiblock_manager<?> manager){
        if(level.getBlockEntity(center) instanceof mio_icif_nuclear_reactor_generator reactor)reactor.onMultiblockBroken();
    }
    @Override public String getStructureName(){return "FluidReactor5x5x5";}
    public static boolean isValidStructureBlock(Block block){return functional(block)||block instanceof mio_icif_Block_reactorvessel||block instanceof mio_icif_Block_Reactor_Chamber||block instanceof mio_icif_Block_Nuclear_Reactor_Generator;}
}
