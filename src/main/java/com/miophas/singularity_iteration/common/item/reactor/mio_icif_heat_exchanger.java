package com.miophas.singularity_iteration.common.item.reactor;

import com.miophas.singularity_iteration.core.api.reactor.ReactorComponentType;

import com.miophas.singularity_iteration.common.item.component.ReactorComponentData;
import com.miophas.singularity_iteration.common.registry.mio_icif_data_components;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * Coolant cell (heat exchanger).
 *
 * Properties (based on IC2):
 * - The cell stores heat.
 * - The cell absorbs heat from adjacent fuel rods.
 * - The cell cannot vent heat by itself. Adjacent component heat vents must remove the heat.
 * - The cell adds extra heat capacity to the reactor.
 * - The extra heat capacity equals the initial durability (maxHeatStorage).
 * - The cell melts when its heat reaches the limit. The durability shows the heat.
 *
 * Operation:
 * - Each tick, the cell absorbs heat from adjacent fuel rods.
 * - The cell does not vent heat. Adjacent component heat vents remove the heat.
 * - The cell increases the total heat capacity of the reactor.
 * - Durability decreases as heat increases.
 * - When the heat reaches the limit, the cell melts and is consumed.
 *
 * Usage:
 * - Surround the cell with component heat vents on all 4 sides.
 * - Use the cell to increase the heat storage of the reactor.
 * - Use the cell in reactors that must run for a long time without refueling.
 */
@SuppressWarnings("null")
public class mio_icif_heat_exchanger extends mio_icif_reactor {

    // 最大热量存储（等于最大耐久度）
    public final int maxHeatStorage;

    // 吸热速度（每tick从周围燃料棒吸收的热量）
    public static final int HEAT_ABSORPTION_RATE = 8;  // 每tick 8点热量（已翻倍）

    /**
     * Constructor. The default heat capacity is 10000.
     * @param properties the item properties
 */
    public mio_icif_heat_exchanger(Properties properties) {
        this(properties, 10000);
    }

    /**
     * Constructor with a specified heat storage.
     * @param properties the item properties
     * @param maxHeatStorage the maximum heat that the cell can store
     */
    public mio_icif_heat_exchanger(Properties properties, int maxHeatStorage) {
        super(properties, maxHeatStorage, ReactorComponentType.HEAT_EXCHANGER, false, true);
        this.maxHeatStorage = maxHeatStorage;
    }

    // ==================== 热量存储相关 ====================

    /**
     * Gets the stored heat.
     * @param stack the item stack
     * @return the current heat
     */
    @Override
    public int getStoredHeat(ItemStack stack) {
        ReactorComponentData data = stack.get(mio_icif_data_components.REACTOR_COMPONENT_DATA.get());
        return data != null ? data.storedValue() : 0;
    }

    /**
     * Sets the stored heat.
     * The value is clamped to 0..maxHeatStorage. The cell is destroyed when the heat reaches maxHeatStorage.
     * @param stack the item stack
     * @param heat the new heat value
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
     * Adds heat.
     * @param stack the item stack
     * @param heat the heat to add
     * @return the heat that was added
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
     * Removes heat.
     * @param stack the item stack
     * @param heat the heat to remove
     * @return the heat that was removed
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
     * Checks if the coolant cell has melted.
     * @param stack the item stack
     * @return true if the stored heat is at or above maxHeatStorage
     */
    @Override
    public boolean isMelted(ItemStack stack) {
        return getStoredHeat(stack) >= maxHeatStorage;
    }

    /**
     * Coolant cells can melt.
     * @param stack the item stack
     * @return always true
     */
    @Override
    public boolean shouldMelt(ItemStack stack) {
        return true;
    }

    // ==================== 热量上限相关 ====================

    /**
     * Gets the extra heat capacity that the coolant cell gives to the reactor.
     * The extra heat capacity equals the initial durability, which is maxHeatStorage.
     * @param stack the item stack
     * @return the extra heat capacity, or 0 if the cell is melted or the stack is empty
     */
    public int getExtraHeatCapacity(ItemStack stack) {
        // Return 0 if the cell has melted.
        if (isMelted(stack) || stack.isEmpty()) {
            return 0;
        }
        // The extra heat capacity equals the maximum durability.
    return maxHeatStorage;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, java.util.List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        tooltipComponents.add(Component.translatable("item.mio_icif.reactor.component.stored_heat",
            getStoredHeat(stack), maxHeatStorage).withStyle(ChatFormatting.GRAY));
    }
}

