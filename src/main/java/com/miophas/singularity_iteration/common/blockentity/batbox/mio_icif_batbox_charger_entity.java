package com.miophas.singularity_iteration.common.blockentity.batbox;

import com.miophas.singularity_iteration.common.block.energycontainer.mio_icif_batbox_charger;
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
 * 充电座方块实体?
 * 继承 batbox_entity，增加了玩家充电功能
 * 当玩家站在上面时，为玩家身上的所有电力设备充电
 */
@SuppressWarnings("null")
public class mio_icif_batbox_charger_entity extends mio_icif_batbox_entity {

    // 最大单次传输速率（每 tick 每个物品最多接受 32 EU，与LV电压等级一致）
    private static final long MAX_TRANSFER_PER_ITEM = 32L;
    private static final EquipmentSlot[] CHARGE_SLOTS = {
        EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };
    private final AABB detectionBox;

    public mio_icif_batbox_charger_entity(BlockPos pos, BlockState state) {
        super(pos, state, mio_icif_block_entities.BATBOX_CHARGER.get());
        this.detectionBox = createDetectionBox(pos);
    }

    public mio_icif_batbox_charger_entity(BlockPos pos, BlockState state, BlockEntityType<?> type) {
        super(pos, state, type);
        this.detectionBox = createDetectionBox(pos);
    }

    private static AABB createDetectionBox(BlockPos pos) {
        return new AABB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 2.5, pos.getZ() + 1);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, mio_icif_batbox_charger_entity blockEntity) {
        if (level.isClientSide()) {
            return;
        }

        // 先执行基础的储能方法 tick 逻辑（能量存储更新、电网交互等）
        AbstractEnergyStorageBlockEntity.tick(level, pos, state, blockEntity);

        // 检查是否有玩家站在充电座上并为其充电
        boolean isCharging = com.miophas.singularity_iteration.core.runtime.machine.ChargepadHelper.chargeNearbyPlayer(blockEntity, MAX_TRANSFER_PER_ITEM);

        // 切换 lit 状态
        boolean currentLit = state.getValue(mio_icif_batbox_charger.LIT);
        if (isCharging != currentLit) {
            level.setBlock(pos, state.setValue(mio_icif_batbox_charger.LIT, isCharging), 3);
        }
    }

    // Charging (priority order, OFF items skipped) is shared with the other charge pads: ChargepadHelper.
}
