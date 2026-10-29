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
 * 0.1.10.36: the tritium fuel rod ({@code normal/item_tritium_cell}, bred from a lithium cell) as a neutron source, as a
 * deuterium-tritium neutron generator: tritium does not fission, but its neutrons make each adjacent fuel rod fission
 * once more (one more pulse of heat and EU). It decays by one per reactor cycle ({@link ReactorInventory#TRITIUM_LIFE}),
 * gives 1 HU of decay heat per cycle and then leaves the empty fuel rod. {@code storedValue} is the decay.
 */
@SuppressWarnings("null")
public class mio_icif_tritium_cell extends mio_icif_reactor {

    public mio_icif_tritium_cell(Properties properties) {
        super(properties, ReactorInventory.TRITIUM_LIFE, ReactorComponentType.OTHER, false, true);
    }

    /** Cycles of decay so far. */
    public int getDecay(ItemStack stack) {
        ReactorComponentData data = stack.get(mio_icif_data_components.REACTOR_COMPONENT_DATA.get());
        return data == null ? 0 : data.storedValue();
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.translatable("item.mio_icif.reactor.component.neutron_source").withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.translatable("item.mio_icif.reactor.component.decay",
            getDecay(stack), ReactorInventory.TRITIUM_LIFE).withStyle(ChatFormatting.GRAY));
    }
}
