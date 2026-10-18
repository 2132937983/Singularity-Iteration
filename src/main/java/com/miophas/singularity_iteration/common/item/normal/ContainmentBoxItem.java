// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.item.normal;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;

/**
 * Radiation Containment Box (0.1.10.5; before, a plain item without a recipe): a lead box with
 * {@link ContainmentBoxMenu#SLOTS} slots. Use it to open it. Radioactive items inside do not
 * irradiate the carrier (the radiation check reads the inventory, not the box). The contents are
 * the vanilla {@code minecraft:container} component, so they stay with the item; when the box
 * burns or falls into the void as an item, its contents drop.
 */
public class ContainmentBoxItem extends Item {
    public ContainmentBoxItem(Properties properties) {
        super(properties.stacksTo(1).component(DataComponents.CONTAINER, ItemContainerContents.EMPTY));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack box = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer server) {
            server.openMenu(new SimpleMenuProvider((id, inventory, p) -> new ContainmentBoxMenu(id, inventory, box, hand), box.getHoverName()));
        }
        return InteractionResultHolder.sidedSuccess(box, level.isClientSide);
    }

    @Override
    public boolean canFitInsideContainerItems() {
        return false;
    }

    @Override
    public void onDestroyed(ItemEntity entity) {
        ItemContainerContents contents = entity.getItem().get(DataComponents.CONTAINER);
        if (contents != null) {
            entity.getItem().set(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
            ItemUtils.onContainerDestroyed(entity, contents.nonEmptyItemsCopy());
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        ItemContainerContents contents = stack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
        long used = contents.nonEmptyStream().count();
        tooltip.add(Component.translatable("tooltip.mio_icif.containment_box.slots", used, ContainmentBoxMenu.SLOTS).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.mio_icif.containment_box.info").withStyle(ChatFormatting.DARK_GRAY));
    }
}
