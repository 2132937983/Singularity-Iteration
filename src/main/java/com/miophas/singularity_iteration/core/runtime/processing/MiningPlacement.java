// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.runtime.processing;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.EventHooks;

/** Standard public placement event and conditional restoration for the miner's two simple block states. */
public final class MiningPlacement {
    private MiningPlacement() { }
    public static PipeAdvance.Outcome replace(ServerLevel level, BlockPos pos, BlockState expected, BlockState placed,
                                               ItemStack tool, MachineActionOwner owner) {
        if (!level.getServer().isSameThread() || owner == null || !owner.canAct()
                || !level.getChunkSource().hasChunk(pos.getX() >> 4, pos.getZ() >> 4)
                || !level.getWorldBorder().isWithinBounds(pos) || level.isOutsideBuildHeight(pos)) return PipeAdvance.Outcome.RETRY;
        if (expected.hasBlockEntity() || placed.hasBlockEntity() || level.getBlockState(pos) != expected) return PipeAdvance.Outcome.RETRY;
        var snapshot = BlockSnapshot.create(level.dimension(), level, pos);
        var actor = FakePlayerFactory.get(level, owner.actorProfile());
        var oldTool = actor.getMainHandItem().copy(); var oldPosition = actor.position();
        try {
            actor.setItemInHand(InteractionHand.MAIN_HAND, tool.copy()); actor.setPos(pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5);
            if (!level.setBlock(pos, placed, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE))
                return level.getBlockState(pos) == expected ? PipeAdvance.Outcome.RETRY : PipeAdvance.Outcome.UNCERTAIN;
            boolean canceled = EventHooks.onBlockPlace(actor, snapshot, Direction.DOWN);
            if (canceled) {
                if (!loadedAndEqual(level, pos, placed)) return PipeAdvance.Outcome.UNCERTAIN;
                return level.setBlock(pos, expected, Block.UPDATE_ALL) && loadedAndEqual(level, pos, expected)
                    ? PipeAdvance.Outcome.RETRY : PipeAdvance.Outcome.UNCERTAIN;
            }
            if (!loadedAndEqual(level, pos, placed)) return PipeAdvance.Outcome.UNCERTAIN;
            level.updateNeighborsAt(pos, placed.getBlock());
            return loadedAndEqual(level, pos, placed) ? PipeAdvance.Outcome.APPLIED : PipeAdvance.Outcome.UNCERTAIN;
        } finally {
            actor.setItemInHand(InteractionHand.MAIN_HAND, oldTool); actor.setPos(oldPosition.x, oldPosition.y, oldPosition.z);
        }
    }
    private static boolean loadedAndEqual(ServerLevel level, BlockPos pos, BlockState state) {
        return level.getChunkSource().hasChunk(pos.getX() >> 4, pos.getZ() >> 4) && level.getBlockState(pos) == state;
    }
}
