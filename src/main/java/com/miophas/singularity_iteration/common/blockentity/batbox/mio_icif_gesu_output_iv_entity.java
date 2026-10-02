package com.miophas.singularity_iteration.common.blockentity.batbox;

import com.miophas.singularity_iteration.common.registry.mio_icif_block_entities;
import com.miophas.singularity_iteration.core.api.MioIcifAPI;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** GESU IV output port: relays core energy to the network at IV tier (see {@link GesuOutputPort}). */
@SuppressWarnings("null")
public class mio_icif_gesu_output_iv_entity extends GesuOutputPort {

    public mio_icif_gesu_output_iv_entity(BlockPos pos, BlockState state) {
        super(mio_icif_block_entities.GESU_OUTPUT_IV.get(), pos, state, MioIcifAPI.instance().getEnergyNetAPI().getCableTier("iv"));
    }

    public static void tick(Level level, BlockPos pos, BlockState state, mio_icif_gesu_output_iv_entity blockEntity) {
        tickPort(level, blockEntity);
    }

    @Override
    public net.minecraft.network.chat.Component getDisplayName() {
        return net.minecraft.network.chat.Component.translatable("container.mio_icif.gesu_output_iv");
    }
}
