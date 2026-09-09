package com.singularity_iteration.mio_icif.api.upgrade.tile;

import java.util.Collection;
import java.util.Set;
import net.minecraft.world.item.ItemStack;

/**
 * 升级物品接口。
 * <p>
 * 对应 IC2 1.12.2 的 {@code IUpgradeItem}。
 * 实现此接口的物品可以作为机器的升级模块，提供特殊功能和行为。
 * <p>
 * 升级物品有三个主要回调：
 * <ul>
 *   <li>{@link #isSuitableFor} - 检查升级是否适用于指定机器</li>
 *   <li>{@link #onTick} - 每 tick 调用，可以执行持续效果</li>
 *   <li>{@link #onProcessEnd} - 配方处理完成后调用，可以修改输出</li>
 * </ul>
 */
public interface IUpgradeItem {

    /**
     * 检查此升级物品是否适用于具有指定属性的机器。
     *
     * @param stack 升级物品堆
     * @param properties 机器的可升级属性集合
     * @return 如果适用则返回 true
     */
    boolean isSuitableFor(ItemStack stack, Set<UpgradableProperty> properties);

    /**
     * 每 tick 调用此方法。
     * <p>
     * 升级可以在此执行持续效果，如自动弹出物品、红石控制等。
     *
     * @param stack 升级物品堆
     * @param block 安装了此升级的机器
     * @return 如果此 tick 被升级消耗（阻止机器正常 tick）则返回 true
     */
    boolean onTick(ItemStack stack, IUpgradableBlock block);

    /**
     * 配方处理完成后调用此方法。
     * <p>
     * 升级可以在此修改输出物品，如增加产量、添加副产物等。
     *
     * @param stack 升级物品堆
     * @param block 安装了此升级的机器
     * @param outputs 当前输出物品列表（可修改）
     * @return 修改后的输出物品列表
     */
    Collection<ItemStack> onProcessEnd(ItemStack stack, IUpgradableBlock block, Collection<ItemStack> outputs);

    /**
     * 获取升级类型的名称
     */
    default String getUpgradeTypeName() {
        return "unknown";
    }

    /**
     * 获取 API 级升级类型枚举
     */
    default com.singularity_iteration.mio_icif.api.upgrade.IUpgradeAPI.UpgradeType getApiUpgradeType() {
        return com.singularity_iteration.mio_icif.api.upgrade.IUpgradeAPI.UpgradeType.CUSTOM;
    }

    /**
     * 获取升级类型枚举（便捷方法，等价于 {@link #getApiUpgradeType()}）
     *
     * <p>子类应覆盖此方法以返回正确的升级类型，
     * 供 API 层在不加载具体实现类的情况下识别升级种类。
     *
     * @return 升级类型，默认返回 {@code CUSTOM}
     */
    default com.singularity_iteration.mio_icif.api.upgrade.IUpgradeAPI.UpgradeType getUpgradeType() {
        return getApiUpgradeType();
    }

    /**
     * 获取升级效果对象。
     *
     * <p>将当前升级的所有效果参数（速度、能耗、容量等）封装为
     * {@link com.singularity_iteration.mio_icif.api.upgrade.IUpgradeAPI.IUpgradeEffect}，
     * 供 API 层统一计算组合效果。
     *
     * <p>默认实现根据此接口的各 {@code getXxxBonus()} 方法组合出一个效果对象。
     * 子类可覆盖以提供更精确的效果计算。
     *
     * @param stack 升级物品堆
     * @return 升级效果对象，不为 null
     */
    default com.singularity_iteration.mio_icif.api.upgrade.IUpgradeAPI.IUpgradeEffect getUpgradeEffect(ItemStack stack) {
        return new com.singularity_iteration.mio_icif.api.upgrade.IUpgradeAPI.IUpgradeEffect() {
            @Override
            public double getSpeedMultiplier() {
                return 1.0 / getSpeedBonus();
            }

            @Override
            public double getEnergyMultiplier() {
                return getEnergyBonus();
            }

            @Override
            public long getExtraCapacity() {
                return getExtraCapacity();
            }

            @Override
            public int getExtraOutputSlots() {
                return getExtraOutputSlots();
            }

            @Override
            public boolean hasAutoEject() {
                return hasAutoEject();
            }

            @Override
            public boolean hasAutoImport() {
                return hasAutoImport();
            }

            @Override
            public boolean hasRedstoneControl() {
                return hasRedstoneControl();
            }

            @Override
            public int getTierUpgrade() {
                return getTierUpgrade();
            }
        };
    }

    /**
     * 是否有自动弹出功能
     */
    default boolean hasAutoEject() {
        return false;
    }

    /**
     * 是否有自动导入功能
     */
    default boolean hasAutoImport() {
        return false;
    }

    /**
     * 是否有红石控制功能
     */
    default boolean hasRedstoneControl() {
        return false;
    }

    /**
     * 获取额外能量容量加成
     */
    default long getExtraCapacity() {
        return 0;
    }

    /**
     * 获取额外输出槽位数量
     */
    default int getExtraOutputSlots() {
        return 0;
    }

    /**
     * 获取升级等级加成
     */
    default int getTierUpgrade() {
        return 0;
    }

    /**
     * 检查此升级是否是定向的
     */
    default boolean isDirectional() {
        return false;
    }

    /**
     * 获取速度加成倍率（值越小越快）
     */
    default float getSpeedBonus() {
        return 1.0f;
    }

    /**
     * 获取能量消耗倍率
     */
    default float getEnergyBonus() {
        return 1.0f;
    }

    /**
     * 获取升级等级 (1-4, 对应 LV-MV-HV-EV)
     */
    default int getUpgradeTier(ItemStack stack) {
        return 1;
    }
}
