// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.builder;

import com.miophas.singularity_iteration.common.block.energycontainer.TesuRelayBlock;
import com.miophas.singularity_iteration.common.blockentity.pipe.FluidPipeBlockEntity;
import com.miophas.singularity_iteration.common.registry.mio_icif_blocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.neoforged.neoforge.common.util.FakePlayer;

/**
 * 0.1.11.188: what a block placed by the Blueprint Builder needs besides its block state, as a player's placement gives it.
 * The blueprint has the block states only (no block entity data), and the builder used to set the state alone:
 * <ul>
 *   <li>A fluid pipe (bronze, steel, ...) joins only through ports opened by its placement or a wrench, kept in its block
 *       entity; a built pipe had every port shut and dropped the joins its state showed at once. Now the sides the
 *       blueprint's state joins are opened.</li>
 *   <li>A block of this mod gets its placement step ({@code setPlacedBy}) with the builder's owner as the placer: the owner
 *       of a miner, pump, chunk loader, trade box or terminal, the column of a TESU relay (only when the two blocks above
 *       are free, as a player's placement checks). Two-part blocks are left out (the builder places both halves).</li>
 * </ul>
 */
final class BuiltBlocks {
    private BuiltBlocks() {}

    static void afterPlace(ServerLevel world, BlockPos pos, BlockState target, FakePlayer placer) {
        BlockState now = world.getBlockState(pos);
        if (now.getBlock() != target.getBlock()) return;
        if (Blueprint.ours(target) && !twoPart(target) && roomFor(world, pos, target)) {
            Item item = target.getBlock().asItem();
            now.getBlock().setPlacedBy(world, pos, now, placer, item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item));
        }
        if (world.getBlockEntity(pos) instanceof FluidPipeBlockEntity pipe) {
            for (Direction d : Direction.values()) {
                if (target.getBlock().getStateDefinition().getProperty(d.getSerializedName()) instanceof BooleanProperty side && target.getValue(side))
                    pipe.unblockDirection(d);
            }
        }
    }

    private static boolean twoPart(BlockState s) {
        return s.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF) || s.hasProperty(BlockStateProperties.BED_PART);
    }

    /** A TESU relay stands its column on the two blocks above it: only when they are free (or already its column). */
    private static boolean roomFor(ServerLevel world, BlockPos pos, BlockState target) {
        if (!(target.getBlock() instanceof TesuRelayBlock)) return true;
        return pos.getY() + 2 < world.getMaxBuildHeight() && free(world, pos.above()) && free(world, pos.above(2));
    }

    private static boolean free(ServerLevel world, BlockPos at) {
        BlockState s = world.getBlockState(at);
        return s.canBeReplaced() || s.is(mio_icif_blocks.TESU_RELAY_COLUMN.get());
    }
}
