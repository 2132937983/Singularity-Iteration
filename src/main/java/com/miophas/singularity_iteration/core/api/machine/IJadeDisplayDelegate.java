package com.miophas.singularity_iteration.core.api.machine;

import net.minecraft.world.level.block.entity.BlockEntity;

import org.jetbrains.annotations.Nullable;

public interface IJadeDisplayDelegate {

    @Nullable
    BlockEntity getJadeDisplayTarget();
}