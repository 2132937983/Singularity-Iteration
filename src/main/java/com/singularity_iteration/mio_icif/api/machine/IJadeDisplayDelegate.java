package com.singularity_iteration.mio_icif.api.machine;

import net.minecraft.world.level.block.entity.BlockEntity;

import org.jetbrains.annotations.Nullable;

public interface IJadeDisplayDelegate {

    @Nullable
    BlockEntity getJadeDisplayTarget();
}