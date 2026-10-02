package com.miophas.singularity_iteration.common.blockentity.batbox;

import com.miophas.singularity_iteration.common.block.energycontainer.mio_icif_mfe_charger;
import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractEnergyStorageBlockEntity;
import com.miophas.singularity_iteration.common.registry.mio_icif_block_entities;
import com.miophas.singularity_iteration.core.api.MioIcifAPI;
import com.miophas.singularity_iteration.core.api.item.IItemAPI;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * MFE 充电座方块实体，继承 mfe_entity，增加了玩家充电功能
 * 容量 600,000 EU，输入/输出速率 512 EU/tick
 */
@SuppressWarnings("null")
public class mio_icif_mfe_charger_entity extends mio_icif_mfe_entity {

    // 最大单次传输速率（每 tick 每个物品最多接受 512 EU，与HV电压等级一致）
    private static final long MAX_TRANSFER_PER_ITEM = 512L;
    private static final EquipmentSlot[] CHARGE_SLOTS = {
        EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };
    private final AABB detectionBox;

    public mio_icif_mfe_charger_entity(BlockPos pos, BlockState state) {
        super(pos, state, mio_icif_block_entities.MFE_CHARGER.get());
        this.detectionBox = createDetectionBox(pos);
    }

    public mio_icif_mfe_charger_entity(BlockPos pos, BlockState state, BlockEntityType<?> type) {
        super(pos, state, type);
        this.detectionBox = createDetectionBox(pos);
    }

    private static AABB createDetectionBox(BlockPos pos) {
        return new AABB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 2.5, pos.getZ() + 1);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, mio_icif_mfe_charger_entity blockEntity) {
        if (level.isClientSide()) {
            return;
        }
        AbstractEnergyStorageBlockEntity.tick(level, pos, state, blockEntity);
        boolean isCharging = com.miophas.singularity_iteration.core.runtime.machine.ChargepadHelper.chargeNearbyPlayer(blockEntity, MAX_TRANSFER_PER_ITEM);
        boolean currentLit = state.getValue(mio_icif_mfe_charger.LIT);
        if (isCharging != currentLit) {
            level.setBlock(pos, state.setValue(mio_icif_mfe_charger.LIT, isCharging), 3);
        }
    }

    // Charging (priority order, OFF items skipped) is shared with the other charge pads: ChargepadHelper.
}
