// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.item.normal;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * 0.1.10.42: the Trade Box upgrade kit: used by the owner on a placed Basic Trade Box it makes an Advanced Trade Box with the
 * contents and settings ({@code mio_icif_block_trade_box#useItemOn}).
 */
public class TradeBoxUpgradeItem extends Item {
    public TradeBoxUpgradeItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.mio_icif.normal.item_trade_box_upgrade.desc").withStyle(ChatFormatting.GRAY));
    }
}
