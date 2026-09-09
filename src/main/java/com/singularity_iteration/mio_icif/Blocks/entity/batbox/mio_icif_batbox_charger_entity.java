package com.singularity_iteration.mio_icif.Blocks.entity.batbox;

import com.singularity_iteration.mio_icif.Blocks.EnergyContainer.mio_icif_batbox_charger;
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
 * 充电座方块实体?
 * 继承 batbox_entity，增加了玩家充电功能
 * 当玩家站在上面时，为玩家身上的所有电力设备充电
 */
@SuppressWarnings("null")
public class mio_icif_batbox_charger_entity extends mio_icif_batbox_entity {

    // 最大单次传输速率（每 tick 每个物品最多接受 32 EU，与LV电压等级一致）
    private static final long MAX_TRANSFER_PER_ITEM = 32L;

    public mio_icif_batbox_charger_entity(BlockPos pos, BlockState state) {
        super(pos, state, mio_icif_block_entities.BATBOX_CHARGER.get());
    }

    public mio_icif_batbox_charger_entity(BlockPos pos, BlockState state, BlockEntityType<?> type) {
        super(pos, state, type);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, mio_icif_batbox_charger_entity blockEntity) {
        if (level.isClientSide()) {
            return;
        }

        // 先执行基础的储能方法 tick 逻辑（能量存储更新、电网交互等）
        mio_icif_Energy_Container.tick(level, pos, state, blockEntity);

        // 检查是否有玩家站在充电座上并为其充电
        boolean isCharging = blockEntity.chargeNearbyPlayer();

        // 切换 lit 状态
        boolean currentLit = state.getValue(mio_icif_batbox_charger.LIT);
        if (isCharging != currentLit) {
            level.setBlock(pos, state.setValue(mio_icif_batbox_charger.LIT, isCharging), 3);
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

        // 使用 AABB 检测方块上方 2.5 格范围内的玩家
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

    /**
     * 为玩家的所有电力设备充电?
     * @return 是否进行了充电
     */
    private boolean chargePlayerEquipment(Player player) {
        // 检查是否有可用能量
        long availableEnergy = energyStorage.getAmount();
        if (availableEnergy <= 0) {
            return false;
        }

        // 追踪实际提取的能量
        long totalExtracted = 0;

        // 1. 充能装备槽中的护甲（HEAD, CHEST, LEGS, FEET）
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            if (totalExtracted >= availableEnergy) break;

            ItemStack armorStack = player.getItemBySlot(slot);
            if (MioIcifAPI.instance().getItemAPI().isElectricArmor(armorStack)) {
                totalExtracted += chargeArmorByAPI(armorStack, availableEnergy - totalExtracted);
            }
        }

        // 2. 充能主手和副手的工具
        if (totalExtracted < availableEnergy) {
            totalExtracted += chargeToolByAPI(player.getMainHandItem(), availableEnergy - totalExtracted);
        }
        if (totalExtracted < availableEnergy) {
            totalExtracted += chargeToolByAPI(player.getOffhandItem(), availableEnergy - totalExtracted);
        }

        // 3. 充能物品栏中的电池和工具
        if (totalExtracted < availableEnergy) {
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                if (totalExtracted >= availableEnergy) break;
                ItemStack stack = player.getInventory().getItem(i);
                totalExtracted += chargeInventoryItem(stack, availableEnergy - totalExtracted);
            }
        }

        // 如果有能量被提取，标记为已更新
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
        } else if (api.isElectricTool(stack)) {
            return chargeToolByAPI(stack, availableEnergy);
        } else if (api.isBattery(stack)) {
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