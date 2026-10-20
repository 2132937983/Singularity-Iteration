// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.crop;

import com.miophas.singularity_iteration.common.blockentity.crop.mio_icif_crop_entity;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

/**
 * Water washing a crop rack away (both crop rack blocks are {@code LiquidBlockContainer}s for this).
 * <ul>
 *   <li>A rack with a crop planted: water entering its space (flowing water, a bucket, cell or
 *       dispenser emptied into it) destroys it, and it gives exactly two seed bags of the crop, with its
 *       stats and scan level, and nothing else: no rack, no harvest.</li>
 *   <li>An empty rack, or any other fluid: as before, the fluid takes the space (water drops the rack,
 *       lava burns it).</li>
 * </ul>
 * Watering a crop by hand (using a water container on the rack) does not wash it: the rack takes the
 * water as hydration ({@link CropInteractions#use}).
 */
public final class CropWashing {
    private CropWashing() {}

    /** Seed bags a washed-away crop gives. */
    public static final int SEED_BAGS = 2;

    /** Every fluid can enter the rack's space, as when the rack was a plain non-solid block. */
    public static boolean canPlaceLiquid() {
        return true;
    }

    /** {@code LiquidBlockContainer#placeLiquid}: the fluid takes the rack's space. */
    public static boolean placeLiquid(LevelAccessor level, BlockPos pos, BlockState state, FluidState fluid) {
        if (level.isClientSide()) return true;
        BlockEntity be = level.getBlockEntity(pos);
        if (fluid.is(FluidTags.WATER) && be instanceof mio_icif_crop_entity crop && crop.getPlant() != null) {
            ItemStack seeds = crop.makeSeeds(crop.getPlant(), 1, crop.getGrowthSpeed(), crop.getYield(), crop.getResilience());
            // without a plant the rack's removal (crop.pick) drops no seeds of its own
            crop.reset();
            level.setBlock(pos, fluid.createLegacyBlock(), Block.UPDATE_ALL);
            if (level instanceof Level world) {
                for (int i = 0; i < SEED_BAGS; i++) Block.popResource(world, pos, seeds.copy());
            }
            return true;
        }
        // the fluids' own handling (FlowingFluid#beforeDestroyingBlock): lava fizzes, the others drop the block
        if (fluid.is(FluidTags.LAVA)) level.levelEvent(1501, pos, 0);
        else Block.dropResources(state, level, pos, be);
        level.setBlock(pos, fluid.createLegacyBlock(), Block.UPDATE_ALL);
        return true;
    }
}
