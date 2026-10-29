// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.blockentity.reactor;

import com.miophas.singularity_iteration.common.blockentity.generator.mio_icif_nuclear_reactor_generator;
import com.miophas.singularity_iteration.common.registry.mio_icif_block_entities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

/**
 * 0.1.10.36: a steam reactor chamber. It joins one adjacent steam reactor only (an electric reactor never takes it), and like
 * IC2C's steam chamber it is also a fluid port of that reactor: water in, steam out.
 */
public class mio_icif_steam_reactor_chamber extends mio_icif_reactor_chamber {
    public mio_icif_steam_reactor_chamber(BlockPos pos, BlockState state) {
        super(mio_icif_block_entities.STEAM_REACTOR_CHAMBER_ENTITY_TYPE.get(), pos, state);
    }

    @Override
    protected boolean accepts(mio_icif_nuclear_reactor_generator core) {
        return core.isSteamReactor();
    }

    /** The reactor's water and steam tanks, or null without a connected reactor. */
    public @Nullable IFluidHandler getFluidHandlerCapability(@Nullable Direction side) {
        var core = getConnectedReactor();
        return core == null ? null : core.getFluidHandlerCapability(side);
    }
}
