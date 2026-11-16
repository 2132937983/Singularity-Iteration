package com.miophas.singularity_iteration.common.client.render;

import com.miophas.singularity_iteration.common.block.wire.mio_icif_block_wire;
import com.miophas.singularity_iteration.common.blockentity.build.mio_icif_block_foam_entity;
import com.miophas.singularity_iteration.common.blockentity.mio_icif_wire;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 0.1.11.3: skips the per-frame drawing of a disguised block (construction foam, a disguised wire) that no face of can
 * be seen. Disguises are drawn by block entity renderers every frame, without the face culling of the chunk mesh, so a
 * wall of a few thousand disguised blocks drew a few thousand full models (two for a wire) each frame. Inside such a wall
 * every neighbour is an opaque block or another opaque disguise, and only the surface layer is drawn now.
 */
final class DisguiseCulling {
    private DisguiseCulling() {}

    /** True when all six neighbours hide the block at {@code pos}. */
    @SuppressWarnings("deprecation")
    static boolean enclosed(Level level, BlockPos pos) {
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        for (Direction side : Direction.values()) {
            at.setWithOffset(pos, side);
            BlockState state = level.getBlockState(at);
            if (state.isSolidRender(level, at)) continue;
            if (!opaqueDisguise(level, at, level.getBlockEntity(at))) return false;
        }
        return true;
    }

    private static boolean opaqueDisguise(Level level, BlockPos pos, BlockEntity be) {
        Block disguise = null;
        if (be instanceof mio_icif_block_foam_entity foam && foam.hasDisguise()) disguise = foam.getDisguisedBlock();
        else if (be instanceof mio_icif_wire wire && wire.hasDisguise()) {
            BlockState state = wire.getBlockState();
            boolean shown = state.hasProperty(mio_icif_block_wire.DISGUISED) && state.getValue(mio_icif_block_wire.DISGUISED)
                && state.hasProperty(mio_icif_block_wire.FOAMLOGGED) && state.getValue(mio_icif_block_wire.FOAMLOGGED);
            if (shown) disguise = wire.getDisguisedBlock();
        }
        return disguise != null && disguise != Blocks.AIR && disguise.defaultBlockState().isSolidRender(level, pos);
    }
}
