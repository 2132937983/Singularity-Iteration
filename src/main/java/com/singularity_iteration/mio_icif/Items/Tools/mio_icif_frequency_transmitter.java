package com.singularity_iteration.mio_icif.Items.Tools;

import com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_teleporter_elc;
import com.singularity_iteration.mio_icif.api.item.IFrequencyTransmitterItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.List;

@SuppressWarnings("null")
public class mio_icif_frequency_transmitter extends Item implements IFrequencyTransmitterItem {

    private static final String TAG_TARGET_SET = "targetSet";
    private static final String TAG_TARGET_X = "targetX";
    private static final String TAG_TARGET_Y = "targetY";
    private static final String TAG_TARGET_Z = "targetZ";

    public mio_icif_frequency_transmitter(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.SUCCESS;
        }
        ItemStack stack = player.getItemInHand(context.getHand());

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof mio_icif_teleporter_elc teleporter)) {
            return InteractionResult.PASS;
        }

        CompoundTag tag = getOrCreateTag(stack);
        boolean targetSet = tag.getBoolean(TAG_TARGET_SET);

        if (!targetSet) {
            tag.putBoolean(TAG_TARGET_SET, true);
            tag.putInt(TAG_TARGET_X, pos.getX());
            tag.putInt(TAG_TARGET_Y, pos.getY());
            tag.putInt(TAG_TARGET_Z, pos.getZ());
            saveTag(stack, tag);
            player.sendSystemMessage(Component.translatable("item.mio_icif.normal/item_freq.linked_first",
                pos.getX(), pos.getY(), pos.getZ()));
        } else {
            BlockPos firstPos = new BlockPos(
                tag.getInt(TAG_TARGET_X),
                tag.getInt(TAG_TARGET_Y),
                tag.getInt(TAG_TARGET_Z)
            );

            if (firstPos.equals(pos)) {
                player.sendSystemMessage(Component.translatable("item.mio_icif.normal/item_freq.same_teleporter"));
                return InteractionResult.CONSUME;
            }

            BlockEntity firstBlockEntity = level.getBlockEntity(firstPos);
            if (!(firstBlockEntity instanceof mio_icif_teleporter_elc firstTeleporter)) {
                player.sendSystemMessage(Component.translatable("item.mio_icif.normal/item_freq.first_invalid"));
                tag.putBoolean(TAG_TARGET_SET, false);
                saveTag(stack, tag);
                return InteractionResult.CONSUME;
            }

            firstTeleporter.setTargetPos(pos);
            teleporter.setTargetPos(firstPos);

            firstTeleporter.setChanged();
            teleporter.setChanged();

            player.sendSystemMessage(Component.translatable("item.mio_icif.normal/item_freq.link_established",
                firstPos.getX(), firstPos.getY(), firstPos.getZ(),
                pos.getX(), pos.getY(), pos.getZ()));

            tag.putBoolean(TAG_TARGET_SET, false);
            saveTag(stack, tag);
        }

        return InteractionResult.CONSUME;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide()) {
            return InteractionResultHolder.success(stack);
        }

        CompoundTag tag = getOrCreateTag(stack);
        boolean targetSet = tag.getBoolean(TAG_TARGET_SET);

        if (targetSet) {
            tag.putBoolean(TAG_TARGET_SET, false);
            saveTag(stack, tag);
            player.sendSystemMessage(Component.translatable("item.mio_icif.normal/item_freq.cleared"));
        } else {
            player.sendSystemMessage(Component.translatable("item.mio_icif.normal/item_freq.help"));
        }

        return InteractionResultHolder.success(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);

        CompoundTag tag = getTag(stack);
        if (tag != null && tag.getBoolean(TAG_TARGET_SET)) {
            BlockPos pos = new BlockPos(
                tag.getInt(TAG_TARGET_X),
                tag.getInt(TAG_TARGET_Y),
                tag.getInt(TAG_TARGET_Z)
            );
            tooltipComponents.add(Component.translatable("item.mio_icif.normal/item_freq.tooltip_linked",
                pos.getX(), pos.getY(), pos.getZ()));
        } else {
            tooltipComponents.add(Component.translatable("item.mio_icif.normal/item_freq.tooltip_unlinked"));
        }
    }

    private CompoundTag getOrCreateTag(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            CompoundTag existing = customData.copyTag();
            if (existing != null && !existing.isEmpty()) {
                return existing;
            }
        }
        return new CompoundTag();
    }

    private CompoundTag getTag(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        return customData != null ? customData.copyTag() : null;
    }

    private void saveTag(ItemStack stack, CompoundTag tag) {
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }
}