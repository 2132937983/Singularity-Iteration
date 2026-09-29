package com.miophas.singularity_iteration.common.blockentity.batbox;

import com.miophas.singularity_iteration.common.block.energycontainer.mio_icif_eesu_charger;
import com.miophas.singularity_iteration.common.registry.mio_icif_block_entities;
import com.miophas.singularity_iteration.core.runtime.machine.ChargepadHelper;
import com.miophas.singularity_iteration.core.prefab.blockentity.GenericEnergyContainerBlockEntity;
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