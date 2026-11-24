package com.miophas.singularity_iteration.common.item.tools;

import com.miophas.singularity_iteration.core.api.energy.IWirelessPowerNode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
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
public class mio_icif_electric_wireless_manager extends com.miophas.singularity_iteration.core.prefab.item.AbstractElectricTool {

    private static final int MAX_ENERGY = 50000;
    private static final int TRANSFER_SPEED = 128;
    private static final int ENERGY_COST = 10;
    private static final String TAG_TARGET_X = "WirelessTargetX";
    private static final String TAG_TARGET_Y = "WirelessTargetY";
    private static final String TAG_TARGET_Z = "WirelessTargetZ";
    /** 0.1.11.193 (T0, U-03): dimension of the target; a copy without it is from before and counts as the node's own. */
    private static final String TAG_TARGET_DIM = "WirelessTargetDim";

    public mio_icif_electric_wireless_manager() {
        // 0.1.11.194 (T1, I-10): on the shared tool base (pack pull, holder tracking, atomic debit), no longer on the battery base
        super(new Item.Properties(), MAX_ENERGY, 0L, TRANSFER_SPEED, ENERGY_COST, 2);
    }





    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();

        if (player == null || level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!hasEnoughEnergy(stack, ENERGY_COST)) {   // 0.1.11.194 (T1, U-20): a word when the manager is flat
            player.displayClientMessage(Component.translatable("message.mio_icif.tool.no_energy"), true);
            return InteractionResult.SUCCESS;
        }

        if (player.isShiftKeyDown()) {
            // 0.1.11.193 (T0, U-04): a target is a block that holds EU
            if (!acceptsEnergy(level, pos)) {
                player.displayClientMessage(Component.translatable("message.mio_icif.electric_wireless_manager.not_energy"), true);
                return InteractionResult.SUCCESS;
            }
            setTargetPosition(stack, pos.getX(), pos.getY(), pos.getZ());
            setTargetDimension(stack, level.dimension().location().toString());
            consumeEnergy(stack, ENERGY_COST);   // 0.1.11.194 (T1, I-09)

            player.sendSystemMessage(Component.translatable("message.mio_icif.electric_wireless_manager.target_set",
                pos.getX(), pos.getY(), pos.getZ()));
            return InteractionResult.SUCCESS;
        }

        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof IWirelessPowerNode node) {
            // 0.1.11.193 (T0, U-04): re-aiming a node is an edit of it, so a claimed node stays its owner's
            if (!com.miophas.singularity_iteration.common.util.BlockEditPermission.canEdit(player, level, pos, context.getClickedFace(), stack)) {
                player.displayClientMessage(Component.translatable("message.mio_icif.tool.no_permission"), true);
                return InteractionResult.SUCCESS;
            }
            CompoundTag tag = getTag(stack);
            if (tag != null && tag.contains(TAG_TARGET_DIM) && !tag.getString(TAG_TARGET_DIM).equals(level.dimension().location().toString())) {
                // 0.1.11.193 (T0, U-03): the node sends inside its own dimension only
                player.displayClientMessage(Component.translatable("message.mio_icif.tool.other_dimension"), true);
                return InteractionResult.SUCCESS;
            }
            if (tag != null && tag.contains(TAG_TARGET_X)) {
                int targetX = tag.getInt(TAG_TARGET_X);
                int targetY = tag.getInt(TAG_TARGET_Y);
                int targetZ = tag.getInt(TAG_TARGET_Z);
                if (pos.equals(new BlockPos(targetX, targetY, targetZ))) {
                    // 0.1.11.3: a node linked to itself duplicated energy; the node refuses it too
                    player.sendSystemMessage(Component.translatable("message.mio_icif.electric_wireless_manager.self_target"));
                    return InteractionResult.SUCCESS;
                }

                node.setTargetPosition(new BlockPos(targetX, targetY, targetZ));
                consumeEnergy(stack, ENERGY_COST);   // 0.1.11.194 (T1, I-09)

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
            consumeEnergy(stack, ENERGY_COST);   // 0.1.11.194 (T1, I-09)
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

    private static void setTargetDimension(ItemStack stack, String dimension) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putString(TAG_TARGET_DIM, dimension);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    /** A block that stores EU: an SI energy block or anything with the EU capability. */
    private static boolean acceptsEnergy(Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof com.miophas.singularity_iteration.core.prefab.blockentity.AbstractEnergyBlockEntity
            || level.getCapability(com.miophas.singularity_iteration.core.api.energy.storage.EUApi.SIDED, pos, null) != null;
    }

    private static CompoundTag getTag(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null && !customData.isEmpty()) {
            return customData.copyTag();
        }
        return null;
    }

}
