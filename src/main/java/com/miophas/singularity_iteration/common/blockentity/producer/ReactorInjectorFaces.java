// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.blockentity.producer;

import com.miophas.singularity_iteration.common.blockentity.generator.mio_icif_nuclear_reactor_generator;
import com.miophas.singularity_iteration.common.blockentity.reactor.mio_icif_reactor_chamber;
import com.miophas.singularity_iteration.core.runtime.energy.IndependentSiEnergy;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jetbrains.annotations.Nullable;

/**
 * Energy faces of the reactor coolant injectors (0.1.10.4).
 *
 * <p>An injector is an MV machine (128 EU per packet) and must touch the reactor with its front.
 * Up to 0.1.10.3 it took energy on all six faces, so the reactor (and every chamber, which are
 * electrical contacts of the reactor) fed its own output straight into it: as soon as the reactor
 * made more than 128 EU/t, the packet blew the injector up (sink overvoltage), right next to the
 * reactor. Now the injector never connects electrically on its front, nor on a face that touches
 * a reactor part (reactor, chamber, vessel, access hatch, fluid or redstone port). Power it through
 * a cable from an MV (or lower) source on one of its free faces.
 *
 * <p>The injector has no redstone output (only a comparator level) and moves no heat; it changes
 * nothing in the reactor but the stored heat of the condensators it repairs.
 */
final class ReactorInjectorFaces {
    private ReactorInjectorFaces() {}

    /** True when the injector must not connect on {@code side}. Never loads a chunk. */
    static boolean closed(BlockEntity injector, @Nullable Direction side) {
        if (side == null) return false;
        BlockState own = injector.getBlockState();
        if (own.hasProperty(HorizontalDirectionalBlock.FACING) && own.getValue(HorizontalDirectionalBlock.FACING) == side) return true;
        Level level = injector.getLevel();
        if (level == null) return false;
        BlockPos at = injector.getBlockPos().relative(side);
        LevelChunk chunk = level.getChunkSource().getChunkNow(at.getX() >> 4, at.getZ() >> 4);
        if (chunk == null) return false;
        BlockEntity be = chunk.getBlockEntity(at, LevelChunk.EntityCreationType.CHECK);
        if (be instanceof mio_icif_nuclear_reactor_generator || be instanceof mio_icif_reactor_chamber) return true;
        return isReactorBlock(chunk.getBlockState(at));
    }

    /** Blocks of the reactor families: {@code mio_icif:reactor/block_reactor_*} and the reactor itself. */
    static boolean isReactorBlock(BlockState state) {
        var id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        return "mio_icif".equals(id.getNamespace())
            && (id.getPath().startsWith("reactor/block_reactor_") || id.getPath().equals("generator/block_nuclear_reactor_generator"));
    }

    /** Bit mask of the closed faces (bit i = {@code Direction} ordinal i). */
    static int closedMask(BlockEntity injector) {
        int mask = 0;
        for (Direction side : Direction.values()) if (closed(injector, side)) mask |= 1 << side.ordinal();
        return mask;
    }

    /**
     * Server, once a second: tells the energy network when the closed faces changed (a chamber was
     * placed or broken next to the injector, the injector was turned). Returns the new mask.
     */
    static int refresh(BlockEntity injector, int lastMask) {
        int mask = closedMask(injector);
        if (mask != lastMask) IndependentSiEnergy.changed(injector);
        return mask;
    }
}
