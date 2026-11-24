// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.suit.quantum;

import com.miophas.singularity_iteration.common.suit.SuitModules;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;

/**
 * Living Nano Lining: installed onto any Quantum armor piece in the Universal Suit Modification Station
 * to awaken its internal nano undersuit layer via living circuitry. Grants an independent
 * {@link SuitModules#LIVING_LINING_LP} LP internal nano lattice (does not consume outer quantum slots),
 * unified coherence-paid nano bursts, and bio-neural coherence/regeneration synergy.
 */
public final class LivingNanoLiningItem extends Item {
    public LivingNanoLiningItem(Properties properties) {
        super(properties.stacksTo(16).rarity(Rarity.EPIC));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.mio_icif.module.item_living_nano_lining.desc", SuitModules.LIVING_LINING_LP)
            .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.mio_icif.module.item_living_nano_lining.bonus")
            .withStyle(ChatFormatting.GREEN));
        tooltip.add(Component.translatable("module.mio_icif.tooltip.station").withStyle(ChatFormatting.DARK_GRAY));
    }
}
