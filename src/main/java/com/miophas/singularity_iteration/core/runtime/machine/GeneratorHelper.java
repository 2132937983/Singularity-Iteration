package com.miophas.singularity_iteration.core.runtime.machine;

import com.miophas.singularity_iteration.core.api.machine.IBurnControl;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Shared helper for generator forceStart/forceStop.
 * For internal API use only.
 */
public final class GeneratorHelper {

    private GeneratorHelper() {}

    /**
     * Forces a generator to start. Sets its burn time to the default burn time, or to 200 ticks if the default is 0 or less.
     * Server side only.
     *
     * @return true if the block entity at {@code pos} is an {@link IBurnControl}; false on the client or otherwise
     */
    public static boolean forceStartGenerator(Level world, BlockPos pos) {
        if (world.isClientSide) return false;

        BlockEntity be = world.getBlockEntity(pos);
        if (be instanceof IBurnControl gen) {
            int maxBurn = gen.getDefaultBurnTime();
            gen.setBurnTime(maxBurn > 0 ? maxBurn : 200);
            return true;
        }

        return false;
    }

    /**
     * Forces a generator to stop. Sets its burn time to 0.
     * Server side only.
     *
     * @return true if the block entity at {@code pos} is an {@link IBurnControl}; false on the client or otherwise
     */
    public static boolean forceStopGenerator(Level world, BlockPos pos) {
        if (world.isClientSide) return false;

        BlockEntity be = world.getBlockEntity(pos);
        if (be instanceof IBurnControl gen) {
            gen.setBurnTime(0);
            return true;
        }

        return false;
    }
}