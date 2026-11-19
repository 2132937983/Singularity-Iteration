package com.miophas.singularity_iteration.common.item.reactor;

import com.miophas.singularity_iteration.common.item.component.ReactorComponentData;
import com.miophas.singularity_iteration.common.reactor.ReactorInventory;
import com.miophas.singularity_iteration.common.registry.mio_icif_data_components;
import com.miophas.singularity_iteration.core.api.reactor.ReactorComponentType;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import java.util.List;

/**
 * 0.1.11.94: the deuterium fuel rod ({@code normal/item_deuterium_cell}; the deuterium cell of 0.1.11.92 is now canned from
 * unstable hot water into an empty fuel rod). In a fission reactor it is a neutron source like the tritium rod, as a D-D
 * neutron generator: each adjacent fuel rod fissions once more per cycle while it lasts. It is used up by one per cycle
 * ({@link ReactorInventory#DEUTERIUM_LIFE}, half the tritium rod's life: D-D fusion gives far fewer neutrons), gives no decay
 * heat (deuterium is stable) and then leaves the empty fuel rod. The fusion reactor burns it as fuel. {@code storedValue} is
 * the burn-up.
 */
@SuppressWarnings("null")
public class mio_icif_deuterium_rod extends mio_icif_reactor {

    public mio_icif_deuterium_rod(Properties properties) {
        super(properties, ReactorInventory.DEUTERIUM_LIFE, ReactorComponentType.OTHER, false, true);
    }

    /** Cycles used so far. */
    public int getBurnup(ItemStack stack) {
        ReactorComponentData data = stack.get(mio_icif_data_components.REACTOR_COMPONENT_DATA.get());
        return data == null ? 0 : data.storedValue();
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.translatable("item.mio_icif.reactor.component.neutron_source").withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.translatable("item.mio_icif.reactor.component.burnup",
            getBurnup(stack), ReactorInventory.DEUTERIUM_LIFE).withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.translatable("item.mio_icif.reactor.component.fusion_fuel").withStyle(ChatFormatting.DARK_AQUA));
    }
}
