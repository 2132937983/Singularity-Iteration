// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.suit.core;

import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;

/** A frame core item (0.1.9.0). The Universal Suit Modification Station puts it into a core socket. */
public class FrameCoreItem extends Item {
    private final FrameCore core;

    public FrameCoreItem(FrameCore core, Properties properties) {
        super(properties.stacksTo(1).rarity(core == FrameCore.PROPULSION || core == FrameCore.SOLAR_ARRAY_3 ? Rarity.EPIC : Rarity.RARE));
        this.core = core;
    }

    public FrameCore core() { return core; }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(core.descriptionKey()).withStyle(ChatFormatting.GRAY));
        String socket = core == FrameCore.SOLAR_ARRAY_3 || !core.isSolar() ? "quantum_" + core.slot().getName() : core.slot().getName();
        tooltip.add(Component.translatable("core.mio_icif.tooltip.socket", Component.translatable("core.mio_icif.socket." + socket))
            .withStyle(ChatFormatting.DARK_AQUA));
        if (core.isSolar()) {
            tooltip.add(Component.translatable("core.mio_icif.tooltip.solar", core.solarDay(), core.solarNight(),
                String.format(Locale.ROOT, "%,d", core.transferLimit())).withStyle(ChatFormatting.YELLOW));
        }
        if (core == FrameCore.PROPULSION) {
            tooltip.add(Component.translatable("core.mio_icif.tooltip.burst").withStyle(ChatFormatting.GOLD));
        }
        tooltip.add(Component.translatable("core.mio_icif.tooltip.station").withStyle(ChatFormatting.DARK_GRAY));
    }
}
