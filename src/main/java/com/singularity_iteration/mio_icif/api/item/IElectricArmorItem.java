package com.singularity_iteration.mio_icif.api.item;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

import java.util.Collections;
import java.util.List;

/**
 * 电力装甲功能接口。
 * 所有消耗电力运行的装甲都应实现此接口。
 *
 * <h3>伤害吸收机制</h3>
 * <p>电力装甲的伤害吸收由以下两个参数决定：
 * <ul>
 *   <li>{@link #getDamageAbsorptionRatio(EquipmentSlot)} — 吸收比例，决定该部位装甲吸收多少比例的伤害</li>
 *   <li>{@link #getEnergyPerDamage()} — 每点伤害消耗的能量，决定吸收伤害时的能量消耗</li>
 * </ul>
 * <p>当 getEnergyPerDamage() > 0 且 getDamageAbsorptionRatio() > 0 时，装甲将参与伤害吸收。
 * 吸收比例 × 伤害点数 = 该装甲吸收的伤害量，同时消耗 吸收量 × energyPerDamage 的能量。
 *
 * <h3>IC2 参考值</h3>
 * <table>
 *   <tr><th>装甲类型</th><th>部位</th><th>吸收比例</th><th>每点伤害消耗</th></tr>
 *   <tr><td>量子</td><td>头盔</td><td>0.15</td><td>20,000 EU</td></tr>
 *   <tr><td>量子</td><td>胸甲</td><td>0.48 (0.40×1.2)</td><td>20,000 EU</td></tr>
 *   <tr><td>量子</td><td>护腿</td><td>0.30</td><td>20,000 EU</td></tr>
 *   <tr><td>量子</td><td>靴子</td><td>0.15</td><td>20,000 EU</td></tr>
 *   <tr><td>纳米</td><td>头盔</td><td>0.15</td><td>5,000 EU</td></tr>
 *   <tr><td>纳米</td><td>胸甲</td><td>0.40</td><td>5,000 EU</td></tr>
 *   <tr><td>纳米</td><td>护腿</td><td>0.30</td><td>5,000 EU</td></tr>
 *   <tr><td>纳米</td><td>靴子</td><td>0.15</td><td>5,000 EU</td></tr>
 * </table>
 */
public interface IElectricArmorItem extends IBatteryItem {

    /**
     * 获取每 tick 的能量消耗 (EU)
     */
    long getEnergyPerTick();

    /**
     * 获取装甲等级
     */
    int getArmorTier();

    /**
     * 检查是否有足够能量使用一 tick
     */
    default boolean hasEnoughEnergy(ItemStack stack) {
        return getEnergy(stack) >= getEnergyPerTick();
    }

    /**
     * 消耗指定数量的能量
     * @return 是否成功消耗
     */
    default boolean consumeEnergy(ItemStack stack, long amount) {
        if (getEnergy(stack) >= amount) {
            extractEnergy(stack, amount);
            return true;
        }
        return false;
    }

    /**
     * 获取装甲的特性列表。
     * <p>子类可以重写此方法以提供特定的特性列表。
     * 默认实现返回空列表。
     *
     * @param stack 装甲物品堆
     * @return 特性信息列表
     */
    default List<ArmorFeatureInfo> getFeatures(ItemStack stack) {
        return Collections.emptyList();
    }

    /**
     * 获取每点伤害消耗的能量。
     * <p>用于计算装甲吸收伤害时的能量消耗。
     * 默认实现返回 0，表示不消耗能量吸收伤害。
     *
     * @return 每点伤害消耗的能量 (EU)
     */
    default long getEnergyPerDamage() {
        return 0;
    }

    /**
     * 获取该装甲在指定装备槽位的伤害吸收比例。
     * <p>此比例基于 IC2 的 {@code getBaseAbsorptionRatio() × getDamageAbsorptionRatio()} 计算。
     * <p>当返回值 > 0 且 {@link #getEnergyPerDamage()} > 0 时，该装甲将参与伤害吸收。
     * 吸收量 = 剩余伤害 × 吸收比例，能量消耗 = 吸收量 × energyPerDamage。
     *
     * <p>默认实现返回 0，表示不吸收伤害。Addon 开发者应覆盖此方法
     * 来定义自定义装甲的伤害吸收行为。
     *
     * @param slot 装备槽位
     * @return 伤害吸收比例 (0.0 ~ 1.0+)，0 表示不吸收
     */
    default float getDamageAbsorptionRatio(EquipmentSlot slot) {
        return 0.0F;
    }
}