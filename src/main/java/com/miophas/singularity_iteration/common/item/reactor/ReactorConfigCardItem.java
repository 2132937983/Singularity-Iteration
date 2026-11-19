// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.item.reactor;

import com.miophas.singularity_iteration.common.item.component.ReactorLayout;
import com.miophas.singularity_iteration.common.registry.mio_icif_data_components;
import java.util.List;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * 0.1.11.76: the reactor config card. In the card slot of a reactor screen it records the layout of the reactor (import)
 * and projects its layout onto a reactor as a guide (export); see {@code ReactorLayoutGuide}. The layout is the
 * {@link mio_icif_data_components#REACTOR_LAYOUT} component; a blank card has none. Sneak + use clears a card.
 */
public class ReactorConfigCardItem extends Item {
    /** Kinds of part listed in the tooltip before "and n more". */
    private static final int TOOLTIP_KINDS = 6;

    public ReactorConfigCardItem(Properties properties) {
        super(properties);
    }

    /** The layout on a card, or null when the stack is blank or not a card. */
    public static ReactorLayout layout(ItemStack stack) {
        if (!(stack.getItem() instanceof ReactorConfigCardItem)) return null;
        ReactorLayout layout = stack.get(mio_icif_data_components.REACTOR_LAYOUT.get());
        return layout == null || layout.isEmpty() ? null : layout;
    }

    public static boolean isCard(ItemStack stack) {
        return stack.getItem() instanceof ReactorConfigCardItem;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!player.isShiftKeyDown() || layout(stack) == null) return InteractionResultHolder.pass(stack);
        if (!level.isClientSide) {
            stack.remove(mio_icif_data_components.REACTOR_LAYOUT.get());
            level.playSound(null, player.blockPosition(), SoundEvents.UI_CARTOGRAPHY_TABLE_TAKE_RESULT, SoundSource.PLAYERS, 0.6F, 1.4F);
            player.displayClientMessage(Component.translatable("message.mio_icif.reactor_config_card.cleared"), true);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        ReactorLayout layout = layout(stack);
        if (layout == null) {
            tooltip.add(Component.translatable("tooltip.mio_icif.reactor_config_card.blank").withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.translatable("tooltip.mio_icif.reactor_config_card.how").withStyle(ChatFormatting.DARK_GRAY));
            return;
        }
        tooltip.add(Component.translatable("tooltip.mio_icif.reactor_config_card.parts", layout.size(), layout.columnsNeeded())
            .withStyle(ChatFormatting.GOLD));
        int shown = 0;
        Map<Item, Integer> counts = layout.counts();
        for (var e : counts.entrySet()) {
            if (shown++ >= TOOLTIP_KINDS) break;
            Component name = e.getKey() == Items.AIR ? Component.translatable("tooltip.mio_icif.reactor_config_card.unknown")
                : e.getKey().getDescription();
            tooltip.add(Component.literal(" " + e.getValue() + " × ").append(name).withStyle(ChatFormatting.GRAY));
        }
        if (counts.size() > TOOLTIP_KINDS)
            tooltip.add(Component.translatable("tooltip.mio_icif.reactor_config_card.more", counts.size() - TOOLTIP_KINDS).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.mio_icif.reactor_config_card.clear").withStyle(ChatFormatting.DARK_GRAY));
    }
}
