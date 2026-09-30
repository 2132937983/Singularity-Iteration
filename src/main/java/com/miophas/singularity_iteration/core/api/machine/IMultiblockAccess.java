package com.miophas.singularity_iteration.core.api.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/** Host-provided structure discovery and construction. */
public interface IMultiblockAccess {
    @Nullable IMultiblockStructure findStructure(Level level, BlockPos position);
    IMultiblockBuilder createBuilder();

    /** Register during common setup, before worlds start. Matches the controller's exact block class. */
    default void registerController(Class<? extends net.minecraft.world.level.block.Block> controllerType,
            java.util.function.Supplier<? extends IMultiblockValidator> validatorFactory, String name) {
        throw new UnsupportedOperationException("Controller registration is not supported by this provider");
    }

    default boolean isControllerRegistered(Class<? extends net.minecraft.world.level.block.Block> controllerType) {
        throw new UnsupportedOperationException("Controller queries are not supported by this provider");
    }
}
