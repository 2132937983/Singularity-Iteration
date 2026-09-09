package com.singularity_iteration.mio_icif.Blocks.entity.generator;

import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_block_entities;
import com.singularity_iteration.mio_icif.Blocks.generator.mio_icif_Block_PhotonResonanceSolarGenerator;
import com.singularity_iteration.mio_icif.Menu.Generator.PhotonResonanceSolarGeneratorMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("null")
public class mio_icif_PhotonResonanceSolarGenerator extends mio_icif_MetsSolarGeneratorBase {

    public static final int DAY_POWER = 512;
    public static final int PRODUCTION = 2048;
    public static final long CAPACITY = 40000000L;
    public static final int TIER = 4;
    public static final int SKY_UPDATE_INTERVAL = 128;
    public static final float MIN_SKY_BRIGHTNESS = 0.1F;

    public mio_icif_PhotonResonanceSolarGenerator(BlockPos pos, BlockState state) {
        super(pos, state, DAY_POWER, PRODUCTION, CAPACITY, TIER, SKY_UPDATE_INTERVAL, MIN_SKY_BRIGHTNESS,
            mio_icif_block_entities.PHOTON_RESONANCE_SOLAR_GENERATOR_ENTITY_TYPE.get(),
            mio_icif_Block_PhotonResonanceSolarGenerator.LIT);
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new PhotonResonanceSolarGeneratorMenu(id, playerInventory, this);
    }

    @Override
    public net.minecraft.network.chat.Component getDisplayName() {
        return net.minecraft.network.chat.Component.translatable("container.mio_icif.photon_resonance_solar_generator");
    }
}