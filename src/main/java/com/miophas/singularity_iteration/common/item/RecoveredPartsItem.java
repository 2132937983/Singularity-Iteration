package com.miophas.singularity_iteration.common.item;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;

/**
 * Recovered Parts Case (0.1.9.1): the items that were inside a machine used as a crafting
 * material (upgrade plugins, batteries, materials). It unpacks into the player's inventory as
 * soon as it is in it, or with a right click; what does not fit drops at the player. Automation
 * (a Crafter, an AE2 molecular assembler) returns the case as one item.
 */
public class RecoveredPartsItem extends Item {
    public RecoveredPartsItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    /** A case that holds {@code contents} (at most 256 stacks, the container component limit). */
    public static ItemStack pack(List<ItemStack> contents) {
        ItemStack box = new ItemStack(com.miophas.singularity_iteration.common.registry.mio_icif_items.RECOVERED_PARTS.get());
        box.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(contents.size() > 256 ? contents.subList(0, 256) : contents));
        return box;
    }

    public static List<ItemStack> contents(ItemStack box) {
        ItemContainerContents data = box.get(DataComponents.CONTAINER);
        if (data == null) return List.of();
        List<ItemStack> out = new ArrayList<>();
        data.nonEmptyItemsCopy().forEach(out::add);
        return out;
    }

    /** Moves the contents into the player's inventory (the rest drops) and uses up the case. */
    public static void unpack(Player player, ItemStack box) {
        List<ItemStack> contents = contents(box);
        box.shrink(1);
        for (ItemStack stack : contents) {
            if (!player.getInventory().add(stack)) player.drop(stack, false);
        }
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BUNDLE_DROP_CONTENTS, SoundSource.PLAYERS,
            0.8F, 0.9F + player.level().getRandom().nextFloat() * 0.2F);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack box = player.getItemInHand(hand);
        if (!level.isClientSide) unpack(player, box);
        return InteractionResultHolder.sidedSuccess(box, level.isClientSide);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        // in the player's own inventory: unpack at once (the case is only a carrier)
        if (!level.isClientSide && entity instanceof ServerPlayer player) unpack(player, stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        List<ItemStack> contents = contents(stack);
        tooltip.add(Component.translatable("tooltip.mio_icif.recovered_parts", contents.size()).withStyle(ChatFormatting.AQUA));
        listContents(contents, tooltip, 8);
        tooltip.add(Component.translatable("tooltip.mio_icif.recovered_parts.use").withStyle(ChatFormatting.DARK_GRAY));
    }

    /** Adds "- 2 x Overclocker Upgrade" lines, at most {@code max}, then "... and N more". */
    public static void listContents(List<ItemStack> contents, List<Component> tooltip, int max) {
        for (int i = 0; i < contents.size() && i < max; i++) {
            ItemStack s = contents.get(i);
            tooltip.add(Component.literal("  " + s.getCount() + " x ").append(s.getHoverName()).withStyle(ChatFormatting.GRAY));
        }
        if (contents.size() > max) {
            tooltip.add(Component.translatable("tooltip.mio_icif.recovered_parts.more", contents.size() - max).withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
