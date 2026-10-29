// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.blockentity.generator;

import com.miophas.singularity_iteration.common.block.environment.fluid.mio_icif_fluids;
import com.miophas.singularity_iteration.common.menu.generator.SteamTurbineMenu;
import com.miophas.singularity_iteration.common.registry.mio_icif_block_entities;
import com.miophas.singularity_iteration.core.api.energy.storage.CableTier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import org.jetbrains.annotations.Nullable;

/**
 * 0.1.10.36: the steam turbine, the EU end of the steam reactor (IC2 Classic's steam turbine: at most 128 mB of steam a tick,
 * 64 EU/t, about 2 mB of steam per EU). It is the semifluid generator with steam for fuel: the same tank, cell slots, battery
 * slot, screen and grid behaviour (it turns steam into EU only while its buffer has room), at MV.
 */
public class mio_icif_steam_turbine extends mio_icif_Semifluid_generator {
    public static final int STEAM_CAPACITY = 16_000;
    /** mB of steam per EU. */
    public static final int STEAM_PER_EU = 2;
    public static final long MAX_OUTPUT = 64L;
    public static final long ENERGY_CAPACITY = 32_000L;
    private static final SemifluidFuels.GeneratorValue STEAM_VALUE = new SemifluidFuels.GeneratorValue(1, MAX_OUTPUT);

    public mio_icif_steam_turbine(BlockPos pos, BlockState state) {
        super(pos, state, mio_icif_block_entities.STEAM_TURBINE_ENTITY_TYPE.get(), STEAM_CAPACITY, MAX_OUTPUT, ENERGY_CAPACITY, 128L, CableTier.MV);
    }

    public static boolean steam(@Nullable Fluid fluid) {
        return fluid != null && fluid.isSame(mio_icif_fluids.STEAM.get());
    }

    @Override protected boolean acceptsFuel(@Nullable Fluid fluid) { return steam(fluid); }
    @Override protected @Nullable SemifluidFuels.GeneratorValue fuelValue(Fluid fluid) { return steam(fluid) ? STEAM_VALUE : null; }
    @Override protected int millibucketsPerUnit() { return STEAM_PER_EU; }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.mio_icif.generator.block_steam_turbine");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new SteamTurbineMenu(containerId, playerInventory, this, this.getItemHandler(), null);
    }
}
