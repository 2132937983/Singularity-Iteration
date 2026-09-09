package com.singularity_iteration.mio_icif.Items.Reactor;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.function.Supplier;

/**
 * MOX燃料棒类
 *
 * 特性（参考原版IC2）：
 * - 基础发电量较低（10 EU/t），但会根据反应堆温度提升效果? * - 发电效率公式：输�?= 4.0 × (当前热量/最大热�? + 1.0
 *   - 堆温0%时：1倍输出（10 EU/t�? *   - 堆温100%时：5倍输出（50 EU/t�? * - 流体模式下，当堆温超�?0%时，发热量翻译? * - 寿命10000 tick（比铀燃料棒短�? */
@SuppressWarnings("null")
public class mio_icif_mox_reactor extends mio_icif_nuclear_reactor {

    /**
     * 构造函数
 * @param properties 物品属性
 * @param maxDurability 最大耐久度（使用寿命，单位：tick�
 * @param energyOutput 基础发电量（EU/tick）
 * @param heatOutput 基础发热量（HU/tick�
 * @param rodType 燃料棒类�
 */
    public mio_icif_mox_reactor(Properties properties, int maxDurability,
                                 int energyOutput, int heatOutput, FuelRodType rodType) {
        super(properties, maxDurability, energyOutput, heatOutput, rodType, () -> null);
    }

    /**
     * 构造函数（带枯竭物品供应商�
 * @param properties 物品属性
 * @param maxDurability 最大耐久度（使用寿命，单位：tick�
 * @param energyOutput 基础发电量（EU/tick）
 * @param heatOutput 基础发热量（HU/tick�
 * @param rodType 燃料棒类�
 * @param depletedItemSupplier 枯竭燃料棒物品供应商
     */
    public mio_icif_mox_reactor(Properties properties, int maxDurability,
                                 int energyOutput, int heatOutput, FuelRodType rodType,
                                 Supplier<Item> depletedItemSupplier) {
        super(properties, maxDurability, energyOutput, heatOutput, rodType, depletedItemSupplier);
    }

    /**
     * 获取MOX燃料棒的实际发电量（考虑堆温加成�
 * 参考IC2：每脉冲发电力?= 4.0 * heatRatio + 1.0
     * @param stack 燃料棒物品堆
     * @param receivedPulses 收到的中子脉冲数
     * @param reactorHeatRatio 反应堆热量比例（当前热量/最大热量）
     * @return 实际发电力
 */
    public int getMoxEnergyOutput(ItemStack stack, int receivedPulses, float reactorHeatRatio) {
        if (isDepleted(stack)) {
            return 0;
        }

        // MOX发电公式：每脉冲 = (4.0 × 热量比例 + 1.0) EU
        // 堆温0%时：1 EU/脉冲，堆�?00%时：5 EU/脉冲
        float moxMultiplier = 4.0f * reactorHeatRatio + 1.0f;

        // This method is invoked once per internal fuel cell, matching IC2's pulse callback.
        int totalPulses = getBaseSelfPulses() + receivedPulses;

        return (int) (5.0f * totalPulses * moxMultiplier);
    }

    /**
     * 获取MOX燃料棒的热量输出（考虑流体模式下的堆温加成�
 * @param stack 燃料棒物品堆
     * @param totalPulses 总脉冲数（自身脉�?+ 收到脉冲�
 * @param reactorHeatRatio 反应堆热量比例（当前热量/最大热量）
     * @param isFluidMode 是否为流体模式
 * @return 实际发热�
 */
    public int getMoxHeatOutput(ItemStack stack, int totalPulses, float reactorHeatRatio, boolean isFluidMode) {
        if (isDepleted(stack)) {
            return 0;
        }

        // 基础发热量（使用三角形数公式�
    int baseHeat = calculateHeatOutput(totalPulses);

        // 流体模式下，当堆温超�?0%时，发热量翻译
    if (isFluidMode && reactorHeatRatio > 0.5f) {
            baseHeat *= 2;
        }

        return baseHeat;
    }

    /**
     * 运行一�?tick（发电模式）
     * @param stack 燃料棒物品堆
     * @param receivedPulses 收到的中子脉冲数
     * @param selfPulses 自身脉冲�
 * @param reactorHeatRatio 反应堆热量比�
 * @return 运行结果
     */
    public OperationResult operateMox(ItemStack stack, int receivedPulses, int selfPulses, float reactorHeatRatio) {
        if (isDepleted(stack)) {
            return new OperationResult(0, 0, true);
        }

        // 获取当前 tick 计数
        int tickCounter = getTickCounter(stack);
        tickCounter++;

        // MOX发电量（根据堆温加成�
    int actualEnergy = getMoxEnergyOutput(stack, receivedPulses, reactorHeatRatio);

        // MOX发热量（发电模式下不翻倍，使用总脉冲数据
    int totalPulses = selfPulses + receivedPulses;
        int actualHeat = calculateHeatOutput(totalPulses);

        boolean depleted = false;

        // �?20 tick 消耗一次耐久�
    if (tickCounter >= OPERATION_INTERVAL) {
            tickCounter = 0;
            depleted = damageItem(stack, 1);
        }

        // 保存 tick 计数
        if (!stack.isEmpty()) {
            setTickCounter(stack, tickCounter);
        }

        return new OperationResult(actualEnergy, actualHeat, depleted);
    }

    /**
     * 获取热量输出（流体模式）
     * @param stack 燃料棒物品堆
     * @param receivedPulses 收到的中子脉冲数
     * @param selfPulses 自身脉冲�
 * @param reactorHeatRatio 反应堆热量比�
 * @return 运行结果
     */
    public OperationResult getMoxHeatOutputForFluid(ItemStack stack, int receivedPulses, int selfPulses, float reactorHeatRatio) {
        if (isDepleted(stack)) {
            return new OperationResult(0, 0, true);
        }

        // 获取当前 tick 计数
        int tickCounter = getTickCounter(stack);
        tickCounter++;

        // 计算总脉冲数
        int totalPulses = selfPulses + receivedPulses;
        
        // MOX发热量（流体模式下根据堆温可能翻倍，然后再�?因为流体模式基础翻倍）
        int actualHeat = getMoxHeatOutput(stack, totalPulses, reactorHeatRatio, true) * 2;

        boolean depleted = false;

        // �?20 tick 消耗一次耐久�
    if (tickCounter >= OPERATION_INTERVAL) {
            tickCounter = 0;
            depleted = damageItem(stack, 1);
        }

        // 保存 tick 计数
        if (!stack.isEmpty()) {
            setTickCounter(stack, tickCounter);
        }

        return new OperationResult(0, actualHeat, depleted);
    }
}


