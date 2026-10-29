// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.item.reactor;

import com.miophas.singularity_iteration.core.runtime.reactor.FluidReactorCycle;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * 0.1.10.36: a steam vent (IC2 Classic). In a steam reactor it turns its heat into steam, the more the hotter it is
 * (ReactorCycle STEAM_VENT); in any other reactor it is an ordinary heat vent.
 */
public class mio_icif_steam_vent extends mio_icif_heat_vent {
    private final int selfCooling;

    public mio_icif_steam_vent(Properties properties, int capacity, int selfCooling, int hullCooling) {
        super(properties, capacity, selfCooling, hullCooling);
        this.selfCooling = selfCooling;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        super.appendHoverText(stack, context, lines, flag);
        lines.add(Component.translatable("item.mio_icif.reactor.component.steam_vent",
            selfCooling * FluidReactorCycle.HU_OUTPUT_MODIFIER).withStyle(ChatFormatting.AQUA));
    }
}
