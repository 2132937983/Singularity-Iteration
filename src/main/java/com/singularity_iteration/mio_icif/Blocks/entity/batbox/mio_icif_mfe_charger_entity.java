package com.singularity_iteration.mio_icif.Blocks.entity.batbox;

import com.singularity_iteration.mio_icif.Blocks.EnergyContainer.mio_icif_mfe_charger;
import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_Energy_Container;
import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_block_entities;
import com.singularity_iteration.mio_icif.api.MioIcifAPI;
import com.singularity_iteration.mio_icif.api.item.IItemAPI;
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

    public mio_icif_mfe_charger_entity(BlockPos pos, BlockState state) {
        super(pos, state, mio_icif_block_entities.MFE_CHARGER.get());
    }

    public mio_icif_mfe_charger_entity(BlockPos pos, BlockState state, BlockEntityType<?> type) {
        super(pos, state, type);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, mio_icif_mfe_charger_entity blockEntity) {
        if (level.isClientSide()) {
            return;
        }
        mio_icif_Energy_Container.tick(level, pos, state, blockEntity);
        boolean isCharging = blockEntity.chargeNearbyPlayer();
        boolean currentLit = state.getValue(mio_icif_mfe_charger.LIT);
        if (isCharging != currentLit) {
            level.setBlock(pos, state.setValue(mio_icif_mfe_charger.LIT, isCharging), 3);
        }
    }

    private boolean chargeNearbyPlayer() {
        // 检查红石模式是否允许输出能量
        if (!shouldEmitEnergy()) {
            return false;
        }

        if (energyStorage.getAmount() <= 0) {
            return false;
        }
        AABB detectionBox = new AABB(
            worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(),
            worldPosition.getX() + 1, worldPosition.getY() + 2.5, worldPosition.getZ() + 1
        );
        List<Player> players = level.getEntitiesOfClass(Player.class, detectionBox);

        boolean charged = false;
        for (Player player : players) {
            if (chargePlayerEquipment(player)) {
                charged = true;
            }
        }
        return charged;
    }

    private boolean chargePlayerEquipment(Player player) {
        long availableEnergy = energyStorage.getAmount();
        if (availableEnergy <= 0) return false;

        long totalExtracted = 0;

        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            if (totalExtracted >= availableEnergy) break;
            ItemStack stack = player.getItemBySlot(slot);
            if (MioIcifAPI.instance().getItemAPI().isElectricArmor(stack)) {
                totalExtracted += chargeArmorByAPI(stack, availableEnergy - totalExtracted);
            }
        }

        if (totalExtracted < availableEnergy) {
            totalExtracted += chargeToolByAPI(player.getMainHandItem(), availableEnergy - totalExtracted);
        }
        if (totalExtracted < availableEnergy) {
            totalExtracted += chargeToolByAPI(player.getOffhandItem(), availableEnergy - totalExtracted);
        }

        if (totalExtracted < availableEnergy) {
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                if (totalExtracted >= availableEnergy) break;
                ItemStack stack = player.getInventory().getItem(i);
                totalExtracted += chargeInventoryItem(stack, availableEnergy - totalExtracted);
            }
        }

        if (totalExtracted > 0) {
            setChanged();
            return true;
        }
        return false;
    }

    private long chargeArmorByAPI(ItemStack stack, long availableEnergy) {
        IItemAPI api = MioIcifAPI.instance().getItemAPI();
        long current = api.getElectricArmorStored(stack);
        long max = api.getElectricArmorMaxEnergy(stack);
        if (current >= max) return 0;
        long spaceInItem = max - current;
        long energyToTransfer = Math.min(availableEnergy, Math.min(MAX_TRANSFER_PER_ITEM, spaceInItem));
        if (energyToTransfer <= 0) return 0;
        long extracted = energyStorage.extract(energyToTransfer, false);
        if (extracted > 0) {
            api.chargeElectricArmor(stack, extracted, false);
        }
        return extracted;
    }

    private long chargeToolByAPI(ItemStack stack, long availableEnergy) {
        if (stack.isEmpty()) return 0;
        IItemAPI api = MioIcifAPI.instance().getItemAPI();
        if (!api.isElectricTool(stack)) return 0;
        long current = api.getElectricToolStored(stack);
        long max = api.getElectricToolMaxEnergy(stack);
        if (current >= max) return 0;
        long spaceInItem = max - current;
        long energyToTransfer = Math.min(availableEnergy, Math.min(MAX_TRANSFER_PER_ITEM, spaceInItem));
        if (energyToTransfer <= 0) return 0;
        long extracted = energyStorage.extract(energyToTransfer, false);
        if (extracted > 0) {
            api.chargeElectricTool(stack, extracted, false);
        }
        return extracted;
    }

    private long chargeInventoryItem(ItemStack stack, long availableEnergy) {
        if (stack.isEmpty()) return 0;
        IItemAPI api = MioIcifAPI.instance().getItemAPI();
        if (api.isElectricArmor(stack)) {
            return chargeArmorByAPI(stack, availableEnergy);
        }
        if (api.isElectricTool(stack)) {
            return chargeToolByAPI(stack, availableEnergy);
        }
        if (api.isBattery(stack)) {
            return chargeBatteryByAPI(stack, availableEnergy);
        }
        return 0;
    }

    private long chargeBatteryByAPI(ItemStack stack, long availableEnergy) {
        IItemAPI api = MioIcifAPI.instance().getItemAPI();
        if (api.isBatteryFull(stack)) return 0;
        long current = api.getBatteryStored(stack);
        long max = api.getBatteryCapacity(stack);
        long chargeRate = api.getChargeRate(stack);
        long spaceInItem = max - current;
        long maxTransfer = Math.min(MAX_TRANSFER_PER_ITEM, Math.min(spaceInItem, chargeRate));
        long energyToTransfer = Math.min(availableEnergy, maxTransfer);
        if (energyToTransfer <= 0) return 0;
        long extracted = energyStorage.extract(energyToTransfer, false);
        if (extracted > 0) {
            api.chargeBattery(stack, extracted, false);
        }
        return extracted;
    }
}