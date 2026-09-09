package com.singularity_iteration.mio_icif.Blocks.entity.batbox;

import com.singularity_iteration.mio_icif.Blocks.EnergyContainer.mio_icif_eesu_charger;
import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_block_entities;
import com.singularity_iteration.mio_icif.api.internal.ChargepadHelper;
import com.singularity_iteration.mio_icif.api.internal.energy.GenericEnergyContainerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

@SuppressWarnings("null")
public class mio_icif_eesu_charger_entity extends mio_icif_eesu_entity {

    private static final long MAX_TRANSFER_PER_ITEM = 8192L;

    public mio_icif_eesu_charger_entity(BlockPos pos, BlockState state) {
        super(pos, state, mio_icif_block_entities.EESU_CHARGER.get());
    }

    public mio_icif_eesu_charger_entity(BlockPos pos, BlockState state, BlockEntityType<?> type) {
        super(pos, state, type);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, mio_icif_eesu_charger_entity blockEntity) {
        if (level.isClientSide()) {
            return;
        }
        GenericEnergyContainerBlockEntity.containerTick(level, pos, state, blockEntity);
        boolean isCharging = ChargepadHelper.chargeNearbyPlayer(blockEntity, MAX_TRANSFER_PER_ITEM);
        boolean currentLit = state.getValue(mio_icif_eesu_charger.LIT);
        if (isCharging != currentLit) {
            level.setBlock(pos, state.setValue(mio_icif_eesu_charger.LIT, isCharging), 3);
        }
    }
}