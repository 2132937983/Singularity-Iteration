package com.miophas.singularity_iteration.core.api.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.fml.ModList;

import java.util.ArrayList;
import java.util.List;

/**
 * 能量重分配接口。
 * 实现此接口的物品在玩家右键时，会将自身能量分配给背包中的其他电力装备。
 *
 * <p>此功能属于电池类专有，电动工具不应实现此接口。</p>
 *
 * <h3>使用方式</h3>
 * <ul>
 *   <li>电池类：实现此接口即可自动获得右键放电功能</li>
 *   <li>附属电池：实现 {@link IBatteryItem} 和此接口即可</li>
 *   <li>电动工具：不要实现此接口</li>
 * </ul>
 */
public interface IEnergyDistributable extends IBatteryItem {

    /**
     * 将电池能量分配给玩家背包中的其他电力装备。
     *
     * @param player        玩家
     * @param batteryStack  电池物品堆
     * @param totalEnergy   可分配的总能量
     */
    default void distributeEnergyToItems(Player player, ItemStack batteryStack, long totalEnergy) {
        if (batteryStack.isEmpty() || batteryStack.getCount() != 1) return;
        totalEnergy = Math.min(Math.max(0, totalEnergy), getEnergy(batteryStack));
        if (totalEnergy == 0) return;
        List<ItemStack> targets = new ArrayList<>();
        Inventory inv = player.getInventory();

        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.isEmpty() || s == batteryStack) continue;
            if (BatteryTransfer.isEquipment(s) && s.getItem() instanceof IBatteryItem item) {
                if (item.getEnergy(s) < item.getMaxEnergy(s)) targets.add(s);
            }
        }

        if (ModList.get().isLoaded("curios")) {
            BatteryTargetProviders.appendTargets(player, targets, batteryStack);
        }

        if (targets.isEmpty()) return;

        long energyPerTarget = totalEnergy / targets.size();
        if (energyPerTarget <= 0) return;

        for (ItemStack target : targets) {
            BatteryTransfer.move(batteryStack, this, target, (IBatteryItem) target.getItem(), energyPerTarget);
        }
    }

    /**
     * 处理蹲下右键的能量分配交互。
     * 电池类可在 {@link Item#use} 中调用此方法。
     *
     * @param level  世界
     * @param player 玩家
     * @param hand   手
     * @return 交互结果
     */
    default InteractionResultHolder<ItemStack> handleDistributeUse(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && stack.getCount() == 1) {
            long batteryEnergy = getEnergy(stack);
            if (batteryEnergy > 0) {
                distributeEnergyToItems(player, stack, batteryEnergy);
                player.displayClientMessage(
                    Component.translatable("message.mio_icif.bat.distribute", getEnergy(stack), getMaxEnergy(stack)),
                    true
                );
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        return null;
    }
}