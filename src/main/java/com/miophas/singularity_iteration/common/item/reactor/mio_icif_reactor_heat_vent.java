package com.miophas.singularity_iteration.common.item.reactor;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/**
 * Reactor heat vent.
 *
 * Behavior (IC2 reference):
 * - Maximum heat storage: 1000.
 * - Self cooling: 5 heat per tick.
 * - It takes heat from fuel rods first.
 * - When no fuel rod sends heat, it pulls 5 heat per tick from the reactor hull.
 * - It melts and disappears when its heat reaches the maximum.
 *
 * How it works:
 * - It receives heat from adjacent fuel rods first.
 * - If no fuel rod sends heat, it pulls 5 heat per tick from the reactor hull.
 * - At the same time, it vents 5 heat per tick to the environment.
 * - Stored heat is the inverse of durability. More heat gives less durability.
 * - When the heat reaches MAX_HEAT (1000), the vent melts and is consumed.
 */
@SuppressWarnings("null")
public class mio_icif_reactor_heat_vent extends mio_icif_heat_vent {

    // 反应堆散热片参数
    public static final int MAX_HEAT = 1000;
    public static final int SELF_COOLING = 5;
    public static final int REACTOR_ABSORPTION = 5;

    // NBT键名
    private static final String RECEIVED_FROM_FUEL_ROD_KEY = "ReceivedFromFuelRod";

    /**
     * Creates a reactor heat vent.
     * @param properties the item properties
     */
    public mio_icif_reactor_heat_vent(Properties properties) {
        super(properties, MAX_HEAT, SELF_COOLING, REACTOR_ABSORPTION);
    }

    /**
     * Checks whether the vent received heat from a fuel rod in this tick.
     * @param stack the item stack
     * @return true if the vent received heat from a fuel rod
     */
    public boolean hasReceivedFromFuelRod(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            CompoundTag tag = customData.copyTag();
            if (tag.contains(RECEIVED_FROM_FUEL_ROD_KEY)) {
                return tag.getBoolean(RECEIVED_FROM_FUEL_ROD_KEY);
            }
        }
        return false;
    }

    /**
     * Sets whether the vent received heat from a fuel rod.
     * @param stack the item stack
     * @param received true if the vent received heat from a fuel rod
     */
    public void setReceivedFromFuelRod(ItemStack stack, boolean received) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        CompoundTag tag = customData != null ? customData.copyTag() : new CompoundTag();
        tag.putBoolean(RECEIVED_FROM_FUEL_ROD_KEY, received);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    /**
     * Clears the "received from fuel rod" flag. Call this at the start of each tick.
     * @param stack the item stack
     */
    public void resetReceivedFromFuelRod(ItemStack stack) {
        setReceivedFromFuelRod(stack, false);
    }

    /**
     * Sets the "received from fuel rod" flag.
     * @param stack the item stack
     */
    public void markReceivedFromFuelRod(ItemStack stack) {
        setReceivedFromFuelRod(stack, true);
    }

    /**
     * Vents heat. Call this one time per tick.
     * This follows the IC2 mechanism:
     * 1. Pull heat from the reactor hull (reactorVent).
     * 2. Vent its own heat (selfVent).
     * The reactor heat vent pulls heat from the reactor hull first.
     * After cooling, it clears the "received from fuel rod" flag.
     * @param stack the item stack
     * @param reactorHeat the current reactor hull heat
     * @param reactorHeatDissipated the total heat that the reactor vented, passed by reference
     * @return the heat actually pulled from the reactor hull
     */
    @Override
    public int cool(ItemStack stack, long reactorHeat, java.util.concurrent.atomic.AtomicInteger reactorHeatDissipated) {
        int absorbed = super.cool(stack, reactorHeat, reactorHeatDissipated);
        resetReceivedFromFuelRod(stack);
        return absorbed;
    }

}