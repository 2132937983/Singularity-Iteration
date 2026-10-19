// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.item.reactor;

import com.miophas.singularity_iteration.common.reactor.audit.ReactorIncidents;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * 0.1.10.12: the reactor blackbox. A failed reactor drops it at the site; it carries the id of the
 * incident. Using it opens the audit report of that incident (whoever holds it may read it).
 */
public class ReactorBlackboxItem extends Item {
    public ReactorBlackboxItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer server) {
            var incident = ReactorIncidents.get(server.getServer(), ReactorIncidents.idOf(stack));
            if (incident == null) server.displayClientMessage(Component.translatable("message.mio_icif.reactor_audit.missing")
                .withStyle(ChatFormatting.RED), true);
            else ReactorIncidents.open(server, incident);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return ReactorIncidents.idOf(stack) > 0;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        var data = stack.get(DataComponents.CUSTOM_DATA);
        int id = ReactorIncidents.idOf(stack);
        if (id <= 0) {
            tooltip.add(Component.translatable("tooltip.mio_icif.reactor_blackbox.empty").withStyle(ChatFormatting.GRAY));
            return;
        }
        var tag = data.copyTag();
        tooltip.add(Component.translatable("tooltip.mio_icif.reactor_blackbox.incident", id).withStyle(ChatFormatting.GOLD));
        if (tag.contains(ReactorIncidents.ITEM_CAUSE))
            tooltip.add(Component.translatable("reactor_audit.cause." + tag.getString(ReactorIncidents.ITEM_CAUSE)).withStyle(ChatFormatting.RED));
        if (tag.contains(ReactorIncidents.ITEM_TIME))
            tooltip.add(Component.literal(new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date(tag.getLong(ReactorIncidents.ITEM_TIME))))
                .withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("tooltip.mio_icif.reactor_blackbox.use").withStyle(ChatFormatting.GRAY));
    }
}
