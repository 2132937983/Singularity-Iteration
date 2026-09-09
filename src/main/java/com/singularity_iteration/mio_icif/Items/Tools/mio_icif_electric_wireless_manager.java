package com.singularity_iteration.mio_icif.Items.Tools;

import com.singularity_iteration.mio_icif.api.energy.IWirelessPowerNode;
import com.singularity_iteration.mio_icif.Items.Normal.mio_icif_bat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
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
public class mio_icif_electric_wireless_manager extends mio_icif_bat implements com.singularity_iteration.mio_icif.api.item.IElectricToolItem {

    private static final int MAX_ENERGY = 50000;
    private static final int TRANSFER_SPEED = 128;
    private static final int ENERGY_COST = 10;
    private static final String TAG_TARGET_X = "WirelessTargetX";
    private static final String TAG_TARGET_Y = "WirelessTargetY";
    private static final String TAG_TARGET_Z = "WirelessTargetZ";

    public mio_icif_electric_wireless_manager() {
        super(new Item.Properties(), MAX_ENERGY, MAX_ENERGY, "item_electric_wireless_manager", TRANSFER_SPEED);
    }

    @Override
    public long getEnergyPerUse() {
        return ENERGY_COST;
    }

    @Override
    public int getToolTier() {
        return 2;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();

        if (player == null || level.isClientSide() || getEnergy(stack) < ENERGY_COST) {
            return InteractionResult.SUCCESS;
        }

        if (player.isShiftKeyDown()) {
            setTargetPosition(stack, pos.getX(), pos.getY(), pos.getZ());
            extractEnergy(stack, ENERGY_COST);

            player.sendSystemMessage(Component.translatable("message.mio_icif.electric_wireless_manager.target_set",
                pos.getX(), pos.getY(), pos.getZ()));
            return InteractionResult.SUCCESS;
        }

        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof IWirelessPowerNode node) {
            CompoundTag tag = getTag(stack);
            if (tag != null && tag.contains(TAG_TARGET_X)) {
                int targetX = tag.getInt(TAG_TARGET_X);
                int targetY = tag.getInt(TAG_TARGET_Y);
                int targetZ = tag.getInt(TAG_TARGET_Z);

                node.setTargetPosition(new BlockPos(targetX, targetY, targetZ));
                extractEnergy(stack, ENERGY_COST);

                player.sendSystemMessage(Component.translatable("message.mio_icif.electric_wireless_manager.node_set",
                    targetX, targetY, targetZ, pos.getX(), pos.getY(), pos.getZ()));
            } else {
                player.sendSystemMessage(Component.translatable("message.mio_icif.electric_wireless_manager.no_target"));
            }
            return InteractionResult.SUCCESS;
        }

        CompoundTag tag = getTag(stack);
        if (tag != null && tag.contains(TAG_TARGET_X)) {
            int targetX = tag.getInt(TAG_TARGET_X);
            int targetY = tag.getInt(TAG_TARGET_Y);
            int targetZ = tag.getInt(TAG_TARGET_Z);

            player.sendSystemMessage(Component.translatable("message.mio_icif.electric_wireless_manager.target_info",
                targetX, targetY, targetZ));
            extractEnergy(stack, ENERGY_COST);
        } else {
            player.sendSystemMessage(Component.translatable("message.mio_icif.electric_wireless_manager.no_target"));
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        tooltipComponents.add(Component.translatable("tooltip.mio_icif.electric_wireless_manager.desc"));

        CompoundTag tag = getTag(stack);
        if (tag != null && tag.contains(TAG_TARGET_X)) {
            tooltipComponents.add(Component.translatable("tooltip.mio_icif.electric_wireless_manager.target",
                tag.getInt(TAG_TARGET_X), tag.getInt(TAG_TARGET_Y), tag.getInt(TAG_TARGET_Z)));
        }
    }

    private static void setTargetPosition(ItemStack stack, int x, int y, int z) {
        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = customData.copyTag();
        tag.putInt(TAG_TARGET_X, x);
        tag.putInt(TAG_TARGET_Y, y);
        tag.putInt(TAG_TARGET_Z, z);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    private static CompoundTag getTag(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null && !customData.isEmpty()) {
            return customData.copyTag();
        }
        return null;
    }
}