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
 * Neutron reflector reactor component (IC2 style).
 *
 * Behavior:
 * - Place it next to fuel rods to reflect neutrons back into them.
 * - Reflected neutrons make the fuel rods produce more energy and more heat.
 * - Each reflected pulse uses one unit of durability.
 * - The component breaks when its pulse count reaches the maximum.
 *
 * How it works:
 * - Each fuel rod emits neutron pulses to its adjacent components.
 * - The reflector sends these pulses back to the fuel rod.
 * - The fuel rod counts each reflected pulse as an extra pulse for energy and heat.
 * - The durability decreases by the number of reflected pulses.
 *
 * Reflector types:
 * - Neutron reflector: 30000 pulses.
 * - Thick neutron reflector: 120000 pulses.
 */
@SuppressWarnings("null")
public class mio_icif_neutron_reflector extends mio_icif_reactor {

    // Maximum pulse count. This value is equal to the maximum durability.
    private final int maxPulses;

    /**
     * Creates a neutron reflector.
     * @param properties the item properties
     * @param maxPulses the maximum pulse count, equal to the maximum durability
     */
    public mio_icif_neutron_reflector(Properties properties, int maxPulses) {
        super(properties, maxPulses, ReactorComponentType.NEUTRON_REFLECTOR, false, true);
        this.maxPulses = maxPulses;
    }

    /**
     * Gets the maximum pulse count.
     * @return the maximum pulse count
     */
    public int getMaxPulses() {
        return maxPulses;
    }

    /**
     * Gets the remaining pulse count.
     * @param stack the item stack
     * @return the remaining pulse count
     */
    public int getRemainingPulses(ItemStack stack) {
        ReactorComponentData data = stack.get(mio_icif_data_components.REACTOR_COMPONENT_DATA.get());
        if (data == null) return maxPulses;
        return data.getRemaining();
    }

    /**
     * Consumes pulses, which reduces the durability.
     * The stack is destroyed when the stored value reaches the maximum.
     * @param stack the item stack
     * @param amount the number of pulses to consume
     * @return true if the reflector is depleted
     */
    public boolean consumePulses(ItemStack stack, int amount) {
        ReactorComponentData data = stack.get(mio_icif_data_components.REACTOR_COMPONENT_DATA.get());
        if (data == null) return true;
        int newStored = data.storedValue() + amount;
        if (newStored >= data.maxValue()) {
            stack.set(mio_icif_data_components.REACTOR_COMPONENT_DATA.get(), data.withStoredValue(data.maxValue()));
            stack.setCount(0);
            return true;
        } else {
            stack.set(mio_icif_data_components.REACTOR_COMPONENT_DATA.get(), data.withStoredValue(newStored));
            return false;
        }
    }

    /**
     * Checks whether the reflector is depleted.
     * @param stack the item stack
     * @return true if the reflector is depleted
     */
    public boolean isDepleted(ItemStack stack) {
        ReactorComponentData data = stack.get(mio_icif_data_components.REACTOR_COMPONENT_DATA.get());
        return data == null || data.isDepleted();
    }

    /**
     * Always shows the durability bar of the neutron reflector.
     */
    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    /**
     * Gets the durability bar color. Green shows normal state.
     */
    @Override
    public int getBarColor(ItemStack stack) {
        return 0x00FF00; // 绿色
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, java.util.List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        tooltipComponents.add(Component.translatable("item.mio_icif.reactor.component.remaining_pulses",
            getRemainingPulses(stack), maxPulses).withStyle(ChatFormatting.GRAY));
    }
}

