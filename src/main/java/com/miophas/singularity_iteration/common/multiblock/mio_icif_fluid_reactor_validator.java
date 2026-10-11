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

/**
 * Shape check for the Fluid Reactor multiblock.
 *
 * <p>The Fluid Reactor is a 5x5x5 cube with the reactor block at its center.
 * The cube covers offsets -2 to +2 on each axis. Reference: R119.
 *
 * <p>Layout. {@code R} is the reactor at {@code (0, 0, 0)}. {@code K} is a
 * chamber position. {@code S} is a shell position. The validator does not check
 * the positions marked {@code .}. The sketch shows the middle layer (Y = 0).
 * The layers Y = -1 and Y = +1 have {@code K} only at the center and {@code .} at
 * the other inner positions. The layers Y = -2 and Y = +2 are fully {@code S}.
 * <pre>
 *   S S S S S
 *   S . K . S
 *   S K R K S
 *   S . K . S
 *   S S S S S
 * </pre>
 *
 * <p>Rules:
 * <ul>
 *   <li>All chunks that the cube touches must be loaded. The validator does not load chunks.</li>
 *   <li>The center must be a {@code mio_icif_Block_Nuclear_Reactor_Generator}.</li>
 *   <li>The 6 face neighbors of the reactor must be {@code mio_icif_Block_Reactor_Chamber}.</li>
 *   <li>The 98 shell positions (the outer layer of the cube) must be a reactor vessel
 *       or a functional port: fluid port, access hatch or redstone port.</li>
 *   <li>At least 1 shell position must be a functional port.</li>
 *   <li>The other 20 inner positions can hold any block.</li>
 * </ul>
 * <p>The shape has no facing.
 *
 * <p>Structure data keys: {@code "fluidPortCount"}, {@code "accessHatchCount"} and
 * {@code "redstonePortCount"} ({@code Integer}), and {@code "redstonePortPositions"}
 * ({@code Set<BlockPos>}). {@link mio_icif_multiblock_manager#getRedstonePorts}
 * reads the position set. The reactor reads a redstone signal at these positions.
 */
public class mio_icif_fluid_reactor_validator implements mio_icif_multiblock_validator {
    /** Distance in blocks from the reactor to the outer layer of the cube. */
    public static final int STRUCTURE_RADIUS=2;
    /**
     * Checks the 5x5x5 cube around the reactor at {@code center}.
     *
     * <p>Unlike the other builtin validators, this check first makes sure that each
     * chunk of the cube is loaded. A missing chunk gives a failure result. Thus a
     * structure at a chunk border does not load chunks and does not form while a
     * chunk is missing.
     */
    @Override public mio_icif_multiblock_validation_result validate(Level level,BlockPos center){
        // Test the chunk of each X/Z column of the cube. The cube spans at most 2 chunks on each axis.
        for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++)
            if(!level.getChunkSource().hasChunk((center.getX()+x)>>4,(center.getZ()+z)>>4))return mio_icif_multiblock_validation_result.failure("Structure chunk unavailable");
        if(!(level.getBlockState(center).getBlock() instanceof mio_icif_Block_Nuclear_Reactor_Generator))return mio_icif_multiblock_validation_result.failure("Missing reactor");
        var members=new HashSet<BlockPos>();members.add(center);
        // The 6 face neighbors of the reactor must all be chambers.
        for(var side:Direction.values()){
            var at=center.relative(side);if(!(level.getBlockState(at).getBlock() instanceof mio_icif_Block_Reactor_Chamber))return mio_icif_multiblock_validation_result.failure("Missing chamber");members.add(at);
        }
        var redstone=new HashSet<BlockPos>();int fluids=0,hatches=0;
        // Walk the full cube. Keep only the outer layer: the Chebyshev distance from the center is 2.
        for(int x=-2;x<=2;x++)for(int y=-2;y<=2;y++)for(int z=-2;z<=2;z++){
            if(Math.max(Math.max(Math.abs(x),Math.abs(y)),Math.abs(z))!=2)continue;
            var at=center.offset(x,y,z);Block block=level.getBlockState(at).getBlock();
            if(!(block instanceof mio_icif_Block_reactorvessel)&&!functional(block))return mio_icif_multiblock_validation_result.failure("Incomplete vessel shell");
            members.add(at);
            if(block instanceof mio_icif_Block_Reactor_Fluid_Port)fluids++;
            if(block instanceof mio_icif_Block_Reactor_Access_Hatch)hatches++;
            if(block instanceof mio_icif_block_reactor_redstone_port)redstone.add(at);
        }
        // A closed shell with no port gives no access to the reactor. Thus at least 1 port is necessary.
        if(fluids+hatches+redstone.size()==0)return mio_icif_multiblock_validation_result.failure("Missing functional port");
        return mio_icif_multiblock_validation_result.builder().setValid(true).addBlocks(members)
            .setStructureData("fluidPortCount",fluids).setStructureData("accessHatchCount",hatches)
            .setStructureData("redstonePortCount",redstone.size()).setStructureData("redstonePortPositions",redstone).build();
    }
    /** Tells if a shell block is a functional port: fluid port, access hatch or redstone port. */
    private static boolean functional(Block block){return block instanceof mio_icif_Block_Reactor_Fluid_Port||block instanceof mio_icif_Block_Reactor_Access_Hatch||block instanceof mio_icif_block_reactor_redstone_port;}
    /**
     * Gives the manager to the reactor block entity.
     *
     * <p>The cast is unchecked. The type argument is correct only when the reactor
     * made the manager with this validator. A manager from the registry path holds a
     * {@link CoreValidatorAdapter}. In that case a call to
     * {@link mio_icif_multiblock_manager#getValidator} through the reactor field
     * causes a {@code ClassCastException}.
     */
    @Override @SuppressWarnings("unchecked") public void onStructureFormed(Level level,BlockPos center,mio_icif_multiblock_manager<?> manager){
        if(level.getBlockEntity(center) instanceof mio_icif_nuclear_reactor_generator reactor)reactor.setFluidReactorMultiblock((mio_icif_multiblock_manager<mio_icif_fluid_reactor_validator>)manager);
    }
    /** Tells the reactor block entity that the structure is broken. */
    @Override public void onStructureBroken(Level level,BlockPos center,mio_icif_multiblock_manager<?> manager){
        if(level.getBlockEntity(center) instanceof mio_icif_nuclear_reactor_generator reactor)reactor.onMultiblockBroken();
    }
    @Override public String getStructureName(){return "FluidReactor5x5x5";}
    /**
     * Tells if a block can be a member of a Fluid Reactor.
     *
     * @param block the block to test
     * @return {@code true} for the reactor, a chamber, a vessel and the 3 port types
     */
    public static boolean isValidStructureBlock(Block block){return functional(block)||block instanceof mio_icif_Block_reactorvessel||block instanceof mio_icif_Block_Reactor_Chamber||block instanceof mio_icif_Block_Nuclear_Reactor_Generator;}
}
