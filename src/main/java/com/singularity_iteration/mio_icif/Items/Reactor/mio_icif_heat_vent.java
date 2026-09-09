package com.singularity_iteration.mio_icif.Items.Reactor;

import com.singularity_iteration.mio_icif.api.reactor.ReactorComponentType;

import com.singularity_iteration.mio_icif.Items.DataComponent.ReactorComponentData;
import com.singularity_iteration.mio_icif.Items.Normal.mio_icif_data_components;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * 散热片基�? *
 * 特点�? * - 自身存储热量，热量存储与耐久度成反比
 * - 每增加一点热量，耐久度就减少一�? * - 每秒向反应堆外散发一定热�? * - 可以从反应堆吸收热量
 * - 超过热量上限会熔毁（耐久度变化?�? *
 * 参考IC2超频散热片：
 * - 存储1000点热�? * - 每秒散发自身20点热�? * - 每秒从反应堆吸收36点热�? */
@SuppressWarnings("null")
public class mio_icif_heat_vent extends mio_icif_reactor {

    // 最大热量存�
protected final int maxHeatStorage;

    // 自身散热速度（每秒散发到环境中的热量�
protected final int selfCoolingRate;

    // 从反应堆吸热速度（每秒从反应堆吸收的热量�
protected final int reactorHeatAbsorptionRate;

    // 操作间隔�?0 tick = 1秒）
    public static final int OPERATION_INTERVAL = 20;

    /**
     * 构造函数
 * @param properties 物品属性
 * @param maxDurability 最大耐久度（也是最大热量存储，因为热量与耐久度成反比�
 * @param selfCoolingRate 自身散热速度（每秒散发到环境中的热量�
 * @param reactorHeatAbsorptionRate 从反应堆吸热速度（每秒从反应堆吸收的热量�
 */
    public mio_icif_heat_vent(Properties properties, int maxDurability,
                               int selfCoolingRate, int reactorHeatAbsorptionRate) {
        super(properties, maxDurability, ReactorComponentType.HEAT_SINK, false, true);
        this.maxHeatStorage = maxDurability;
        this.selfCoolingRate = selfCoolingRate;
        this.reactorHeatAbsorptionRate = reactorHeatAbsorptionRate;
    }

    // ==================== 热量存储相关 ====================

    /**
     * 获取当前存储的热�
 * @param stack 物品栈
 * @return 当前热量
     */
    @Override
    public int getStoredHeat(ItemStack stack) {
        ReactorComponentData data = stack.get(mio_icif_data_components.REACTOR_COMPONENT_DATA.get());
        return data != null ? data.storedValue() : 0;
    }

    /**
     * 设置存储的热�
 * @param stack 物品栈
 * @param heat 热量�
 */
    @Override
    public void setStoredHeat(ItemStack stack, int heat) {
        heat = Math.max(0, Math.min(heat, maxHeatStorage));
        ReactorComponentData data = stack.get(mio_icif_data_components.REACTOR_COMPONENT_DATA.get());
        if (data != null) {
            stack.set(mio_icif_data_components.REACTOR_COMPONENT_DATA.get(), data.withStoredValue(heat));
        }
        if (heat >= maxHeatStorage) {
            stack.setCount(0);
        }
    }

    /**
     * 增加热量
     * @param stack 物品栈
 * @param amount 增加的热�
 * @return 实际增加的热�
 */
    public int addHeat(ItemStack stack, int amount) {
        int currentHeat = getStoredHeat(stack);
        int newHeat = currentHeat + amount;

        // 检查是否会超过上限
        if (newHeat >= maxHeatStorage) {
            // 熔毁
            setStoredHeat(stack, maxHeatStorage);
            return maxHeatStorage - currentHeat;
        }

        setStoredHeat(stack, newHeat);
        return amount;
    }

