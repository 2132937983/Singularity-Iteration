package com.singularity_iteration.mio_icif.integration.ae2;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

interface AE2Bridge {

    boolean hasGridNode(Level world, BlockPos pos);

    @Nullable
    Object getGridNode(Level world, BlockPos pos);

    @Nullable
    Object getEnergyService(Object gridNode);

    double injectAePower(Object energyService, double aeAmount);

    double getAeEnergyDemand(Object energyService);
}