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
 * Quantum bay expander (0.1.10.0): the Universal Suit Modification Station builds it into a quantum
 * suit piece, which gains {@link SuitModules#BAY_STEP} unit slot, at most {@link SuitModules#BAY_MAX_LEVEL}
 * time per piece (helmet 5, chestplate 4, leggings 3, boots 3). The expansion is permanent.
 */
public final class QuantumBayItem extends Item {
    public QuantumBayItem(Properties properties) {
        super(properties.stacksTo(16).rarity(Rarity.EPIC));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.mio_icif.module.item_quantum_bay_expander.desc", SuitModules.BAY_STEP, SuitModules.BAY_MAX_LEVEL)
            .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.mio_icif.module.item_quantum_bay_expander.slots").withStyle(ChatFormatting.DARK_AQUA));
        tooltip.add(Component.translatable("module.mio_icif.tooltip.station").withStyle(ChatFormatting.DARK_GRAY));
    }
}
