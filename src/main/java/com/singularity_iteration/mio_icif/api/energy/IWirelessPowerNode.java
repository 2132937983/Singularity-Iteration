package com.singularity_iteration.mio_icif.api.energy;

import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;

public interface IWirelessPowerNode {

    @Nullable
    BlockPos getTargetPosition();

    void setTargetPosition(BlockPos target);

    boolean isTransmitting();
}