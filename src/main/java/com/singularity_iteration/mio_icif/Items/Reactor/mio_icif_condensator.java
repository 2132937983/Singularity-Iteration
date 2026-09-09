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
 * 冷凝模块基类
 *
 * 特性（参考IC2）：
 * - 可以吸收附近元件的热�? * - 不能被其他元件（如散热片）散�? * - 可以通过合成方式回复耐久
 * - 自身有热量存储上�? * - 超过热量上限会失效（不会消失，但无法继续吸热�? *
 * 工作原理�? * - 从相邻散热片、冷却单元、热交换器等元件吸收热量
 * - 吸收的热量存储在自身
 * - 可以通过合成修复来减少存储的热量（恢复耐久�? *
 * 冷凝模块类型�? * - 红石冷凝模方块?0000热量存储，红石修复?0000耐久
 * - 青金石冷凝模块：100000热量存储，红石修复?0000，青金石修复40000
 */
@SuppressWarnings("null")
public class mio_icif_condensator extends mio_icif_reactor {

    // 最大热量存储（等于最大耐久度）
    protected final int maxHeatStorage;

    // 每tick吸收的热�
protected final int heatAbsorptionRate;

    // 红石修复�
protected final int redstoneRepairAmount;

    // 青金石修复量（红石冷凝模块为0�
protected final int lapisRepairAmount;

    /**
     * 构造函数
 * @param properties 物品属性
 * @param maxHeatStorage 最大热量存�
 * @param heatAbsorptionRate 每tick吸收的热�
 * @param redstoneRepairAmount 红石修复�
 * @param lapisRepairAmount 青金石修复量
     */
    public mio_icif_condensator(Properties properties, int maxHeatStorage,
                                int heatAbsorptionRate, int redstoneRepairAmount, int lapisRepairAmount) {
        super(properties, maxHeatStorage, ReactorComponentType.CONDENSATOR, false, true);
        this.maxHeatStorage = maxHeatStorage;
        this.heatAbsorptionRate = heatAbsorptionRate;
        this.redstoneRepairAmount = redstoneRepairAmount;
        this.lapisRepairAmount = lapisRepairAmount;
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
    }

    /**
     * 添加热量（吸收热量）
     * 参考IC2 ItemReactorCondensator.alterHeat：只吸收正热量，不散�
 * @param stack 物品栈
 * @param heat 要添加的热量
     * @return 实际添加的热�
 */
    public int addHeat(ItemStack stack, int heat) {
        if (heat < 0) return heat; // 不散发热�?
        int currentHeat = getStoredHeat(stack);
        int maxAddable = maxHeatStorage - currentHeat;
        int actualAdd = Math.min(heat, maxAddable);

        if (actualAdd > 0) {
            setStoredHeat(stack, currentHeat + actualAdd);
        }

        return heat - actualAdd; // 返回剩余未吸收的热量
    }

    /**
     * 减少热量（修复）
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

    // ==================== 修复相关 ====================

    /**
     * 使用红石修复
     * @param stack 物品栈
 * @return 是否修复成功
     */
    public boolean repairWithRedstone(ItemStack stack) {
        if (getStoredHeat(stack) > 0) {
            removeHeat(stack, redstoneRepairAmount);
            return true;
        }
        return false;
    }

    /**
     * 使用青金石修复
 * @param stack 物品栈
 * @return 是否修复成功
     */
    public boolean repairWithLapis(ItemStack stack) {
        if (lapisRepairAmount > 0 && getStoredHeat(stack) > 0) {
            removeHeat(stack, lapisRepairAmount);
            return true;
        }
        return false;
    }

    /**
     * 获取红石修复�
 * @return 红石修复�
 */
    public int getRedstoneRepairAmount() {
        return redstoneRepairAmount;
    }

    /**
     * 获取青金石修复量
     * @return 青金石修复量
     */
    public int getLapisRepairAmount() {
        return lapisRepairAmount;
    }

    // ==================== 吸热相关 ====================

    /**
     * 获取每tick吸收的热�
 * @return 吸收速率
     */
    public int getHeatAbsorptionRate() {
        return heatAbsorptionRate;
    }

    /**
     * 获取最大热量存�
 * @return 最大热�
 */
    public int getMaxHeatStorage() {
        return maxHeatStorage;
    }

    /**
     * 检查冷凝模块是否已满
 * @param stack 物品栈
 * @return 是否已满
     */
    public boolean isFull(ItemStack stack) {
        return getStoredHeat(stack) >= maxHeatStorage;
    }

    /**
     * 冷凝模块是否显示耐久�
 */
    @Override
    public boolean isBarVisible(ItemStack stack) {
        return getStoredHeat(stack) > 0;
    }

    /**
     * 获取耐久条颜色（蓝色表示冷凝模方块
 */
    @Override
    public int getBarColor(ItemStack stack) {
        return 0x00BFFF; // 深天蓝色
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, java.util.List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        tooltipComponents.add(Component.translatable("item.mio_icif.reactor.component.stored_heat",
            getStoredHeat(stack), maxHeatStorage).withStyle(ChatFormatting.GRAY));
    }
}

