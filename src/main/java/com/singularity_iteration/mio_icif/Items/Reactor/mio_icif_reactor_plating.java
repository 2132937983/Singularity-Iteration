package com.singularity_iteration.mio_icif.Items.Reactor;

import com.singularity_iteration.mio_icif.api.reactor.ReactorComponentType;

import net.minecraft.world.item.ItemStack;

/**
 * 反应堆隔板（防护板）
 *
 * 特性（参考IC2）：
 * - 增加反应堆的热量上限
 * - 降低反应堆爆炸范围? * - 不储存热量，不吸热，不散�? * - 纯粹的被动防护组�? *
 * 工作原理�? * - 每个隔板提供固定的热量上限加载? * - 每个隔板降低一定百分比的爆炸范围? * - 多个隔板的效果可以叠�? *
 * 隔板类型�? * - 基础反应堆隔板：+1000热量上限制?5%爆炸范围
 * - 密封反应堆隔热板�?500热量上限制?10%爆炸范围
 * - 高热容反应堆隔板�?1700热量上限制?1%爆炸范围
 */
@SuppressWarnings("null")
public class mio_icif_reactor_plating extends mio_icif_reactor {

    // 提供的热量上�
private final int heatCapacityBonus;

    // 爆炸范围降低百分比（0-100�
private final int explosionReductionPercent;

    /**
     * 构造函数
 * @param properties 物品属性
 * @param heatCapacityBonus 提供的热量上�
 * @param explosionReductionPercent 爆炸范围降低百分配
 */
    public mio_icif_reactor_plating(Properties properties, int heatCapacityBonus, int explosionReductionPercent) {
        super(properties.stacksTo(64), 1, ReactorComponentType.REACTOR_PLATING);
        this.heatCapacityBonus = heatCapacityBonus;
        this.explosionReductionPercent = explosionReductionPercent;
    }

    /**
     * 获取提供的热量上�
 * @param stack 物品栈
 * @return 热量上限加成
     */
    public int getHeatCapacityBonus(ItemStack stack) {
        return heatCapacityBonus;
    }

    /**
     * 获取爆炸范围降低百分配
 * @param stack 物品栈
 * @return 爆炸范围降低百分比（0-100�
 */
    public int getExplosionReductionPercent(ItemStack stack) {
        return explosionReductionPercent;
    }

    /**
     * 反应堆隔板不显示耐久�
 */
    @Override
    public boolean isBarVisible(ItemStack stack) {
        return false;
    }
}

