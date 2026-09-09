package com.singularity_iteration.mio_icif.api.item;

import com.singularity_iteration.mio_icif.api.item.electric.IElectricItem;
import com.singularity_iteration.mio_icif.api.item.electric.IElectricItemManager;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * IBatteryItem 与 IElectricItem 互适配器工具类。
 *
 * <p>用于在两套电池 API 之间桥接，使实现其中一套接口的物品
 * 可以透明地被另一套 API 的消费者使用。
 *
 * <h3>使用场景</h3>
 * <ul>
 *   <li>附属模组的物品实现了 {@link IElectricItem}，但需要在 mio_icif 机器中作为
 *       {@link IBatteryItem} 使用 → 使用 {@link #asBatteryItem(IElectricItem, IElectricItemManager)}</li>
 *   <li>附属模组的物品实现了 {@link IBatteryItem}，但需要被 IC2 兼容的充电设备识别 →
 *       使用 {@link #asElectricItem(IBatteryItem)}</li>
 * </ul>
 *
 * @deprecated 此 IC2 兼容 API 已不再需要。计划在下一个主要版本中移除此类及其相关接口。
 */
@Deprecated(since = "1.21.1", forRemoval = true)
public final class BatteryElectricAdapter {

    private BatteryElectricAdapter() {
    }

    /**
     * 将 {@link IBatteryItem} 适配为 {@link IElectricItem} + {@link IElectricItemManager}。
     *
     * <p>返回的对象同时实现两个接口，可直接作为 IC2 兼容的电力物品使用。
     *
     * @param batteryItem 要适配的电池物品实例
     * @return 适配后的 IElectricItem（同时是 IElectricItemManager）
     */
    @Deprecated(since = "1.21.1", forRemoval = true)
    public static ElectricItemWrapper asElectricItem(IBatteryItem batteryItem) {
        return new ElectricItemWrapper(batteryItem);
    }

    /**
     * 将 {@link IElectricItem} 适配为 {@link IBatteryItem}。
     *
     * <p>需要一个 {@link IElectricItemManager} 来执行实际充放电操作。
     *
     * @param electricItem 要适配的电力物品实例
     * @param manager      对应的电力物品管理器
     * @return 适配后的 IBatteryItem
     */
    @Deprecated(since = "1.21.1", forRemoval = true)
    public static IBatteryItem asBatteryItem(IElectricItem electricItem, IElectricItemManager manager) {
        return new BatteryItemWrapper(electricItem, manager);
    }

    /**
     * {@link IElectricItem} + {@link IElectricItemManager} 的组合包装器。
     * 将 {@link IBatteryItem} 的行为桥接到 IC2 兼容 API。
     */
    @Deprecated(since = "1.21.1", forRemoval = true)
    public static class ElectricItemWrapper implements IElectricItem, IElectricItemManager {

        private final IBatteryItem batteryItem;

        @Deprecated(since = "1.21.1", forRemoval = true)
        public ElectricItemWrapper(IBatteryItem batteryItem) {
            this.batteryItem = batteryItem;
        }

        @Override
        @Deprecated(since = "1.21.1", forRemoval = true)
        public boolean canProvideEnergy(ItemStack stack) {
            return !batteryItem.isEmpty(stack);
        }

        @Override
        @Deprecated(since = "1.21.1", forRemoval = true)
        public long getMaxCharge(ItemStack stack) {
            return batteryItem.getMaxEnergy();
        }

        @Override
        @Deprecated(since = "1.21.1", forRemoval = true)
        public int getTier(ItemStack stack) {
            if (batteryItem instanceof Item) {
                long max = batteryItem.getMaxEnergy();
                if (max <= 32) return 1;
                if (max <= 128) return 2;
                if (max <= 512) return 3;
                if (max <= 2048) return 4;
                return 5;
            }
            return 1;
        }

        @Override
        @Deprecated(since = "1.21.1", forRemoval = true)
        public long getTransferLimit(ItemStack stack) {
            return batteryItem.getChargeRate(stack);
        }

        @Override
        @Deprecated(since = "1.21.1", forRemoval = true)
        public long charge(ItemStack stack, long amount, int tier, boolean ignoreTransferLimit, boolean simulate) {
            if (simulate) {
                long current = batteryItem.getEnergy(stack);
                long max = batteryItem.getMaxEnergy();
                long limit = ignoreTransferLimit ? amount : Math.min(amount, getTransferLimit(stack));
                return Math.min(limit, max - current);
            }
            return batteryItem.addEnergy(stack, amount);
        }

        @Override
        @Deprecated(since = "1.21.1", forRemoval = true)
        public long discharge(ItemStack stack, long amount, int tier, boolean ignoreTransferLimit, boolean simulate, boolean remove) {
            if (simulate) {
                long current = batteryItem.getEnergy(stack);
                long limit = ignoreTransferLimit ? amount : Math.min(amount, getTransferLimit(stack));
                return Math.min(limit, current);
            }
            if (remove) {
                return batteryItem.extractEnergy(stack, amount);
            }
            return Math.min(amount, batteryItem.getEnergy(stack));
        }

        @Override
        @Deprecated(since = "1.21.1", forRemoval = true)
        public long getCharge(ItemStack stack) {
            return batteryItem.getEnergy(stack);
        }

        @Override
        @Deprecated(since = "1.21.1", forRemoval = true)
        public boolean canUse(ItemStack stack, long amount) {
            return batteryItem.getEnergy(stack) >= amount;
        }

        @Override
        @Deprecated(since = "1.21.1", forRemoval = true)
        public boolean use(ItemStack stack, long amount, LivingEntity entity) {
            if (canUse(stack, amount)) {
                batteryItem.extractEnergy(stack, amount);
                return true;
            }
            return false;
        }

        @Override
        @Deprecated(since = "1.21.1", forRemoval = true)
        public void chargeFromArmor(ItemStack stack, LivingEntity entity) {
        }

        @Override
        @Deprecated(since = "1.21.1", forRemoval = true)
        public String getToolTip(ItemStack stack) {
            return batteryItem.getEnergy(stack) + " / " + batteryItem.getMaxEnergy();
        }
    }

    /**
     * 将 {@link IElectricItem} 桥接为 {@link IBatteryItem}。
     */
    @Deprecated(since = "1.21.1", forRemoval = true)
    public static class BatteryItemWrapper implements IBatteryItem {

        private final IElectricItem electricItem;
        private final IElectricItemManager manager;

        @Deprecated(since = "1.21.1", forRemoval = true)
        public BatteryItemWrapper(IElectricItem electricItem, IElectricItemManager manager) {
            this.electricItem = electricItem;
            this.manager = manager;
        }

        @Override
        @Deprecated(since = "1.21.1", forRemoval = true)
        public long getMaxEnergy() {
            throw new UnsupportedOperationException(
                "IBatteryItem.getMaxEnergy() 无法在无 ItemStack 的情况下获取 IElectricItem 的最大容量。" +
                "请使用 getMaxEnergy(stack) 代替。");
        }

        @Deprecated(since = "1.21.1", forRemoval = true)
        public long getMaxEnergy(ItemStack stack) {
            return electricItem.getMaxCharge(stack);
        }

        @Override
        @Deprecated(since = "1.21.1", forRemoval = true)
        public long getEnergy(ItemStack stack) {
            return manager.getCharge(stack);
        }

        @Override
        @Deprecated(since = "1.21.1", forRemoval = true)
        public long addEnergy(ItemStack stack, long amount) {
            return manager.charge(stack, amount, electricItem.getTier(stack), false, false);
        }

        @Override
        @Deprecated(since = "1.21.1", forRemoval = true)
        public long extractEnergy(ItemStack stack, long amount) {
            return manager.discharge(stack, amount, electricItem.getTier(stack), false, false, true);
        }

        @Override
        @Deprecated(since = "1.21.1", forRemoval = true)
        public boolean isFull(ItemStack stack) {
            return manager.getCharge(stack) >= electricItem.getMaxCharge(stack);
        }

        @Override
        @Deprecated(since = "1.21.1", forRemoval = true)
        public boolean isEmpty(ItemStack stack) {
            return manager.getCharge(stack) <= 0;
        }

        @Override
        @Deprecated(since = "1.21.1", forRemoval = true)
        public long getChargeRate(ItemStack stack) {
            return electricItem.getTransferLimit(stack);
        }
    }
}