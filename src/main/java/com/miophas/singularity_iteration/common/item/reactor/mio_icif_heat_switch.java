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
 * Base class for heat exchangers (heat switches).
 *
 * Behavior (IC2 reference):
 * - It absorbs heat from fuel rods.
 * - It moves heat between components.
 * - It can move heat to the reactor hull.
 * - It has its own heat storage.
 * - It melts when its heat reaches the maximum.
 *
 * How it works:
 * - It absorbs heat from adjacent fuel rods.
 * - It moves heat to adjacent components.
 * - It moves heat to the reactor hull.
 * - Stored heat is the inverse of durability.
 *
 * Heat exchanger types:
 * - Basic heat exchanger: 2500 heat, 12 to adjacent components, 4 to the reactor.
 * - Reactor heat exchanger: 5000 heat, 0 to adjacent components, 72 to the reactor.
 * - Component heat exchanger: 5000 heat, 36 to adjacent components, 0 to the reactor.
 * - Advanced heat exchanger: 10000 heat, 24 to adjacent components, 8 to the reactor.
 */
@SuppressWarnings("null")
public class mio_icif_heat_switch extends mio_icif_reactor {

    // Maximum heat storage.
protected final int maxHeatStorage;

    // 向相邻元件传递的热量
    protected final int heatTransferToAdjacent;

    // 向核反应堆传递的热量
    protected final int heatTransferToReactor;

    /**
     * Creates a heat exchanger.
     * @param properties the item properties
     * @param maxHeatStorage the maximum heat storage
     * @param heatTransferToAdjacent the heat moved to adjacent components
     * @param heatTransferToReactor the heat moved to the reactor hull
     */
    public mio_icif_heat_switch(Properties properties, int maxHeatStorage,
                                int heatTransferToAdjacent, int heatTransferToReactor) {
        super(properties, maxHeatStorage, ReactorComponentType.HEAT_SWITCH, false, true);
        this.maxHeatStorage = maxHeatStorage;
        this.heatTransferToAdjacent = heatTransferToAdjacent;
        this.heatTransferToReactor = heatTransferToReactor;
    }

    // ==================== 热量存储相关 ====================

    /**
     * Gets the stored heat.
     * @param stack the item stack
     * @return the stored heat
     */
    @Override
    public int getStoredHeat(ItemStack stack) {
        ReactorComponentData data = stack.get(mio_icif_data_components.REACTOR_COMPONENT_DATA.get());
        return data != null ? data.storedValue() : 0;
    }

    /**
     * Sets the stored heat. The value is clamped to 0..maxHeatStorage.
     * The stack is destroyed when the heat reaches the maximum.
     * @param stack the item stack
     * @param heat the heat value
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
     * Adds heat, limited by the free heat capacity.
     * @param stack the item stack
     * @param heat the heat to add
     * @return the heat that was actually added
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
     * Removes heat, limited by the stored heat.
     * @param stack the item stack
     * @param heat the heat to remove
     * @return the heat that was actually removed
     */
    public int removeHeat(ItemStack stack, int heat) {
        int currentHeat = getStoredHeat(stack);
        int actualRemove = Math.min(heat, currentHeat);

        if (actualRemove > 0) {
            setStoredHeat(stack, currentHeat - actualRemove);
        }

        return actualRemove;
    }

    // ==================== Heat transfer ====================

    /**
     * Gets the heat moved to adjacent components.
     * @return the heat moved per transfer
     */
    public int getHeatTransferToAdjacent() {
        return heatTransferToAdjacent;
    }

    /**
     * Gets the heat moved to the reactor hull.
     * @return the heat moved per transfer
     */
    public int getHeatTransferToReactor() {
        return heatTransferToReactor;
    }

    /**
     * Gets the maximum heat storage.
     * @return the maximum heat storage
     */
    public int getMaxHeatStorage() {
        return maxHeatStorage;
    }

    /**
     * Calculates the heat percentage.
     * @param stack the item stack
     * @return the heat percentage, from 0.0 to 100.0
     */
    public double getHeatPercentage(ItemStack stack) {
        return (double) getStoredHeat(stack) * 100.0 / maxHeatStorage;
    }

    // ==================== 熔毁相关 ====================

    /**
     * Checks whether the heat exchanger has melted.
     * @param stack the item stack
     * @return true if the stored heat is at or above the maximum
     */
    @Override
    public boolean isMelted(ItemStack stack) {
        return getStoredHeat(stack) >= maxHeatStorage;
    }

    /**
     * Heat exchangers can melt.
     * @param stack the item stack
     * @return always true
     */
    @Override
    public boolean shouldMelt(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, java.util.List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        tooltipComponents.add(Component.translatable("item.mio_icif.reactor.component.stored_heat",
            getStoredHeat(stack), maxHeatStorage).withStyle(ChatFormatting.GRAY));
    }
}

