// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.item.normal;

import com.miophas.singularity_iteration.common.exchange.ExchangeService;
import com.miophas.singularity_iteration.common.exchange.IdCardData;
import com.miophas.singularity_iteration.common.exchange.IdCardRules;
import com.miophas.singularity_iteration.common.exchange.IdCards;
import com.miophas.singularity_iteration.common.registry.mio_icif_data_components;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * 0.1.10.28 (X4): the ID card of the exchange ({@link IdCards}). Blank: using it makes it the user's personal card. Personal
 * card: using it opens the holder's account from anywhere (orders, auction orders, mailbox, issued cards). Delegate card
 * (written at a Trade Terminal): carried by its holder, it gives the card's level at the issuer's terminals; using it shows
 * whether it is valid. A card works only for its holder.
 */
public class IdCardItem extends Item {
    /** Client hook (installed by the client setup): opens the account screen of a personal card. */
    @Nullable public static volatile Runnable OPEN_ACCOUNT;

    public IdCardItem(Properties properties) {
        super(properties.stacksTo(16));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        IdCardData c = stack.get(mio_icif_data_components.ID_CARD.get());
        if (c == null) {
            if (!level.isClientSide && player instanceof ServerPlayer sp) {
                ItemStack card = IdCards.writePersonal(sp);
                stack.shrink(1);
                if (stack.isEmpty()) player.setItemInHand(hand, card);
                else if (!player.getInventory().add(card)) player.drop(card, false);
                sp.displayClientMessage(Component.translatable("message.mio_icif.card.personal"), true);
            }
            return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
        }
        if (!c.holder().equals(player.getUUID())) {
            if (!level.isClientSide) player.displayClientMessage(Component.translatable("message.mio_icif.card.not_yours", c.holderName()), true);
            return InteractionResultHolder.fail(stack);
        }
        if (c.personal()) {
            if (level.isClientSide) {
                Runnable open = OPEN_ACCOUNT;
                if (open != null) open.run();
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        if (!level.isClientSide && player instanceof ServerPlayer sp) {
            IdCardRules.Check check = IdCards.check(sp.server, c, sp.getUUID());
            sp.displayClientMessage(Component.translatable("message.mio_icif.card.status." + check.name().toLowerCase(java.util.Locale.ROOT),
                c.issuerName(), Component.translatable("gui.mio_icif.terminal.level." + c.level()), c.expiry() - ExchangeService.today(sp.server) + 1), false);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        IdCardData c = stack.get(mio_icif_data_components.ID_CARD.get());
        if (c == null) {
            tooltip.add(Component.translatable("item.mio_icif.normal.item_id_card.blank").withStyle(ChatFormatting.GRAY));
            return;
        }
        tooltip.add(Component.translatable("item.mio_icif.normal.item_id_card.holder", c.holderName()).withStyle(ChatFormatting.AQUA));
        if (c.personal()) {
            tooltip.add(Component.translatable("item.mio_icif.normal.item_id_card.personal").withStyle(ChatFormatting.GRAY));
        } else {
            tooltip.add(Component.translatable("item.mio_icif.normal.item_id_card.delegate", c.issuerName(),
                Component.translatable("gui.mio_icif.terminal.level." + Math.max(0, Math.min(4, c.level())))).withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.translatable("item.mio_icif.normal.item_id_card.expiry", c.expiry() + 1).withStyle(ChatFormatting.DARK_GRAY));
        }
        tooltip.add(Component.translatable("item.mio_icif.normal.item_id_card.serial", c.serial()).withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        IdCardData c = stack.get(mio_icif_data_components.ID_CARD.get());
        return c != null && c.personal();
    }
}
