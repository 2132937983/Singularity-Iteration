package com.miophas.singularity_iteration.common.blockentity.generator;

import com.miophas.singularity_iteration.common.registry.mio_icif_block_entities;
import com.miophas.singularity_iteration.common.block.generator.mio_icif_Block_MetsAdvancedSolarGenerator;
import com.miophas.singularity_iteration.common.menu.generator.MetsAdvancedSolarGeneratorMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("null")
public class mio_icif_MetsAdvancedSolarGenerator extends mio_icif_MetsSolarGeneratorBase {

    /** 0.1.11.102: route B 48 EU/t (was 64), 15 % of it at night under a clear sky. */
    public static final int DAY_POWER = 48;
    public static final long CAPACITY = 200000L;
    public static final int TIER = 2;
    public static final int SKY_UPDATE_INTERVAL = 128;
    public static final float NIGHT_SHARE = com.miophas.singularity_iteration.common.energy.SolarRoutes.METS_NIGHT;
    /** 0.1.11.102: route B, photon resonance (see SolarRoutes). */
    public static final com.miophas.singularity_iteration.common.energy.SolarProfile PROFILE =
        com.miophas.singularity_iteration.common.energy.SolarRoutes.METS;

    public mio_icif_MetsAdvancedSolarGenerator(BlockPos pos, BlockState state) {
        super(pos, state, DAY_POWER, CAPACITY, TIER, NIGHT_SHARE, mio_icif_block_entities.METS_ADVANCED_SOLAR_GENERATOR_ENTITY_TYPE.get(), PROFILE);
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new MetsAdvancedSolarGeneratorMenu(id, playerInventory, this);
    }

    @Override
    public net.minecraft.network.chat.Component getDisplayName() {
        return net.minecraft.network.chat.Component.translatable("container.mio_icif.mets_advanced_solar_generator");
    }
}