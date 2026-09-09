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
 * 冷却单元（热交换器）
 *
 * 特性（参考IC2）：
 * - 自身可以储存热量
 * - 从周围燃料棒吸收热量
 * - 无法自身散热（需要配合元件散热片来散发热量）
 * - 可以给反应堆添加额外的热量上�? * - 提供的热量上�?= 初始耐久�? * - 超过热量上限会熔毁（耐久度变化?�? *
 * 工作原理�? * - 每tick从周围的燃料棒吸收热�? * - 自身不散发热量，热量需要通过相邻的元件散热片来散�? * - 冷却单元的存在会增加反应堆的总热量上�? * - 热量与耐久度成反比，热量越高耐久度越�? * - 当热量达到上限时，冷却单元熔毁消耗? *
 * 使用建议�? * - 必须配合元件散热片使用（四周都摆满）
 * - 用于增加反应堆的热量存储容量
 * - 适合需要长时间运行但又不想频繁换料的反应堆
 */
@SuppressWarnings("null")
public class mio_icif_heat_exchanger extends mio_icif_reactor {

    // 最大热量存储（等于最大耐久度）
    public final int maxHeatStorage;

    // 吸热速度（每tick从周围燃料棒吸收的热量）
    public static final int HEAT_ABSORPTION_RATE = 8;  // 每tick 8点热量（已翻倍）

    /**
     * 构造函数?- 默认10000热量
     * @param properties 物品属性
 */
    public mio_icif_heat_exchanger(Properties properties) {
        this(properties, 10000);
    }

    /**
     * 构造函数?- 指定热量存储
     * @param properties 物品属性
 * @param maxHeatStorage 最大热量存�
 */
    public mio_icif_heat_exchanger(Properties properties, int maxHeatStorage) {
        super(properties, maxHeatStorage, ReactorComponentType.HEAT_EXCHANGER, false, true);
        this.maxHeatStorage = maxHeatStorage;
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
     * 添加热量
     * @param stack 物品栈
 * @param heat 要添加的热量
     * @return 实际添加的热�
 */
    public int addHeat(ItemStack stack, int heat) {
        int currentHeat = getStoredHeat(stack);
        int maxAddable = maxHeatStorage - currentHeat;
        int actualAdd = Math.min(heat, maxAddable);

        if (actualAdd > 0) {
            setStoredHeat(stack, currentHeat + actualAdd);
        }

        return actualAdd;
    }

    /**
     * 减少热量
     * @param stack 物品栈
 * @param heat 要减少的热量
     * @return 实际减少的热�
 */
    public int removeHeat(ItemStack stack, int heat) {
        int currentHeat = getStoredHeat(stack);
        int actualRemove = Math.min(heat, currentHeat);

        if (actualRemove > 0) {
            setStoredHeat(stack, currentHeat - actualRemove);
        }

        return actualRemove;
    }

    // ==================== 熔毁相关 ====================

    /**
     * 检查冷却单元是否熔�
 * @param stack 物品栈
 * @return 是否熔毁
     */
    @Override
    public boolean isMelted(ItemStack stack) {
        return getStoredHeat(stack) >= maxHeatStorage;
    }

    /**
     * 冷却单元会熔�
 * @param stack 物品栈
 * @return 始终返回true
     */
    @Override
    public boolean shouldMelt(ItemStack stack) {
        return true;
    }

    // ==================== 热量上限相关 ====================

    /**
     * 获取冷却单元提供的额外热量上�
 * 冷却单元的额外热量上�?= 初始耐久�?= maxHeatStorage
     * @param stack 物品栈
 * @return 提供的额外热量上�
 */
    public int getExtraHeatCapacity(ItemStack stack) {
        // 如果已经熔毁，返回?
        if (isMelted(stack) || stack.isEmpty()) {
            return 0;
        }
        // 冷却单元提供的额外热量上限等于它的最大耐久�
    return maxHeatStorage;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, java.util.List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        tooltipComponents.add(Component.translatable("item.mio_icif.reactor.component.stored_heat",
            getStoredHeat(stack), maxHeatStorage).withStyle(ChatFormatting.GRAY));
    }
}

