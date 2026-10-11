package com.miophas.singularity_iteration.core.runtime.world;

import com.miophas.singularity_iteration.core.api.block.IRubberWood;
import com.miophas.singularity_iteration.core.api.item.IElectricToolItem;
import com.miophas.singularity_iteration.core.api.item.ITreeTapItem;
import com.miophas.singularity_iteration.core.api.world.RubberHarvestResult;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Core logic of the rubber tree system.
 *
 * <p>It holds the rules for resin harvest and regrowth in one place. Rubber wood blocks and {@code IRubberTreeAPI} both use it.
 * It does not depend on specific registry content (resin item, block and item classes): the caller gives the resin item as an {@link Item}.
 *
 * <p>All methods that change the world run only on the server. A call on the client does nothing.
 */
@SuppressWarnings("null")
public final class RubberTreeSystem {

    /** Probability that rubber wood regrows resin on each random tick. */
    public static final float RESIN_REGROW_CHANCE = 0.05F;

    private RubberTreeSystem() {
    }

    // ==================== Checks ====================

    /**
     * Checks if the item stack is a treetap.
     */
    public static boolean isTreeTap(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.getItem() instanceof ITreeTapItem;
    }

    /**
     * Checks if the item stack is an electric treetap.
     */
    public static boolean isElectricTreeTap(ItemStack stack) {
        return stack != null && !stack.isEmpty()
            && stack.getItem() instanceof ITreeTapItem
            && stack.getItem() instanceof IElectricToolItem;
    }

    /**
     * Gets the rubber wood interface for the block state.
     *
     * @return {@code null} if the block is not rubber wood
     */
    public static IRubberWood rubberWoodOf(BlockState state) {
        return state != null && state.getBlock() instanceof IRubberWood wood ? wood : null;
    }

    /**
     * Checks if the block state is rubber wood.
     */
    public static boolean isRubberWood(BlockState state) {
        return rubberWoodOf(state) != null;
    }

    /**
     * Checks if the rubber wood contains resin now.
     */
    public static boolean hasResin(BlockState state) {
        IRubberWood wood = rubberWoodOf(state);
        return wood != null && wood.hasResin(state);
    }

    /**
     * Checks if the rubber wood meets the harvest conditions.
     */
    public static boolean isTappable(BlockState state) {
        IRubberWood wood = rubberWoodOf(state);
        return wood != null && wood.isTappable(state);
    }

    /**
     * Checks if the rubber wood can be harvested now (it meets the harvest conditions and contains resin).
     */
    public static boolean canHarvest(BlockState state) {
        IRubberWood wood = rubberWoodOf(state);
        return wood != null && wood.isTappable(state) && wood.hasResin(state);
    }

    /**
     * Checks if the given tool can harvest this rubber wood.
     *
     * <p>An electric tool needs sufficient energy.
     */
    public static boolean canUseTapOn(BlockState state, ItemStack tool) {
        if (!canHarvest(state) || !isTreeTap(tool)) {
            return false;
        }
        if (tool.getItem() instanceof IElectricToolItem electricTool) {
            return electricTool.hasEnoughEnergy(tool);
        }
        return true;
    }

    // ==================== Quantity ====================

    /**
     * Rolls the quantity of resin for this harvest from the treetap parameters.
     */
    public static int rollResinCount(ITreeTapItem tap, RandomSource random) {
        if (tap == null || random == null) {
            return 0;
        }
        int min = Math.max(0, tap.getResinDropMin());
        int max = Math.max(min, tap.getResinDropMax());
        return max == min ? min : min + random.nextInt(max - min + 1);
    }

    // ==================== Harvest ====================

    /**
     * Does one resin harvest.
     *
     * <p>It uses treetap durability or electric energy, drops resin, and sets the block to the state without resin.
     *
     * @param resinItem the resin item
     * @return the harvest result
     */
    public static RubberHarvestResult harvest(Level level, BlockPos pos, BlockState state,
                                              Player player, InteractionHand hand,
                                              ItemStack tool, Item resinItem) {
        if (level == null || level.isClientSide || player == null || pos == null || resinItem == null) {
            return RubberHarvestResult.NONE;
        }
        IRubberWood wood = rubberWoodOf(state);
        if (wood == null || !wood.isTappable(state) || !wood.hasResin(state)) {
            return RubberHarvestResult.NONE;
        }
        if (tool == null || !(tool.getItem() instanceof ITreeTapItem tap)) {
            return RubberHarvestResult.NONE;
        }

        boolean electric = false;
        if (tool.getItem() instanceof IElectricToolItem electricTool) {
            if (!electricTool.hasEnoughEnergy(tool)) {
                return RubberHarvestResult.NONE;
            }
            if (!player.isCreative()) {
                electricTool.extractEnergy(tool, com.miophas.singularity_iteration.core.api.item.EnergySaving.apply(tool, electricTool.getEnergyPerUse()));
            }
            electric = true;
        } else {
            int cost = tap.getDurabilityCost();
            if (cost > 0) {
                EquipmentSlot slot = hand == InteractionHand.OFF_HAND ? EquipmentSlot.OFFHAND : EquipmentSlot.MAINHAND;
                tool.hurtAndBreak(cost, player, slot);
            }
        }

        int count = rollResinCount(tap, level.getRandom());
        dropResin(level, pos, player, resinItem, count);
        level.setBlock(pos, wood.withResin(state, false), 3);

        if (electric && hand != null) {
            // Synchronize the energy change to the item in the player's hand
            player.setItemInHand(hand, tool);
        }
        return RubberHarvestResult.success(count);
    }

    /**
     * Drops the given quantity of resin between the block and the player.
     */
    public static void dropResin(Level level, BlockPos pos, Player player, Item resinItem, int count) {
        if (level == null || pos == null || player == null || resinItem == null || count <= 0) {
            return;
        }
        ItemStack resinStack = new ItemStack(resinItem, count);

        double midX = (pos.getX() + 0.5 + player.getX()) / 2.0;
        double midZ = (pos.getZ() + 0.5 + player.getZ()) / 2.0;
        double midY = (pos.getY() + 0.5 + player.getY()) / 2.0 + 1.0;

        ItemEntity itemEntity = new ItemEntity(level, midX, midY, midZ, resinStack);
        itemEntity.setDefaultPickUpDelay();
        level.addFreshEntity(itemEntity);
    }

    // ==================== Regrowth ====================

    /**
     * Checks if the rubber wood can regrow resin naturally now.
     */
    public static boolean canRegrowResin(BlockState state) {
        IRubberWood wood = rubberWoodOf(state);
        return wood != null && wood.canRegrowResin(state);
    }

    /**
     * Tries to make the rubber wood regrow resin (random tick logic).
     *
     * @return true if resin regrew this time
     */
    public static boolean tryRegrowResin(Level level, BlockPos pos, BlockState state, RandomSource random) {
        if (level == null || level.isClientSide || pos == null || random == null) {
            return false;
        }
        IRubberWood wood = rubberWoodOf(state);
        if (wood == null || !wood.canRegrowResin(state)) {
            return false;
        }
        if (random.nextFloat() >= RESIN_REGROW_CHANCE) {
            return false;
        }
        level.setBlock(pos, wood.withResin(state, true), 3);
        return true;
    }
}