    /**
     * 减少热量
     * @param stack 物品栈
 * @param amount 减少的热�
 * @return 实际减少的热�
 */
    public int removeHeat(ItemStack stack, int amount) {
        int currentHeat = getStoredHeat(stack);
        int actualRemove = Math.min(amount, currentHeat);
        setStoredHeat(stack, currentHeat - actualRemove);
        return actualRemove;
    }

    // ==================== 散热相关 ====================

    /**
     * 获取自身散热速度
     */
    public int getSelfCoolingRate() {
        return selfCoolingRate;
    }

    /**
     * 获取从反应堆吸热速度
     */
    public int getReactorHeatAbsorptionRate() {
        return reactorHeatAbsorptionRate;
    }

    /**
     * 执行散热操作（每tick调用一次）
     * 参考IC2 ItemReactorVent.processChamber 机制造
 * 1. 先从反应堆吸热（reactorVent�
 * 2. 再自身散热（selfVent），散发的热量计入反应堆总散�
 * @param stack 物品栈
 * @param reactorHeat 当前反应堆热�
 * @param reactorHeatDissipated 反应堆总散热量（引用传递，用于累加散发的热量）
     * @return 实际从反应堆吸收的热�
 */
    public int cool(ItemStack stack, long reactorHeat, java.util.concurrent.atomic.AtomicInteger reactorHeatDissipated) {
        if (stack.isEmpty()) return 0;

        int selfCoolingPerTick = selfCoolingRate;
        int reactorAbsorbPerTick = reactorHeatAbsorptionRate;

        int actualAbsorbed = 0;
        if (reactorAbsorbPerTick > 0) {
            int heatToAbsorb = (int) Math.min(reactorHeat, reactorAbsorbPerTick);
            int currentHeat = getStoredHeat(stack);
            int availableSpace = maxHeatStorage - currentHeat;
            int canAbsorb = Math.min(heatToAbsorb, availableSpace);
            if (canAbsorb < heatToAbsorb) {
                return 0;
            }
            actualAbsorbed = addHeat(stack, heatToAbsorb);
        }

        int actualSelfCooling = removeHeat(stack, selfCoolingPerTick);
        if (actualSelfCooling > 0 && reactorHeatDissipated != null) {
            reactorHeatDissipated.addAndGet(actualSelfCooling);
        }

        return actualAbsorbed;
    }

    /**
     * 吸收热量（用于被其他元件传递热量）
     * @param stack 物品栈
 * @param heatToAbsorb 要吸收的热量
     * @return 实际吸收的热�
 */
    @Override
    public int absorbHeat(ItemStack stack, int heatToAbsorb) {
        return addHeat(stack, heatToAbsorb);
    }

    /**
     * 获取最大热量存�
 */
    @Override
    public int getMaxHeatStorage() {
        return maxHeatStorage;
    }

    /**
     * 检查是否已经熔�
 * @param stack 物品栈
 * @return 如果熔毁返回true
     */
    public boolean isMelted(ItemStack stack) {
        return getStoredHeat(stack) >= maxHeatStorage || stack.isEmpty();
    }

    /**
     * 获取剩余可吸收热�
 * @param stack 物品栈
 * @return 剩余热量容量
     */
    public int getRemainingHeatCapacity(ItemStack stack) {
        return maxHeatStorage - getStoredHeat(stack);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return 0x00FF00;
        }
        if (maxHeatStorage <= 0) {
            return 0x00FF00;
        }
        float heatRatio = (float) getStoredHeat(stack) / maxHeatStorage;
        if (heatRatio < 0.3F) {
            return 0x00FF00;
        } else if (heatRatio < 0.6F) {
            return 0xFFFF00;
        } else if (heatRatio < 0.8F) {
            return 0xFF8000;
        } else {
            return 0xFF0000;
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, java.util.List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        if (maxHeatStorage > 0) {
            tooltipComponents.add(Component.translatable("item.mio_icif.reactor.component.stored_heat",
                getStoredHeat(stack), maxHeatStorage).withStyle(ChatFormatting.GRAY));
        }
    }
}