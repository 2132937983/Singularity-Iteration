// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.suit.nano;

import com.miophas.singularity_iteration.common.suit.SuitModules;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;

/**
 * Nano lattice densifier (0.1.8.5): the Universal Suit Modification Station packs it into a nano
 * suit piece, which gains {@link SuitModules#LATTICE_STEP} lattice points, at most
 * {@link SuitModules#LATTICE_MAX_LEVEL} times per piece.
 */
public final class LatticeDensifierItem extends Item {
    public LatticeDensifierItem(Properties properties) {
        super(properties.stacksTo(16).rarity(Rarity.RARE));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.mio_icif.module.item_nano_lattice_densifier.desc",
            SuitModules.LATTICE_STEP, SuitModules.LATTICE_MAX_LEVEL).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("module.mio_icif.tooltip.station").withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return false;
    }
}
